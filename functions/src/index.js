'use strict';

const {initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldValue, Timestamp} = require('firebase-admin/firestore');
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {field} = require('@google-cloud/firestore/pipelines');
const crypto = require('node:crypto');
const policy = require('./policy');

initializeApp();
const db = getFirestore('flowcare');
const options = {region: 'asia-south1', enforceAppCheck: false};
function requirePrincipal(auth, patientOnly = false) {
  try { return patientOnly ? policy.patient(auth) : policy.principal(auth); }
  catch (_) { throw new HttpsError('permission-denied', 'A verified account with the required access is needed.'); }
}
function id(value, field) {
  try { return policy.identifier(value, field); }
  catch (_) { throw new HttpsError('invalid-argument', `Invalid ${field}.`); }
}
function boundedText(value, label, max = 100) {
  if (typeof value !== 'string' || value.trim().length < 1 || value.trim().length > max)
    throw new HttpsError('invalid-argument', `Invalid ${label}.`);
  return value.trim();
}

// Slots are created only by a trusted hospital account for its own hospital.
exports.createSlot = onCall(options, async request => {
  const p = requirePrincipal(request.auth);
  if (p.role !== 'hospital') throw new HttpsError('permission-denied', 'Hospital staff access required.');
  const hospitalId = id(p.hospital, 'hospital assignment');
  const doctorUid = id(request.data?.doctorUid, 'doctor');
  const doctorName = boundedText(request.data?.doctorName, 'doctor name');
  const department = boundedText(request.data?.department, 'department');
  if (typeof request.data?.startsAt !== 'string') throw new HttpsError('invalid-argument', 'Choose a valid appointment time.');
  const date = new Date(request.data.startsAt);
  if (!Number.isFinite(date.getTime()) || date.getTime() <= Date.now() || date.getTime() > Date.now() + 366*86400000)
    throw new HttpsError('invalid-argument', 'Choose a valid future appointment time within one year.');
  const slotId = crypto.createHash('sha256').update(`${doctorUid}:${date.toISOString()}`).digest('hex');
  const ref = db.collection('hospitals').doc(hospitalId).collection('slots').doc(slotId);
  try {
    await ref.create({doctorUid,doctorName,department,startsAt:Timestamp.fromDate(date),status:'available',createdAt:FieldValue.serverTimestamp()});
  } catch (e) {
    if (e.code === 6 || e.code === 'already-exists') throw new HttpsError('already-exists', 'This slot ID already exists.');
    throw e;
  }
  return {slotId,status:'available'};
});

exports.listAvailableSlots = onCall(options, async request => {
  requirePrincipal(request.auth, true);
  const hospitalId = id(request.data?.hospitalId, 'hospital');
  const snap = await db.pipeline().collection(`hospitals/${hospitalId}/slots`)
    .where(field('status').equal('available'))
    .where(field('startsAt').greaterThan(Timestamp.now()))
    .sort(field('startsAt').ascending()).limit(50).execute();
  return {slots:snap.results.map(item => {
    const s=item.data();
    return {slotId:item.id,doctorName:s.doctorName,department:s.department,startsAt:s.startsAt.toDate().toISOString()};
  })};
});

// Slots are created by trusted hospital systems, never directly by patients.
// A deterministic patient/request key makes retries return the same appointment.
exports.bookVisit = onCall(options, async request => {
  const p = requirePrincipal(request.auth, true);
  const hospitalId = id(request.data?.hospitalId, 'hospital');
  const slotId = id(request.data?.slotId, 'slot');
  const requestId = id(request.data?.requestId, 'request');
  const name = boundedText(request.data?.patientName, 'patient name');
  const visitId = crypto.createHash('sha256').update(`${p.uid}:${requestId}`).digest('hex');
  const visitRef = db.collection('visits').doc(visitId);
  const slotRef = db.collection('hospitals').doc(hospitalId).collection('slots').doc(slotId);
  return db.runTransaction(async tx => {
    const [prior, slot] = await Promise.all([tx.get(visitRef), tx.get(slotRef)]);
    if (prior.exists) {
      const v = prior.data();
      if (v.patientUid !== p.uid || v.hospitalId !== hospitalId || v.slotId !== slotId || v.patientName !== name)
        throw new HttpsError('already-exists', 'This request ID was already used for a different booking.');
      return {visitId, status: v.status};
    }
    if (!slot.exists || slot.get('status') !== 'available') throw new HttpsError('failed-precondition', 'This appointment time is no longer available.');
    const data = slot.data();
    if (!data.startsAt || data.startsAt.toMillis() <= Date.now() || !data.doctorUid || !data.doctorName || !data.department)
      throw new HttpsError('failed-precondition', 'This appointment time is unavailable.');
    tx.create(visitRef, {patientUid:p.uid, patientName:name, hospitalId, slotId,
      doctorUid:data.doctorUid, doctorName:data.doctorName, department:data.department,
      startsAt:data.startsAt, status:'confirmed', createdAt:FieldValue.serverTimestamp()});
    tx.update(slotRef, {status:'booked', visitId});
    return {visitId, status:'confirmed'};
  });
});

