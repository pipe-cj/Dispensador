package com.example.dispensador.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Traduce los errores de Firebase Auth a mensajes en español.
 * Seguridad: en el login no se dice si el correo existe o no
 * ("correo o contraseña incorrectos"), para no revelar qué cuentas hay.
 */
object MensajesAuth {

    fun deLogin(e: Exception?): String = when (e) {
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera unos minutos"
        is FirebaseNetworkException -> "Sin conexión a internet"
        else -> "No se pudo iniciar sesión. Intenta de nuevo"
    }

    fun deRegistro(e: Exception?): String = when (e) {
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil"
        is FirebaseAuthInvalidCredentialsException -> "Correo no válido"
        is FirebaseNetworkException -> "Sin conexión a internet"
        else -> "No se pudo crear la cuenta. Intenta de nuevo"
    }
}
