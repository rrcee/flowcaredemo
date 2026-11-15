package com.flowcare

data class CareDepartment(val id:String,val name:String,val specialty:String,val description:String,val services:List<String>)
data class CareDoctor(val id:String,val name:String,val departmentId:String,val specialty:String,val years:Int,val fee:Int) {
 val initials:String get()=name.removePrefix("Dr. ").split(' ').take(2).joinToString(""){it.take(1)}
}
object CareCatalog {
 val departments=listOf(
  CareDepartment("general","General medicine","General physician","A place to start for everyday health questions and follow-up visits.",listOf("General consultations","Routine check-ups","Follow-up care")),
  CareDepartment("heart","Heart care","Cardiology","Explore the hospital's fictional heart-care clinic and its consultation options.",listOf("Cardiology consultations","Heart-health follow-ups","Review of existing reports")),
  CareDepartment("bones","Bones & joints","Orthopedics","Find consultation options for bone, joint and movement-related concerns.",listOf("Joint consultations","Mobility follow-ups","Orthopedic reviews")),
  CareDepartment("children","Child health","Pediatrics","A dedicated demo clinic for children's check-ups and follow-up visits.",listOf("Child health consultations","Growth check-ups","Follow-up care")),
  CareDepartment("mind","Mental wellbeing","Psychiatry","Explore a fictional mental-health clinic, or use the offline grounding exercise.",listOf("Psychiatry consultations","Follow-up appointments","Grounding exercise"))
 )
 val doctors=listOf(
  CareDoctor("anya","Dr. Anya Sharma","general","General medicine",14,500),
  CareDoctor("neha","Dr. Neha Joseph","heart","Cardiology",12,700),
  CareDoctor("arjun","Dr. Arjun Menon","bones","Orthopedics",10,650),
  CareDoctor("sara","Dr. Sara Thomas","children","Pediatrics",9,550),
  CareDoctor("vikram","Dr. Vikram Nair","mind","Psychiatry",11,600)
 )
 fun department(id:String)=departments.firstOrNull{it.id==id} ?: departments.first()
 fun doctor(id:String)=doctors.firstOrNull{it.id==id} ?: doctors.first()
 private fun matches(text:String,query:String)=query.trim().lowercase().split(Regex("\\s+")).filter{it.isNotEmpty()}.all{text.lowercase().contains(it)}
 fun searchDepartments(query:String)=departments.filter{matches("${it.name} ${it.specialty} ${it.description}",query)}
 fun searchDoctors(query:String)=doctors.filter{matches("doctor ${it.name} ${it.specialty} ${department(it.departmentId).name} ${department(it.departmentId).description}",query)}
}
