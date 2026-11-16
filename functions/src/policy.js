'use strict';

// Privileged roles are set by trusted administrators, never by client input.
const ID = /^[A-Za-z0-9_-]{1,80}$/;
function identifier(value, field) {
  if (typeof value !== 'string' || !ID.test(value)) throw new Error(`Invalid ${field}`);
  return value;
}
function principal(auth) {
  if (!auth || !auth.uid || !auth.token) throw new Error('Verified sign-in required');
  const role = auth.token.flowcare_role || 'patient';
  if (!['patient', 'doctor', 'hospital', 'hq'].includes(role)) throw new Error('Invalid role');
  const verifiedPhone = role === 'patient' && auth.token.firebase?.sign_in_provider === 'phone' && typeof auth.token.phone_number === 'string' && auth.token.phone_number.length > 0;
  if (auth.token.email_verified !== true && !verifiedPhone) throw new Error('Verified sign-in required');
  return {uid: identifier(auth.uid, 'user'), role, hospital: auth.token.hospital_id};
}
function patient(auth) {
  const p = principal(auth);
  if (p.role !== 'patient') throw new Error('Patient account required');
  return p;
}
function staffMayRead(p, visit) {
  return (p.role === 'hospital' || p.role === 'doctor') && typeof p.hospital === 'string' &&
    p.hospital === visit.hospitalId && (p.role !== 'doctor' || p.uid === visit.doctorUid);
}
function mayRead(p, visit) {
  return (p.role === 'patient' && p.uid === visit.patientUid) || staffMayRead(p, visit);
}
function mayCancel(p, visit) {
  return p.role === 'patient' && p.uid === visit.patientUid && visit.status === 'confirmed';
}
function mayTransition(p, visit, next) {
  if (!staffMayRead(p, visit)) return false;
  const transitions = p.role === 'doctor'
    ? {waiting: ['active'], active: ['complete']}
    : {confirmed: ['waiting']};
  return (transitions[visit.status] || []).includes(next);
}
module.exports = {identifier, principal, patient, mayRead, mayCancel, mayTransition};

