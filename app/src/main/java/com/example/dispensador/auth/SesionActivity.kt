package com.example.dispensador.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Clase base para TODAS las pantallas que requieren sesión
 * (Control, Dispensador, Horarios, Historial, elegir modo...).
 *
 * Uso: en vez de   class ControlActivity : AppCompatActivity()
 *      escribir    class ControlActivity : SesionActivity()
 *
 * Si no hay usuario (nunca inició sesión, cerró sesión o el token expiró)
 * la pantalla se cierra y se abre el login. Así nadie entra sin sesión.
 */
abstract class SesionActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    private val oyente = FirebaseAuth.AuthStateListener { a ->
        if (a.currentUser == null && !isFinishing) Sesion.irAlLogin(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) {
            Sesion.irAlLogin(this)
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(oyente)
    }

    override fun onStop() {
        auth.removeAuthStateListener(oyente)
        super.onStop()
    }

    /** Llamar desde el botón o menú "Cerrar sesión". */
    protected fun cerrarSesion() = Sesion.cerrarSesion(this)
}
