package com.flowcare

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Binder
import android.os.Process
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal val DEMO_URI: Uri = Uri.parse("content://com.flowcare.demo.hospital.demo")
private fun obj(vararg pairs:Pair<String,Any?>)=JSONObject().apply {pairs.forEach {put(it.first,it.second)}}
internal fun JSONArray.rows():List<JSONObject> = (0 until length()).map {getJSONObject(it)}
internal fun JSONObject.rows(key:String):List<JSONObject> = optJSONArray(key)?.rows() ?: emptyList()

/** Signature-protected, on-device demo exchange. No network or real hospital integration. */
class HospitalDemoProvider:ContentProvider() {
 override fun onCreate()=true
 override fun call(method:String,arg:String?,extras:Bundle?):Bundle {
    val owner=requireNotNull(context)
    if(Binder.getCallingUid()!=Process.myUid())owner.enforceCallingPermission("com.flowcare.demo.permission.SHARED_DEMO","Only matching FlowCare demo apps can access this store")
    val engine=LocalDemoEngine(owner)
    val result=if(method=="state")engine.read() else engine.mutate(JSONObject(arg ?: "{}"))
    return Bundle().apply {putString("state",result.toString())}
 }
 override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor?=null
 override fun getType(uri:Uri)="application/json"
 override fun insert(uri:Uri,values:ContentValues?):Uri?=null
 override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?)=0
 override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?)=0
}
internal class DemoStore(private val context:Context) {
 private val engine=LocalDemoEngine(context)
 fun shared():Boolean=try {context.packageManager.resolveContentProvider(DEMO_URI.authority!!,0)!=null} catch(_:Exception){false}
 fun read():JSONObject=try {context.contentResolver.call(DEMO_URI,"state",null,null)?.getString("state")?.let(::JSONObject) ?: engine.read()}catch(_:Exception){engine.read()}
 fun mutate(event:JSONObject):JSONObject=try {context.contentResolver.call(DEMO_URI,"mutate",event.toString(),null)?.getString("state")?.let(::JSONObject) ?: engine.mutate(event)}catch(_:Exception){engine.mutate(event)}
}
private class LocalDemoEngine(private val context:Context) {
 companion object {private val lock=Any()}
 private val prefs=context.getSharedPreferences("flowcare_demo_v3",Context.MODE_PRIVATE)
 private fun notifySharedChange() {try {context.contentResolver.notifyChange(DEMO_URI,null)} catch (_:SecurityException) {} catch (_:IllegalArgumentException) {}}
 private fun seed()=obj("patients" to JSONArray().put(obj("id" to "self","name" to "Maya Mathew","relation" to "Self","age" to "34")).put(obj("id" to "parent","name" to "Mary Mathew","relation" to "Mother","age" to "68")),"appointments" to JSONArray(),"bills" to JSONArray(),"prescriptions" to JSONArray(),"paused" to false,"pausedDoctors" to JSONObject(),"notice" to "Welcome to St. Mathew Hospital")
 fun read():JSONObject=synchronized(lock){JSONObject(prefs.getString("state",null) ?: seed().toString())}
 fun mutate(event:JSONObject):JSONObject=synchronized(lock){
    val state=read()
    val type=event.optString("type")
    val id=event.optString("id")
    val visit=state.rows("appointments").find {it.optString("id")==id}
    when(type){
      "book" -> {
        val appointments=state.getJSONArray("appointments")
        if(appointments.rows().any{it.optString("status") in listOf("confirmed","waiting","active")&&it.optString("doctor")==event.optString("doctor")&&it.optString("date")==event.optString("date")&&it.optString("time")==event.optString("time")}){
          state.put("notice","That demo time is already booked. Choose another time.")
          prefs.edit().putString("state",state.toString()).commit()
          return@synchronized state
        }
        val fee=CareCatalog.doctors.firstOrNull{it.name==event.optString("doctor")}?.fee ?: 500
        val patient=state.rows("patients").find {it.getString("id")==event.optString("patient")} ?: state.getJSONArray("patients").getJSONObject(0)
        val key=UUID.randomUUID().toString().take(8)
        val appointment=obj("id" to key,"patient" to patient.getString("id"),"name" to patient.getString("name"),"doctor" to event.optString("doctor","Dr. Anya Sharma"),"department" to event.optString("department","General medicine"),"date" to event.optString("date","Today"),"time" to event.optString("time","09:30 AM"),"token" to (1001+appointments.length()),"status" to "confirmed","ahead" to 0)
        appointments.put(appointment)
        state.getJSONArray("bills").put(obj("id" to "registration-$key","visit" to key,"department" to "Registration","amount" to 100,"paid" to true))
        state.getJSONArray("bills").put(obj("id" to "consultation-$key","visit" to key,"department" to "Consultation","amount" to fee,"paid" to false))
        state.put("lastVisit",key);state.put("notice","Appointment confirmed")
      }
      "checkin" -> if(visit?.optString("status")=="confirmed"){visit.put("status","waiting");state.put("notice","Patient checked in")}
      "cancel" -> if(visit?.optString("status") in listOf("confirmed","waiting")){
        visit?.put("status","cancelled")
        state.put("bills",JSONArray(state.rows("bills").filter {it.optString("visit")!=id || it.optBoolean("paid")}));state.put("notice","Appointment cancelled")
      }
      "call" -> if(visit?.optString("status")=="waiting" && !(state.optJSONObject("pausedDoctors")?.optBoolean(visit.optString("doctor")) ?: state.optBoolean("paused"))){
        state.rows("appointments").filter {it.optString("doctor")==visit.optString("doctor")&&it.optString("status")=="active"}.forEach {it.put("status","complete")}
        visit.put("status","active");visit.put("ahead",0);state.put("notice","Doctor is ready for you")
      }
      "advance" -> if(visit!=null){visit.put("ahead",(visit.optInt("ahead")-1).coerceAtLeast(0));state.put("notice","Queue updated")}
      "pause" -> {
        val doctor=event.optString("doctor","Dr. Anya Sharma")
        val paused=state.optJSONObject("pausedDoctors") ?: JSONObject()
        paused.put(doctor,!paused.optBoolean(doctor));state.put("pausedDoctors",paused)
        state.put("notice",if(paused.optBoolean(doctor))"Doctor attending an emergency" else "Queue resumed")
      }
      "complete" -> if(visit?.optString("status")=="active"){visit.put("status","complete");state.put("notice","Consultation completed")}
      "order" -> if(visit?.optString("status")=="active"){
        val department=event.optString("department","Blood test")
        if(state.rows("bills").none{it.optString("visit")==id&&it.optString("department")==department})state.getJSONArray("bills").put(obj("id" to UUID.randomUUID().toString().take(8),"visit" to id,"department" to department,"amount" to if(department=="Pharmacy")250 else 450,"paid" to false,"ready" to false))
        state.put("notice","Order added to patient bills")
      }
      "prescribe" -> if(visit?.optString("status")=="active"){
        state.put("prescriptions",JSONArray(state.rows("prescriptions").filter{it.optString("visit")!=id}).put(obj("visit" to id,"doctor" to visit.optString("doctor"),"text" to event.optString("text"),"patient" to visit.optString("name"))))
        state.put("notice","Prescription shared")
      }
      "pay" -> {state.rows("bills").find{it.optString("id")==id}?.put("paid",true);state.put("notice","Demo payment completed — no money charged")}
      "ready" -> {state.rows("bills").find{it.optString("id")==id}?.put("ready",true);state.put("notice","Order marked ready")}
      "family" -> if(event.optString("name").isNotBlank()){
        state.getJSONArray("patients").put(obj("id" to UUID.randomUUID().toString().take(8),"name" to event.optString("name").trim(),"relation" to event.optString("relation","Family"),"age" to event.optString("age")));state.put("notice","Family member added")
      }
      "review" -> if(visit?.optString("status")=="complete"){visit.put("review",event.optInt("stars",5).coerceIn(1,5));state.put("notice","Thank you for your feedback")}
      "reset" -> {prefs.edit().remove("state").commit();notifySharedChange();return@synchronized seed()}
    }
    if(type in listOf("book","checkin","cancel","call","complete")){
      state.rows("appointments").filter{it.optString("status") in listOf("waiting","active")}.groupBy{it.optString("doctor")}.values.forEach{queue->
        val ordered=queue.sortedBy{if(it.optString("status")=="active")0 else 1}
        ordered.forEachIndexed{i,v->v.put("ahead",i)}
      }
    }
    prefs.edit().putString("state",state.toString()).commit()
    notifySharedChange()
    state
 }
}

