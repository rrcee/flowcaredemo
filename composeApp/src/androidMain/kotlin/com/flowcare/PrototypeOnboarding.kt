package com.flowcare

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Fictional-only onboarding for the offline prototype; no files are uploaded. */
@Composable internal fun PrototypeOnboarding(onFinish: (String) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var blood by rememberSaveable { mutableStateOf("") }
    var allergies by rememberSaveable { mutableStateOf("") }
    var medicines by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var selectedHospital by rememberSaveable { mutableStateOf(false) }
    var selectedDoctor by rememberSaveable { mutableStateOf("") }
    var fileSelected by rememberSaveable { mutableStateOf(false) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> fileSelected = uri != null }
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.background) {
        Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("A little about you", style = MaterialTheme.typography.headlineMedium)
            Text("Prototype setup · ${step + 1} of 3", color = c.primary, style = MaterialTheme.typography.titleSmall)
            Text("Use fictional details only. This offline prototype does not save your medical history or upload files.", style = MaterialTheme.typography.bodyMedium)
            when (step) {
                0 -> {
                    Text("Your profile", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(name, { name = it.take(60) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(phone, { phone = it.take(20) }, label = { Text("Phone number (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(age, { age = it.filter(Char::isDigit).take(3) }, label = { Text("Age (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(height, { height = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6) }, label = { Text("Height in cm (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(weight, { weight = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6) }, label = { Text("Weight in kg (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    val h = height.toDoubleOrNull(); val w = weight.toDoubleOrNull()
                    if (h != null && h in 30.0..300.0 && w != null && w in 1.0..500.0) {
                        Text("BMI: ${String.format(Locale.US, "%.1f", w / ((h / 100) * (h / 100)))} · a general measure, not a diagnosis", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                1 -> {
                    Text("Optional health details", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(blood, { blood = it.take(8) }, label = { Text("Blood type") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(allergies, { allergies = it.take(200) }, label = { Text("Allergies") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(medicines, { medicines = it.take(200) }, label = { Text("Current medications") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(contact, { contact = it.take(80) }, label = { Text("Emergency contact") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Choose a sample PDF") }
                    if (fileSelected) Text("File selected for preview only; it is not uploaded or saved.", style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    Text("Choose your care", style = MaterialTheme.typography.titleLarge)
                    Text("Choose a sample hospital and doctor, or skip and decide later.")
                    FilterChip(selected = selectedHospital, onClick = { selectedHospital = !selectedHospital; if (!selectedHospital) selectedDoctor = "" }, label = { Text("St. Mathew Hospital") })
                    if (selectedHospital) CareCatalog.doctors.take(3).forEach { doctor ->
                        FilterChip(selected = selectedDoctor == doctor.id, onClick = { selectedDoctor = doctor.id }, label = { Text(doctor.name) })
                    }
                }
            }
            Button(onClick = { if (step < 2) step++ else onFinish(name.trim()) }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) { Text(if (step < 2) "Continue" else "Finish setup") }
            TextButton(onClick = { onFinish("") }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Skip for now") }
        }
    }
}
