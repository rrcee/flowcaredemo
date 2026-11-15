import os
import tempfile
os.chdir(tempfile.mkdtemp())
import app as module
from fastapi.testclient import TestClient

P={'X-Token':'patient-demo'}
D={'X-Token':'doctor-demo'}

def test_journey():
    with TestClient(module.app) as c:
        assert c.get('/api/state').status_code==401
        bid=c.post('/api/bookings',headers=P,json={'patient':'Test','doctor':'anya','slot':'2030-01-01 09:00'}).json()['id']
        assert c.post('/api/bookings',headers=P,json={'patient':'Other','doctor':'anya','slot':'2030-01-01 09:00'}).status_code==409
        assert c.post('/api/doctors/anya/pause',headers=P).status_code==401
        c.post('/api/doctors/anya/pause',headers=D)
        assert c.post('/api/doctors/anya/next',headers=D).status_code==409
        c.post('/api/doctors/anya/pause',headers=D)
        c.post('/api/records/'+bid,headers=P,json={'text':'Synthetic record'})
        assert c.post('/api/records/'+bid+'/consume',headers=D).json()['text']=='Synthetic record'
        assert c.post('/api/records/'+bid+'/consume',headers=D).status_code==410
        c.post('/api/records/'+bid,headers=P,json={'text':'Expired'})
        module.VAULT[bid]=(0,module.VAULT[bid][1])
        assert c.post('/api/records/'+bid+'/consume',headers=D).status_code==410
        c.post('/api/doctors/anya/next',headers=D)
        assert c.post('/api/bookings/'+bid+'/cancel',headers=P).status_code==409
        assert c.post('/api/bookings/'+bid+'/prescription',headers=D,json={'text':'Synthetic instructions'}).status_code==200
        c.post('/api/orders',headers=D,json={'booking':bid,'department':'Lab','amount':45000})
        bill=c.get('/api/state',headers=P).json()['bills'][0]['id']
        assert c.post('/api/bills/'+bill+'/pay',headers=P).json()['simulated']
        assert c.post('/api/bills/'+bill+'/pay',headers=P).status_code==200
        assert c.post('/api/bookings/'+bid+'/review/5',headers=P).status_code==409
        c.post('/api/doctors/anya/next',headers=D)
        assert c.post('/api/bookings/'+bid+'/review/5',headers=P).status_code==200
        with c.websocket_connect('/ws') as ws:
            ws.send_json({'token':'patient-demo'})
            assert 'doctors' in ws.receive_json()

def test_cancellation_reindexes_without_changing_identity():
    with TestClient(module.app) as c:
        ids=[c.post('/api/bookings',headers=P,json={'patient':str(i),'doctor':'vikram','slot':f'2030-02-01 {9+i:02d}:00'}).json()['id'] for i in range(2)]
        c.post('/api/bookings/'+ids[0]+'/cancel',headers=P)
        q=next(x for x in c.get('/api/state',headers=P).json()['queue'] if x['id']==ids[1])
        assert q['token']==1001 and q['ahead']==0


def test_invalid_input_and_unknown_doctor():
    with TestClient(module.app) as c:
        assert c.post('/api/doctors/missing/next',headers=D).status_code==404
        assert c.post('/api/bookings',headers=P,json={'patient':'  ','doctor':'anya','slot':'2030-01-01 12:00'}).status_code==422
        assert c.post('/api/bookings',headers=P,json={'patient':'A','doctor':'anya','slot':'garbage'}).status_code==422
        assert c.post('/api/comfort',headers=P,json={'text':'  '}).status_code==422
        with c.websocket_connect('/ws') as ws:
            ws.send_text('not JSON')
            assert ws.receive()['type']=='websocket.close'

def test_cancel_removes_unpaid_bills_and_temporary_record():
    with TestClient(module.app) as c:
        bid=c.post('/api/bookings',headers=P,json={'patient':'Cancelled','doctor':'anya','slot':'2030-03-01 09:00'}).json()['id']
        assert c.post('/api/bookings/'+bid+'/prescription',headers=D,json={'text':'Test'}).status_code==409
        assert c.post('/api/orders',headers=D,json={'booking':bid,'department':'Lab','amount':100}).status_code==409
        c.post('/api/records/'+bid,headers=P,json={'text':'Synthetic'})
        c.post('/api/bookings/'+bid+'/cancel',headers=P)
        assert not any(x['booking']==bid for x in c.get('/api/state',headers=P).json()['bills'])
        assert c.post('/api/records/'+bid+'/consume',headers=D).status_code==410
        assert c.post('/api/records/'+bid,headers=P,json={'text':'Synthetic'}).status_code==409
