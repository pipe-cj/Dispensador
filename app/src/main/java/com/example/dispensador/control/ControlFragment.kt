package com.example.dispensador.control

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.dispensador.R
import com.example.dispensador.databinding.FragmentControlBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class ControlFragment : Fragment() {

    private var _b: FragmentControlBinding? = null
    private val b get() = _b!!
    private var detener: (() -> Unit)? = null
    private var avisado = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentControlBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val repo = (requireActivity() as ControlHostActivity).repo

        detener = repo.escucharEstado { e -> if (_b != null) pintar(e) }

        b.btnDispensar.setOnClickListener {
            val p = when (b.grupoPorcion.checkedChipId) {
                R.id.chipChica -> Porcion.CHICA
                R.id.chipGrande -> Porcion.GRANDE
                else -> Porcion.MEDIANA
            }
            b.btnDispensar.isEnabled = false
            repo.enviarComando(p) { ok ->
                if (_b == null) return@enviarComando
                b.btnDispensar.isEnabled = true
                Snackbar.make(b.root,
                    if (ok) "Orden enviada: porción ${p.etiqueta.lowercase()}"
                    else "No se pudo enviar. Revisa tu conexión e intenta de nuevo.",
                    Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun pintar(e: Estado) {
        b.progresoNivel.setProgressCompat(e.nivel, true)
        b.txtNivel.text = "${e.nivel}%"
        b.chipEstado.text = if (e.enLinea) "En línea" else "Sin conexión"
        b.chipEstado.isChecked = e.enLinea
        b.txtUltima.text = if (e.ultimaRacion == 0L) "Aún no hay raciones"
        else "Última ración: " + SimpleDateFormat("dd/MM HH:mm", Locale("es", "CL")).format(Date(e.ultimaRacion))

        // Aviso de nivel bajo (<20 %). Banner dentro de la app: no requiere permiso de notificaciones.
        val bajo = e.nivel < 20
        b.bannerBajo.visibility = if (bajo) View.VISIBLE else View.GONE
        if (bajo && !avisado) {
            avisado = true
            Snackbar.make(b.root, "Queda poca comida (${e.nivel}%). Recarga el dispensador.",
                Snackbar.LENGTH_LONG).show()
        }
        if (!bajo) avisado = false
        // Sin conexion o sin comida: no tiene sentido dispensar.
        b.btnDispensar.isEnabled = e.enLinea && e.nivel > 0
    }

    override fun onDestroyView() { detener?.invoke(); _b = null; super.onDestroyView() }
}
