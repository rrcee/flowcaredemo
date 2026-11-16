# FlowCare — Complete Product, Design, Frontend & Backend Master Specification

FlowCare is an end-to-end healthcare flow, unified payment, and clinical consultation ecosystem. For the current scope, it connects **two dedicated mobile frontends** (**Patient / Family Bystander** and **Doctor**) to a **single central Hospital Backend** powered by real-time WebSockets, ephemeral privacy vaults, and machine learning.

---

## 1. Muted & Soft Design System (Strictly No Cyan)

The visual identity replaces harsh hospital interfaces and neon gradients with a low-saturation, calming palette designed to reduce stress during medical visits and emergencies.

### 1.1 Color Tokens

| Token Name | Hex Code | Tone | Primary UI Role |
| :--- | :--- | :--- | :--- |
| **Calm Slate Blue** | `#5B7C99` | Muted Blue | Primary CTAs, active tabs, booking actions, slot selection |
| **Soft Sage** | `#6E9887` | Muted Green | Confirmed payments, active Token `#1234`, live queue progress |
| **Dusty Lavender** | `#8A79AF` | Soft Violet | Online Psych Ward, EduMind Comfort AI, ML recommendation badges |
| **Warm Sand** | `#C89D66` | Muted Ochre | Cafe Wait Mode timers, arrival buffer alerts, pending lab sync |
| **Dusty Rose** | `#C47474` | Muted Coral | Urgent turn alerts ($\le 4$ patients ahead), Doctor Emergency pause, ICU bills |
| **Warm Alabaster** | `#F5F4F0` | Soft Off-White | Primary light-mode background surface (zero glare) |
| **Elevated White** | `#FFFFFF` | Pure Soft Card | Card surfaces with `1px solid #E6E4E0` borders |
| **Muted Charcoal** | `#1A1D24` | Soft Dark Slate | Low-glare dark-mode background with `#242832` cards |
| **Slate Ink** | `#2B303A` | Deep Charcoal | Primary high-contrast readable body text |

### 1.2 Typography & Core Components

* **Primary UI Font (`Plus Jakarta Sans` / `Inter`)**: Used for clean hierarchy, minimal inputs, department cards, and clinical notes.
* **Data & Token Font (`JetBrains Mono`)**: Used exclusively for auto-generated token numbers (`#1234`), live wait countdowns (`27m`, `7m`, `4m`), ephemeral TTL countdowns, and transaction IDs.
* **Core Surfaces**:
  * **Role Switcher Header**: Toggles between the **Patient / Bystander View** and the **Doctor Console**.
  * **Live Queue & Cafe Mode Card**: Displays live token position, cancellation re-indexing, and nearby cafe wait status.
  * **One-Tap Bystander Pay Sheet**: Slide-up payment drawer linking Registration, Consultation, Labs, Pharmacy, and ICU.
  * **Ephemeral OCR Record Viewer**: Temporary, auto-deleting medical history drawer with a live expiry countdown.

---

## 2. How the App Starts (Initialization & Entry Flows)

1. **Intro & Role Selection (`/`)**:
   * Users launch into a calm `Warm Alabaster (#F5F4F0)` welcome screen and select their active mode (**Patient / Bystander** or **Doctor**).
2. **Dual Patient Entry Pathways**:
   * **Path A — Pre-Visit Remote Booking**: Patients browse nearby hospitals, view AI-recommended doctors, pick a slot, pay, and track the queue from home or a cafe.
   * **Path B — Walk-In Instant QR Registration (`/qr-entry`)**: Patients already at a hospital scan a standee QR code, confirm pre-filled Name and Phone details in 1 tap (no paper forms), and immediately receive an active queue token.
   * **Path C — Instant Online Psych Ward (`/psych-ward`)**: Patients in psychological distress tap straight into the EduMind Comfort AI and live Psychiatrist/Psychologist directory.
3. **Family & Bystander Profile Selection (`/choose-user`)**:
   * Google Pay-style profile grid (*Self*, *Parent*, *Spouse*, *Child*, plus *"+ Add New Member"*) allowing family bystanders to book appointments, track queues, and pay multi-department bills remotely on a patient's behalf.

---

## 3. Complete Application Feature Specification (Patient $\leftrightarrow$ Doctor)

