package com.example.dispensador.dispensador

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Conexion Bluetooth Classic (SPP) con el ESP32, igual que en el laboratorio del HC-05.
 * El ESP32 entiende un caracter: C (chica), M (mediana), G (grande), P (ping).
 * Los permisos de Bluetooth los pide DispensadorActivity antes de llamar a conectar().
 */
@SuppressLint("MissingPermission")
class EspBluetooth(
    private val ctx: Context,
    private val onMensaje: (String) -> Unit,
    private val onConexion: (Boolean) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var socket: BluetoothSocket? = null

    val conectado: Boolean get() = socket?.isConnected == true

    fun conectar(nombre: String) {
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return avisar("Este teléfono no tiene Bluetooth")
        if (!adapter.isEnabled) return avisar("Enciende el Bluetooth del teléfono")
        val esp = adapter.bondedDevices.firstOrNull { it.name == nombre }
            ?: return avisar("Primero vincula \"$nombre\" en Ajustes > Bluetooth")

        avisar("Conectando con $nombre...")
        thread(name = "bt-conectar") {
            try {
                adapter.cancelDiscovery()
                val s = esp.createRfcommSocketToServiceRecord(SPP)
                s.connect()
                socket = s
                main.post { onConexion(true); onMensaje("Conectado a $nombre") }
                escuchar(s)
            } catch (e: IOException) {
                cerrar()
                main.post { onConexion(false); onMensaje("No se pudo conectar. ¿Está encendido el ESP32?") }
            }
        }
    }

    /** Lee las respuestas del ESP32 (OK:M, PONG, ERR:...) y las muestra en el registro. */
    private fun escuchar(s: BluetoothSocket) {
        try {
            val lector = s.inputStream.bufferedReader()
            while (true) {
                val linea = lector.readLine() ?: break
                main.post { onMensaje("ESP32: ${linea.trim()}") }
            }
        } catch (_: IOException) { }
        if (socket === s) {
            cerrar()
            main.post { onConexion(false); onMensaje("Se desconectó el ESP32") }
        }
    }

    /** Envia un caracter al ESP32. Devuelve false si no hay conexion. */
    fun enviar(c: Char): Boolean {
        val s = socket ?: return false
        return try {
            s.outputStream.write(c.code)
            s.outputStream.flush()
            true
        } catch (e: IOException) {
            cerrar()
            main.post { onConexion(false) }
            false
        }
    }

    fun cerrar() {
        val s = socket
        socket = null
        try { s?.close() } catch (_: IOException) { }
    }

    private fun avisar(m: String) { main.post { onMensaje(m) } }

    companion object {
        /** UUID estandar del perfil Serial Port (SPP). */
        val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
