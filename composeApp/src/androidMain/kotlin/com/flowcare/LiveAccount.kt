package com.flowcare

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/** Account access is isolated from the offline demo until live clinical services are ready. */
@Composable
internal fun LiveAccount(onBack: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(onClick = onBack) { Text("Back to demo") }
            Text("FlowCare account", style = MaterialTheme.typography.headlineMedium)
            Text("Account sign-in is being connected. Appointments and clinical services are not available through this account yet.",
                style = MaterialTheme.typography.bodyMedium)
            val current = user
            if (current != null) {
                Text("Signed in as ${current.email ?: "FlowCare user"}", style = MaterialTheme.typography.titleMedium)
                Text(if (current.isEmailVerified) "Email verified" else "Email not verified", style = MaterialTheme.typography.bodyMedium)
                if (!current.isEmailVerified) {
                    OutlinedButton(onClick = {
                        busy = true
                        current.sendEmailVerification().addOnCompleteListener { result ->
                            busy = false
                            message = if (result.isSuccessful) "Verification email sent. Check your inbox." else "Could not send verification email. Try again."
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Send verification email") }
                    OutlinedButton(onClick = {
                        busy = true
                        current.reload().addOnCompleteListener { result ->
                            busy = false
                            user = auth.currentUser
                            message = if (result.isSuccessful && auth.currentUser?.isEmailVerified == true) "Email verified." else "Email is not verified yet. Check your inbox and try again."
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("I've verified my email") }
                }
                Button(onClick = { auth.signOut(); password = ""; message = "Signed out." },
                    enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Sign out") }
            } else {
                Text(if (register) "Create a patient account" else "Sign in", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(email, { email = it }, label = { Text("Email address") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (!email.contains('@') || password.length < 6) {
                        message = "Enter a valid email address and a password of at least 6 characters."
                    } else {
                        busy = true
                        val task = if (register) auth.createUserWithEmailAndPassword(email.trim(), password)
                            else auth.signInWithEmailAndPassword(email.trim(), password)
                        task.addOnCompleteListener { result ->
                            busy = false
                            if (result.isSuccessful) {
                                password = ""
                                message = if (register) "Account created. Verify your email before using live services." else "Signed in."
                            } else message = "Could not ${if (register) "create your account" else "sign in"}. Check your details and try again."
                        }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(if (busy) "Please wait…" else if (register) "Create patient account" else "Sign in")
                }
                OutlinedButton(onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val google = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
                            val request = GetCredentialRequest.Builder().addCredentialOption(google).build()
                            val result = CredentialManager.create(context).getCredential(context, request)
                            val credential = result.credential
                            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                busy = false
                                message = "Google sign-in was not completed. Please try again."
                            } else {
                                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).addOnCompleteListener { task ->
                                    busy = false
                                    message = if (task.isSuccessful) "Signed in." else "Google sign-in was not completed. Please try again."
                                }
                            }
                        } catch (_: androidx.credentials.exceptions.GetCredentialException) {
                            busy = false
                            message = "Google sign-in was cancelled or unavailable. Please try again."
                        } catch (_: IllegalArgumentException) {
                            busy = false
                            message = "Google sign-in was not completed. Please try again."
                        }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Continue with Google") }
                TextButton(onClick = { register = !register; password = ""; message = "" }, enabled = !busy) {
                    Text(if (register) "Already have an account? Sign in" else "Create a patient account")
                }
                TextButton(onClick = {
                    if (!email.contains('@')) message = "Enter your email address above first."
                    else {
                        busy = true
                        auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener {
                            busy = false
                            message = "If an account exists for that address, a reset email may arrive shortly."
                        }
                    }
                }, enabled = !busy) { Text("Reset password") }
            }
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium)
            Text("Care Team and HQ access requires an authorized staff account; creating an account does not grant staff permissions.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