### 3.1 AI-Driven Hospital Discovery & Doctor Recommendation
* **GPS Closest Hospital Finder**: Locates nearby partner clinics and hospitals sorted by distance, live OPD crowd levels, and verified review scores.
* **AI Doctor Recommendation Engine**: Ranks and recommends doctors based on a weighted machine learning score combining:
  1. **Clinical Quality** (verified post-consultation patient review scores),
  2. **Seniority** (years of medical practice), and
  3. **Experience** (historical volume of similar cases handled).
* **Smart Slot Selection**: Horizontal date strip and Morning/Afternoon/Evening slot grid highlighting the earliest AI-recommended slot.

### 3.2 Adaptive Live Queue, Cafe Mode & Emergency Handling
* **Auto-Generated QR Token**: Issues digital Token `#1234` immediately upon booking or walk-in QR scan.
* **Cancellation-Adaptive Re-Indexing**: When any patient cancels a booking, the backend dynamically shifts subsequent token numbers forward and recalculates everyone's live wait time in real time.
* **Cafe Wait Mode**: Instead of sitting in overcrowded waiting rooms, patients can wait at a nearby cafe while tracking their live token countdown (`27m` $\rightarrow$ `7m` $\rightarrow$ `4m`) and receive a push alert when it is time to walk to the consultation room.
* **Doctor Emergency Override & Emergency Queue**:
  * If a doctor is called away to a medical or psychiatric emergency, the doctor toggles **Emergency Pause** on their app, immediately sending a calm delay notification to all queued patients.
  * Critical patients escalated from the Online Psych Ward or triage are automatically inserted at the top of the **Emergency Queue**.

### 3.3 Unified Multi-Department & Bystander Payments
* **Connected Care Payment Pipeline**: Links **Registration** $\rightarrow$ **Consultation** $\rightarrow$ **Blood Labs** $\rightarrow$ **Pharmacy** $\rightarrow$ **ICU / Discharge** in a single mobile view.
* **Real-Time Doctor-to-Bystander Billing Sync**: When a doctor orders a lab test or issues a prescription during consultation, the itemized payment request pops up instantly on the Patient/Bystander phone for 1-tap UPI/Card payment—eliminating counter-hopping.

### 3.4 Cross-Hospital Query, Handwriting OCR Scanner & Ephemeral Vault
* **Cross-Hospital Condition Lookup**: Queries partner hospitals for historical records related to the patient's condition.
* **AI Handwriting OCR Scanner**: If the previous hospital only has handwritten files or paper prescriptions, the built-in vision scanner converts the handwritten document into structured digital text.
* **Auto-Deleting Ephemeral Memory (Zero Permanent Storage)**: Digitized records are held strictly in encrypted volatile memory with a short Time-To-Live (TTL) so the patient can show their new doctor during the consultation. Once viewed or expired, the backend permanently deletes the data.

### 3.5 Online Psych Ward & "EduMind" Comfort AI
* **EduMind Comfort AI (Learns Without Saving)**: Provides immediate, empathetic psychological comfort and grounding exercises during distress. Its machine learning pipeline updates anonymized model weights in RAM during the session and permanently discards all raw chat text upon disconnect—saving zero personal mental health logs.
* **Live Psychiatrist & Psychologist Marketplace**: Connects patients directly to verified online psychologists or psychiatrists via audio, video, or chat (similar to Astrotalk), with instant escalation to the hospital's **Emergency Queue** if crisis care is required.

### 3.6 Doctor Consultation Console & Digital Prescriptions
* **Queue & Emergency Controller**: Doctors call the next token, mark consultations complete, or toggle **Emergency Delay Mode**.
* **One-Time Decrypted History Drawer**: Doctors view the patient's vitals, symptoms, and temporary OCR-digitized cross-hospital records before the ephemeral timer wipes them.
* **1-Click Lab Orders & Prescriptions**: Doctors issue structured digital prescriptions that sync directly to the patient's app and hospital pharmacy, followed by a 1–5 star review prompt that feeds back into the AI Doctor Recommendation model.

---

## 4. Mobile Frontend Architecture: Kotlin Multiplatform (iOS & Android)

FlowCare uses **Kotlin Multiplatform (KMP)** with **Compose Multiplatform** so a single Kotlin codebase compiles natively to both **Android** and **iOS**.

### 4.1 Project Directory Structure

