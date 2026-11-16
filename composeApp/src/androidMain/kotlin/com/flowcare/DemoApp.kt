package com.flowcare

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.database.ContentObserver
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowcare.resources.Res
import com.flowcare.resources.jakarta
import org.jetbrains.compose.resources.Font
import org.json.JSONObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset

private val Blue=Color(0xFF2454C6)
private val Ink=Color(0xFF162E58)
private val Cream=Color(0xFFFFF8E8)
private val Paper=Color(0xFFFFFDF5)
private fun event(type:String,vararg values:Pair<String,Any>)=JSONObject().put("type",type).apply{values.forEach{put(it.first,it.second)}}
private data class Destination(val label:String,val icon:ImageVector)

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun DemoApp(appRole:String,onThemeChanged:(Boolean)->Unit={}) {
 if(appRole=="hq"){HqApp();return}
 var workspace by rememberSaveable {mutableStateOf("hospital")}
 val role=if(appRole=="hospital")workspace else appRole
 val context=LocalContext.current
 val prefs=remember {context.getSharedPreferences("flowcare_account",0)}
 val store=remember {DemoStore(context)}
 var data by remember {mutableStateOf(store.read())}
 var logged by rememberSaveable {mutableStateOf(prefs.getBoolean("logged",false))}
 var signup by rememberSaveable {mutableStateOf(false)}
 var email by rememberSaveable {mutableStateOf(when(role){"doctor"->"doctor@flowcare.demo";"hospital"->"desk@stmathew.demo";else->"maya@flowcare.demo"})}
 var password by remember {mutableStateOf("")}
 var loginName by rememberSaveable {mutableStateOf("")}
 var loginError by remember {mutableStateOf("")}
 var tab by rememberSaveable {mutableIntStateOf(0)}
 var page by rememberSaveable {mutableStateOf("")}
 var selected by rememberSaveable {mutableStateOf("")}
 var patientId by rememberSaveable {mutableStateOf("self")}
 var doctor by rememberSaveable {mutableStateOf("Dr. Anya Sharma")}
 var department by rememberSaveable {mutableStateOf("General medicine")}
 var date by rememberSaveable {mutableStateOf(LocalDate.now().toString())}
 var time by rememberSaveable {mutableStateOf("09:30 AM")}
 var showDate by remember {mutableStateOf(false)}
 var menu by remember {mutableStateOf(false)}
 var cancelConfirm by remember {mutableStateOf(false)}
 var familyName by remember {mutableStateOf("")}
 var familyRelation by remember {mutableStateOf("Family")}
 var familyAge by remember {mutableStateOf("")}
 var carePlan by remember(selected) {mutableStateOf("")}
 var careQuery by rememberSaveable {mutableStateOf("")}
 var careFilter by rememberSaveable {mutableStateOf("All")}
 var browseDepartment by rememberSaveable {mutableStateOf("general")}
 var browseDoctor by rememberSaveable {mutableStateOf("anya")}
 var departmentParent by rememberSaveable {mutableStateOf("")}
 var doctorParent by rememberSaveable {mutableStateOf("doctors")}
 var doctorListParent by rememberSaveable {mutableStateOf("")}
 var patientSearch by rememberSaveable {mutableStateOf("")}
 var clinicianId by rememberSaveable {mutableStateOf(prefs.getString("clinician","anya") ?: "anya")}
 val clinician=CareCatalog.doctor(clinicianId)
 fun pausedFor(name:String)=data.optJSONObject("pausedDoctors")?.optBoolean(name) ?: data.optBoolean("paused")
 fun openDepartment(d:CareDepartment,parent:String=""){browseDepartment=d.id;departmentParent=parent;page="department"}
 fun openDoctor(d:CareDoctor,parent:String="doctors"){browseDoctor=d.id;doctorParent=parent;page="doctor-detail"}
 fun back(){if(page.isNotEmpty())page=when(page){"book-doctor"->"book-hospital";"book-time"->"book-doctor";"book-review"->"book-time";"queue","prescription","consultation"->"visit";"doctor-detail"->doctorParent;"department"->departmentParent;"doctors"->doctorListParent;else->""}else tab=0}
 var dark by rememberSaveable {mutableStateOf(prefs.getBoolean("dark",false))}
 val snack=remember {SnackbarHostState()};val scope=rememberCoroutineScope()
 val listState=rememberLazyListState()
 LaunchedEffect(page,tab,patientId,clinicianId){if(logged)listState.scrollToItem(0)}
 fun notify(message:String){scope.launch{snack.showSnackbar(message)}}
 fun act(e:JSONObject){data=store.mutate(e);notify(data.optString("notice"))}
 fun signIn(){logged=true;password="";loginError="";prefs.edit().putBoolean("logged",true).putString("email",email).apply()}
 DisposableEffect(store){
  val observer=object:ContentObserver(Handler(Looper.getMainLooper())){override fun onChange(selfChange:Boolean){data=store.read()}}
  context.contentResolver.registerContentObserver(DEMO_URI,true,observer)
  onDispose {context.contentResolver.unregisterContentObserver(observer)}
 }
 SideEffect{onThemeChanged(dark)}
 BackHandler(!logged&&signup){signup=false;loginError=""}
 BackHandler(logged&&(page.isNotEmpty()||tab!=0)){back()}
 val colors=if(dark) darkColorScheme(primary=Color(0xFFABC5FF),onPrimary=Ink,background=Color(0xFF111D32),surface=Color(0xFF1B2B46),onSurface=Cream,onBackground=Cream,onSurfaceVariant=Color(0xFFD5DFEF),secondaryContainer=Color(0xFF2D4777),onSecondaryContainer=Cream,outline=Color(0xFFABC5FF),outlineVariant=Color(0xFF6D83AC)) else lightColorScheme(primary=Blue,onPrimary=Color.White,background=Cream,surface=Paper,onSurface=Ink,onBackground=Ink,onSurfaceVariant=Color(0xFF41516C),secondaryContainer=Color(0xFFE1EAFE),onSecondaryContainer=Ink,outline=Ink,outlineVariant=Color(0xFFA2ADC0),surfaceTint=Blue)
 val font=FontFamily(Font(Res.font.jakarta))
 val type=Typography(bodyLarge=Typography().bodyLarge.copy(fontFamily=font,fontSize=18.sp,lineHeight=28.sp),bodyMedium=Typography().bodyMedium.copy(fontFamily=font,fontSize=17.sp,lineHeight=26.sp),titleLarge=Typography().titleLarge.copy(fontFamily=font,fontSize=24.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold),titleMedium=Typography().titleMedium.copy(fontFamily=font,fontSize=19.sp,lineHeight=27.sp,fontWeight=FontWeight.SemiBold),titleSmall=Typography().titleSmall.copy(fontFamily=font,fontSize=17.sp,lineHeight=25.sp,fontWeight=FontWeight.SemiBold),headlineMedium=Typography().headlineMedium.copy(fontFamily=font,fontSize=28.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold),labelLarge=Typography().labelLarge.copy(fontFamily=font,fontSize=16.sp,lineHeight=24.sp))
 val destinations=when(role){"doctor"->listOf(Destination("Queue",Icons.Default.Groups),Destination("Patients",Icons.Default.People),Destination("Orders",Icons.Default.Assignment),Destination("Profile",Icons.Default.Person));"hospital"->listOf(Destination("Overview",Icons.Default.Dashboard),Destination("Check-in",Icons.Default.QrCodeScanner),Destination("Orders",Icons.Default.ReceiptLong),Destination("Profile",Icons.Default.Person));else->listOf(Destination("Home",Icons.Default.Home),Destination("Visits",Icons.Default.CalendarMonth),Destination("Bills",Icons.Default.ReceiptLong),Destination("Profile",Icons.Default.Person))}
 val patients=data.rows("patients")
 val patient=patients.find{it.optString("id")==patientId} ?: patients.first()
 val visits=data.rows("appointments")
 val myVisits=visits.filter{it.optString("patient")==patientId}
 val assignedVisits=if(role=="doctor")visits.filter{it.optString("doctor")==clinician.name} else visits
 val selectedDoctor=CareCatalog.doctors.firstOrNull{it.name==doctor} ?: CareCatalog.doctors.first()
 val catalogDepartment=CareCatalog.department(browseDepartment)
 val catalogDoctor=CareCatalog.doctor(browseDoctor)
 val assignedIds=assignedVisits.map{it.optString("id")}
 val visit=visits.find{it.optString("id")==selected}
 val current=myVisits.firstOrNull{it.optString("status") !in listOf("complete","cancelled")}
 val appName=when(role){"doctor"->"FlowCare Doctor";"hospital"->"St. Mathew Hospital";else->"FlowCare"}
 MaterialTheme(colorScheme=colors,typography=type,shapes=Shapes(small=RoundedCornerShape(16.dp),medium=RoundedCornerShape(22.dp),large=RoundedCornerShape(26.dp))){
  if(!logged){
   Surface(Modifier.fillMaxSize(),color=colors.background){
    Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
     Spacer(Modifier.height(16.dp));Image(painterResource(R.drawable.ic_launcher),contentDescription="$appName icon",modifier=Modifier.size(84.dp))
     Text(if(signup)"Set up your demo profile" else "Welcome to $appName",style=type.headlineMedium)
     Text("St. Mathew Hospital",style=type.titleMedium)
     Text("Offline demo. No real account or internet connection is needed.",style=type.bodyMedium)
     if(signup)OutlinedTextField(loginName,{loginName=it},label={Text("Full name")},singleLine=true,modifier=Modifier.fillMaxWidth())
     OutlinedTextField(email,{email=it},label={Text("Email address")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),singleLine=true,modifier=Modifier.fillMaxWidth())
     OutlinedTextField(password,{password=it},label={Text("Demo password")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
     if(loginError.isNotBlank())Text(loginError,color=colors.error)
     Action(if(signup)"Create demo profile" else "Sign in to demo",primary=true){if(!email.contains("@")||password.length<4)loginError="Enter a valid email and a demo password of at least 4 characters." else {if(signup&&loginName.isNotBlank())prefs.edit().putString("name",loginName).apply();signIn()}}
     Action("Use demo account"){signIn()}
     TextButton(onClick={signup=!signup;loginError=""}){Text(if(signup)"Already have a demo profile? Sign in" else "Create a demo profile")}
     Text("Do not enter a real password or medical information. This demo validates the form locally; it does not authenticate a real account.",style=type.bodyMedium)
    }
   }
  } else Scaffold(
   containerColor=colors.background,
   topBar={TopAppBar(title={Text(if(page.isBlank())appName else when(page){"book-hospital"->"Choose your care";"book-doctor"->"Choose a doctor";"book-time"->"Choose a time";"book-review"->"Review your visit";"visit"->"Visit details";"queue"->"Live queue";"family"->"Family members";"add-family"->"Add family member";"consultation"->"Consultation";"prescription"->"Care instructions";"success"->"Booking confirmed";"help"->"Help & support";"search"->"Find your care";"departments"->"Care departments";"department"->catalogDepartment.name;"doctors"->"Our doctors";"doctor-detail"->"Doctor profile";"clinician-switch"->"Demo clinician";else->"Settings"},style=type.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)},navigationIcon={if(page.isNotBlank())IconButton(onClick={back()}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}},actions={
     IconButton(onClick={notify(data.optString("notice","No new updates"))}){Icon(Icons.Default.NotificationsNone,"Notifications")}
     Box {IconButton(onClick={menu=true}){Icon(Icons.Default.MoreVert,"More options")};DropdownMenu(menu,{menu=false}){
      if(appRole=="hospital")DropdownMenuItem(text={Text(if(role=="hospital")"Doctor workspace" else "Hospital workspace")},onClick={workspace=if(role=="hospital")"doctor" else "hospital";menu=false;page="";tab=0})
      DropdownMenuItem(text={Text("Settings")},onClick={menu=false;page="settings"})
      DropdownMenuItem(text={Text("Help & support")},onClick={menu=false;page="help"})
      DropdownMenuItem(text={Text("Sign out")},onClick={menu=false;logged=false;page="";tab=0;prefs.edit().putBoolean("logged",false).apply()})
     }}
   },colors=TopAppBarDefaults.topAppBarColors(containerColor=colors.background))},
   bottomBar={if(page.isBlank())NavigationBar(containerColor=colors.surface,tonalElevation=0.dp){destinations.forEachIndexed{i,d->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(d.icon,null)},label={Text(d.label)},alwaysShowLabel=true)}}},
   snackbarHost={SnackbarHost(snack)}
  ){padding->
   LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),state=listState,contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
    item {Column(verticalArrangement=Arrangement.spacedBy(18.dp)){
     if(page.isBlank()) {
      when {
       tab==3 -> {
        Image(painterResource(R.drawable.ic_launcher),contentDescription=null,modifier=Modifier.size(72.dp))
        Heading(when(role){"doctor"->clinician.name;"hospital"->"Hospital front desk";else->prefs.getString("name",null) ?: patient.optString("name")})
        Text(prefs.getString("email",email) ?: email)
        Info("St. Mathew Hospital","Fictional hospital · offline demo")
        if(role=="doctor")MenuRow("Demo clinician",clinician.specialty,Icons.Default.Badge){page="clinician-switch"}
        if(role=="patient")MenuRow("Family members","Manage who you book and pay for",Icons.Default.People){page="family"}
        MenuRow("Settings","Appearance and demo data",Icons.Default.Settings){page="settings"}
        MenuRow("Help & support","Understand the offline demo",Icons.Default.HelpOutline){page="help"}
        Text(if(store.shared())"Shared demo data is available on this device." else "Local demo data. Install the Hospital app on this device to share visits across all three apps.",style=type.bodyMedium)
        Action("Sign out"){logged=false;page="";tab=0;prefs.edit().putBoolean("logged",false).apply()}
       }
       role=="patient" && tab==0 -> {
        Heading("Hello, ${patient.optString("name").substringBefore(' ')}")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
         Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.LocationOn,null,tint=colors.primary,modifier=Modifier.size(18.dp));Text("St. Mathew Hospital",style=type.bodySmall)}
         TextButton(onClick={page="family"}){Text("Change patient")}
        }
        CareSearchEntry {careQuery="";careFilter="All";page="search"}
        VisitHero(current,onBook={page="book-hospital"},onVisit={selected=current?.optString("id") ?: "";page=if(current?.optString("status")=="confirmed")"visit" else "queue"})
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Care departments",style=type.titleMedium,modifier=Modifier.weight(1f));TextButton(onClick={page="departments"}){Text("See all")}}
        DepartmentTiles(CareCatalog.departments.take(4)){openDepartment(it)}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Meet your doctors",style=type.titleMedium,modifier=Modifier.weight(1f));TextButton(onClick={browseDepartment="all";doctorListParent="";page="doctors"}){Text("View all")}}
        CareCatalog.doctors.take(2).forEach{d->DoctorPreview(d){openDoctor(d,"")}}
        MenuRow("A moment of calm","An offline grounding exercise",Icons.Default.Spa){page="calm"}
       }
       role=="patient" && tab==1 -> {
        Heading("Your visits")
        Text("For ${patient.optString("name")}")
        Action("Book an appointment",primary=true){page="book-hospital"}
        if(myVisits.isEmpty())Info("No visits yet","Your appointments will appear here after booking.")
        myVisits.reversed().forEach {v->VisitCard(v){selected=v.optString("id");page="visit"}}
       }
       role=="patient" && tab==2 -> {
        Heading("Bills & receipts")
        val ids=myVisits.map{it.optString("id")}
        val bills=data.rows("bills").filter{it.optString("visit") in ids}
        val due=bills.filter{!it.optBoolean("paid")}.sumOf{it.optInt("amount")}
        Info("₹$due pending","For ${patient.optString("name")} · demo payments only")
        if(bills.isEmpty())Text("No bills yet. Charges will appear after booking or a doctor's order.")
        bills.forEach{bill->BillCard(bill){act(event("pay","id" to bill.optString("id")))}}
       }
       role=="doctor" && tab==0 -> {
        Heading("Today's queue")
        Text("${clinician.name} · ${clinician.specialty}")
        Info(if(pausedFor(clinician.name))"Queue paused" else "Ready to see patients","${assignedVisits.count{it.optString("status")=="waiting"}} waiting · ${assignedVisits.count{it.optString("status")=="active"}} in consultation")
        Action(if(pausedFor(clinician.name))"Resume queue" else "Pause for an emergency"){act(event("pause","doctor" to clinician.name))}
        val queue=assignedVisits.filter{it.optString("status") in listOf("waiting","active")}
        if(queue.isEmpty())Info("No patients waiting","Visits for ${clinician.name} appear here. Choose another demo clinician from Profile to view their queue.")
        queue.forEach{v->VisitCard(v){selected=v.optString("id");page="visit"}}
       }
       role=="doctor" && tab==1 -> {
        Heading("Patients")
        OutlinedTextField(patientSearch,{patientSearch=it},label={Text("Search patient or token")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
        val results=assignedVisits.filter{patientSearch.isBlank()||it.optString("name").contains(patientSearch,true)||it.optInt("token").toString().contains(patientSearch)}
        if(results.isEmpty())Text("No matching patients for this demo clinician.")
        results.reversed().forEach{v->VisitCard(v){selected=v.optString("id");page="visit"}}
       }
       role=="hospital" && tab==0 -> {
        Heading("Hospital overview")
        Text("St. Mathew Hospital · offline front desk")
        Info("${visits.count{it.optString("status")!="cancelled"}} appointments","${visits.count{it.optString("status")=="waiting"}} waiting · ${visits.count{it.optString("status")=="complete"}} completed")
        Action("Check in a patient",primary=true){tab=1}
        MenuRow("Department orders","Labs, pharmacy and billing status",Icons.Default.Assignment){tab=2}
        Heading("Recent appointments",small=true)
        if(visits.isEmpty())Info("Ready for your first patient","Use the Patient app to book a demo visit on this device.")
        visits.reversed().take(5).forEach{v->VisitCard(v){selected=v.optString("id");page="visit"}}
       }
       role=="hospital" && tab==1 -> {
        Heading("Patient check-in")
        Text("Select a confirmed visit and verify its token with the patient's pass.")
        OutlinedTextField(patientSearch,{patientSearch=it},label={Text("Search patient or token")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
        val pending=visits.filter{it.optString("status")=="confirmed"&&(patientSearch.isBlank()||it.optString("name").contains(patientSearch,true)||it.optInt("token").toString().contains(patientSearch))}
        if(pending.isEmpty())Info("No arrivals to check in","Confirmed bookings appear here automatically when all three apps are installed on the same device.")
        pending.forEach{v->VisitCard(v){selected=v.optString("id");page="visit"}}
       }
       else -> {
        Heading(if(role=="hospital")"Department orders" else "Patient orders")
        val bills=data.rows("bills").filter{it.optString("department") !in listOf("Registration","Consultation")&&(role!="doctor"||it.optString("visit") in assignedIds)}
        if(bills.isEmpty())Info("No department orders","Lab and pharmacy requests appear here after a doctor's order.")
        bills.forEach{b->Info(b.optString("department"),"₹${b.optInt("amount")} · ${if(b.optBoolean("paid"))"Paid" else "Payment pending"}\n${if(b.optBoolean("ready"))"Ready for collection" else "Being prepared"}");if(role=="hospital"&&!b.optBoolean("ready"))Action("Mark ${b.optString("department")} ready"){act(event("ready","id" to b.optString("id")))}}
       }
      }
     } else when(page){
      "search" -> {
       Heading("Find the care you need")
       OutlinedTextField(careQuery,{careQuery=it},label={Text("Search doctors or departments")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(careQuery.isNotEmpty())IconButton(onClick={careQuery=""}){Icon(Icons.Default.Close,"Clear search")}},singleLine=true,modifier=Modifier.fillMaxWidth())
       FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){listOf("All","Departments","Doctors").forEach{filter->FilterChip(selected=careFilter==filter,onClick={careFilter=filter},label={Text(filter)},modifier=Modifier.heightIn(min=52.dp))}}
       val departmentResults=if(careFilter=="Doctors")emptyList() else CareCatalog.searchDepartments(careQuery)
       val doctorResults=if(careFilter=="Departments")emptyList() else CareCatalog.searchDoctors(careQuery)
       if(departmentResults.isEmpty()&&doctorResults.isEmpty())Info("No matches found","Try a department such as heart care, or a doctor's name.")
       if(departmentResults.isNotEmpty()){Heading("Departments",small=true);departmentResults.forEach{d->MenuRow(d.name,d.specialty,departmentIcon(d.id)){openDepartment(d,"search")}}}
       if(doctorResults.isNotEmpty()){Heading("Doctors",small=true);doctorResults.forEach{d->DoctorPreview(d){openDoctor(d,"search")}}}
      }
      "departments" -> {
       Heading("Care, by department")
       Text("Choose a clinic to see its services and fictional demo doctors.")
       DepartmentTiles(CareCatalog.departments){openDepartment(it,"departments")}
      }
      "department" -> {
       Surface(shape=RoundedCornerShape(24.dp),color=colors.secondaryContainer,modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Icon(departmentIcon(catalogDepartment.id),null,tint=colors.primary,modifier=Modifier.size(56.dp));Text(catalogDepartment.name,style=type.headlineMedium);Text(catalogDepartment.specialty,style=type.bodyMedium)}
       }
       Text(catalogDepartment.description)
       Heading("Care at this clinic",small=true)
       catalogDepartment.services.forEach{service->Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.CheckCircle,null,tint=colors.primary,modifier=Modifier.size(20.dp));Text(service,modifier=Modifier.weight(1f))}}
       Heading("Meet the team",small=true)
       CareCatalog.doctors.filter{it.departmentId==catalogDepartment.id}.forEach{d->DoctorPreview(d){openDoctor(d,"department")}}
       Action("View ${catalogDepartment.name.lowercase()} doctors",primary=true){browseDepartment=catalogDepartment.id;doctorListParent="department";page="doctors"}
       if(catalogDepartment.id=="mind")Action("Try a grounding exercise"){page="calm"}
       Text("Fictional department and clinician profiles for the offline demo.",style=type.bodySmall)
      }
      "doctors" -> {
       Heading("Find your doctor")
       LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){
        item{FilterChip(selected=browseDepartment=="all",onClick={browseDepartment="all"},label={Text("All doctors")},modifier=Modifier.heightIn(min=52.dp))}
        items(CareCatalog.departments.size){i->val d=CareCatalog.departments[i];FilterChip(selected=browseDepartment==d.id,onClick={browseDepartment=d.id},label={Text(d.name)},modifier=Modifier.heightIn(min=52.dp))}
       }
       CareCatalog.doctors.filter{browseDepartment=="all"||it.departmentId==browseDepartment}.forEach{d->DoctorPreview(d){openDoctor(d)}}
      }
      "doctor-detail" -> {
       Surface(shape=RoundedCornerShape(26.dp),color=colors.secondaryContainer,modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
         Surface(shape=RoundedCornerShape(26.dp),color=colors.primary){Box(Modifier.size(96.dp),contentAlignment=Alignment.Center){Text(catalogDoctor.initials,color=colors.onPrimary,style=type.headlineMedium)}}
         Text(catalogDoctor.name,style=type.titleLarge);Text(catalogDoctor.specialty,style=type.bodyLarge)
        }
       }
       Info("About this demo clinician","${catalogDoctor.years} years of sample experience\nSt. Mathew Hospital · ${CareCatalog.department(catalogDoctor.departmentId).name}")
       Info("Consultation fee","₹${catalogDoctor.fee} + ₹100 registration\nDemo charges only; no real payment is processed.")
       Action("Book ${catalogDoctor.name.removePrefix("Dr. ").substringBefore(' ')}",primary=true){doctor=catalogDoctor.name;department=catalogDoctor.specialty;page="book-hospital"}
       Text("All clinicians are fictional. Initials identify demo profiles; they are not verified medical providers.",style=type.bodySmall)
      }
      "clinician-switch" -> {
       Heading("Choose the demo clinician")
       Text("The Doctor app shows only this clinician's patients and orders.")
       CareCatalog.doctors.forEach{d->DoctorPreview(d,clinicianId==d.id){clinicianId=d.id;prefs.edit().putString("clinician",d.id).apply();page="";tab=0}}
      }

      "book-hospital" -> {
       Text("Step 1 of 3 · Hospital & doctor",style=type.labelLarge,color=colors.primary)
       Info("St. Mathew Hospital","Demo hospital · Main outpatient department")
       Text("Who is this visit for?",style=type.titleMedium)
       patients.forEach{p->Choice(p.optString("name"),p.optString("relation"),patientId==p.optString("id")){patientId=p.optString("id")}}
       Heading("Choose a department",small=true)
       FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){CareCatalog.departments.forEach{d->FilterChip(selected=selectedDoctor.departmentId==d.id,onClick={val first=CareCatalog.doctors.first{it.departmentId==d.id};doctor=first.name;department=first.specialty},label={Text(d.name)},modifier=Modifier.heightIn(min=54.dp))}}
       Action("Continue",primary=true){page="book-doctor"}
      }
      "book-doctor" -> {
       Text("Step 1 of 3 · Choose your doctor",style=type.labelLarge)
       CareCatalog.doctors.filter{it.departmentId==selectedDoctor.departmentId}.forEach{d->DoctorPreview(d,doctor==d.name){doctor=d.name;department=d.specialty}}
       Text("All doctors and profiles are fictional demo data.")
       Action("Choose appointment time",primary=true){page="book-time"}
      }
      "book-time" -> {
       Text("Step 2 of 3 · Date & time",style=type.labelLarge)
       Heading("When would you like to visit?")
       Action("Date: $date"){showDate=true}
       Text("Available demo times",style=type.titleMedium)
       FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        listOf("09:30 AM","10:00 AM","10:30 AM","11:00 AM","02:00 PM","03:00 PM").forEach{slot->FilterChip(selected=time==slot,onClick={time=slot},label={Text(slot)},modifier=Modifier.heightIn(min=56.dp))}
       }
       Action("Review appointment",primary=true){page="book-review"}
      }
      "book-review" -> {
       Text("Step 3 of 3 · Review & confirm",style=type.labelLarge)
       Info(patient.optString("name"),"$doctor\n$department\n$date · $time\nSt. Mathew Hospital")
       Info("Visit charges","Registration ₹100 · Consultation ₹${selectedDoctor.fee}\nTotal ₹${100+selectedDoctor.fee} · simulated charges only")
       Text("No money is charged. Registration is marked paid in the demo; consultation appears in Bills.")
       Action("Confirm demo booking",primary=true){val previous=data.optString("lastVisit");act(event("book","patient" to patientId,"doctor" to doctor,"department" to department,"date" to date,"time" to time));if(data.optString("lastVisit")!=previous){selected=data.optString("lastVisit");page="success"}}
      }
      "success" -> {
       Icon(Icons.Default.CheckCircle,null,tint=colors.primary,modifier=Modifier.size(60.dp))
       Heading("You're booked")
       Info("St. Mathew Hospital","$doctor\n$date · $time\nFor ${patient.optString("name")}")
       Text("Your pass and check-in details are ready.")
       Action("View appointment",primary=true){page="visit"}
       Action("Go to home"){page="";tab=0}
      }
      "visit" -> {
       if(visit==null)Info("Visit not found","Return to your visits to choose an appointment.") else {
        VisitSummary(visit)
        val status=visit.optString("status")
        if(status=="confirmed"){
         if(role!="doctor")Action("Check in",primary=true){act(event("checkin","id" to selected));if(role=="patient")page="queue"}
         Pass(visit)
        }
        if(status in listOf("waiting","active")){
         if(role=="doctor"&&status=="waiting")Action("Call patient",primary=true,enabled=!pausedFor(visit.optString("doctor"))){act(event("call","id" to selected));page="consultation"}
         if(role=="doctor"&&status=="active")Action("Open consultation",primary=true){page="consultation"}
         if(role=="patient")Action("View live queue",primary=true){page="queue"}
        }
        if(status=="complete"){
         Action("View care instructions",primary=true){page="prescription"}
         if(role=="patient")FlowRow{(1..5).forEach{stars->TextButton(onClick={act(event("review","id" to selected,"stars" to stars))}){Text("$stars / 5")}}}
        }
        if(role=="patient")Action("View bills"){page="";tab=2}
        if(status in listOf("confirmed","waiting")&&role!="doctor")Action("Cancel appointment"){cancelConfirm=true}
       }
      }
      "queue" -> if(visit!=null){
       Heading(when(visit.optString("status")){"active"->"It's your turn";"complete"->"Visit complete";else->"${visit.optInt("ahead")} patients before you"})
       Info("Token #${visit.optInt("token")}",if(pausedFor(visit.optString("doctor")))"Doctor attending an emergency. Your wait estimate is paused." else if(visit.optString("status")=="complete")"Your consultation is complete." else if(visit.optString("status")=="active")"Please proceed to the consultation room." else "About ${visit.optInt("ahead")*7} minutes · an estimate, not a guarantee")
       Text("${visit.optString("doctor")}\nSt. Mathew Hospital · Outpatient room 2")
       Info("You can wait nearby","Keep the app open for updates. Background arrival notifications are not enabled in this demo.")
       Action("View appointment"){page="visit"}
       if(!store.shared())Action("Demo: move queue forward"){act(event("advance","id" to selected))}
      }
      "consultation" -> if(visit!=null){
       VisitSummary(visit)
       Action("Order blood test"){act(event("order","id" to selected,"department" to "Blood test"))}
       Action("Order pharmacy items"){act(event("order","id" to selected,"department" to "Pharmacy"))}
       OutlinedTextField(carePlan,{carePlan=it},label={Text("Demo care instructions")},supportingText={Text("Fictional notes only. Do not enter real medical information.")},minLines=4,modifier=Modifier.fillMaxWidth())
       Action("Share care instructions",primary=true,enabled=carePlan.isNotBlank()){act(event("prescribe","id" to selected,"text" to carePlan.trim()));carePlan=""}
       Action("Complete consultation"){act(event("complete","id" to selected));page="";tab=0}
      }
      "prescription" -> {
       Heading("After your visit")
       val rx=data.rows("prescriptions").find{it.optString("visit")==selected}
       if(rx==null)Info("No instructions yet","Your clinician's demo notes will appear here when shared.") else Info(rx.optString("doctor"),rx.optString("text"))
       if(role=="patient")Action("View bills"){page="";tab=2}
      }
      "family" -> {
       Heading("Who are you caring for?")
       patients.forEach{p->Choice(p.optString("name"),"${p.optString("relation")} · ${p.optString("age")} years",patientId==p.optString("id")){patientId=p.optString("id");page="";tab=0}}
       Action("Add family member",primary=true){page="add-family"}
      }
      "add-family" -> {
       OutlinedTextField(familyName,{familyName=it},label={Text("Full name")},modifier=Modifier.fillMaxWidth())
       OutlinedTextField(familyRelation,{familyRelation=it},label={Text("Relationship")},modifier=Modifier.fillMaxWidth())
       OutlinedTextField(familyAge,{familyAge=it.filter(Char::isDigit).take(3)},label={Text("Age")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
       Action("Save family member",primary=true,enabled=familyName.isNotBlank()){act(event("family","name" to familyName,"relation" to familyRelation,"age" to familyAge));familyName="";page="family"}
      }
      "settings" -> {
       Heading("Make yourself comfortable")
       Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Dark appearance",modifier=Modifier.weight(1f));Switch(checked=dark,onCheckedChange={dark=it;prefs.edit().putBoolean("dark",it).apply()})}
       Info("Text size","This app follows Android's text-size setting. Use your device Accessibility settings for larger text.")
       Info("Offline, on this device",if(store.shared())"All three signed demo apps share local hospital data through the Hospital app. No server is involved." else "This app works on its own. Install the Hospital app to share visits with the other demo apps on this device.")
       Action("Reset demo data"){act(event("reset"));selected="";patientId="self"}
      }
      "calm" -> {
       Heading("Take a quiet moment")
       Info("A simple grounding exercise","Notice five things you can see.\nFour things you can feel.\nThree things you can hear.\nTake a gentle breath.")
       Text("This is an offline exercise, not an AI chat or a clinical assessment. For urgent help, contact local emergency services or a trusted person.")
       Action("Return home",primary=true){page="";tab=0}
      }
      else -> {
       Heading("About this demo")
       Info("St. Mathew Hospital","A fictional hospital for demonstrating connected care. All patient and staff profiles are sample data.")
       Text("Install Patient, Doctor and Hospital apps from the same release on one device to demonstrate the complete journey. The Hospital app keeps shared data on this device; each app also works independently.")
       Text("Sign-in and payments are simulated. There is no live hospital connection, online consultation, medical advice service or emergency dispatch.")
       Text("For a real emergency, contact local emergency services.")
      }
     }
     Spacer(Modifier.height(12.dp))
    }}
   }
  }
  if(showDate){val dateState=rememberDatePickerState(initialSelectedDateMillis=LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());DatePickerDialog(onDismissRequest={showDate=false},confirmButton={TextButton(onClick={dateState.selectedDateMillis?.let{date=Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()};showDate=false}){Text("Choose date")}},dismissButton={TextButton(onClick={showDate=false}){Text("Cancel")}}){DatePicker(state=dateState)}}
  if(cancelConfirm)AlertDialog(onDismissRequest={cancelConfirm=false},title={Text("Cancel this appointment?")},text={Text("The demo booking will be cancelled and unpaid charges removed.")},confirmButton={TextButton(onClick={act(event("cancel","id" to selected));cancelConfirm=false;page="";tab=1}){Text("Cancel appointment")}},dismissButton={TextButton(onClick={cancelConfirm=false}){Text("Keep appointment")}})
 }
}
@Composable private fun Heading(text:String,small:Boolean=false){Text(text,style=if(small)MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics{heading()})}
@Composable private fun Action(label:String,primary:Boolean=false,enabled:Boolean=true,onClick:()->Unit){
 val c=MaterialTheme.colorScheme
 Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=60.dp),shape=RoundedCornerShape(18.dp),border=BorderStroke(2.dp,if(enabled)c.onSurface else c.outlineVariant),colors=ButtonDefaults.buttonColors(containerColor=if(primary)c.primary else c.surface,contentColor=if(primary)c.onPrimary else c.onSurface),contentPadding=PaddingValues(18.dp)) {Text(label,style=MaterialTheme.typography.titleSmall)}
}
@Composable private fun Info(title:String,description:String){Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable private fun MenuRow(title:String,subtitle:String,icon:ImageVector,onClick:()->Unit){Surface(onClick=onClick,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.bodyMedium)};Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,null)}}}
@Composable private fun Choice(title:String,subtitle:String,selected:Boolean,onClick:()->Unit){Surface(onClick=onClick,shape=RoundedCornerShape(18.dp),color=if(selected)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,border=BorderStroke(if(selected)2.dp else 1.dp,MaterialTheme.colorScheme.outline),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodyMedium)};RadioButton(selected,onClick=null)}}}
@Composable private fun VisitCard(v:JSONObject,onClick:()->Unit){MenuRow(v.optString("name"),"${v.optString("doctor")}\n${v.optString("date")} · ${v.optString("time")}\n${v.optString("status").replaceFirstChar{it.uppercase()}} · #${v.optInt("token")}",Icons.Default.CalendarMonth,onClick)}
@Composable private fun VisitSummary(v:JSONObject){Heading(v.optString("name"));Info(v.optString("doctor"),"${v.optString("department")}\n${v.optString("date")} · ${v.optString("time")}\nSt. Mathew Hospital\n${v.optString("status").replaceFirstChar{it.uppercase()}} · Token #${v.optInt("token")}")}
@Composable private fun BillCard(b:JSONObject,onPay:()->Unit){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Info(b.optString("department"),"₹${b.optInt("amount")} · ${if(b.optBoolean("paid"))"Paid" else "Payment pending"}${if(b.optBoolean("ready"))" · Ready for collection" else ""}");if(!b.optBoolean("paid"))Action("Pay ₹${b.optInt("amount")} in demo",primary=true,onClick=onPay)}}
@Composable private fun Pass(v:JSONObject){val image=remember(v.optString("id")){val bits=MultiFormatWriter().encode("flowcare-demo:${v.optString("id")}",BarcodeFormat.QR_CODE,256,256);Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888).apply{for(y in 0..255)for(x in 0..255)setPixel(x,y,if(bits[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE)}.asImageBitmap()};Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){Image(image,"Demo appointment QR pass",modifier=Modifier.size(180.dp).background(Color.White));Text("Pass ${v.optString("id")} · Token #${v.optInt("token")}",style=MaterialTheme.typography.bodyMedium)}}
