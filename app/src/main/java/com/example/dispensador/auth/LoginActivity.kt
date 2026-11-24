package com.example.dispensador.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay sesión guardada por Firebase, entra directo.
        if (auth.currentUser != null) {
            Sesion.irAlInicio(this)
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIngresar.setOnClickListener { intentarLogin() }
        binding.btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
        binding.btnOlvide.setOnClickListener { recuperarPassword() }
    }

    private fun intentarLogin() {
        val correo = binding.etCorreo.text.toString().trim()
        val password = binding.etPassword.text.toString()

        binding.tilCorreo.error = Validaciones.errorCorreo(correo)
        binding.tilPassword.error = Validaciones.errorPasswordLogin(password)
        if (binding.tilCorreo.error != null || binding.tilPassword.error != null) return

        cargando(true)
        auth.signInWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { tarea ->
                cargando(false)
                if (tarea.isSuccessful) {
                    Sesion.irAlInicio(this)
                } else {
                    binding.etPassword.text = null // no dejar la contraseña escrita
                    mostrar(MensajesAuth.deLogin(tarea.exception))
                }
            }
    }

    private fun recuperarPassword() {
        val correo = binding.etCorreo.text.toString().trim()
        val error = Validaciones.errorCorreo(correo)
        if (error != null) {
            binding.tilCorreo.error = "Escribe tu correo para recuperar la contraseña"
            return
        }
        binding.tilCorreo.error = null
        auth.sendPasswordResetEmail(correo).addOnCompleteListener(this) {
            // Mismo mensaje exista o no la cuenta (no revelar qué correos están registrados).
            mostrar("Si el correo está registrado, te llegará un enlace para cambiar la contraseña")
        }
    }

    private fun cargando(activo: Boolean) {
        binding.progreso.visibility = if (activo) View.VISIBLE else View.GONE
        binding.btnIngresar.isEnabled = !activo
        binding.btnIrRegistro.isEnabled = !activo
    }

    private fun mostrar(mensaje: String) =
        Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
}