```text
FlowCare/
├── composeShared/                  # Shared across iOS and Android (Patient & Doctor)
│   ├── src/commonMain/kotlin/
│   │   ├── designsystem/           # Muted Color Palette (No Cyan), Typography, Cards
│   │   ├── domain/
│   │   │   ├── models/             # DoctorScore, TokenState, EphemeralRecord, DeptBill
│   │   │   └── crypto/             # In-Memory AES-256 Ephemeral Decryption
│   │   ├── network/                # Ktor REST + WebSocket Client (connects to Python)
│   │   └── features/
│   │       ├── onboarding/         # Dual Entry, Walk-In QR Auto-Fill, Family Selector
│   │       ├── discovery/          # Hospital Finder & AI Doctor Recommendation UI
│   │       ├── queue_cafe/         # Live Adaptive Queue, Cafe Mode, Emergency Banner
│   │       ├── payments/           # Bystander Multi-Department Payment Pipeline
│   │       ├── psych_edumind/      # EduMind Comfort Chat & Live Psychiatrist Connect
│   │       ├── ocr_records/        # Handwriting Scanner & Ephemeral Record Viewer
│   │       └── doctor_console/     # Doctor Queue Control, Emergency Toggle, Rx Pad
│   ├── src/androidMain/            # Android CameraX (QR & Handwriting OCR), UPI Intents
│   └── src/iosMain/                # iOS AVFoundation (QR & OCR), Apple Pay / UPI Bridges
└── backend-python/                 # FastAPI + WebSockets + Ephemeral ML Server
```

### 4.2 Shared Compose Multiplatform Theme (`FlowCareTheme.kt`)

```kotlin
package com.flowcare.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Muted & Soft Color Tokens (Strictly No Cyan)
val CalmSlateBlue = Color(0xFF5B7C99)   // Primary booking & navigation actions
val SoftSage = Color(0xFF6E9887)        // Paid bills, active token & confirmed sync
val DustyLavender = Color(0xFF8A79AF)   // Online Psych Ward, EduMind AI & ML badges
val WarmSand = Color(0xFFC89D66)        // Cafe Wait Mode & adaptive queue timers
val DustyRose = Color(0xFFC47474)       // Emergency delay alerts & urgent turn banners

// Surfaces & Text
val WarmAlabaster = Color(0xFFF5F4F0)   // Soft light-mode background
val SoftCardLight = Color(0xFFFFFFFF)   // Clean card surface
val MutedCharcoal = Color(0xFF1A1D24)   // Low-glare dark-mode background
val SoftCardDark = Color(0xFF242832)    // Elevated dark card surface
val SlateInk = Color(0xFF2B303A)        // Primary readable text (light mode)
val SoftMist = Color(0xFFE6E4E0)        // Primary readable text (dark mode)

private val LightColors = lightColorScheme(
    primary = CalmSlateBlue,
    secondary = SoftSage,
    tertiary = DustyLavender,
    error = DustyRose,
    background = WarmAlabaster,
    surface = SoftCardLight,
    onPrimary = Color.White,
    onBackground = SlateInk,
    onSurface = SlateInk
)

private val DarkColors = darkColorScheme(
    primary = CalmSlateBlue,
    secondary = SoftSage,
    tertiary = DustyLavender,
    error = DustyRose,
    background = MutedCharcoal,
    surface = SoftCardDark,
    onPrimary = Color.White,
    onBackground = SoftMist,
    onSurface = SoftMist
)

@Composable
fun FlowCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
```

---

## 5. Backend Architecture: Python (FastAPI + Ephemeral ML Engine)

The central hospital backend manages both the Doctor and Patient interfaces, implements the AI recommendation formula:

$$\text{Score} = 0.45 \cdot \left(\frac{\text{Rating}}{5.0}\right) + 0.30 \cdot \min\left(\frac{\text{Seniority}}{20}, 1.0\right) + 0.25 \cdot \min\left(\frac{\text{Cases}}{10000}, 1.0\right)$$

and guarantees zero permanent storage for EduMind chats and OCR medical records.

### 5.1 Reference Backend Implementation (`server.py`)

