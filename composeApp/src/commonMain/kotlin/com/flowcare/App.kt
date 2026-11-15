package com.flowcare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true }
private fun JsonObject.s(key: String) = this[key]?.jsonPrimitive?.content ?: ""
private fun JsonObject.items(key: String) = this[key]?.jsonArray?.map { it.jsonObject } ?: emptyList()
private val Slate=Color(0xFF5B7C99)
private val Sage=Color(0xFF6E9887)
private val Rose=Color(0xFFC47474)
private val Sand=Color(0xFFC89D66)
private val Lavender=Color(0xFF8A79AF)

@Composable
fun App(initialUrl: String) {
    var base by remember { mutableStateOf(initialUrl) }
    var doctorRole by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("Welcome") }
    var state by remember { mutableStateOf(JsonObject(emptyMap())) }
    var errorMessage by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var patient by remember { mutableStateOf("Self") }
    var selectedDoctor by remember { mutableStateOf("anya") }
    var selectedBooking by remember { mutableStateOf("") }
    var slot by remember { mutableStateOf("2026-12-01 09:00") }
    var notes by remember { mutableStateOf("") }
    var history by remember { mutableStateOf("") }
    var chat by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    var dark by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val client=remember { HttpClient { install(WebSockets) } }
    DisposableEffect(Unit) { onDispose { client.close() } }
    val token=if(doctorRole) "doctor-demo" else "patient-demo"
    suspend fun refresh() {
        val response=client.get("$base/api/state") { header("X-Token",token) }
        if (!response.status.isSuccess()) error("Cannot connect: ${response.status}")
        state=json.parseToJsonElement(response.bodyAsText()).jsonObject
    }
    fun action(path: String, body: JsonObject?=null, done:(JsonObject)->Unit={}) {
        if(busy) return
        busy=true; errorMessage="";notice=""
        scope.launch {
            try {
                val response=client.post("$base/api/$path") {
                    header("X-Token",token)
                    if(body!=null) { contentType(ContentType.Application.Json);setBody(body.toString()) }
                }
                val data=json.parseToJsonElement(response.bodyAsText()).jsonObject
                if(!response.status.isSuccess()) error(data.s("detail").ifBlank { "Request failed" })
                done(data);refresh()
            } catch(e:Exception) { errorMessage=e.message ?: "Connection failed" }
            finally { busy=false }
        }
    }
    fun body(vararg pairs:Pair<String,String>)=buildJsonObject { pairs.forEach { put(it.first,it.second) } }
    LaunchedEffect(base,doctorRole) {
        while(isActive) {
            try {
                refresh()
                client.webSocket(urlString=base.replaceFirst("http","ws")+"/ws") {
                    send(body("token" to token).toString());connected=true
                    for(frame in incoming) if(frame is Frame.Text) state=json.parseToJsonElement(frame.readText()).jsonObject
                }
            } catch(e:Exception) { connected=false }
            finally { connected=false }
            delay(3000)
        }
    }
    LaunchedEffect(page) { history="";notes="";chat="";reply="" }
    val colors=if(dark) darkColorScheme(primary=Slate,secondary=Sage,tertiary=Lavender,background=Color(0xFF1A1D24),surface=Color(0xFF242832)) else lightColorScheme(primary=Slate,secondary=Sage,tertiary=Lavender,background=Color(0xFFF5F4F0),surface=Color.White,onSurface=Color(0xFF2B303A))
    MaterialTheme(colorScheme=colors) {
        Surface(Modifier.fillMaxSize(),color=colors.background) {
            Column(Modifier.safeContentPadding().padding(horizontal=20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Spacer(Modifier.height(6.dp))
                Text("FlowCare",style=MaterialTheme.typography.headlineLarge)
                Text("Care, with room to breathe.",style=MaterialTheme.typography.bodyMedium)
                Text(if(connected) "● Connected · synthetic demo" else "○ Connecting to hospital…",color=if(connected) Sage else Sand)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected=!doctorRole,onClick={doctorRole=false;page="Welcome"},label={Text("Patient / Family")})
                    FilterChip(selected=doctorRole,onClick={doctorRole=true;page="Console"},label={Text("Doctor")})
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick={page=if(doctorRole) "Console" else "Welcome"}) { Text("Home") }
                    TextButton(onClick={page="Settings"}) { Text("Settings") }
                    TextButton(onClick={dark=!dark}) { Text(if(dark) "Light" else "Dark") }
                }
                if(errorMessage.isNotBlank()) Text(errorMessage,color=Rose)
                if(notice.isNotBlank()) Text(notice,color=Sage)
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(page,style=MaterialTheme.typography.headlineMedium)
                when(page) {
                    "Welcome" -> {
                        Panel("Your care, connected","Book a visit, follow your queue, and manage family bills in one place.")
                        Nav("Book hospital visit") { page="Family" }
                        Nav("Walk-in registration") { page="Walk-in" }
                        Nav("Mental health support") { page="EduMind" }
                        Nav("My appointments & bills") { page="Appointments" }
                    }
                    "Family" -> {
                        Text("Choose who you are booking for")
                        listOf("Self","Parent","Spouse","Child").forEach { name -> Nav(name) { patient=name;page="Hospitals" } }
                        OutlinedTextField(patient,{patient=it},label={Text("Family member name")},modifier=Modifier.fillMaxWidth())
                        Nav("Continue") { page="Hospitals" }
                    }
                    "Hospitals" -> {
                        Panel("FlowCare Community Hospital","Demo partner · General medicine & psychiatry · location data simulated")
                        Nav("Choose a doctor") { page="Doctors" }
                    }
                    "Doctors" -> state.items("doctors").forEach { d ->
                        Panel(d.s("name"),"${d.s("specialty")} · ${d.s("rating")} stars · ${d.s("years")} years · ${d.s("cases")} cases · Match ${d.s("score")}")
                        Nav("Select ${d.s("name")}") { selectedDoctor=d.s("id");page="Slots" }
                    }
                    "Slots", "Walk-in" -> {
                        OutlinedTextField(patient,{patient=it},label={Text("Patient name")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(slot,{slot=it},label={Text("Visit date & time (YYYY-MM-DD HH:mm)")},modifier=Modifier.fillMaxWidth())
                        Text("Walk-in desk and scanner integration pending. Enter a visit time to register in this demo.")
                        Nav("Confirm appointment",!busy) {
                            action("bookings",body("patient" to patient,"doctor" to selectedDoctor,"slot" to slot)) { selectedBooking=it.s("id");page="Queue" }
                        }
                    }
                    "Appointments" -> {
                        if(state.items("queue").isEmpty()) Text("No waiting appointments. Book your first visit.")
                        state.items("queue").forEach { q -> Nav("${q.s("patient")} · #${q.s("token")}") {selectedBooking=q.s("id");page="Queue"} }
                        Nav("All bills") { page="Payments" }
                        Nav("Prescriptions & reviews") { page="Prescriptions" }
                    }
                    "Queue" -> {
                        val q=state.items("queue").find {it.s("id")==selectedBooking}
                        if(q==null) Text("This appointment has left the live queue. Check prescriptions for completed visits.") else {
                            Text("#${q.s("token")}",style=MaterialTheme.typography.displayLarge,fontFamily=FontFamily.Monospace,color=Sage)
                            Panel(q.s("patient"),"${q.s("ahead")} patients ahead · about ${q.s("wait")} minutes · ${q.s("status")}")
                            if(q.s("paused")=="true") Panel("Doctor attending an emergency","Your wait estimate may change. We will update this screen.")
                            Panel("Cafe wait mode","Keep this app open for updates. Background push alerts are not connected in this demo.")
                            Text("Appointment reference: $selectedBooking",fontFamily=FontFamily.Monospace)
                            if(q.s("status")=="waiting") Nav("Cancel appointment") {action("bookings/$selectedBooking/cancel")}
                        }
                        Nav("Department bills") {page="Payments"}
                        Nav("Temporary records") {page="Records"}
                        Nav("Prescriptions") {page="Prescriptions"}
                    }
                    "Payments" -> {
                        Text("Sandbox payments · no money is charged")
                        state.items("bills").filter {selectedBooking.isBlank() || it.s("booking")==selectedBooking}.forEach { bill ->
                            Panel(bill.s("department"),"₹${(bill.s("amount").toIntOrNull() ?: 0)/100.0} · ${if(bill.s("paid")=="1") "Paid" else "Pending"}")
                            if(bill.s("paid")=="0") Nav("Simulate payment",!busy) {action("bills/${bill.s("id")}/pay")}
                        }
                    }
                    "Records" -> {
                        Text("Paste a verified record. Camera OCR and partner-hospital lookup are pending integrations.")
                        OutlinedTextField(notes,{notes=it},label={Text("Temporary clinical record")},minLines=4,modifier=Modifier.fillMaxWidth())
                        Nav("Share temporarily (30 minutes)",selectedBooking.isNotBlank() && notes.isNotBlank() && !busy) {
                            action("records/$selectedBooking",body("text" to notes)) { notes="";notice="Shared for up to 30 minutes, or until first doctor view." }
                        }
                        Text("Server memory only; first doctor view consumes the record. Device memory is cleared when leaving this screen.")
                    }
                    "Console" -> {
                        state.items("doctors").forEach { d ->
                            FilterChip(selected=selectedDoctor==d.s("id"),onClick={selectedDoctor=d.s("id")},label={Text(d.s("name"))})
                        }
                        Nav("Toggle emergency pause",!busy) {action("doctors/$selectedDoctor/pause")}
                        Nav("Complete current / call next",!busy) {action("doctors/$selectedDoctor/next")}
                        state.items("queue").filter {it.s("doctor")==selectedDoctor}.forEach {q ->
                            Nav("#${q.s("token")} · ${q.s("patient")} · ${q.s("status")}") {selectedBooking=q.s("id");page="Consultation"}
                        }
                    }
                    "Consultation" -> {
                        Text("Appointment $selectedBooking",fontFamily=FontFamily.Monospace)
                        Nav("Consume temporary history",!busy) {action("records/$selectedBooking/consume") {history=it.s("text")}}
                        if(history.isNotBlank()) Panel("Temporary history · consumed",history)
                        Nav("Order blood lab · ₹450",!busy) {
                            action("orders",buildJsonObject {put("booking",selectedBooking);put("department","Blood lab");put("amount",45000)}) {notice="Lab bill sent to patient"}
                        }
                        OutlinedTextField(notes,{notes=it},label={Text("Prescription · medication, dose, instructions")},minLines=4,modifier=Modifier.fillMaxWidth())
                        Nav("Issue prescription",notes.isNotBlank() && !busy) {action("bookings/$selectedBooking/prescription",body("text" to notes)) {notice="Prescription sent";notes=""}}
                    }
                    "Prescriptions" -> state.items("prescriptions").forEach { rx ->
                        Panel("Prescription · ${rx.s("booking")}",rx.s("text"))
                        Row { (1..5).forEach { stars -> TextButton(onClick={action("bookings/${rx.s("booking")}/review/$stars") {notice="Review saved"}},enabled=!busy) {Text("$stars★")} } }
                    }
                    "EduMind" -> {
                        Panel("A moment of calm","Scripted grounding support. This demo does not provide clinical assessment or live crisis monitoring.")
                        OutlinedTextField(chat,{chat=it},label={Text("How are you feeling?")},modifier=Modifier.fillMaxWidth())
                        Nav("Try a grounding exercise",chat.isNotBlank() && !busy) {action("comfort",body("text" to chat)) {reply=it.s("reply");chat=""}}
                        if(reply.isNotBlank()) Panel("EduMind",reply)
                        Panel("Need urgent help?","Contact your local emergency service or a trusted person now. Live clinician calls are not connected.")
                        Nav("Book psychiatry appointment") {selectedDoctor="vikram";page="Slots"}
                    }
                    "Settings" -> {
                        Text("Use Android emulator 10.0.2.2, iOS simulator localhost, or your computer’s LAN address on a physical device. Synthetic data only.")
                        OutlinedTextField(base,{base=it.trimEnd('/')},label={Text("Backend URL")},modifier=Modifier.fillMaxWidth())
                        Nav("Reconnect") {scope.launch {try {refresh();notice="Connected"} catch(e:Exception) {errorMessage=e.message ?: "Connection failed"}}}
                        Text("Demo roles share synthetic records. Production identity, family permissions and hospital isolation must be implemented before real patient use.")
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
@Composable private fun Nav(label:String,enabled:Boolean=true,click:()->Unit) {
    Button(onClick=click,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)) { Text(label) }
}
@Composable private fun Panel(title:String,description:String) {
    Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(title,style=MaterialTheme.typography.titleMedium);Text(description)}}
}
