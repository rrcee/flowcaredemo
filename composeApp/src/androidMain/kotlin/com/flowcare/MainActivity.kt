package com.flowcare
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
        var showSplash by rememberSaveable { mutableStateOf(true) }
        if (showSplash) {
            val reveal = remember { Animatable(0f) }
            LaunchedEffect(Unit) { reveal.animateTo(1f, tween(450)); delay(250); showSplash = false }
            FlowCareTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Image(painterResource(R.drawable.ic_launcher), contentDescription = null,
                            modifier = Modifier.size(120.dp).graphicsLayer { alpha = reveal.value; scaleX = .9f + .1f * reveal.value; scaleY = scaleX })
                        Spacer(Modifier.height(18.dp))
                        Text("FlowCare", style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.graphicsLayer { alpha = reveal.value })
                    }
                }
            }
        } else DemoApp(BuildConfig.APP_ROLE,onThemeChanged={ dark ->
            val style=if(dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                      else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT)
            enableEdgeToEdge(statusBarStyle=style,navigationBarStyle=style)
        })
    }
 }
}

