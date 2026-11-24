package com.example.dispensador.dispensador

import android.os.Handler
import android.os.Looper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lógica del "dispensador IoT simulado".
 *
 * Firebase:
 *  dispensadores/{id}/estado    -> nivel (0..100), enLinea (bool), ultimaRacion (ms)
 *  dispensadores/{id}/comando   -> tipo, porcion, uid, timestamp   (lo escribe el modo Control)
 *  dispensadores/{id}/horarios  -> {hId}: hora (HH:mm), porcion, activo
 *  dispensadores/{id}/historial -> {eId}: fecha (ms), porcion, origen, uid
 *
 * Todos los callbacks del Listener llegan en el hilo principal.
 */
class DispensadorController(
    dispensadorId: String,
    private val uidLocal: String,
    private val listener: Listener
) {

    interface Listener {
        fun onNivel(nivel: Int)
        fun onDispensando(porcion: Porcion?)          // null = terminó de dispensar
        fun onUltimaRacion(porcion: Porcion, fechaMs: Long)
        fun onConexion(conectado: Boolean)
        fun onMensaje(texto: String)
    }

    companion object {
        private const val DURACION_DISPENSADO_MS = 2000L
        private const val REVISION_HORARIOS_MS = 10_000L
    }

    private val db = FirebaseDatabase.getInstance()
    private val raiz: DatabaseReference = db.getReference("dispensadores").child(dispensadorId)
    private val estadoRef = raiz.child("estado")
    private val comandoRef = raiz.child("comando")
    private val horariosRef = raiz.child("horarios")
    private val historialRef = raiz.child("historial")
    private val conectadoRef = db.getReference(".info/connected")

    private val handler = Handler(Looper.getMainLooper())

    private var nivel = 100
    private var listo = false            // true cuando ya se leyó el nivel guardado
    private var dispensando = false
    private val cola = ArrayDeque<Pedido>()

    private var primerComando = true
    private var ultimoComando: Long? = null

    private val horarios = mutableMapOf<String, Horario>()
    private val ultimaEjecucion = mutableMapOf<String, String>() // hId -> minuto ya ejecutado

    private var lConectado: ValueEventListener? = null
    private var lComando: ValueEventListener? = null
    private var lHorarios: ValueEventListener? = null

    // ---------------------------------------------------------------- ciclo de vida

    fun iniciar() {
        registrarConexion()

        // Recupera el nivel guardado (si el teléfono se reinicia no vuelve a 100 %).
        estadoRef.child("nivel").get()
            .addOnSuccessListener { snap ->
                nivel = ((snap.value as? Number)?.toInt() ?: 100).coerceIn(0, 100)
                listo = true
                estadoRef.child("nivel").setValue(nivel)
                listener.onNivel(nivel)
                escucharComandos()
                escucharHorarios()
                handler.post(relojHorarios)
                procesarCola()
            }
            .addOnFailureListener {
                listener.onMensaje("No se pudo leer el estado: ${it.message}")
            }
    }

    fun detener() {
        handler.removeCallbacksAndMessages(null)
        lConectado?.let { conectadoRef.removeEventListener(it) }
        lComando?.let { comandoRef.removeEventListener(it) }
        lHorarios?.let { horariosRef.removeEventListener(it) }
        lConectado = null; lComando = null; lHorarios = null
        estadoRef.child("enLinea").setValue(false)
    }

    // ---------------------------------------------------------------- enLinea

    /**
     * Marca enLinea=true al conectar y deja registrado en el servidor que, si el teléfono
     * se desconecta (se cierra la app, se cae el WiFi), Firebase pone enLinea=false solo.
     * Así el botón "Dispensar" del modo Control se desactiva.
     */
    private fun registrarConexion() {
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val conectado = s.getValue(Boolean::class.java) == true
                listener.onConexion(conectado)
                if (conectado) {
                    val ref = estadoRef.child("enLinea")
                    ref.onDisconnect().setValue(false).addOnCompleteListener {
                        ref.setValue(true)   // primero se registra onDisconnect, después true
                    }
                }
            }

            override fun onCancelled(e: DatabaseError) {
                listener.onMensaje("Error de conexión: ${e.message}")
            }
        }
        lConectado = l
        conectadoRef.addValueEventListener(l)
    }

    // ---------------------------------------------------------------- comandos manuales

    private fun escucharComandos() {
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val ts = (s.child("timestamp").value as? Number)?.toLong()

                // El primer valor que llega es el comando viejo que ya estaba guardado:
                // se registra pero no se ejecuta (si no, dispensaría al abrir la app).
                if (primerComando) {
                    primerComando = false
                    ultimoComando = ts
                    return
                }
                if (ts == null || ts == ultimoComando) return
                ultimoComando = ts

                val tipo = s.child("tipo").getValue(String::class.java)
                val porcion = Porcion.desdeClave(s.child("porcion").getValue(String::class.java))
                val uid = s.child("uid").getValue(String::class.java) ?: ""

                if (!"dispensar".equals(tipo?.trim(), ignoreCase = true) || porcion == null) {
                    listener.onMensaje("Comando inválido ignorado")
                    return
                }
                encolar(Pedido(porcion, Origen.MANUAL, uid))
            }

            override fun onCancelled(e: DatabaseError) {
                listener.onMensaje("Error escuchando comandos: ${e.message}")
            }
        }
        lComando = l
        comandoRef.addValueEventListener(l)
    }

    // ---------------------------------------------------------------- horarios

    private fun escucharHorarios() {
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                horarios.clear()
                for (h in s.children) {
                    val id = h.key ?: continue
                    val hora = normalizarHora(h.child("hora").getValue(String::class.java)) ?: continue
                    val porcion = Porcion.desdeClave(h.child("porcion").getValue(String::class.java)) ?: continue
                    val activo = h.child("activo").getValue(Boolean::class.java) ?: false
                    horarios[id] = Horario(hora, porcion, activo)
                }
            }

            override fun onCancelled(e: DatabaseError) {
                listener.onMensaje("Error leyendo horarios: ${e.message}")
            }
        }
        lHorarios = l
        horariosRef.addValueEventListener(l)
    }

    private val relojHorarios = object : Runnable {
        override fun run() {
            revisarHorarios()
            handler.postDelayed(this, REVISION_HORARIOS_MS)
        }
    }

    /** Cada 10 s mira la hora; un horario activo se ejecuta una sola vez por minuto. */
    private fun revisarHorarios() {
        val ahora = Date()
        val hhmm = SimpleDateFormat("HH:mm", Locale.US).format(ahora)
        val minuto = SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(ahora)
        for ((id, h) in horarios) {
            if (!h.activo || h.hora != hhmm) continue
            if (ultimaEjecucion[id] == minuto) continue
            ultimaEjecucion[id] = minuto
            encolar(Pedido(h.porcion, Origen.HORARIO, uidLocal))
        }
    }

    private fun normalizarHora(h: String?): String? {
        val p = h?.trim()?.split(":") ?: return null
        if (p.size != 2) return null
        val hh = p[0].toIntOrNull() ?: return null
        val mm = p[1].toIntOrNull() ?: return null
        if (hh !in 0..23 || mm !in 0..59) return null
        return String.format(Locale.US, "%02d:%02d", hh, mm)
    }

    // ---------------------------------------------------------------- dispensar

    private fun encolar(p: Pedido) {
        cola.addLast(p)
        procesarCola()
    }

    private fun procesarCola() {
        if (!listo || dispensando || cola.isEmpty()) return
        val p = cola.removeFirst()

        if (nivel <= 0) {
            listener.onMensaje("Sin comida: se rechazó la ración ${p.porcion.clave}")
            procesarCola()
            return
        }

        dispensando = true
        listener.onDispensando(p.porcion)
        handler.postDelayed({ completar(p) }, DURACION_DISPENSADO_MS)
    }

    private fun completar(p: Pedido) {
        val ahora = System.currentTimeMillis()
        nivel = (nivel - p.porcion.descuento).coerceAtLeast(0)

        // Publica el estado
        estadoRef.updateChildren(mapOf("nivel" to nivel, "ultimaRacion" to ahora))

        // Guarda la ración en el historial
        historialRef.push().setValue(
            mapOf(
                "fecha" to ahora,
                "porcion" to p.porcion.clave,
                "origen" to p.origen.clave,
                "uid" to p.uid
            )
        )

        listener.onNivel(nivel)
        listener.onUltimaRacion(p.porcion, ahora)
        listener.onDispensando(null)
        dispensando = false
        procesarCola()
    }

    /** Botón Recargar: el nivel vuelve a 100 %. */
    fun recargar() {
        if (!listo) return
        nivel = 100
        estadoRef.child("nivel").setValue(nivel)
        listener.onNivel(nivel)
        listener.onMensaje("Depósito recargado al 100 %")
    }
}
