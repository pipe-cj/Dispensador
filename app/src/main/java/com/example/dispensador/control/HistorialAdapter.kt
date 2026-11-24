package com.example.dispensador.control

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dispensador.databinding.ItemEventoBinding
import java.text.SimpleDateFormat
import java.util.*

class HistorialAdapter : ListAdapter<Evento, HistorialAdapter.VH>(object : DiffUtil.ItemCallback<Evento>() {
    override fun areItemsTheSame(a: Evento, b: Evento) = a.id == b.id
    override fun areContentsTheSame(a: Evento, b: Evento) = a == b
}) {
    private val fmt = SimpleDateFormat("EEE dd/MM · HH:mm", Locale("es", "CL"))
    inner class VH(val b: ItemEventoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemEventoBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = getItem(pos)
        h.b.txtFecha.text = fmt.format(Date(e.fecha))
        h.b.txtDetalle.text = "Porción ${Porcion.desde(e.porcion).etiqueta.lowercase()} · " +
            if (e.origen == "horario") "por horario" else "pedida desde la app"
    }
}
