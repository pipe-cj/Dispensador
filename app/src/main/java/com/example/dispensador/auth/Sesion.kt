package com.example.dispensador.auth

import android.app.Activity
import android.content.Intent
import com.example.dispensador.modo.ModoActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Punto único para saber si hay sesión, ir al inicio y cerrar sesión.
 * PIPE: cambia PANTALLA_INICIO por la pantalla que va después del login
 * (por ejemplo la de elegir modo / vincular dispensador).
 */
object Sesion {

    val PANTALLA_INICIO: Class<out Activity> = ModoActivity::class.java

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun hayUsuario(): Boolean = auth.currentUser != null

    fun uid(): String? = auth.currentUser?.uid

    /** Va a la pantalla de inicio y borra el login del historial (botón Atrás no vuelve). */
    fun irAlInicio(desde: Activity) {
        desde.startActivity(
            Intent(desde, PANTALLA_INICIO)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        desde.finish()
    }

    /** Va al login y borra todas las pantallas anteriores. */
    fun irAlLogin(desde: Activity) {
        desde.startActivity(
            Intent(desde, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        desde.finish()
    }

    /** Cierra la sesión en Firebase y vuelve al login. */
    fun cerrarSesion(desde: Activity) {
        auth.signOut()
        irAlLogin(desde)
    }
}
