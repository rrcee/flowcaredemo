'use strict';
// Run only with the Firebase emulators and a demo project, never production.
const assert = require('node:assert/strict');
const {initializeApp} = require('firebase-admin/app');
const {getAuth} = require('firebase-admin/auth');
const {getFirestore, Timestamp} = require('firebase-admin/firestore');
if (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST || process.env.GCLOUD_PROJECT !== 'demo-flowcare') {
  throw new Error('Refusing to run outside the demo Firebase emulators');
}
initializeApp({projectId:'demo-flowcare'});
const db = getFirestore('flowcare');
const auth = getAuth();
const password = 'emulator-only-password';
async function account(uid, role, hospital) {
  const email = `${uid}@example.test`;
  await auth.createUser({uid,email,password,emailVerified:true});
  if (role) await auth.setCustomUserClaims(uid,{flowcare_role:role,hospital_id:hospital});
  const response = await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo', {
    method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({email,password,returnSecureToken:true})});
  const body = await response.json();
  assert.ok(body.idToken, 'auth emulator returned an ID token');
  return body.idToken;
}
async function call(name, token, data) {
  const res=await fetch(`http://127.0.0.1:5001/demo-flowcare/asia-south1/${name}`, {
    method:'POST',headers:{'content-type':'application/json',authorization:`Bearer ${token}`},body:JSON.stringify({data})});
  return {status:res.status,body:await res.json()};
}
async function main() {
  const [one,two,doctor,otherDoctor,hq,desk] = await Promise.all([
    account('patient1'),account('patient2'),account('doctor1','doctor','hospital1'),
    account('doctor2','doctor','hospital1'),account('hq1','hq','hospital1'),account('desk1','hospital','hospital1')]);
  const slotData={doctorUid:'doctor1',doctorName:'Dr Demo',department:'General medicine',startsAt:new Date(Date.now()+86400000).toISOString()};
  assert.notEqual((await call('createSlot',hq,slotData)).status,200);
  assert.notEqual((await call('createSlot',one,slotData)).status,200);
  const createdSlot=await call('createSlot',desk,slotData);
  assert.equal(createdSlot.status,200);
  assert.notEqual((await call('createSlot',desk,slotData)).status,200);
  assert.equal((await call('listAvailableSlots',one,{hospitalId:'hospital1'})).body.result.slots.length,1);
  assert.notEqual((await call('listAvailableSlots',hq,{hospitalId:'hospital1'})).status,200);
  const args={hospitalId:'hospital1',slotId:createdSlot.body.result.slotId,requestId:'request1',patientName:'Demo Patient'};
  const args2={...args,requestId:'request2'};
  const results=await Promise.all([call('bookVisit',one,args),call('bookVisit',two,args2)]);
  assert.equal(results.filter(r=>r.status===200).length,1,JSON.stringify(results));
  const winningIndex=results.findIndex(r=>r.status===200);
  const owner=winningIndex===0?one:two;
  const stranger=winningIndex===0?two:one;
  const winningArgs=winningIndex===0?args:args2;
  const visitId=results[winningIndex].body.result.visitId;
  assert.equal((await call('bookVisit',owner,winningArgs)).body.result.visitId,visitId);
  for (const token of [stranger,otherDoctor,hq]) assert.notEqual((await call('getVisit',token,{visitId})).status,200);
  assert.equal((await call('getVisit',owner,{visitId})).status,200);
  assert.equal((await call('listVisits',owner,{})).body.result.visits.length,1);
  assert.equal((await call('listVisits',stranger,{})).body.result.visits.length,0);
  assert.equal((await call('listVisits',doctor,{})).body.result.visits.length,1);
  assert.equal((await call('listVisits',otherDoctor,{})).body.result.visits.length,0);
  assert.notEqual((await call('listVisits',hq,{})).status,200);
  const directUrl=`http://127.0.0.1:8080/v1/projects/demo-flowcare/databases/flowcare/documents/visits/${visitId}`;
  assert.equal((await fetch(directUrl)).status,403,'anonymous Firestore access must be denied');
  assert.equal((await fetch(directUrl,{headers:{authorization:`Bearer ${hq}`}})).status,403,'HQ Firestore access must be denied');
  assert.notEqual((await call('cancelVisit',stranger,{visitId})).status,200);
  assert.equal((await call('cancelVisit',owner,{visitId})).status,200);
  assert.equal((await call('cancelVisit',owner,{visitId})).status,200,'cancel retry must be idempotent');
  const replacement=await call('bookVisit',owner,{...winningArgs,requestId:'replacement'});
  assert.equal(replacement.status,200,'cancelled slot should become available');
  const replacementId=replacement.body.result.visitId;
  assert.equal((await call('advanceVisit',desk,{visitId:replacementId,status:'waiting'})).status,200);
  assert.notEqual((await call('cancelVisit',owner,{visitId:replacementId})).status,200);
  assert.notEqual((await call('advanceVisit',otherDoctor,{visitId:replacementId,status:'active'})).status,200);
  assert.notEqual((await call('advanceVisit',doctor,{visitId:replacementId,status:'complete'})).status,200);
  assert.equal((await call('advanceVisit',doctor,{visitId:replacementId,status:'active'})).status,200);
  const orderArgs={visitId:replacementId,requestId:'lab-request',kind:'lab'};
  assert.notEqual((await call('addDemoOrder',otherDoctor,orderArgs)).status,200);
  assert.notEqual((await call('addDemoOrder',owner,orderArgs)).status,200);
  const order=await call('addDemoOrder',doctor,orderArgs);
  assert.equal(order.status,200,JSON.stringify(order.body));
  const orderId=order.body.result.orderId;
  assert.equal((await call('addDemoOrder',doctor,orderArgs)).body.result.orderId,orderId);
  assert.notEqual((await call('saveCareInstructions',otherDoctor,{visitId:replacementId,text:'Demo instructions'})).status,200);
  assert.equal((await call('saveCareInstructions',doctor,{visitId:replacementId,text:'Demo instructions'})).status,200);
  assert.notEqual((await call('getVisitDetails',hq,{visitId:replacementId})).status,200);
  assert.notEqual((await call('getVisitDetails',stranger,{visitId:replacementId})).status,200);
  assert.equal((await call('getVisitDetails',owner,{visitId:replacementId})).body.result.careInstructions,'Demo instructions');
  assert.notEqual((await call('updateDemoOrder',desk,{visitId:replacementId,orderId,status:'ready'})).status,200);
  assert.equal((await call('updateDemoOrder',owner,{visitId:replacementId,orderId,status:'paid_demo'})).status,200);
  assert.equal((await call('updateDemoOrder',desk,{visitId:replacementId,orderId,status:'ready'})).status,200);
  assert.equal((await call('advanceVisit',doctor,{visitId:replacementId,status:'complete'})).status,200);
  console.log('PASS: booking retry and slot exclusion; patient, hospital, clinician and HQ access; visit transitions.');
}
main().catch(error=>{ console.error(error); process.exitCode=1; });
