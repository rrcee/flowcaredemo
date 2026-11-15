package com.flowcare
import kotlin.test.*
class CareCatalogTest {
 @Test fun searchFindsLayAndClinicalTerms(){
  assertEquals("neha",CareCatalog.searchDoctors("  HEART  ").single().id)
  assertEquals("heart",CareCatalog.searchDepartments("cardiology").single().id)
  assertEquals("anya",CareCatalog.searchDoctors("anya general").single().id)
  assertEquals("sara",CareCatalog.searchDoctors("children").single().id)
 }
 @Test fun noMatchHasAnEmptyResult(){assertTrue(CareCatalog.searchDoctors("nonexistent clinic").isEmpty());assertTrue(CareCatalog.searchDepartments("zzzz").isEmpty())}
 @Test fun everyDoctorHasABookableDepartment(){CareCatalog.doctors.forEach{d->assertTrue(CareCatalog.departments.any{it.id==d.departmentId});assertTrue(d.fee>0)}}
}