exports.getVisit = onCall(options, async request => {
  const p = requirePrincipal(request.auth);
  const ref = db.collection('visits').doc(id(request.data?.visitId, 'visit'));
  const snapshot = await ref.get();
  // Identical error for absent and unauthorized records avoids leaking existence.
  if (!snapshot.exists || !policy.mayRead(p, snapshot.data())) throw new HttpsError('not-found', 'Visit not found.');
  const v = snapshot.data();
  return {visitId:snapshot.id, patientName:v.patientName, hospitalId:v.hospitalId,
    doctorUid:v.doctorUid, doctorName:v.doctorName, department:v.department, startsAt:v.startsAt.toDate().toISOString(), status:v.status};
});

// Restrict the pipeline before it reads results; HQ never enters this path.
exports.listVisits = onCall(options, async request => {
  const p = requirePrincipal(request.auth);
  let pipeline = db.pipeline().collection('visits');
  if (p.role === 'patient') pipeline = pipeline.where(field('patientUid').equal(p.uid));
  else if (p.role === 'hospital' || p.role === 'doctor') {
    if (typeof p.hospital !== 'string' || !p.hospital) throw new HttpsError('permission-denied', 'Hospital assignment required.');
    pipeline = pipeline.where(field('hospitalId').equal(p.hospital));
    if (p.role === 'doctor') pipeline = pipeline.where(field('doctorUid').equal(p.uid));
  } else throw new HttpsError('permission-denied', 'Clinical access is not available.');
  const snapshot = await pipeline.sort(field('startsAt').descending()).limit(50).execute();
  return {visits:snapshot.results.map(item => {
    const v = item.data();
    return {visitId:item.id,patientName:v.patientName,hospitalId:v.hospitalId,doctorUid:v.doctorUid,doctorName:v.doctorName,department:v.department,
      startsAt:v.startsAt.toDate().toISOString(),status:v.status};
  })};
});

exports.cancelVisit = onCall(options, async request => {
  const p = requirePrincipal(request.auth, true);
  const visitId = id(request.data?.visitId, 'visit');
  const visitRef = db.collection('visits').doc(visitId);
  return db.runTransaction(async tx => {
    const snapshot = await tx.get(visitRef);
    if (!snapshot.exists || !policy.mayRead(p, snapshot.data())) throw new HttpsError('not-found', 'Visit not found.');
    const visit = snapshot.data();
    if (visit.status === 'cancelled') return {visitId, status:'cancelled'};
    if (!policy.mayCancel(p, visit)) throw new HttpsError('failed-precondition', 'This visit can no longer be cancelled here.');
    const slotRef = db.collection('hospitals').doc(visit.hospitalId).collection('slots').doc(visit.slotId);
    const slot = await tx.get(slotRef);
    if (!slot.exists || slot.get('visitId') !== visitId || slot.get('status') !== 'booked')
      throw new HttpsError('failed-precondition', 'The appointment needs help from the care team.');
    tx.update(visitRef, {status:'cancelled', updatedAt:FieldValue.serverTimestamp()});
    tx.update(slotRef, {status:'available', visitId:FieldValue.delete()});
    return {visitId, status:'cancelled'};
  });
});

exports.advanceVisit = onCall(options, async request => {
  const p = requirePrincipal(request.auth);
  const visitId = id(request.data?.visitId, 'visit');
  const next = request.data?.status;
  if (!['waiting', 'active', 'complete'].includes(next)) throw new HttpsError('invalid-argument', 'Invalid visit status.');
  const ref = db.collection('visits').doc(visitId);
  return db.runTransaction(async tx => {
    const snapshot = await tx.get(ref);
    if (!snapshot.exists || !policy.mayRead(p, snapshot.data())) throw new HttpsError('not-found', 'Visit not found.');
    const visit = snapshot.data();
    if (visit.status === next) return {visitId, status:next};
    if (!policy.mayTransition(p, visit, next)) throw new HttpsError('permission-denied', 'This transition is not allowed.');
    tx.update(ref, {status:next, updatedAt:FieldValue.serverTimestamp()});
    return {visitId, status:next};
  });
});
