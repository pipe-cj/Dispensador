package com.example.dispensador.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador.databinding.ActivityRegistroBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegistrar.setOnClickListener { intentarRegistro() }
        binding.btnVolverLogin.setOnClickListener { finish() }
    }

    private fun intentarRegistro() {
        val nombre = binding.etNombre.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmacion = binding.etConfirmar.text.toString()

        binding.tilNombre.error = Validaciones.errorNombre(nombre)
        binding.tilCorreo.error = Validaciones.errorCorreo(correo)
        binding.tilPassword.error = Validaciones.errorPasswordNueva(password)
        binding.tilConfirmar.error = Validaciones.errorConfirmacion(password, confirmacion)

        val hayErrores = listOf(
            binding.tilNombre, binding.tilCorreo, binding.tilPassword, binding.tilConfirmar
        ).any { it.error != null }
        if (hayErrores) return

        cargando(true)
        // La contraseña la recibe Firebase por HTTPS y la guarda con hash.
        // La app nunca la guarda en el teléfono.
        auth.createUserWithEmailAndPassword(correo, password)
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user ?: return@addOnSuccessListener
                usuario.updateProfile(userProfileChangeRequest { displayName = nombre })
                guardarPerfil(usuario.uid, nombre, correo)
            }
            .addOnFailureListener { e ->
                cargando(false)
                mostrar(MensajesAuth.deRegistro(e))
            }
    }

    /** Crea usuarios/{uid} con nombre y correo. El modo y el dispensador se eligen después. */
    private fun guardarPerfil(uid: String, nombre: String, correo: String) {
        val perfil = mapOf("nombre" to nombre, "correo" to correo)
        FirebaseDatabase.getInstance().reference
            .child("usuarios").child(uid)
            .updateChildren(perfil)
            .addOnCompleteListener(this) {
                cargando(false)
                // Aunque falle guardar el perfil, la cuenta ya existe: se puede completar después.
                Sesion.irAlInicio(this)
            }
    }

    private fun cargando(activo: Boolean) {
        binding.progreso.visibility = if (activo) View.VISIBLE else View.GONE
        binding.btnRegistrar.isEnabled = !activo
    }

    private fun mostrar(mensaje: String) =
        Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
}
