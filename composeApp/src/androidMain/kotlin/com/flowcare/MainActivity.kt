package com.flowcare
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
        DemoApp(BuildConfig.APP_ROLE,onThemeChanged={ dark ->
            val style=if(dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                      else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT)
            enableEdgeToEdge(statusBarStyle=style,navigationBarStyle=style)
        })
    }
 }
}
