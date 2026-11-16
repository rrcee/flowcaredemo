package com.flowcare

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Firebase sends and verifies the code. No code or phone number is stored by the demo. */
@Composable internal fun PatientPhoneAccount(onBack: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf(auth.currentUser) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showCare by remember { mutableStateOf(false) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    if (showCare && user != null) { LiveCare(user = user!!, staffApp = false, onBack = { showCare = false }); return }

    fun signIn(credential: PhoneAuthCredential) {
        busy = true
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            busy = false
            message = if (task.isSuccessful) "Signed in." else "The code could not be verified. Check it and try again."
            if (task.isSuccessful) code = ""
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(onClick = onBack) { Text("Back to demo") }
            Text("FlowCare account", style = MaterialTheme.typography.headlineMedium)
            if (user != null) {
                Text("Signed in", style = MaterialTheme.typography.titleLarge)
                Text(user?.phoneNumber ?: user?.email ?: "FlowCare account", style = MaterialTheme.typography.bodyLarge)
                if (user?.isEmailVerified == true || user?.phoneNumber != null) {
                    OutlinedButton(onClick = { showCare = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Open connected care") }
                } else Text("Verify your email before opening connected care.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { auth.signOut(); verificationId = ""; code = "" }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Sign out") }
            } else {
                Text("Sign in with your phone", style = MaterialTheme.typography.titleLarge)
                Text("Enter your number with country code. Firebase sends a one-time code by SMS.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) },
                    label = { Text("Phone number") }, placeholder = { Text("+91 98765 43210") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                Text("Your number is sent to Google for sign-in and abuse prevention. SMS charges may apply.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    if (!phone.matches(Regex("\\+[1-9][0-9]{7,14}"))) message = "Enter a phone number with its country code, for example +91."
                    else if (activity == null) message = "Unable to start phone verification. Please reopen the app."
                    else {
                        busy = true; message = ""
                        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) { signIn(credential) }
                            override fun onVerificationFailed(error: FirebaseException) { busy = false; message = "Could not send the code. Phone sign-in may not be available yet; check your connection and try again." }
                            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) { verificationId = id; busy = false; message = "Code sent. Check your messages." }
                        }
                        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone).setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity).setCallbacks(callbacks).build()
                        PhoneAuthProvider.verifyPhoneNumber(options)
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (busy) "Please wait…" else "Send code") }
                if (verificationId.isNotBlank()) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("One-time code") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Button(onClick = { signIn(PhoneAuthProvider.getCredential(verificationId, code)) }, enabled = !busy && code.length == 6,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Verify and sign in") }
                }
                OutlinedButton(onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
                            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                            val credential = CredentialManager.create(context).getCredential(context, request).credential
                            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).addOnCompleteListener { result ->
                                    busy = false
                                    message = if (result.isSuccessful) "Signed in." else "Google sign-in could not be completed. Please try again."
                                }
                            } else { busy = false; message = "Google sign-in was not completed." }
                        } catch (_: androidx.credentials.exceptions.GetCredentialException) {
                            busy = false; message = "Google sign-in was cancelled or unavailable."
                        } catch (_: IllegalArgumentException) {
                            busy = false; message = "Google sign-in could not be completed."
                        }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Continue with Google") }
            }
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium)
            Text("The offline demo is separate from your real account.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