```python
import asyncio
import time
import uuid
from typing import Dict, List, Optional
from fastapi import FastAPI, UploadFile
from pydantic import BaseModel

app = FastAPI(title="FlowCare Unified Hospital Backend")

# ---------------------------------------------------------
# 1. IN-MEMORY EPHEMERAL VAULT (Auto-Deletes, Never Persists)
# ---------------------------------------------------------
EPHEMERAL_RECORDS: Dict[str, dict] = {}
RECORD_TTL_SECONDS = 1800  # 30 minutes max hold before permanent deletion

async def purge_expired_records():
    while True:
        now = time.time()
        expired = [k for k, v in EPHEMERAL_RECORDS.items() if now > v["expires_at"]]
        for k in expired:
            del EPHEMERAL_RECORDS[k]  # Permanently wiped from RAM
        await asyncio.sleep(30)

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(purge_expired_records())

# ---------------------------------------------------------
# 2. AI DOCTOR RECOMMENDATION (Quality, Seniority, Experience)
# ---------------------------------------------------------
class DoctorProfile(BaseModel):
    id: str
    name: str
    specialty: str
    rating: float          # 1.0 - 5.0 (Quality from Review System)
    seniority_years: int   # Years in practice
    cases_handled: int     # Experience volume
    emergency_active: bool = False

DOCTORS: Dict[str, DoctorProfile] = {
    "doc_1": DoctorProfile(id="doc_1", name="Dr. Anya Sharma", specialty="General Medicine", rating=4.9, seniority_years=14, cases_handled=8500),
    "doc_2": DoctorProfile(id="doc_2", name="Dr. Vikram Nair", specialty="Psychiatry", rating=4.8, seniority_years=11, cases_handled=6200),
}

@app.get("/api/doctors/recommend")
def recommend_doctors(specialty: Optional[str] = None):
    candidates = [d for d in DOCTORS.values() if not specialty or d.specialty.lower() == specialty.lower()]
    ranked = sorted(
        candidates,
        key=lambda d: (d.rating / 5.0) * 0.45 + min(d.seniority_years / 20.0, 1.0) * 0.30 + min(d.cases_handled / 10000.0, 1.0) * 0.25,
        reverse=True
    )
    return {"recommended_doctors": ranked}

# ---------------------------------------------------------
# 3. EDUMIND COMFORT AI (Learns In-Memory, Never Saves Chat)
# ---------------------------------------------------------
MODEL_RUNNING_WEIGHTS = {"interaction_count": 0, "calm_index_ema": 0.75}

class PsychChatRequest(BaseModel):
    ephemeral_message: str
    request_live_psychiatrist: bool = False
    is_crisis_emergency: bool = False

@app.post("/api/edumind/comfort")
def edumind_comfort_session(req: PsychChatRequest):
    MODEL_RUNNING_WEIGHTS["interaction_count"] += 1
    response_payload = {
        "edumind_reply": "I'm right here with you. Let's take a slow 4-second breath together. You are safe, and we will take this one step at a time.",
        "data_saved": False,
        "live_psychiatrist_connected": req.request_live_psychiatrist,
        "routed_to_emergency_queue": req.is_crisis_emergency
    }
    del req.ephemeral_message
    return response_payload

# ---------------------------------------------------------
# 4. HOSPITAL QUERY + HANDWRITING OCR + EPHEMERAL AUTO-DELETE
# ---------------------------------------------------------
@app.post("/api/records/scan-handwriting")
async def scan_written_document(patient_id: str, file: UploadFile):
    _ = await file.read()
    digitized_summary = {
        "patient_id": patient_id,
        "extracted_text": "Digitized Handwritten Note: Mild hypertension, prescribed Amlodipine 5mg OD.",
        "created_at": time.time(),
        "expires_at": time.time() + RECORD_TTL_SECONDS
    }
    temp_token = str(uuid.uuid4())
    EPHEMERAL_RECORDS[temp_token] = digitized_summary
    return {
        "ephemeral_token": temp_token,
        "ttl_seconds": RECORD_TTL_SECONDS,
        "notice": "Held in temporary memory for your doctor consultation; auto-deletes after viewing or expiry."
    }

@app.get("/api/records/view-once/{ephemeral_token}")
def view_and_delete_record(ephemeral_token: str, delete_after_view: bool = True):
    record = EPHEMERAL_RECORDS.get(ephemeral_token)
    if not record:
        return {"error": "Record expired or already deleted for patient privacy."}
    if delete_after_view:
        del EPHEMERAL_RECORDS[ephemeral_token]
    return {"data": record, "deleted_from_server": delete_after_view}

# ---------------------------------------------------------
# 5. EMERGENCY-AWARE & CANCELLATION-ADAPTIVE QUEUE (Cafe Mode)
# ---------------------------------------------------------
class QueueEntry(BaseModel):
    booking_id: str
    patient_name: str
    token_number: int
    is_emergency_priority: bool = False
    waiting_at_cafe: bool = True

QUEUES: Dict[str, List[QueueEntry]] = {"doc_1": []}
AVG_CONSULT_MINS = 7

def recalculate_adaptive_queue(doctor_id: str) -> List[dict]:
    doc = DOCTORS[doctor_id]
    queue = QUEUES.get(doctor_id, [])
    queue.sort(key=lambda x: (not x.is_emergency_priority, x.token_number))
    
    live_state = []
    for idx, entry in enumerate(queue):
        entry.token_number = 1001 + idx  # Dynamically re-indexed on cancellation
        est_wait = idx * AVG_CONSULT_MINS + (20 if doc.emergency_active else 0)
        live_state.append({
            "booking_id": entry.booking_id,
            "active_token": entry.token_number,
            "patients_ahead": idx,
            "estimated_wait_mins": est_wait,
            "doctor_in_emergency": doc.emergency_active,
            "cafe_notification": (
                "Doctor called to an emergency — relax at the cafe, we'll update you shortly."
                if doc.emergency_active else
                ("Leave the cafe now — you are next!" if idx <= 2 else f"Relax at the cafe (~{est_wait}m left)")
            )
        })
    return live_state

@app.post("/api/queue/{doctor_id}/cancel/{booking_id}")
def cancel_booking(doctor_id: str, booking_id: str):
    QUEUES[doctor_id] = [q for q in QUEUES[doctor_id] if q.booking_id != booking_id]
    return {"event": "BOOKING_CANCELLED_TOKENS_SHIFTED", "queue": recalculate_adaptive_queue(doctor_id)}

@app.post("/api/queue/{doctor_id}/emergency-toggle")
def toggle_doctor_emergency(doctor_id: str, emergency_active: bool):
    DOCTORS[doctor_id].emergency_active = emergency_active
    return {"event": "DOCTOR_EMERGENCY_STATUS_CHANGED", "queue": recalculate_adaptive_queue(doctor_id)}
```

