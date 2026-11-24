package com.example.dispensador.control

/** Valores permitidos de porcion (validacion ISO/IEC 27400: solo estos tres). */
enum class Porcion(val clave: String, val etiqueta: String, val porcentaje: Int) {
    CHICA("chica", "Chica", 5),
    MEDIANA("mediana", "Mediana", 10),
    GRANDE("grande", "Grande", 20);

    companion object {
        fun desde(clave: String?) = values().firstOrNull { it.clave == clave } ?: MEDIANA
    }
}

data class Estado(
    val nivel: Int = 0,
    val enLinea: Boolean = false,
    val ultimaRacion: Long = 0L
)

data class Horario(
    val id: String = "",
    val hora: String = "08:00",      // formato HH:mm
    val porcion: String = "mediana",
    val activo: Boolean = true
)

data class Evento(
    val id: String = "",
    val fecha: Long = 0L,
    val porcion: String = "mediana",
    val origen: String = "manual",   // "manual" o "horario"
    val uid: String = ""
)
