package com.flowcare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

internal fun departmentIcon(id:String):ImageVector=when(id){"heart"->Icons.Default.FavoriteBorder;"bones"->Icons.Default.AccessibilityNew;"children"->Icons.Default.ChildCare;"mind"->Icons.Default.Spa;else->Icons.Default.MedicalServices}
@Composable internal fun CareSearchEntry(onClick:()->Unit){
 val c=MaterialTheme.colorScheme
 Surface(onClick=onClick,color=c.surface,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,c.outlineVariant),modifier=Modifier.fillMaxWidth()){
  Row(Modifier.padding(horizontal=18.dp,vertical=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Search,null,tint=c.primary);Text("Search doctors or departments",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f));Icon(Icons.Default.Tune,null,tint=c.primary)}
 }
}
@Composable internal fun DepartmentTiles(items:List<CareDepartment>,onSelect:(CareDepartment)->Unit){
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  items.chunked(2).forEach{pair->Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   pair.forEach{d->Surface(onClick={onSelect(d)},modifier=Modifier.weight(1f).fillMaxHeight(),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
    Column(Modifier.padding(18.dp).heightIn(min=128.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
     Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.secondaryContainer){Icon(departmentIcon(d.id),null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(12.dp).size(28.dp))}
     Text(d.name,style=MaterialTheme.typography.titleSmall)
     Row(verticalAlignment=Alignment.CenterVertically){Text("View care",style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f));Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,null,modifier=Modifier.size(18.dp))}
    }
   }}
   if(pair.size==1)Spacer(Modifier.weight(1f))
  }}
 }
}
@Composable internal fun DoctorPreview(d:CareDoctor,selected:Boolean=false,onClick:()->Unit){
 val c=MaterialTheme.colorScheme
 Surface(onClick=onClick,color=if(selected)c.secondaryContainer else c.surface,shape=RoundedCornerShape(22.dp),border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)c.primary else c.outlineVariant),modifier=Modifier.fillMaxWidth()){
  Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
   Surface(shape=RoundedCornerShape(18.dp),color=c.primary){Box(Modifier.size(64.dp),contentAlignment=Alignment.Center){Text(d.initials,color=c.onPrimary,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(d.name,style=MaterialTheme.typography.titleMedium);Text(d.specialty,style=MaterialTheme.typography.bodyMedium,color=c.onSurfaceVariant);Text("${d.years} years · ₹${d.fee} demo visit",style=MaterialTheme.typography.bodySmall,color=c.onSurfaceVariant)}
   if(selected)Icon(Icons.Default.CheckCircle,null,tint=c.primary) else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,null)
  }
 }
}
@Composable internal fun VisitHero(current:JSONObject?,onBook:()->Unit,onVisit:()->Unit){
 val c=MaterialTheme.colorScheme
 Surface(color=c.primary,contentColor=c.onPrimary,shape=RoundedCornerShape(26.dp),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
   Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){
     Text(if(current==null)"Your care,\none clear step." else when(current.optString("status")){"active"->"The doctor is ready";"waiting"->"You're checked in";else->"Your next visit"},style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     Text(if(current==null)"Find the right department and plan your visit at St. Mathew Hospital." else "${current.optString("doctor")}\n${current.optString("date")} · ${current.optString("time")}",style=MaterialTheme.typography.bodyMedium)
    }
    Icon(if(current==null)Icons.Default.EventAvailable else Icons.Default.CalendarMonth,null,modifier=Modifier.size(48.dp))
   }
   Button(onClick=if(current==null)onBook else onVisit,modifier=Modifier.fillMaxWidth().heightIn(min=58.dp),colors=ButtonDefaults.buttonColors(containerColor=c.background,contentColor=c.primary),shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(16.dp)){
    Text(if(current==null)"Book an appointment" else if(current.optString("status")=="confirmed")"View appointment" else "View live queue",style=MaterialTheme.typography.titleSmall)
   }
  }
 }
}
