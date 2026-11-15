"""Synthetic-data demo. Bind to localhost; demo credentials are not production auth."""
import asyncio
import json
from datetime import datetime
import secrets
import sqlite3
import time
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Header, WebSocket, WebSocketDisconnect
from pydantic import BaseModel, Field, field_validator
from cryptography.fernet import Fernet

DB = sqlite3.connect("flowcare.db", check_same_thread=False)
DB.row_factory = sqlite3.Row
DB.executescript("""
CREATE TABLE IF NOT EXISTS bookings(id TEXT PRIMARY KEY, patient TEXT, doctor TEXT, slot TEXT, status TEXT, priority INTEGER DEFAULT 0);
CREATE UNIQUE INDEX IF NOT EXISTS reserved_slot ON bookings(doctor,slot) WHERE status IN ('waiting','active');
CREATE TABLE IF NOT EXISTS bills(id TEXT PRIMARY KEY, booking TEXT, department TEXT, amount INTEGER, paid INTEGER DEFAULT 0);
CREATE TABLE IF NOT EXISTS prescriptions(booking TEXT PRIMARY KEY, text TEXT);
CREATE TABLE IF NOT EXISTS reviews(booking TEXT PRIMARY KEY, stars INTEGER);
""")
DOCTORS = [dict(id="anya",name="Dr. Anya Sharma",specialty="General Medicine",rating=4.9,years=14,cases=8500),dict(id="vikram",name="Dr. Vikram Nair",specialty="Psychiatry",rating=4.8,years=11,cases=6200)]
PAUSED = set()
VAULT = {}
CIPHER = Fernet(Fernet.generate_key())
LOCK = asyncio.Lock()
CLIENTS = set()

def purge():
    for key in list(VAULT):
        if VAULT[key][0] <= time.time(): VAULT.pop(key, None)

@asynccontextmanager
async def lifespan(app):
    async def cleanup():
        while True:
            purge()
            await asyncio.sleep(1)
    task=asyncio.create_task(cleanup())
    yield
    task.cancel()
    try: await task
    except asyncio.CancelledError: pass

app=FastAPI(title="FlowCare Demo API", lifespan=lifespan)
def auth(token, doctor=False):
    if token not in (["doctor-demo"] if doctor else ["patient-demo","doctor-demo"]):
        raise HTTPException(401,"Invalid demo credential")
def booking(bid):
    row=DB.execute("SELECT * FROM bookings WHERE id=?",(bid,)).fetchone()
    if not row: raise HTTPException(404,"Appointment not found")
    return dict(row)
def snapshot():
    queue=[]
    for doctor in DOCTORS:
        rows=DB.execute("SELECT * FROM bookings WHERE doctor=? AND status IN ('waiting','active') ORDER BY priority DESC,rowid",(doctor['id'],)).fetchall()
        for i,row in enumerate(rows):
            queue.append(dict(row)|dict(token=1001+i,ahead=i,wait=i*7,paused=doctor['id'] in PAUSED))
    docs=[]
    for d in DOCTORS:
        avg=DB.execute("SELECT AVG(stars) FROM reviews r JOIN bookings b ON r.booking=b.id WHERE b.doctor=?",(d['id'],)).fetchone()[0]
        item=d|dict(rating=round(avg,1) if avg else d['rating'])
        item['score']=round(.45*item['rating']/5+.30*min(item['years']/20,1)+.25*min(item['cases']/10000,1),3)
        docs.append(item)
    return dict(doctors=sorted(docs,key=lambda x:-x['score']),queue=queue,bills=[dict(x) for x in DB.execute('SELECT * FROM bills')],prescriptions=[dict(x) for x in DB.execute('SELECT * FROM prescriptions')],paused=list(PAUSED))
async def broadcast():
    data=snapshot()
    for ws in list(CLIENTS):
        try: await asyncio.wait_for(ws.send_json(data),2)
        except Exception: CLIENTS.discard(ws)
@app.get('/health')
def health(): return {'status':'ok','mode':'synthetic demo'}
@app.get('/api/state')
async def state(x_token: str=Header(default='')):
    auth(x_token);return snapshot()
class Booking(BaseModel):
    patient: str=Field(min_length=1,max_length=80)
    doctor: str
    slot: str=Field(min_length=1,max_length=80)

    @field_validator('patient')
    @classmethod
    def patient_required(cls, value):
        value=value.strip()
        if not value: raise ValueError('Enter a patient name')
        return value

    @field_validator('slot')
    @classmethod
    def valid_slot(cls, value):
        try: return datetime.strptime(value.strip(), '%Y-%m-%d %H:%M').strftime('%Y-%m-%d %H:%M')
        except ValueError: raise ValueError('Use YYYY-MM-DD HH:mm for the visit time')
class Order(BaseModel):
    booking: str
    department: str=Field(min_length=1,max_length=60)
    amount: int=Field(gt=0,le=10000000)
class Text(BaseModel):
    text: str=Field(min_length=1,max_length=10000)

    @field_validator('text')
    @classmethod
    def nonblank(cls, value):
        value=value.strip()
        if not value: raise ValueError('Enter some text')
        return value
@app.post('/api/bookings')
async def book(req:Booking,x_token:str=Header(default='')):
    auth(x_token)
    if req.doctor not in [d['id'] for d in DOCTORS]: raise HTTPException(404,'Doctor not found')
    async with LOCK:
        bid=secrets.token_urlsafe(12)
        try:
            with DB:
                DB.execute("INSERT INTO bookings VALUES(?,?,?,?,?,0)",(bid,req.patient,req.doctor,req.slot,'waiting'))
                for dept,amount in [('Registration',10000),('Consultation',50000)]:
                    DB.execute('INSERT INTO bills VALUES(?,?,?,?,0)',(secrets.token_hex(8),bid,dept,amount))
        except sqlite3.IntegrityError: raise HTTPException(409,'Slot already reserved')
    await broadcast();return {'id':bid}
