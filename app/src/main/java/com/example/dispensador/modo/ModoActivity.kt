package com.example.dispensador.modo

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.dispensador.auth.Sesion
import com.example.dispensador.auth.SesionActivity
import com.example.dispensador.control.ControlHostActivity
import com.example.dispensador.databinding.ActivityModoBinding
import com.example.dispensador.dispensador.DispensadorActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Pantalla despues del login: el usuario escribe el codigo del dispensador
 * y elige si este telefono sera el Control (dueño) o el Dispensador.
 * Guarda usuarios/{uid}: nombre, correo, modo, dispensadorId (formato que exigen las reglas).
 */
class ModoActivity : SesionActivity() {

    private lateinit var b: ActivityModoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usuario = FirebaseAuth.getInstance().currentUser ?: return

        b = ActivityModoBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.txtHola.text = "Hola, ${usuario.displayName ?: usuario.email}"

        // Si ya habia elegido antes, se precarga el codigo.
        FirebaseDatabase.getInstance().reference.child("usuarios").child(usuario.uid)
            .child("dispensadorId").get().addOnSuccessListener { s ->
                s.getValue(String::class.java)?.let { b.etCodigo.setText(it) }
            }

        b.btnControl.setOnClickListener { elegir("control") }
        b.btnDispensador.setOnClickListener { elegir("dispensador") }
        b.btnSalir.setOnClickListener { cerrarSesion() }
    }

    private fun elegir(modo: String) {
        val codigo = b.etCodigo.text.toString().trim().uppercase()
        if (!CODIGO.matches(codigo)) {
            b.tilCodigo.error = "Formato: DISP- seguido de números (ej. DISP-01)"
            return
        }
        b.tilCodigo.error = null
        val u = FirebaseAuth.getInstance().currentUser ?: return Sesion.irAlLogin(this)

        // nombre y correo se reenvian por si el registro no alcanzo a guardar el perfil.
        val nombre = (u.displayName ?: u.email?.substringBefore("@") ?: "Usuario").take(40).padEnd(2, '_')
        val datos = mapOf(
            "nombre" to nombre,
            "correo" to (u.email ?: ""),
            "modo" to modo,
            "dispensadorId" to codigo
        )
        cargando(true)
        FirebaseDatabase.getInstance().reference.child("usuarios").child(u.uid)
            .updateChildren(datos)
            .addOnCompleteListener(this) { t ->
                cargando(false)
                if (!t.isSuccessful) {
                    Snackbar.make(b.root, "No se pudo guardar. Revisa tu conexión.", Snackbar.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }
                val destino = if (modo == "control") ControlHostActivity::class.java
                              else DispensadorActivity::class.java
                startActivity(Intent(this, destino).putExtra(ControlHostActivity.EXTRA_DISP_ID, codigo))
                finish()
            }
    }

    private fun cargando(on: Boolean) {
        b.progreso.visibility = if (on) View.VISIBLE else View.GONE
        b.btnControl.isEnabled = !on
        b.btnDispensador.isEnabled = !on
    }

    companion object { private val CODIGO = Regex("^DISP-[0-9]+$") }
}
