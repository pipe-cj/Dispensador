package com.example.dispensador.dispensador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.dispensador.R
import com.example.dispensador.auth.Sesion
import com.example.dispensador.auth.SesionActivity
import com.example.dispensador.control.ControlHostActivity
import com.example.dispensador.control.Horario
import com.example.dispensador.control.Porcion
import com.example.dispensador.databinding.ActivityDispensadorBinding
import com.example.dispensador.modo.ModoActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modo Dispensador: este telefono representa al aparato IoT.
 * - Escucha dispensadores/{id}/comando en Firebase (lo envia el telefono Control).
 * - Reenvia la orden al ESP32 por Bluetooth (C, M o G) y el servo abre la compuerta.
 * - Ejecuta los horarios programados.
 * - Publica nivel, enLinea y ultimaRacion, y agrega cada racion al historial.
 * Si el ESP32 no esta conectado, la racion se simula (la app sigue funcionando para la demo).
 */
class DispensadorActivity : SesionActivity() {

    private lateinit var b: ActivityDispensadorBinding
    private lateinit var dispId: String
    private lateinit var raiz: DatabaseReference
    private lateinit var esp: EspBluetooth

    private val main = Handler(Looper.getMainLooper())
    private val oyentes = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

    private var nivel = 100
    private var horarios = emptyList<Horario>()
    private val horariosEjecutados = mutableSetOf<String>()
    private var ultimoComandoTs: Long? = null   // null = aun no se lee el primer comando
    private var ocupado = false
    private val registro = ArrayDeque<String>()