@app.post('/api/bookings/{bid}/cancel')
async def cancel(bid:str,x_token:str=Header(default='')):
    auth(x_token); row=booking(bid)
    if row['status']!='waiting': raise HTTPException(409,'Only waiting appointments can be cancelled')
    with DB:
        DB.execute("UPDATE bookings SET status='cancelled' WHERE id=?",(bid,))
        DB.execute("DELETE FROM bills WHERE booking=? AND paid=0",(bid,))
    VAULT.pop(bid,None)
    await broadcast();return {'ok':True}
@app.post('/api/doctors/{did}/pause')
async def pause(did:str,x_token:str=Header(default='')):
    auth(x_token,True)
    if did not in [d['id'] for d in DOCTORS]: raise HTTPException(404,'Doctor not found')
    PAUSED.symmetric_difference_update({did});await broadcast();return {'ok':True}
@app.post('/api/doctors/{did}/next')
async def next_patient(did:str,x_token:str=Header(default='')):
    auth(x_token,True)
    if did not in [d['id'] for d in DOCTORS]: raise HTTPException(404,'Doctor not found')
    if did in PAUSED: raise HTTPException(409,'Resume the queue first')
    async with LOCK:
        with DB:
            DB.execute("UPDATE bookings SET status='complete' WHERE doctor=? AND status='active'",(did,))
            row=DB.execute("SELECT id FROM bookings WHERE doctor=? AND status='waiting' ORDER BY priority DESC,rowid LIMIT 1",(did,)).fetchone()
            if row: DB.execute("UPDATE bookings SET status='active' WHERE id=?",(row['id'],))
    await broadcast();return {'ok':True}
@app.post('/api/orders')
async def order(req:Order,x_token:str=Header(default='')):
    auth(x_token,True)
    if booking(req.booking)['status']!='active': raise HTTPException(409,'Call this patient before placing an order')
    with DB: DB.execute('INSERT INTO bills VALUES(?,?,?,?,0)',(secrets.token_hex(8),req.booking,req.department,req.amount))
    await broadcast();return {'ok':True}
@app.post('/api/bills/{bid}/pay')
async def pay(bid:str,x_token:str=Header(default='')):
    auth(x_token)
    if not DB.execute('SELECT id FROM bills WHERE id=?',(bid,)).fetchone(): raise HTTPException(404,'Bill not found')
    with DB: DB.execute('UPDATE bills SET paid=1 WHERE id=?',(bid,))
    await broadcast();return {'ok':True,'simulated':True}
@app.post('/api/bookings/{bid}/prescription')
async def prescribe(bid:str,req:Text,x_token:str=Header(default='')):
    auth(x_token,True)
    if booking(bid)['status']!='active': raise HTTPException(409,'Call this patient before prescribing')
    with DB: DB.execute('INSERT OR REPLACE INTO prescriptions VALUES(?,?)',(bid,req.text))
    await broadcast();return {'ok':True}
@app.post('/api/bookings/{bid}/review/{stars}')
async def review(bid:str,stars:int,x_token:str=Header(default='')):
    auth(x_token)
    if booking(bid)['status']!='complete' or stars not in range(1,6): raise HTTPException(409,'Complete consultation first; rating must be 1–5')
    with DB: DB.execute('INSERT OR REPLACE INTO reviews VALUES(?,?)',(bid,stars))
    await broadcast();return {'ok':True}
@app.post('/api/records/{bid}')
async def record(bid:str,req:Text,x_token:str=Header(default='')):
    auth(x_token)
    if booking(bid)['status'] not in ('waiting','active'): raise HTTPException(409,'Appointment is no longer active')
    purge()
    VAULT[bid]=(time.time()+1800,CIPHER.encrypt(req.text.encode()))
    return {'expires_at':VAULT[bid][0]}
@app.post('/api/records/{bid}/consume')
async def consume(bid:str,x_token:str=Header(default='')):
    auth(x_token,True);purge()
    row=VAULT.pop(bid,None)
    if not row: raise HTTPException(410,'Record expired or already viewed')
    return {'text':CIPHER.decrypt(row[1]).decode(),'deleted_from_server':True}
@app.delete('/api/records/{bid}')
async def delete_record(bid:str,x_token:str=Header(default='')):
    auth(x_token);VAULT.pop(bid,None);return {'ok':True}
@app.post('/api/comfort')
async def comfort(req:Text,x_token:str=Header(default='')):
    auth(x_token)
    return {'reply':'Take a gentle breath. Name five things you can see, four you can feel, and three you can hear. This is a scripted support demo. For urgent help, contact local emergency services or a trusted person.','simulated':True}
@app.websocket('/ws')
async def ws(ws:WebSocket):
    # Authenticate in the first frame so credentials never enter URL logs.
    await ws.accept()
    try:
        msg=await asyncio.wait_for(ws.receive_json(),5)
        if not isinstance(msg,dict): raise HTTPException(400,'Expected an authentication object')
        auth(msg.get('token',''))
        CLIENTS.add(ws);await ws.send_json(snapshot())
        while True: await ws.receive_text()
    except (WebSocketDisconnect,asyncio.TimeoutError,HTTPException,ValueError,TypeError): pass
    finally:
        CLIENTS.discard(ws)
        try: await ws.close()
        except RuntimeError: pass
