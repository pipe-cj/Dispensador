package com.example.dispensador.auth

/**
 * Validaciones de los datos que ingresa el usuario (ISO/IEC 27400: validación de entradas).
 * No usa clases de Android, así se puede probar con un test unitario simple.
 * Cada función devuelve el mensaje de error, o null si el dato está bien.
 */
object Validaciones {

    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 64
    val PORCIONES_PERMITIDAS = setOf("chica", "mediana", "grande")

    private val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun errorNombre(nombre: String): String? {
        val n = nombre.trim()
        return when {
            n.isEmpty() -> "Ingresa tu nombre"
            n.length < 2 -> "El nombre es muy corto"
            n.length > 40 -> "Máximo 40 caracteres"
            else -> null
        }
    }

    fun errorCorreo(correo: String): String? {
        val c = correo.trim()
        return when {
            c.isEmpty() -> "Ingresa tu correo"
            !REGEX_CORREO.matches(c) -> "Correo no válido"
            else -> null
        }
    }

    /** Reglas para crear una contraseña nueva (registro). */
    fun errorPasswordNueva(password: String): String? = when {
        password.isEmpty() -> "Ingresa una contraseña"
        password.length < MIN_PASSWORD -> "Mínimo $MIN_PASSWORD caracteres"
        password.length > MAX_PASSWORD -> "Máximo $MAX_PASSWORD caracteres"
        password.none { it.isLetter() } || password.none { it.isDigit() } ->
            "Debe tener letras y números"
        else -> null
    }

    /** En el login solo se pide que no esté vacía: Firebase decide si es correcta. */
    fun errorPasswordLogin(password: String): String? =
        if (password.isEmpty()) "Ingresa tu contraseña" else null

    fun errorConfirmacion(password: String, confirmacion: String): String? = when {
        confirmacion.isEmpty() -> "Repite la contraseña"
        password != confirmacion -> "Las contraseñas no coinciden"
        else -> null
    }

    fun porcionValida(porcion: String): Boolean = porcion in PORCIONES_PERMITIDAS
}