    private val pedirPermisoBt = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) esp.conectar(dispId) else log("Sin permiso de Bluetooth: las raciones se simularán")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Sesion.hayUsuario()) return

        dispId = intent.getStringExtra(ControlHostActivity.EXTRA_DISP_ID) ?: "DISP-01"
        raiz = FirebaseDatabase.getInstance().getReference("dispensadores").child(dispId)
        esp = EspBluetooth(this, ::log, ::pintarBluetooth)

        b = ActivityDispensadorBinding.inflate(layoutInflater)
        setContentView(b.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // es el "aparato": no debe dormirse
        b.txtCodigo.text = dispId
        pintarBluetooth(false)

        b.btnConectar.setOnClickListener { conectarEsp() }
        b.btnRecargar.setOnClickListener { recargar() }
        b.btnCambiarModo.setOnClickListener {
            startActivity(Intent(this, ModoActivity::class.java)); finish()
        }
        b.btnSalir.setOnClickListener { cerrarSesion() }

        iniciarEstado()
        escuchar(raiz.child("estado")) { s ->
            nivel = s.child("nivel").getValue(Int::class.java) ?: nivel
            pintarNivel()
        }
        escuchar(raiz.child("comando")) { s -> alLlegarComando(s) }
        escuchar(raiz.child("horarios")) { s ->
            horarios = s.children.mapNotNull { c -> c.getValue(Horario::class.java)?.copy(id = c.key.orEmpty()) }
        }
        main.post(revisarHorarios)
        conectarEsp()
    }

    /** Marca el dispensador en linea y, si es nuevo, lo deja con 100 % de comida. */
    private fun iniciarEstado() {
        val estado = raiz.child("estado")
        estado.child("enLinea").onDisconnect().setValue(false) // si se corta internet, Firebase lo marca solo
        estado.child("enLinea").setValue(true)
        estado.child("nivel").get().addOnSuccessListener { s ->
            if (!s.exists()) estado.child("nivel").setValue(100)
        }
        log("Dispensador $dispId en línea, esperando órdenes")
    }

    // ---------- Comandos desde el telefono Control ----------

    private fun alLlegarComando(s: DataSnapshot) {
        val ts = s.child("timestamp").getValue(Long::class.java) ?: 0L
        val previo = ultimoComandoTs
        ultimoComandoTs = maxOf(ts, previo ?: 0L)
        if (previo == null) return          // el primer valor es una orden vieja: no se ejecuta
        if (ts <= previo) return

        val tipo = s.child("tipo").getValue(String::class.java)
        val porcion = s.child("porcion").getValue(String::class.java).orEmpty()
        val uid = s.child("uid").getValue(String::class.java).orEmpty()
        when (tipo) {
            "dispensar" -> dispensar(porcion, origen = "app", uid = uid)
            "recargar" -> recargar()
            else -> log("Orden ignorada: tipo no permitido")
        }
    }

    // ---------- Horarios ----------

    private val revisarHorarios = object : Runnable {
        override fun run() {
            val ahora = Date()
            val hora = SimpleDateFormat("HH:mm", Locale.US).format(ahora)
            val dia = SimpleDateFormat("yyyyMMdd", Locale.US).format(ahora)
            horarios.filter { it.activo && it.hora == hora }.forEach { h ->
                val clave = "${h.id}-$dia-$hora"
                if (horariosEjecutados.add(clave)) {
                    log("Horario de las $hora")
                    dispensar(h.porcion, origen = "horario", uid = Sesion.uid().orEmpty())
                }
            }
            main.postDelayed(this, 10_000)
        }
    }

    // ---------- Accion principal ----------

    private fun dispensar(clave: String, origen: String, uid: String) {
        if (clave !in setOf("chica", "mediana", "grande")) { log("Porción no permitida: $clave"); return }
        if (ocupado) { log("Ocupado, se ignora la orden"); return }
        if (nivel <= 0) { log("Sin comida: recarga el dispensador"); return }

        val p = Porcion.desde(clave)
        val letra = when (p) { Porcion.CHICA -> 'C'; Porcion.MEDIANA -> 'M'; Porcion.GRANDE -> 'G' }
        ocupado = true
        b.txtAccion.text = "Dispensando porción ${p.etiqueta.lowercase()}..."
        b.txtAccion.setTextColor(ContextCompat.getColor(this, R.color.teal))

        if (esp.enviar(letra)) log("Orden $letra enviada al ESP32 ($origen)")
        else log("ESP32 no conectado: ración ${p.etiqueta.lowercase()} simulada ($origen)")

        val nuevoNivel = (nivel - p.porcentaje).coerceAtLeast(0)
        raiz.child("estado").updateChildren(mapOf(
            "nivel" to nuevoNivel,
            "ultimaRacion" to ServerValue.TIMESTAMP
        ))
        raiz.child("historial").push().setValue(mapOf(
            "fecha" to ServerValue.TIMESTAMP,
            "porcion" to p.clave,
            "origen" to origen,
            "uid" to uid
        )).addOnFailureListener { log("No se pudo guardar en el historial") }

        main.postDelayed({
            ocupado = false
            b.txtAccion.text = "Esperando órdenes"
            b.txtAccion.setTextColor(ContextCompat.getColor(this, R.color.ink))
        }, 1_500)
    }

    private fun recargar() {
        raiz.child("estado").child("nivel").setValue(100)
            .addOnSuccessListener { log("Dispensador recargado: 100 %") }
    }

    // ---------- Bluetooth ----------

    private fun conectarEsp() {
        if (esp.conectado) { log("Ya está conectado"); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermisoBt.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        esp.conectar(dispId)
    }

    // ---------- Pantalla ----------

    private fun pintarNivel() {
        b.progresoNivel.setProgressCompat(nivel, true)
        b.txtNivel.text = "$nivel %"
        b.txtBajo.visibility = if (nivel < 20) View.VISIBLE else View.GONE
    }

    private fun pintarBluetooth(conectado: Boolean) {
        b.txtBluetooth.text = if (conectado) "ESP32 conectado" else "ESP32 sin conectar (modo simulado)"
        b.txtBluetooth.setTextColor(ContextCompat.getColor(this, if (conectado) R.color.teal else R.color.alerta))
        b.btnConectar.visibility = if (conectado) View.GONE else View.VISIBLE
    }

    private fun log(m: String) {
        val hora = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        registro.addFirst("$hora  $m")
        while (registro.size > 10) registro.removeLast()
        if (::b.isInitialized) b.txtLog.text = registro.joinToString("\n")
    }

    private fun escuchar(ref: DatabaseReference, alCambiar: (DataSnapshot) -> Unit) {
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) = alCambiar(s)
            override fun onCancelled(e: DatabaseError) { log("Firebase: ${e.message}") }
        }
        ref.addValueEventListener(l)
        oyentes.add(ref to l)
    }

    override fun onDestroy() {
        if (::raiz.isInitialized) {
            oyentes.forEach { (r, l) -> r.removeEventListener(l) }
            raiz.child("estado").child("enLinea").setValue(false)
            main.removeCallbacksAndMessages(null)
            esp.cerrar()
        }
        super.onDestroy()
    }
}
