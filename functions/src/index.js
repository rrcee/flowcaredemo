'use strict';

const {initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {onCall, HttpsError} = require('firebase-functions/v2/https');
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

// Slots are created by trusted hospital systems, never directly by patients.
// A deterministic patient/request key makes retries return the same appointment.
exports.bookVisit = onCall(options, async request => {
  const p = requirePrincipal(request.auth, true);
  const hospitalId = id(request.data?.hospitalId, 'hospital');
  const slotId = id(request.data?.slotId, 'slot');
  const requestId = id(request.data?.requestId, 'request');
  const name = request.data?.patientName;
  if (typeof name !== 'string' || name.trim().length < 1 || name.trim().length > 100)
    throw new HttpsError('invalid-argument', 'Enter a patient name of up to 100 characters.');
  const visitId = crypto.createHash('sha256').update(`${p.uid}:${requestId}`).digest('hex');
  const visitRef = db.collection('visits').doc(visitId);
  const slotRef = db.collection('hospitals').doc(hospitalId).collection('slots').doc(slotId);
  return db.runTransaction(async tx => {
    const [prior, slot] = await Promise.all([tx.get(visitRef), tx.get(slotRef)]);
    if (prior.exists) {
      const v = prior.data();
      if (v.patientUid !== p.uid || v.hospitalId !== hospitalId || v.slotId !== slotId || v.patientName !== name.trim())
        throw new HttpsError('already-exists', 'This request ID was already used for a different booking.');
      return {visitId, status: v.status};
    }
    if (!slot.exists || slot.get('status') !== 'available') throw new HttpsError('failed-precondition', 'This appointment time is no longer available.');
    const data = slot.data();
    if (!data.startsAt || data.startsAt.toMillis() <= Date.now() || !data.doctorUid)
      throw new HttpsError('failed-precondition', 'This appointment time is unavailable.');
    tx.create(visitRef, {patientUid:p.uid, patientName:name.trim(), hospitalId, slotId,
      doctorUid:data.doctorUid, startsAt:data.startsAt, status:'confirmed', createdAt:FieldValue.serverTimestamp()});
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
    doctorUid:v.doctorUid, startsAt:v.startsAt.toDate().toISOString(), status:v.status};
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
