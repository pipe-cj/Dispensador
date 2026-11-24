package com.example.dispensador.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionesTest {

    @Test fun correoValido() = assertNull(Validaciones.errorCorreo("tomas@duocuc.cl"))
    @Test fun correoSinArroba() = assertNotNull(Validaciones.errorCorreo("tomas.duocuc.cl"))
    @Test fun correoVacio() = assertEquals("Ingresa tu correo", Validaciones.errorCorreo("  "))

    @Test fun passwordCorta() = assertNotNull(Validaciones.errorPasswordNueva("abc123"))
    @Test fun passwordSinNumeros() = assertNotNull(Validaciones.errorPasswordNueva("abcdefgh"))
    @Test fun passwordSinLetras() = assertNotNull(Validaciones.errorPasswordNueva("12345678"))
    @Test fun passwordBuena() = assertNull(Validaciones.errorPasswordNueva("perro2024"))

    @Test fun confirmacionDistinta() = assertNotNull(Validaciones.errorConfirmacion("perro2024", "perro2025"))
    @Test fun confirmacionIgual() = assertNull(Validaciones.errorConfirmacion("perro2024", "perro2024"))

    @Test fun nombreCorto() = assertNotNull(Validaciones.errorNombre("T"))
    @Test fun nombreBueno() = assertNull(Validaciones.errorNombre("Tomás"))

    @Test fun porciones() {
        assertTrue(Validaciones.porcionValida("mediana"))
        assertFalse(Validaciones.porcionValida("gigante"))
    }
}
