package com.flowcare
import androidx.compose.runtime.Composable
@Composable actual fun PlatformBackHandler(enabled:Boolean,onBack:()->Unit) { /* Explicit in-app Back remains available on iOS. */ }
