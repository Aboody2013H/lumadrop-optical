package io.lumadrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.lumadrop.app.ui.LumaDropApp
import io.lumadrop.app.ui.LumaTheme

class MainActivity : ComponentActivity() {
    private val model by viewModels<LumaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LumaTheme { LumaDropApp(model) } }
    }
}

