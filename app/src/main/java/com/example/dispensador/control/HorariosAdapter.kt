package com.example.dispensador.control

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dispensador.databinding.ItemHorarioBinding

class HorariosAdapter(
    private val onActivar: (Horario, Boolean) -> Unit,
    private val onBorrar: (Horario) -> Unit
) : ListAdapter<Horario, HorariosAdapter.VH>(object : DiffUtil.ItemCallback<Horario>() {
    override fun areItemsTheSame(a: Horario, b: Horario) = a.id == b.id
    override fun areContentsTheSame(a: Horario, b: Horario) = a == b
}) {
    inner class VH(val b: ItemHorarioBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(l: List<Horario>) = submitList(l)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemHorarioBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val x = getItem(pos)
        h.b.txtHora.text = x.hora
        h.b.txtPorcion.text = "Porción ${Porcion.desde(x.porcion).etiqueta.lowercase()}"
        h.b.switchActivo.setOnCheckedChangeListener(null)
        h.b.switchActivo.isChecked = x.activo
        h.b.switchActivo.setOnCheckedChangeListener { _, on -> onActivar(x, on) }
        h.b.btnBorrar.setOnClickListener { onBorrar(x) }
    }
}
