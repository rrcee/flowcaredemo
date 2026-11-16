package com.flowcare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.json.JSONObject

internal fun departmentIcon(id: String): ImageVector = when (id) {
    "heart" -> Icons.Default.FavoriteBorder
    "bones" -> Icons.Default.AccessibilityNew
    "children" -> Icons.Default.ChildCare
    "mind" -> Icons.Default.Spa
    else -> Icons.Default.MedicalServices
}

@Composable internal fun CareSearchEntry(onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = c.surface, shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, c.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Search doctors or departments", style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Search, null, tint = c.primary)
        }
    }
}

@Composable internal fun DepartmentTiles(items: List<CareDepartment>, onSelect: (CareDepartment) -> Unit) {
    val c = MaterialTheme.colorScheme
    val columns = if (LocalDensity.current.fontScale > 1.2f) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { department ->
                    Surface(onClick = { onSelect(department) }, modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp), color = c.surface) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(departmentIcon(department.id), null, tint = c.primary,
                                modifier = Modifier.background(c.primaryContainer, RoundedCornerShape(14.dp)).padding(12.dp).size(26.dp))
                            Text(department.name, style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("View care", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(20.dp), tint = c.primary)
                            }
                        }
                    }
                }
                if (row.size < columns) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable internal fun DoctorPreview(d: CareDoctor, selected: Boolean = false, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val largeText = LocalDensity.current.fontScale > 1.2f
    Surface(onClick = onClick, color = if (selected) c.secondaryContainer else c.surface,
        shape = RoundedCornerShape(24.dp), border = if (selected) BorderStroke(2.dp, c.primary) else null,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (!largeText) Surface(shape = RoundedCornerShape(20.dp), color = c.primaryContainer) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Text(d.initials, color = c.onPrimaryContainer, style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(d.name, style = MaterialTheme.typography.titleMedium)
                Text(d.specialty, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                Text("${d.years} years · ₹${d.fee} demo visit", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            Icon(if (selected) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.primary)
        }
    }
}

@Composable internal fun VisitHero(current: JSONObject?, onBook: () -> Unit, onVisit: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.primaryContainer, contentColor = c.onPrimaryContainer,
        shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.background(Brush.verticalGradient(listOf(c.primaryContainer, c.secondaryContainer))).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (current == null) "Your care starts here" else when (current.optString("status")) {
                    "active" -> "The doctor is ready"
                    "waiting" -> "You're checked in"
                    else -> "Your next visit"
                }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
                Icon(Icons.Default.EventAvailable, null, modifier = Modifier.size(32.dp))
            }
            if (current == null) {
                Text("Find your doctor and plan a visit at St. Mathew Hospital.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(current.optString("doctor"), style = MaterialTheme.typography.headlineMedium)
                Text(current.optString("department"), style = MaterialTheme.typography.bodyMedium)
                Surface(color = c.primary, contentColor = c.onPrimary, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(22.dp))
                            Text("${current.optString("date")} · ${current.optString("time")}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(22.dp))
                            Text("Token #${current.optInt("token")} · ${current.optString("status").replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Button(onClick = if (current == null) onBook else onVisit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (current == null) c.primary else c.surface,
                    contentColor = if (current == null) c.onPrimary else c.primary), contentPadding = PaddingValues(16.dp)) {
                Text(if (current == null) "Book an appointment" else if (current.optString("status") == "confirmed") "View appointment" else "View live queue",
                    style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable internal fun CareQuickActions(onVisits: () -> Unit, onBills: () -> Unit, onFamily: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val actions = listOf(Triple("Visits", Icons.Default.CalendarMonth, onVisits),
        Triple("Bills", Icons.Default.ReceiptLong, onBills), Triple("Family", Icons.Default.PeopleOutline, onFamily))
    val columns = if (LocalDensity.current.fontScale > 1.2f) 1 else 3
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, icon, action) ->
                    Surface(onClick = action, shape = RoundedCornerShape(22.dp), color = c.surface,
                        shadowElevation = 1.dp, modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Column(Modifier.padding(vertical = 18.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(icon, null, tint = c.primary)
                            Text(label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
