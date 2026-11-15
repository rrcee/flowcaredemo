package com.flowcare

import androidx.compose.runtime.Composable
import kotlinx.serialization.json.*

internal fun apiError(data: JsonObject): String = when(val detail=data["detail"]) {
    is JsonPrimitive -> detail.content
    is JsonArray -> detail.mapNotNull { (it as? JsonObject)?.get("msg")?.jsonPrimitive?.content }.joinToString(". ").ifBlank { "Check your entries and try again." }
    else -> "Request failed. Please try again."
}
internal fun previousPage(page:String,doctor:Boolean):String = when(page) {
    "Doctors" -> "Hospitals"
    "Slots" -> "Doctors"
    "Hospitals" -> "Family"
    "Consultation" -> "Console"
    "Records" -> "Queue"
    else -> if(doctor) "Console" else "Welcome"
}
@Composable expect fun PlatformBackHandler(enabled:Boolean, onBack:()->Unit)
