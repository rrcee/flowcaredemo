"""Prove the three-app journey offline on a single Android device."""
import subprocess,time,re,sys,traceback,xml.etree.ElementTree as ET
from pathlib import Path
out=Path('build/native-screenshots');out.mkdir(parents=True,exist_ok=True)
def adb(*args):return subprocess.check_output(['adb',*args])
def screen(name):
 time.sleep(.5);(out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
def nodes():
 adb('shell','uiautomator','dump','--compressed','/data/local/tmp/flowcare-ui.xml')
 try:return list(ET.fromstring(adb('exec-out','cat','/data/local/tmp/flowcare-ui.xml')).iter('node'))
 except ET.ParseError:return []
def down():adb('shell','input','swipe','500','1700','500','650','180')
def find(text,timeout=40,scroll=False):
 until=time.monotonic()+timeout;tries=0
 while time.monotonic()<until:
  for n in nodes():
   if n.get('text')==text or n.get('content-desc')==text:return n
  tries+=1
  if scroll and tries%2==0:down()
  time.sleep(.3)
 raise AssertionError(f'Missing UI element: {text}')
def tapnode(n):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds','')));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def tap(text,scroll=False):tapnode(find(text,scroll=scroll))
def launch(role,first=False):
 adb('shell','am','start','-n',f'com.flowcare.demo.{role}/com.flowcare.MainActivity')
 if first:tap('Explore prototype',scroll=True)
def dashboard(role):
 launch(role)
 for _ in range(4):
  if any(n.get('text')=='Profile' or n.get('content-desc')=='Profile' for n in nodes()):return
  adb('shell','input','keyevent','KEYCODE_BACK')
 raise AssertionError('Could not return to main navigation')
def fail(kind,value,tb):
 try:
  screen('failure');(out/'failure.xml').write_bytes(adb('exec-out','cat','/data/local/tmp/flowcare-ui.xml'));(out/'logcat.txt').write_bytes(adb('logcat','-d','-t','700'))
 except Exception:pass
 traceback.print_exception(kind,value,tb)
sys.excepthook=fail
# All functions below must work without network access.
adb('shell','svc','wifi','disable');adb('shell','svc','data','disable')
launch('hospital',True);find('Hospital overview');screen('01-hospital-overview')
launch('patient');tap('Sign in with phone',True);find('FlowCare account');find('Phone number',scroll=True);screen('02-account-entry')
adb('shell','input','keyevent','KEYCODE_BACK');tap('Explore prototype',True);find('Hello, Maya');screen('02-patient-home')
tap('Family');find('Who are you caring for?');adb('shell','input','keyevent','KEYCODE_BACK');find('Hello, Maya')
tap('Search doctors or departments')
fields=[n for n in nodes() if n.get('class')=='android.widget.EditText']
assert fields,'Care-search input is missing'
tapnode(fields[0]);adb('shell','input','text','heart');adb('shell','input','keyevent','KEYCODE_BACK')
tap('Dr. Neha Joseph',True);find('Book Neha',scroll=True);screen('02a-doctor-profile')
adb('shell','input','keyevent','KEYCODE_BACK');find('Find the care you need');adb('shell','input','keyevent','KEYCODE_BACK');find('Hello, Maya')
tap('See all',True);tap('Heart care',True);find('Care at this clinic',scroll=True);screen('02b-department-detail')
adb('shell','input','keyevent','KEYCODE_BACK');adb('shell','input','keyevent','KEYCODE_BACK');find('Hello, Maya')
tap('Book an appointment',True);tap('Continue',True);tap('Choose appointment time',True);tap('Review appointment',True);screen('03-booking-review');tap('Confirm sample booking',True)
find("You're booked");tap('Go to home',True);find('Your next visit',scroll=True);screen('03a-upcoming-visit');tap('View appointment',True);find('Check in');screen('04-patient-pass')
# Hospital sees the patient booking without any server.
launch('hospital');tap('Check-in');tap('Maya Mathew',True);tap('Check in',True);screen('05-hospital-checkin')
dashboard('hospital');tap('More options');tap('Doctor workspace');find("Today's queue");tap('Maya Mathew',True);tap('Call patient',True);find('Consultation');tap('Order blood test',True)
# Scroll to the editable care-plan field if needed.
fields=[n for n in nodes() if n.get('class')=='android.widget.EditText']
if not fields:down();fields=[n for n in nodes() if n.get('class')=='android.widget.EditText']
assert fields,'Care-plan field is missing'
tapnode(fields[0]);adb('shell','input','text','Fictional%sdemo%scare%splan')
adb('shell','input','keyevent','KEYCODE_BACK');tap('Share care instructions',True);screen('06-doctor-consultation');tap('Complete consultation',True)
dashboard('patient');tap('Bills');find('Blood test',scroll=True);screen('07-patient-bills');tap('Pay ₹450 in prototype',True)
# Fulfillment sees the simulated payment.
dashboard('hospital');tap('More options');tap('Hospital workspace');tap('Orders');find('Blood test');screen('08-hospital-orders');tap('Mark Blood test ready',True)
launch('patient');tap('Visits');tap('Maya Mathew',True);tap('View care instructions',True);find('Fictional demo care plan');screen('09-patient-aftercare')
adb('shell','input','keyevent','KEYCODE_BACK');adb('shell','input','keyevent','KEYCODE_BACK');tap('Profile');find('Family members');screen('10-patient-profile')
adb('shell','settings','put','system','font_scale','1.3');time.sleep(2);find('Family members',scroll=True);screen('11-large-text-profile');adb('shell','settings','put','system','font_scale','1.0')
print('PASS: offline patient booking → hospital check-in → doctor consultation/lab/Rx → patient payment → hospital fulfillment → patient aftercare, profile and large text.')

launch('hq');find('HQ control');tap('Notifications');find('Remote service not connected');screen('12-hq-notifications')
tap('Settings');tap('Manage a real account',True);find('FlowCare account');adb('shell','input','keyevent','KEYCODE_BACK');find('Connection & privacy')
