package com.example.dispensador.control

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.example.dispensador.R
import com.example.dispensador.auth.Sesion
import com.example.dispensador.auth.SesionActivity
import com.example.dispensador.databinding.ActivityControlHostBinding
import com.example.dispensador.modo.ModoActivity

/** Contenedor del Modo Control: barra inferior con Control, Horarios e Historial. */
class ControlHostActivity : SesionActivity() {

    private lateinit var b: ActivityControlHostBinding
    lateinit var repo: ControlRepository
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // SesionActivity ya mando al login si no hay sesion.
        if (!Sesion.hayUsuario()) return

        val dispId = intent.getStringExtra(EXTRA_DISP_ID) ?: "DISP-01"
        repo = ControlRepository(dispId)

        b = ActivityControlHostBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        b.toolbar.subtitle = dispId

        if (savedInstanceState == null) mostrar(ControlFragment())
        b.bottomNav.setOnItemSelectedListener {
            mostrar(when (it.itemId) {
                R.id.nav_horarios -> HorariosFragment()
                R.id.nav_historial -> HistorialFragment()
                else -> ControlFragment()
            })
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_MODO, 0, "Cambiar modo")
        menu.add(0, MENU_SALIR, 1, "Cerrar sesión")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        MENU_MODO -> { startActivity(Intent(this, ModoActivity::class.java)); finish(); true }
        MENU_SALIR -> { cerrarSesion(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun mostrar(f: Fragment) =
        supportFragmentManager.beginTransaction().replace(R.id.contenedor, f).commit()

    companion object {
        const val EXTRA_DISP_ID = "dispensadorId"
        private const val MENU_MODO = 1
        private const val MENU_SALIR = 2
    }
}