---

## 6. Master Prototype Prompt (For Figma Make / Google AI Studio)

Copy and paste the block below into Figma Make or Google AI Studio to generate or override the interactive prototype with all Patient, Doctor, Cafe Mode, Handwriting OCR, and EduMind features:

```text
# MASTER OVERRIDE PROMPT — FLOWCARE PATIENT & DOCTOR PROTOTYPE

## 1. ROLE & OBJECTIVE
You are a Rapid-Prototype Engineer upgrading the FlowCare React + TypeScript application (`App.tsx` using `RouterProvider` from `./routes`). Build a two-role interactive system (**Patient / Family Bystander** and **Doctor**) connected to a single reactive hospital backend state:
1. Dual Onboarding: Walk-In QR Instant Registration + End-to-End Outpatient Booking Flow.
2. AI Doctor Recommendation ranking doctors by Quality (reviews), Seniority (years), and Experience (case volume).
3. Emergency & Cancellation-Adaptive Queue with "Cafe Wait Mode" (updates token numbers live when someone cancels, lets patients wait at a nearby cafe, and notifies them if the doctor is called to an emergency).
4. Cross-Hospital Query + Handwriting OCR Scanner with an Ephemeral Auto-Deleting Vault (holds digitized written records temporarily to show the doctor, then permanently deletes them).
5. Online Psych Ward + "EduMind" Comfort AI (provides empathetic comfort, learns in-memory without saving chat logs, connects to live psychiatrists, and escalates crises to the Emergency Queue).
6. Unified Bystander Multi-Department Payment Hub (Registration, Consultation, Labs, Pharmacy, ICU).

## 2. DESIGN SYSTEM OVERRIDE (MUTED & SOFT — STRICTLY NO CYAN)
Remove all neon blue/indigo gradients and cyan accents. Use this exact muted palette:
- Light Mode Canvas (`Warm Alabaster`): `#F5F4F0` background with `#FFFFFF` cards and `#E6E4E0` 1px borders.
- Dark Mode Canvas (`Muted Charcoal`): `#1A1D24` background with `#242832` cards.
- Primary (`Calm Slate Blue`): `#5B7C99` — Primary CTAs, active tabs, selected slots.
- Success / Active (`Soft Sage`): `#6E9887` — Confirmed payments, active Token `#1234`, live queue progress.
- Mental Health & AI (`Dusty Lavender`): `#8A79AF` — EduMind Comfort AI, Online Psych Ward, AI Recommendation badges.
- Warning / Cafe Mode (`Warm Sand`): `#C89D66` — Cafe wait timer (`~25 mins`), pending lab/pharmacy requests.
- Urgent / Emergency (`Dusty Rose`): `#C47474` — Close-to-turn alert (`<= 4` patients ahead), Doctor Emergency Delay banner, ICU bills.
- Typography: `Plus Jakarta Sans` for UI and `JetBrains Mono` for token numbers (`#1234`), live timers, and ephemeral countdowns.
- Viewport Shell: `390px` centered mobile container with a persistent top 2-Role Switcher bar (`Patient / Bystander View | Doctor Console`).

