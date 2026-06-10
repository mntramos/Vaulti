package com.vaulti.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.vaulti.app.data.database.VaultiDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: VaultiDatabase
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            val loggedIn = user != null && user.isEmailVerified
            trySend(loggedIn)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firebaseAuth.currentUser?.isEmailVerified == true)

    private val _authResult = MutableSharedFlow<Result<Unit>>()
    val authResult: SharedFlow<Result<Unit>> = _authResult.asSharedFlow()

    private val _verificationEmailSent = Channel<Unit>(Channel.BUFFERED)
    val verificationEmailSent: Flow<Unit> = _verificationEmailSent.receiveAsFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val user = firebaseAuth.currentUser
                if (user != null && !user.isEmailVerified) {
                    firebaseAuth.signOut()
                    _authResult.emit(Result.failure(EmailNotVerifiedException()))
                } else {
                    _authResult.emit(Result.success(Unit))
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.e(TAG, "Login failed: user not found", e)
                _authResult.emit(Result.failure(Exception("Incorrect e-mail/password. Please try again")))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e(TAG, "Login failed: wrong password", e)
                _authResult.emit(Result.failure(Exception("Incorrect e-mail/password. Please try again")))
            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                _authResult.emit(Result.failure(e))
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                firebaseAuth.currentUser?.sendEmailVerification()?.await()
                firebaseAuth.signOut()
                _verificationEmailSent.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Register failed", e)
                _authResult.emit(Result.failure(e))
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).await()
                _authResult.emit(Result.success(Unit))
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in failed", e)
                _authResult.emit(Result.failure(e))
            }
        }
    }

    fun reportGoogleSignInError(message: String) {
        viewModelScope.launch {
            _authResult.emit(Result.failure(Exception(message)))
        }
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            firebaseAuth.signOut()
        }
    }

    class EmailNotVerifiedException : Exception("Please verify your email before logging in")

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
