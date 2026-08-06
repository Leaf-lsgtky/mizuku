package moe.shizuku.manager.app

import rikka.material.app.MaterialActivity

abstract class AppActivity : MaterialActivity() {

    // Appearance is owned by ThemeStore/ShizukuAppTheme on the Compose side. Deliberately no
    // View-layer theme overlays here: they were only consulted at Activity creation, which is
    // what previously made theme changes require a restart.

    override fun onSupportNavigateUp(): Boolean {
        if (!super.onSupportNavigateUp()) {
            finish()
        }
        return true
    }
} 
