package com.example.dispensador.control

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/**
 * Acceso a Firebase solo para el Modo Control.
 * Si Pipe ya tiene FirebaseRepository, reemplazar estas llamadas por las suyas
 * (las rutas son las del plan: dispensadores/{id}/estado, comando, horarios, historial).
 */
class ControlRepository(private val dispId: String) {

    private val raiz = FirebaseDatabase.getInstance().getReference("dispensadores").child(dispId)
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    fun escucharEstado(onCambio: (Estado) -> Unit): () -> Unit {
        val ref = raiz.child("estado")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                onCambio(
                    Estado(
                        nivel = s.child("nivel").getValue(Int::class.java) ?: 0,
                        enLinea = s.child("enLinea").getValue(Boolean::class.java) ?: false,
                        ultimaRacion = s.child("ultimaRacion").getValue(Long::class.java) ?: 0L
                    )
                )
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(l)
        return { ref.removeEventListener(l) }
    }

    fun enviarComando(porcion: Porcion, onFin: (Boolean) -> Unit) {
        val cmd = mapOf(
            "tipo" to "dispensar",
            "porcion" to porcion.clave,
            "uid" to uid,
            "timestamp" to ServerValue.TIMESTAMP
        )
        raiz.child("comando").setValue(cmd).addOnCompleteListener { onFin(it.isSuccessful) }
    }

    // ---- Horarios ----
    fun escucharHorarios(onCambio: (List<Horario>) -> Unit): () -> Unit {
        val ref = raiz.child("horarios")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                onCambio(s.children.mapNotNull { c ->
                    c.getValue(Horario::class.java)?.copy(id = c.key.orEmpty())
                }.sortedBy { it.hora })
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(l)
        return { ref.removeEventListener(l) }
    }

    fun agregarHorario(hora: String, porcion: Porcion) {
        val ref = raiz.child("horarios").push()
        ref.setValue(mapOf("hora" to hora, "porcion" to porcion.clave, "activo" to true))
    }

    fun activarHorario(id: String, activo: Boolean) {
        raiz.child("horarios").child(id).child("activo").setValue(activo)
    }

    fun borrarHorario(id: String) {
        raiz.child("horarios").child(id).removeValue()
    }

    // ---- Historial ----
    fun escucharHistorial(onCambio: (List<Evento>) -> Unit): () -> Unit {
        val q = raiz.child("historial").orderByChild("fecha").limitToLast(100)
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                onCambio(s.children.mapNotNull { c ->
                    c.getValue(Evento::class.java)?.copy(id = c.key.orEmpty())
                }.sortedByDescending { it.fecha })
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        q.addValueEventListener(l)
        return { q.removeEventListener(l) }
    }
}
