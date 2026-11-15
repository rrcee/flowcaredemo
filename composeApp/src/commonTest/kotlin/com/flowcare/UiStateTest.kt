package com.flowcare
import kotlin.test.*
import kotlinx.serialization.json.*
class UiStateTest {
    @Test fun validationErrorsAreReadable() {
        val response=Json.parseToJsonElement("""{"detail":[{"loc":["body","slot"],"msg":"Use YYYY-MM-DD HH:mm","type":"value_error"}]}""").jsonObject
        assertEquals("Use YYYY-MM-DD HH:mm",apiError(response))
    }
    @Test fun normalErrorsArePreserved() {
        assertEquals("Slot already reserved",apiError(buildJsonObject { put("detail","Slot already reserved") }))
        assertTrue(apiError(JsonObject(emptyMap())).isNotBlank())
    }
    @Test fun doctorBackStaysInConsole() {
        assertEquals("Console",previousPage("Consultation",true))
        assertEquals("Console",previousPage("Settings",true))
        assertEquals("Welcome",previousPage("Settings",false))
    }
}
