package com.example.dispensador.control

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dispensador.databinding.FragmentHistorialBinding

class HistorialFragment : Fragment() {

    private var _b: FragmentHistorialBinding? = null
    private val b get() = _b!!
    private var detener: (() -> Unit)? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHistorialBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val repo = (requireActivity() as ControlHostActivity).repo
        val adapter = HistorialAdapter()
        b.lista.layoutManager = LinearLayoutManager(requireContext())
        b.lista.adapter = adapter
        detener = repo.escucharHistorial { l ->
            if (_b == null) return@escucharHistorial
            adapter.submitList(l)
            b.vacio.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { detener?.invoke(); _b = null; super.onDestroyView() }
}
