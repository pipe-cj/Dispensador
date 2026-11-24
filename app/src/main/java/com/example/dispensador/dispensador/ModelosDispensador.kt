package com.example.dispensador.dispensador

/** Porciones permitidas. "clave" es el texto que se guarda en Firebase. */
enum class Porcion(val clave: String, val descuento: Int) {
    CHICA("chica", 5),
    MEDIANA("mediana", 10),
    GRANDE("grande", 20);

    companion object {
        /** Devuelve null si el valor no es una porción válida (validación de seguridad). */
        fun desdeClave(clave: String?): Porcion? =
            values().firstOrNull { it.clave == clave?.trim()?.lowercase() }
    }
}

enum class Origen(val clave: String) {
    MANUAL("manual"),
    HORARIO("horario")
}

/** Una ración pendiente de dispensar. */
data class Pedido(val porcion: Porcion, val origen: Origen, val uid: String)

/** Horario programado ya validado (hora normalizada a HH:mm). */
data class Horario(val hora: String, val porcion: Porcion, val activo: Boolean)
