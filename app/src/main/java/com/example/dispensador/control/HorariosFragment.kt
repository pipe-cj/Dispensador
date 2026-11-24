package com.example.dispensador.control

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dispensador.R
import com.example.dispensador.databinding.FragmentHorariosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class HorariosFragment : Fragment() {

    private var _b: FragmentHorariosBinding? = null
    private val b get() = _b!!
    private var detener: (() -> Unit)? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHorariosBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val repo = (requireActivity() as ControlHostActivity).repo
        val adapter = HorariosAdapter(
            onActivar = { h, on -> repo.activarHorario(h.id, on) },
            onBorrar = { h ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Borrar horario")
                    .setMessage("¿Borrar el horario de las ${h.hora}?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Borrar") { _, _ -> repo.borrarHorario(h.id) }
                    .show()
            })
        b.lista.layoutManager = LinearLayoutManager(requireContext())
        b.lista.adapter = adapter

        detener = repo.escucharHorarios { l ->
            if (_b == null) return@escucharHorarios
            adapter.submit(l)
            b.vacio.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
        }

        b.fabAgregar.setOnClickListener {
            val tp = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H).setHour(8).setMinute(0)
                .setTitleText("Hora de la comida").build()
            tp.addOnPositiveButtonClickListener {
                val hora = "%02d:%02d".format(tp.hour, tp.minute)
                val opciones = Porcion.values()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Porción")
                    .setItems(opciones.map { it.etiqueta }.toTypedArray()) { _, i ->
                        repo.agregarHorario(hora, opciones[i])
                    }.show()
            }
            tp.show(parentFragmentManager, "hora")
        }
    }

    override fun onDestroyView() { detener?.invoke(); _b = null; super.onDestroyView() }
}
