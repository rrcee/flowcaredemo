package com.flowcare

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.UUID

/** Connected data is accessed only through authorized callable functions. */
@Composable
internal fun LiveCare(user: FirebaseUser, staffApp: Boolean, onBack: () -> Unit) {
    val functions = remember { FirebaseFunctions.getInstance("asia-south1") }
    var role by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Checking account access…") }
    var busy by remember { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf("visits") }
    var slots by remember { mutableStateOf<List<Map<*, *>>>(emptyList()) }
    var visits by remember { mutableStateOf<List<Map<*, *>>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Map<*, *>>>(emptyList()) }
    var careInstructions by remember { mutableStateOf("") }
    var instructionDraft by remember { mutableStateOf("") }
    var detailVisitId by rememberSaveable { mutableStateOf("") }
    var detailVisitStatus by rememberSaveable { mutableStateOf("") }
    var orderRequestId by rememberSaveable { mutableStateOf("") }
    var orderRequestKind by rememberSaveable { mutableStateOf("") }
    var selectedSlot by rememberSaveable { mutableStateOf("") }
    var requestId by rememberSaveable { mutableStateOf("") }
    var patientName by rememberSaveable { mutableStateOf("") }
    var doctorId by rememberSaveable { mutableStateOf("") }
    var doctorName by rememberSaveable { mutableStateOf("") }
    var department by rememberSaveable { mutableStateOf("") }
    var startsAt by rememberSaveable { mutableStateOf("") }
    var cancelId by remember { mutableStateOf<String?>(null) }
    BackHandler { onBack() }

    fun call(name: String, data: Map<String, Any>, success: (Map<*, *>) -> Unit) {
        busy = true
        status = "Connecting…"
        functions.getHttpsCallable(name).call(data).addOnCompleteListener { task ->
            busy = false
            if (task.isSuccessful) {
                val result = task.result?.data as? Map<*, *>
                if (result != null) { status = ""; success(result) }
                else status = "The service returned an unexpected response. Please try again."
            } else {
                val failure = task.exception as? FirebaseFunctionsException
                status = when (failure?.code) {
                    FirebaseFunctionsException.Code.PERMISSION_DENIED, FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Your account does not have access to this action."
                    FirebaseFunctionsException.Code.FAILED_PRECONDITION, FirebaseFunctionsException.Code.ALREADY_EXISTS -> failure.message ?: "This action is no longer available."
                    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> failure.message ?: "Check the information and try again."
                    else -> "Connected care is unavailable. Check your connection or try again later."
                }
            }
        }
    }
    fun loadVisits() = call("listVisits", emptyMap()) { result ->
        visits = (result["visits"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
    }
    fun loadSlots() = call("listAvailableSlots", mapOf("hospitalId" to "st-mathew")) { result ->
        slots = (result["slots"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
    }
    fun loadDetails(visitId: String) = call("getVisitDetails", mapOf("visitId" to visitId)) { result ->
        orders = (result["orders"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
        careInstructions = result["careInstructions"] as? String ?: ""
    }
    LaunchedEffect(user.uid) {
        user.getIdToken(true).addOnCompleteListener { task ->
            if (!task.isSuccessful) status = "Could not check account access. Check your connection and try again."
            else {
                val claim = task.result?.claims?.get("flowcare_role") as? String ?: "patient"
                if ((!staffApp && claim == "patient") || (staffApp && claim in listOf("hospital", "doctor"))) {
                    role = claim
                    loadVisits()
                } else status = "This account does not have access to this workspace."
            }
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack) { Text("Back to account") }
            Text("Connected care", style = MaterialTheme.typography.headlineMedium)
            Text("St. Mathew Hospital · online service", style = MaterialTheme.typography.bodyMedium)
            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium)
            if (role != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = screen == "visits", onClick = { screen = "visits"; loadVisits() },
                        enabled = !busy, label = { Text("Visits") })
                    if (role == "patient") FilterChip(selected = screen == "book", onClick = { screen = "book"; loadSlots() },
                        enabled = !busy, label = { Text("Book") })
                    if (role == "hospital") FilterChip(selected = screen == "slot", onClick = { screen = "slot" },
                        enabled = !busy, label = { Text("Add time") })
                }
                when (screen) {
                    "visits" -> {
                        Text(if (staffApp) "Assigned visits" else "Your visits", style = MaterialTheme.typography.titleLarge)
                        if (visits.isEmpty() && !busy) Text("No visits to show. Refresh when connected.")
                        visits.forEach { visit ->
                            val visitId = visit["visitId"] as? String ?: ""
                            val state = visit["status"] as? String ?: ""
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(visit["patientName"] as? String ?: "Patient", style = MaterialTheme.typography.titleMedium)
                                    Text("${visit["doctorName"] ?: "Care team"} · ${visit["department"] ?: "Care"}")
                                    Text("${visit["startsAt"] ?: ""}\n${state.replaceFirstChar { it.uppercase() }}")
                                    OutlinedButton(onClick = {
                                        detailVisitId = visitId; detailVisitStatus = state; screen = "details"; loadDetails(visitId)
                                    }, enabled = !busy) { Text("Visit details") }
                                    val next = when { role == "hospital" && state == "confirmed" -> "waiting"; role == "doctor" && state == "waiting" -> "active"; role == "doctor" && state == "active" -> "complete"; else -> null }
                                    if (role == "patient" && state == "confirmed") OutlinedButton(onClick = { cancelId = visitId }, enabled = !busy) { Text("Cancel visit") }
                                    if (next != null) Button(onClick = {
                                        call("advanceVisit", mapOf("visitId" to visitId, "status" to next)) { loadVisits() }
                                    }, enabled = !busy) { Text(when (next) { "waiting" -> "Check in"; "active" -> "Call patient"; else -> "Complete visit" }) }
                                }
                            }
                        }
                        OutlinedButton(onClick = { loadVisits() }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Refresh visits") }
                    }
                    "details" -> {
                        TextButton(onClick = { screen = "visits"; loadVisits() }, enabled = !busy) { Text("Back to visits") }
                        Text("Care instructions", style = MaterialTheme.typography.titleLarge)
                        Text(careInstructions.ifBlank { "No instructions have been shared for this visit." })
                        if (role == "doctor" && detailVisitStatus == "active") {
                            OutlinedTextField(instructionDraft, { instructionDraft = it.take(4000) }, label = { Text("Care instructions") },
                                minLines = 3, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                call("saveCareInstructions", mapOf("visitId" to detailVisitId, "text" to instructionDraft.trim())) {
                                    instructionDraft = ""; loadDetails(detailVisitId)
                                }
                            }, enabled = !busy && instructionDraft.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Share care instructions") }
                            listOf("lab" to "Order blood test", "pharmacy" to "Order pharmacy items").forEach { (kind, label) ->
                                OutlinedButton(onClick = {
                                    if (orderRequestId.isBlank() || orderRequestKind != kind) { orderRequestId = UUID.randomUUID().toString(); orderRequestKind = kind }
                                    call("addDemoOrder", mapOf("visitId" to detailVisitId, "requestId" to orderRequestId, "kind" to kind)) {
                                        orderRequestId = ""; orderRequestKind = ""; loadDetails(detailVisitId)
                                    }
                                }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label) }
                            }
                        }
                        Text("Orders and simulated billing", style = MaterialTheme.typography.titleLarge)
                        Text("Payments here are simulated; no money is collected.", style = MaterialTheme.typography.bodyMedium)
                        if (orders.isEmpty() && !busy) Text("No orders have been added.")
                        orders.forEach { order ->
                            val orderId = order["orderId"] as? String ?: ""
                            val orderStatus = order["status"] as? String ?: ""
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(order["label"] as? String ?: "Order", style = MaterialTheme.typography.titleMedium)
                                    Text("₹${order["amount"] ?: 0} · ${when (orderStatus) { "paid_demo" -> "Simulated payment recorded"; "ready" -> "Ready"; else -> "Simulated payment pending" }}")
                                    if (role == "patient" && orderStatus == "pending") Button(onClick = {
                                        call("updateDemoOrder", mapOf("visitId" to detailVisitId, "orderId" to orderId, "status" to "paid_demo")) { loadDetails(detailVisitId) }
                                    }, enabled = !busy) { Text("Record simulated payment") }
                                    if (role == "hospital" && orderStatus == "paid_demo") Button(onClick = {
                                        call("updateDemoOrder", mapOf("visitId" to detailVisitId, "orderId" to orderId, "status" to "ready")) { loadDetails(detailVisitId) }
                                    }, enabled = !busy) { Text("Mark ready") }
                                }
                            }
                        }
                        OutlinedButton(onClick = { loadDetails(detailVisitId) }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Refresh details") }
                    }
                    "book" -> {
                        Text("Choose an appointment", style = MaterialTheme.typography.titleLarge)
                        if (slots.isEmpty() && !busy) Text("No times are available right now.")
                        slots.forEach { slot ->
                            val slotId = slot["slotId"] as? String ?: ""
                            Surface(onClick = { selectedSlot = slotId; requestId = UUID.randomUUID().toString() },
                                color = if (selectedSlot == slotId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(slot["doctorName"] as? String ?: "Care team", style = MaterialTheme.typography.titleMedium)
                                    Text("${slot["department"] ?: "Care"} · ${slot["startsAt"] ?: ""}")
                                    if (selectedSlot == slotId) Text("Selected")
                                }
                            }
                        }
                        OutlinedTextField(patientName, { patientName = it.take(100) }, label = { Text("Patient name") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            call("bookVisit", mapOf("hospitalId" to "st-mathew", "slotId" to selectedSlot,
                                "requestId" to requestId, "patientName" to patientName.trim())) {
                                selectedSlot = ""; requestId = ""; screen = "visits"; loadVisits()
                            }
                        }, enabled = !busy && selectedSlot.isNotBlank() && patientName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Book appointment") }
                        OutlinedButton(onClick = { loadSlots() }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Refresh available times") }
                    }
                    "slot" -> {
                        Text("Add an appointment time", style = MaterialTheme.typography.titleLarge)
                        Text("Only authorized hospital staff can publish a time.")
                        OutlinedTextField(doctorId, { doctorId = it }, label = { Text("Doctor account ID") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(doctorName, { doctorName = it }, label = { Text("Doctor name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(department, { department = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(startsAt, { startsAt = it }, label = { Text("Date and time (ISO 8601)") }, supportingText = { Text("Example: 2027-01-15T09:30:00+05:30") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            call("createSlot", mapOf("doctorUid" to doctorId.trim(), "doctorName" to doctorName.trim(),
                                "department" to department.trim(), "startsAt" to startsAt.trim())) {
                                doctorId = ""; doctorName = ""; department = ""; startsAt = ""; status = "Appointment time added."
                            }
                        }, enabled = !busy && doctorId.isNotBlank() && doctorName.isNotBlank() && department.isNotBlank() && startsAt.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Add appointment time") }
                    }
                }
            }
        }
    }
    cancelId?.let { visitId ->
        AlertDialog(onDismissRequest = { cancelId = null }, title = { Text("Cancel this visit?") },
            text = { Text("This releases the appointment time for someone else.") },
            confirmButton = { TextButton(onClick = { cancelId = null; call("cancelVisit", mapOf("visitId" to visitId)) { loadVisits() } }) { Text("Cancel visit") } },
            dismissButton = { TextButton(onClick = { cancelId = null }) { Text("Keep visit") } })
    }
}
