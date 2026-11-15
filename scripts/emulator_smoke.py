"""Run a synthetic booking against the backend and capture native Android screens."""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

out=Path('build/native-screenshots');out.mkdir(parents=True,exist_ok=True)
def adb(*args):
    return subprocess.check_output(['adb',*args])
def screen(name):
    time.sleep(.4)
    (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
def nodes():
    adb('shell','uiautomator','dump','/sdcard/flowcare-ui.xml')
    return list(ET.fromstring(adb('exec-out','cat','/sdcard/flowcare-ui.xml')).iter('node'))
def find(text,timeout=25):
    until=time.monotonic()+timeout
    while time.monotonic()<until:
        for n in nodes():
            if n.get('text')==text or n.get('content-desc')==text:return n
        time.sleep(.5)
    raise AssertionError(f'Could not find {text!r}')
def tap_node(n):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds','')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def tap(text):tap_node(find(text))
def top():
    for _ in range(3): adb('shell','input','swipe','500','400','500','1600','150')
def down():adb('shell','input','swipe','500','1600','500','500','250')

adb('shell','am','start','-n','com.flowcare.demo/.MainActivity')
find('How can we help?')
screen('01-welcome-light')
tap('Dark');screen('02-welcome-dark')
tap('Light')
tap('Book hospital visit');tap('Self');tap('Choose a doctor')
find('Select Dr. Anya Sharma');screen('03-doctor-discovery')
tap('Select Dr. Anya Sharma')
# Focus the last text field (visit time); patient defaults to Self.
fields=[n for n in nodes() if n.get('class')=='android.widget.EditText']
assert len(fields)==2,'Expected patient name and visit-time fields'
tap_node(fields[-1]);adb('shell','input','text','2030-11-15%s09:00')
adb('shell','input','keyevent','KEYCODE_BACK')
tap('Confirm appointment')
find('Queue');screen('04-patient-queue')
top();tap('Doctor')
find('Console')
find('Complete current / call next')
tap('Complete current / call next')
tap('#1001 · Self · active')
tap('Order blood lab · ₹450')
find('Lab bill sent to patient')
fields=[n for n in nodes() if n.get('class')=='android.widget.EditText']
if not fields:
    down();fields=[n for n in nodes() if n.get('class')=='android.widget.EditText']
assert fields,'Prescription field is missing'
tap_node(fields[-1]);adb('shell','input','text','Synthetic%sinstructions')
adb('shell','input','keyevent','KEYCODE_BACK')
tap('Issue prescription');find('Prescription sent')
screen('05-doctor-consultation')
top();tap('Home');find('Console')
# System Back from Settings should return to the doctor console.
tap('Settings');find('Backend URL')
adb('shell','input','keyevent','KEYCODE_BACK');find('Console')
adb('shell','settings','put','system','font_scale','1.3')
time.sleep(2);screen('06-large-text')
adb('shell','settings','put','system','font_scale','1.0')
print('Native booking, role switching, theme switching and system Back passed.')
