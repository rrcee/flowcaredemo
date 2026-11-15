package com.flowcare

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import com.flowcare.resources.Res
import com.flowcare.resources.jakarta
import com.flowcare.resources.jetbrains
import org.jetbrains.compose.resources.Font
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
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
private fun JsonObject.s(key: String) = (this[key] as? JsonPrimitive)?.content ?: ""
private fun JsonObject.items(key: String) = this[key]?.jsonArray?.map { it.jsonObject } ?: emptyList()
private val CareBlue=Color(0xFF2454C6)
private val CareInk=Color(0xFF162E58)
private val CareCream=Color(0xFFFFF8E8)
private val CarePaper=Color(0xFFFFFDF5)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(initialUrl: String, onThemeChanged:(Boolean)->Unit={}) {
    var base by rememberSaveable { mutableStateOf(initialUrl) }
    var doctorRole by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("Welcome") }
    var state by remember { mutableStateOf(JsonObject(emptyMap())) }
    var errorMessage by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var patient by rememberSaveable { mutableStateOf("Self") }
    var selectedDoctor by rememberSaveable { mutableStateOf("anya") }
    var selectedBooking by rememberSaveable { mutableStateOf("") }
    var slot by rememberSaveable { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var history by remember { mutableStateOf("") }
    var chat by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    var dark by rememberSaveable { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val client=remember { HttpClient {
        install(WebSockets)
        install(HttpTimeout) { requestTimeoutMillis=15000; connectTimeoutMillis=10000 }
    } }
    val currentConnection by rememberUpdatedState(base to doctorRole)
    var previousBase by rememberSaveable { mutableStateOf(initialUrl) }
    var previousRole by rememberSaveable { mutableStateOf(false) }
    var actionJob by remember { mutableStateOf<Job?>(null) }
    var allBills by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(base,doctorRole) { onDispose { actionJob?.cancel(); busy=false } }
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
        val connection=base to doctorRole
        actionJob=scope.launch {
            try {
                val response=client.post("${connection.first}/api/$path") {
                    header("X-Token",token)
                    if(body!=null) { contentType(ContentType.Application.Json);setBody(body.toString()) }
                }
                val data=json.parseToJsonElement(response.bodyAsText()).jsonObject
                if(!response.status.isSuccess()) error(apiError(data))
                if(currentConnection==connection) {
                    done(data)
                    try { refresh() } catch(e:CancellationException) { throw e }
                    catch(e:Exception) { errorMessage="Saved. Live updates are reconnecting; do not submit again." }
                }
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) { errorMessage=e.message ?: "Connection failed. Check your connection in Settings." }
            finally { busy=false }
        }
    }
    fun body(vararg pairs:Pair<String,String>)=buildJsonObject { pairs.forEach { put(it.first,it.second) } }
    LaunchedEffect(base,doctorRole) {
        state=JsonObject(emptyMap());connected=false
        if(previousBase!=base || previousRole!=doctorRole) selectedBooking=""
        previousBase=base;previousRole=doctorRole
        errorMessage="";notice="";history="";notes="";reply="";chat=""
        while(isActive) {
            try {
                refresh()
                client.webSocket(urlString=base.replaceFirst("http","ws")+"/ws") {
                    send(body("token" to token).toString());connected=true
                    for(frame in incoming) if(frame is Frame.Text) state=json.parseToJsonElement(frame.readText()).jsonObject
                }
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) { connected=false }
            finally { connected=false }
            delay(3000)
        }
    }
    LaunchedEffect(page) { history="";notes="";chat="";reply="";errorMessage="";notice="" }
    SideEffect { onThemeChanged(dark) }
    val colors=if(dark) darkColorScheme(
        primary=Color(0xFFABC5FF),onPrimary=Color(0xFF112956),
        primaryContainer=Color(0xFF243F71),onPrimaryContainer=Color(0xFFFFF8E8),
        secondary=Color(0xFFABC5FF),tertiary=Color(0xFFABC5FF),
        background=Color(0xFF111D32),surface=Color(0xFF1B2B46),
        surfaceVariant=Color(0xFF263B5E),onBackground=CareCream,onSurface=CareCream,
        onSurfaceVariant=Color(0xFFD4DEF0),outline=Color(0xFFABC5FF),outlineVariant=Color(0xFF728AB4),error=Color(0xFFFFB4A8)
    ) else lightColorScheme(
        primary=CareBlue,onPrimary=Color.White,
        primaryContainer=Color(0xFFE1EAFE),onPrimaryContainer=CareInk,
        secondary=CareBlue,tertiary=CareBlue,
        secondaryContainer=Color(0xFFE1EAFE),onSecondaryContainer=CareInk,
        tertiaryContainer=Color(0xFFE1EAFE),onTertiaryContainer=CareInk,surfaceTint=CareBlue,
        background=CareCream,surface=CarePaper,surfaceVariant=Color(0xFFF0EAD9),
        onBackground=CareInk,onSurface=CareInk,onSurfaceVariant=Color(0xFF41516C),
        outline=CareInk,outlineVariant=Color(0xFF8B98AA),error=Color(0xFF9D3030)
    )
    val uiFont=FontFamily(Font(Res.font.jakarta))
    val dataFont=FontFamily(Font(Res.font.jetbrains))
    val defaultType=Typography()
    val type=Typography(
        displayLarge=defaultType.displayLarge.copy(fontFamily=uiFont,fontSize=48.sp,lineHeight=56.sp,fontWeight=FontWeight.Bold),
        headlineLarge=defaultType.headlineLarge.copy(fontFamily=uiFont,fontSize=30.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold),
        headlineMedium=defaultType.headlineMedium.copy(fontFamily=uiFont,fontSize=26.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold),
        titleMedium=defaultType.titleMedium.copy(fontFamily=uiFont,fontSize=20.sp,lineHeight=28.sp,fontWeight=FontWeight.SemiBold),
        titleSmall=defaultType.titleSmall.copy(fontFamily=uiFont,fontSize=18.sp,lineHeight=26.sp,fontWeight=FontWeight.SemiBold),
        bodyLarge=defaultType.bodyLarge.copy(fontFamily=uiFont,fontSize=18.sp,lineHeight=28.sp),
        bodyMedium=defaultType.bodyMedium.copy(fontFamily=uiFont,fontSize=18.sp,lineHeight=28.sp),
        bodySmall=defaultType.bodySmall.copy(fontFamily=uiFont,fontSize=16.sp,lineHeight=24.sp),
        labelLarge=defaultType.labelLarge.copy(fontFamily=uiFont,fontSize=17.sp,lineHeight=24.sp,fontWeight=FontWeight.SemiBold)
    )
    val home=if(doctorRole) "Console" else "Welcome"
    val goBack={ page=previousPage(page,doctorRole) }
    PlatformBackHandler(page!=home,goBack)
    MaterialTheme(colorScheme=colors, typography=type, shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp))) {
        Surface(Modifier.fillMaxSize(),color=colors.background) {
            Column(Modifier.safeDrawingPadding().imePadding().widthIn(max=600.dp).padding(horizontal=24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Spacer(Modifier.height(6.dp))
                Text("FlowCare",style=MaterialTheme.typography.headlineLarge)
                Text(if(connected) "Demo hospital · connected" else "Connecting… Check Settings if this continues.",style=MaterialTheme.typography.labelLarge,color=if(connected) colors.secondary else colors.onSurfaceVariant)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected=!doctorRole,onClick={doctorRole=false;page="Welcome"},label={Text("Patient / Family")},modifier=Modifier.heightIn(min=56.dp))
                    FilterChip(selected=doctorRole,onClick={doctorRole=true;page="Console"},label={Text("Doctor")},modifier=Modifier.heightIn(min=56.dp))
                }
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface),onClick={page=if(doctorRole) "Console" else "Welcome"}) { Text("Home") }
                    if(page !in listOf("Welcome","Console")) TextButton(colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface),onClick={goBack()}) { Text("Back") }
                    TextButton(colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface),onClick={page="Settings"}) { Text("Settings") }
                }
                if(errorMessage.isNotBlank()) Text(errorMessage,color=colors.error)
                if(notice.isNotBlank()) Text(notice,color=colors.secondary)
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if(page=="Welcome") "How can we help?" else page,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold,modifier=Modifier.semantics { heading() })
                when(page) {
                    "Welcome" -> {
                        Text("Choose one option to get started.",style=MaterialTheme.typography.bodyLarge)
                        Nav("Book hospital visit",primary=true) { page="Family" }
                        Nav("My appointments & bills") { page="Appointments" }
                        Spacer(Modifier.height(4.dp))
                        Text("Other care options",style=MaterialTheme.typography.titleSmall)
                        Nav("Walk-in registration") { page="Walk-in" }
                        Nav("Mental health support") { page="EduMind" }
                    }
                    "Family" -> {
                        Text("Choose who you are booking for")
                        listOf("Self","Parent","Spouse","Child").forEach { name -> Nav(name) { patient=name;page="Hospitals" } }
                        OutlinedTextField(patient,{patient=it},label={Text("Family member name")},modifier=Modifier.fillMaxWidth())
                        Nav("Continue",primary=true) { page="Hospitals" }
                    }
                    "Hospitals" -> {
                        Panel("FlowCare Community Hospital","Demo partner · General medicine & psychiatry · location data simulated")
                        Nav("Choose a doctor",primary=true) { page="Doctors" }
                    }
                    "Doctors" -> {
                        if(state.items("doctors").isEmpty()) Text("Doctor availability is loading. If you are offline, check Settings.")
                        state.items("doctors").forEach { d ->
                        Panel(d.s("name"),"${d.s("specialty")} · ${d.s("rating")} stars · ${d.s("years")} years")
                        Nav("Select ${d.s("name")}") { selectedDoctor=d.s("id");page="Slots" }
                    }
                    }
                    "Slots", "Walk-in" -> {
                        OutlinedTextField(patient,{patient=it},label={Text("Patient name")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(slot,{slot=it},label={Text("Visit date and time")},supportingText={Text("Use year-month-day and 24-hour time. Example: 2030-11-15 09:00")},modifier=Modifier.fillMaxWidth())
                        Text("Walk-in desk and scanner integration pending. Enter a visit time to register in this demo.")
                        Nav("Confirm appointment",!busy && connected && patient.isNotBlank() && slot.isNotBlank(),primary=true) {
                            action("bookings",body("patient" to patient,"doctor" to selectedDoctor,"slot" to slot)) { selectedBooking=it.s("id");page="Queue" }
                        }
                    }
                    "Appointments" -> {
                        if(state.items("queue").isEmpty()) Text("No waiting appointments. Book your first visit.")
                        state.items("queue").forEach { q -> Nav("${q.s("patient")} · #${q.s("token")}") {selectedBooking=q.s("id");page="Queue"} }
                        Nav("All bills") { allBills=true;page="Payments" }
                        Nav("Prescriptions & reviews") { page="Prescriptions" }
                    }
                    "Queue" -> {
                        val q=state.items("queue").find {it.s("id")==selectedBooking}
                        if(q==null) Text("This appointment has left the live queue. Check prescriptions for completed visits.") else {
                            Text("#${q.s("token")}",style=MaterialTheme.typography.displayLarge,fontFamily=dataFont,color=colors.secondary)
                            Panel(q.s("patient"),"${q.s("ahead")} patients ahead · ${if(q.s("paused")=="true") "Wait estimate paused" else "about ${q.s("wait")} minutes"} · ${q.s("status")}")
                            if(q.s("paused")=="true") Panel("Doctor attending an emergency","Your wait estimate may change. We will update this screen.")
                            Panel("Cafe wait mode","Keep this app open for updates. Background push alerts are not connected in this demo.")
                            Text("Appointment reference: $selectedBooking",fontFamily=dataFont)
                            if(q.s("status")=="waiting") Nav("Cancel appointment") {action("bookings/$selectedBooking/cancel")}
                        }
                        Nav("Department bills") {allBills=false;page="Payments"}
                        Nav("Temporary records") {page="Records"}
                        Nav("Prescriptions") {page="Prescriptions"}
                    }
                    "Payments" -> {
                        Text("Sandbox payments · no money is charged")
                        state.items("bills").filter {allBills || it.s("booking")==selectedBooking}.forEach { bill ->
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
                        Text("Appointment $selectedBooking",fontFamily=dataFont)
                        Nav("Consume temporary history",!busy) {action("records/$selectedBooking/consume") {history=it.s("text")}}
                        if(history.isNotBlank()) Panel("Temporary history · consumed",history)
                        Nav("Order blood lab · ₹450",!busy) {
                            action("orders",buildJsonObject {put("booking",selectedBooking);put("department","Blood lab");put("amount",45000)}) {notice="Lab bill sent to patient"}
                        }
                        OutlinedTextField(notes,{notes=it},label={Text("Prescription · medication, dose, instructions")},minLines=4,modifier=Modifier.fillMaxWidth())
                        Nav("Issue prescription",notes.isNotBlank() && !busy,primary=true) {action("bookings/$selectedBooking/prescription",body("text" to notes)) {notice="Prescription sent";notes=""}}
                    }
                    "Prescriptions" -> {
                        if(state.items("prescriptions").isEmpty()) Text("No prescriptions yet. Your doctor’s instructions will appear here after consultation.")
                        state.items("prescriptions").forEach { rx ->
                        Panel("Prescription · ${rx.s("booking")}",rx.s("text"))
                        FlowRow { (1..5).forEach { stars -> TextButton(colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface),onClick={action("bookings/${rx.s("booking")}/review/$stars") {notice="Review saved"}},enabled=!busy) {Text("$stars / 5")} } }
                    }
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
                        Nav(if(dark) "Use light appearance" else "Use dark appearance") { dark=!dark }
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
@Composable private fun Nav(label:String,enabled:Boolean=true,primary:Boolean=false,click:()->Unit) {
    val colors=MaterialTheme.colorScheme
    val modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).then(
        if(primary && enabled) Modifier.drawBehind {
            drawRoundRect(color=colors.onSurface,topLeft=Offset(3.dp.toPx(),4.dp.toPx()),cornerRadius=CornerRadius(20.dp.toPx()))
        } else Modifier
    )
    Button(
        onClick=click,enabled=enabled,shape=RoundedCornerShape(20.dp),
        border=BorderStroke(2.dp,if(enabled) colors.onSurface else colors.outlineVariant),
        colors=ButtonDefaults.buttonColors(
            containerColor=if(primary) colors.primary else colors.surface,
            contentColor=if(primary) colors.onPrimary else colors.onSurface,
            disabledContainerColor=colors.surfaceVariant,disabledContentColor=colors.onSurfaceVariant
        ),
        contentPadding=PaddingValues(horizontal=20.dp,vertical=18.dp),modifier=modifier
    ) {
        Text(label,modifier=Modifier.fillMaxWidth(),style=MaterialTheme.typography.titleSmall)
    }
}
@Composable private fun Panel(title:String,description:String) {
    Surface(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(2.dp,MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
