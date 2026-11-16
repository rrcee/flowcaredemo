'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const {identifier, principal, patient, mayRead, mayCancel, mayTransition} = require('../src/policy');
const auth = (uid, role, hospital) => ({uid, token:{email_verified:true, flowcare_role:role, hospital_id:hospital}});
const visit = {patientUid:'patient1', doctorUid:'doctor1', hospitalId:'hospital1', status:'waiting'};
test('requires verified identity and prevents privileged users acting as patients', () => {
  assert.throws(() => principal(null));
  assert.throws(() => principal({uid:'p',token:{email_verified:false}}));
  assert.equal(patient({uid:'phone1',token:{firebase:{sign_in_provider:'phone'},phone_number:'+15555550123'}}).uid, 'phone1');
  assert.throws(() => principal({uid:'staff',token:{firebase:{sign_in_provider:'phone'},phone_number:'+15555550123',flowcare_role:'hq'}}));
  assert.throws(() => principal({uid:'p',token:{firebase:{sign_in_provider:'password'},phone_number:'+15555550123'}}));
  assert.throws(() => patient(auth('hq1','hq')));
  assert.equal(patient(auth('patient1','patient')).uid, 'patient1');
  assert.throws(() => identifier('../other','id'));
});
test('patient ownership, hospital boundary, assigned clinician and HQ isolation', () => {
  assert.equal(mayRead(principal(auth('patient1','patient')), visit), true);
  assert.equal(mayRead(principal(auth('other','patient')), visit), false);
  assert.equal(mayRead(principal(auth('doctor1','doctor','hospital1')), visit), true);
  assert.equal(mayRead(principal(auth('doctor2','doctor','hospital1')), visit), false);
  assert.equal(mayRead(principal(auth('staff','hospital','other')), visit), false);
  assert.equal(mayRead(principal(auth('hq','hq','hospital1')), visit), false);
});
test('only the assigned doctor can advance waiting to active and then complete', () => {
  const doctor=principal(auth('doctor1','doctor','hospital1'));
  const desk=principal(auth('staff','hospital','hospital1'));
  assert.equal(mayTransition(doctor, visit, 'active'), true);
  assert.equal(mayTransition(doctor, visit, 'complete'), false);
  assert.equal(mayTransition(desk, visit, 'active'), false);
  assert.equal(mayTransition(desk, {...visit,status:'confirmed'}, 'waiting'), true);
  assert.equal(mayTransition(doctor, {...visit,status:'active'}, 'complete'), true);
});
test('only the owner may cancel a confirmed visit', () => {
  const owner=principal(auth('patient1','patient'));
  assert.equal(mayCancel(owner,{...visit,status:'confirmed'}),true);
  assert.equal(mayCancel(owner,visit),false);
  assert.equal(mayCancel(principal(auth('patient2','patient')),{...visit,status:'confirmed'}),false);
  assert.equal(mayCancel(principal(auth('hq1','hq','hospital1')),{...visit,status:'confirmed'}),false);
});