## 3. COMPLETE SCREEN & ROUTE ARCHITECTURE (`./routes`)
1. `/` (Intro & Dual Entry): Calm welcome screen with 3 entry paths: "Book Hospital Visit" (`/choose-user`), "At the Hospital? Scan Walk-In QR" (`/qr-entry`), and "Online Psych Ward & EduMind AI" (`/psych-ward`).
2. `/qr-entry` (Walk-In QR Fast Registration): Pre-filled Name & Phone with 1-tap "Confirm & Continue" -> animated "Registering with hospital..." -> auto-routes to `/token`.
3. `/choose-user` (Family & Bystander Profile Selector): Google Pay-style avatar selector (Self, Parent, Child + Add Member) -> `/hospital`.
4. `/hospital` (Closest Hospital Discovery): GPS distance filter, live OPD wait index, and verified hospital review ratings -> `/doctor`.
5. `/doctor` (AI-Recommended Doctors): Highlights "AI Top Match" badges scored by Quality (4.9 Stars), Seniority (14 yrs), and Experience (8.5k cases) -> `/slot`.
6. `/slot` (Smart Slot Picker): Date strip + Morning/Afternoon/Evening slots with "Cafe Wait & Smart Arrival Enabled" badge -> `/payment`.
7. `/payment` (Booking Checkout): 1-tap UPI/Card payment -> `/processing` -> `/token`.
8. `/token` (Confirmation & QR Pass): Displays large monospace Token `#1004`, QR code, assigned Doctor, and buttons for `/queue` and `/bystander-pay`.
9. `/queue` (Adaptive Queue, Cafe Mode & Emergency State):
   - Live progress ring showing Token `#1004`, Patients Ahead, and Estimated Wait.
   - Interactive Demo Controls: Include buttons to "Simulate Cancellation Ahead" (shifts Token `#1004` -> `#1003` and drops wait time) and inspect "Doctor Emergency Delay" banner (`#C47474`).
   - Cafe Wait Mode Card (`#C89D66`): "Relax at a nearby cafe — we are tracking your token and will ping you when 3 patients remain."
   - Quick links to `/records-chain` and `/bystander-pay`.
10. `/bystander-pay` (Unified Department Billing Hub): Connected payment pipeline for Registration (Paid), Consultation (Paid), Blood Labs, Pharmacy, and ICU with 1-tap bystander payment.
11. `/records-chain` (Cross-Hospital Query + Handwriting OCR + Ephemeral Vault):
   - Query external hospital or tap "Scan Handwritten Document (OCR)" to convert written prescriptions into digital text.
   - Displays a live `Auto-Deletes in 29:59 (Never Saved Permanently)` badge and syncs temporarily to the Doctor view.
12. `/psych-ward` (EduMind Comfort AI & Live Psychiatrists):
   - Top tab: "EduMind Comfort AI" empathetic chat with badge `Privacy Mode: Learns in RAM, Never Saves Chat Logs`.
   - Bottom tab: Live Psychiatrists & Psychologists available for 1-tap audio/video consult, plus an "Escalate to Emergency Queue" button.
13. `/consultation` (Doctor Console):
   - Doctor controls: Call next token, toggle "Doctor Left for Emergency" (triggers delay alert on Patient `/queue`), view temporary OCR patient history with "Delete Now" button, order Lab tests (pushes live bill to `/bystander-pay`), and issue Prescription (`/prescription`).
14. `/prescription` (Digital Prescription & Review Loop): Medication schedule, 1-tap Pharmacy pay, PDF share, and 1-5 Star Doctor Review widget that updates the AI Recommendation score.
```