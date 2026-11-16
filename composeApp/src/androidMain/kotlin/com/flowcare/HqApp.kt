package com.flowcare

import androidx.compose.foundation.layout.*
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Operational shell deliberately has no clinical store or persisted drafts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HqApp() {
 var tab by remember { mutableIntStateOf(0) }
 var title by remember { mutableStateOf("") }
 var body by remember { mutableStateOf("") }
 var audience by remember { mutableStateOf("Patients") }
 var preview by remember { mutableStateOf(false) }
 var showAccount by remember { mutableStateOf(false) }
 val labels=listOf("Overview","Notifications","Updates","Settings")
 val icons=listOf(Icons.Default.Dashboard,Icons.Default.Notifications,Icons.Default.SystemUpdate,Icons.Default.Settings)
 FlowCareTheme {
 BackHandler(showAccount) { showAccount=false }
 if(showAccount) LiveAccount(onBack={showAccount=false},allowPatientRegistration=false,allowLiveCare=false) else {
 Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={TopAppBar(title={Text("FlowCare HQ", style=MaterialTheme.typography.titleMedium)},colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))},bottomBar={FlowCareDock(labels.zip(icons),tab){tab=it}}) { padding ->
 Crossfade(targetState=tab,label="navigation transition") { shownTab ->
 Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
 Text(when(shownTab){0->"HQ control";1->"Create an announcement";2->"App updates";else->"Connection & privacy"},style=MaterialTheme.typography.headlineMedium)
 Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {Text("Remote service not connected",style=MaterialTheme.typography.titleMedium);Text("Firebase and secure HQ authentication must be configured before announcements or controls can reach devices.",style=MaterialTheme.typography.bodyLarge)}}
 when(shownTab) {
 0->{Text("One place for your care network",style=MaterialTheme.typography.titleLarge);Text("Prepare announcements for patients and the hospital care team. HQ has no access to patient records.",style=MaterialTheme.typography.bodyLarge);Button(onClick={tab=1},modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)){Text("Prepare announcement")};OutlinedButton(onClick={tab=2},modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)){Text("Review app updates")}}
 1->{Text("Audience",style=MaterialTheme.typography.titleMedium);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Patients","Care team").forEach { a->FilterChip(selected=audience==a,onClick={audience=a},label={Text(a)}) }};OutlinedTextField(title,{title=it.take(100)},label={Text("Announcement title")},modifier=Modifier.fillMaxWidth());OutlinedTextField(body,{body=it.take(500)},label={Text("Message")},minLines=4,modifier=Modifier.fillMaxWidth());Button(onClick={preview=true},enabled=title.isNotBlank()&&body.isNotBlank(),modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)){Text("Preview announcement")};Text("Drafts exist only while this screen session is open. Do not include patient or medical information.")}
 2->{Text("Installed build: ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.titleLarge);Text("Update announcements will link to a published APK. Remote publishing and maintenance controls are unavailable until the service is connected.",style=MaterialTheme.typography.bodyLarge)}
 3->{Text("Patient records: no access",style=MaterialTheme.typography.titleLarge);Text("This app does not open the clinical data provider, save announcement drafts, or embed server credentials.",style=MaterialTheme.typography.bodyLarge);Text("Remote HQ controls and push delivery are not connected.",style=MaterialTheme.typography.bodyLarge);OutlinedButton(onClick={showAccount=true},modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)){Text("Manage a real account")}}
 }
 }
 }
 }
 if(preview)AlertDialog(onDismissRequest={preview=false},title={Text(title)},text={Column {Text("To: $audience");Spacer(Modifier.height(16.dp));Text(body);Spacer(Modifier.height(16.dp));Text("Preview only. Nothing has been sent.")}},confirmButton={TextButton(onClick={preview=false}){Text("Back to draft")}})
 }
 }
}

