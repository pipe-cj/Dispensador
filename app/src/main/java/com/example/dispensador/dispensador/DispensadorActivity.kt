package com.example.dispensador.dispensador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.dispensador.R
import com.example.dispensador.auth.Sesion
import com.example.dispensador.auth.SesionActivity
import com.example.dispensador.control.ControlHostActivity
import com.example.dispensador.databinding.ActivityDispensadorBinding
import com.example.dispensador.modo.ModoActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modo Dispensador: este telefono representa al aparato IoT.
 * - La logica (comandos, horarios, cola, nivel, historial, en linea) esta en DispensadorController (Luis).
 * - Esta pantalla muestra el estado y reenvia cada racion al ESP32 por Bluetooth (C, M o G).
 * Si el ESP32 no esta conectado, la racion se simula y la app sigue funcionando.
 */
class DispensadorActivity : SesionActivity(), DispensadorController.Listener {

    private lateinit var b: ActivityDispensadorBinding
    private lateinit var dispId: String
    private lateinit var esp: EspBluetooth
    private var controller: DispensadorController? = null
    private val registro = ArrayDeque<String>()

    private val pedirPermisoBt = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) esp.conectar(dispId) else log("Sin permiso de Bluetooth: las raciones se simularán")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = Sesion.uid() ?: return   // SesionActivity ya mando al login

        dispId = intent.getStringExtra(ControlHostActivity.EXTRA_DISP_ID) ?: "DISP-01"
        esp = EspBluetooth(this, ::log, ::pintarBluetooth)

        b = ActivityDispensadorBinding.inflate(layoutInflater)
        setContentView(b.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // es el "aparato": no debe dormirse
        b.txtCodigo.text = dispId
        pintarBluetooth(false)

        b.btnConectar.setOnClickListener { conectarEsp() }
        b.btnRecargar.setOnClickListener { controller?.recargar() }
        b.btnCambiarModo.setOnClickListener {
            startActivity(Intent(this, ModoActivity::class.java)); finish()
        }
        b.btnSalir.setOnClickListener { cerrarSesion() }

        controller = DispensadorController(dispId, uid, this).also { it.iniciar() }
        log("Dispensador $dispId iniciado, esperando órdenes")
        conectarEsp()
    }

    override fun onDestroy() {
        controller?.detener()
        if (::esp.isInitialized) esp.cerrar()
        super.onDestroy()
    }

    // ---------- DispensadorController.Listener (logica de Luis) ----------

    override fun onNivel(nivel: Int) {
        b.progresoNivel.setProgressCompat(nivel, true)
        b.txtNivel.text = "$nivel %"
        b.txtBajo.visibility = if (nivel < 20) View.VISIBLE else View.GONE
    }

    override fun onDispensando(porcion: Porcion?) {
        if (porcion == null) {
            b.txtAccion.text = "Esperando órdenes"
            b.txtAccion.setTextColor(ContextCompat.getColor(this, R.color.ink))
            return
        }
        b.txtAccion.text = "Dispensando porción ${porcion.clave}..."
        b.txtAccion.setTextColor(ContextCompat.getColor(this, R.color.teal))

        val letra = when (porcion) { Porcion.CHICA -> 'C'; Porcion.MEDIANA -> 'M'; Porcion.GRANDE -> 'G' }
        if (esp.enviar(letra)) log("Orden $letra enviada al ESP32")
        else log("ESP32 no conectado: ración ${porcion.clave} simulada")
    }

    override fun onUltimaRacion(porcion: Porcion, fechaMs: Long) {
        val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(fechaMs))
        log("Ración ${porcion.clave} registrada en el historial ($hora)")
    }

    override fun onConexion(conectado: Boolean) {
        b.txtFirebase.text = if (conectado) "● Firebase en línea" else "○ Sin conexión a internet"
        b.txtFirebase.setTextColor(ContextCompat.getColor(this, if (conectado) R.color.teal else R.color.alerta))
    }

    override fun onMensaje(texto: String) = log(texto)

    // ---------- Bluetooth con el ESP32 ----------

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
}
