package com.example.core.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

typealias AuthenticationService = AuthService

/**
 * Authentication service managing Google Sign-In via Jetpack [CredentialManager]
 * and [FirebaseAuth] session state. Provided as a singleton by [com.example.core.di.AppContainer].
 */
class AuthService(
  private val appContext: Context,
  private val customFirebaseAuth: FirebaseAuth? = null
) {

  val firebaseAuth: FirebaseAuth?
    get() = customFirebaseAuth ?: runCatching { FirebaseAuth.getInstance() }.getOrNull()

  val currentUser: FirebaseUser?
    get() = runCatching { firebaseAuth?.currentUser }.getOrNull()

  val currentUserId: String?
    get() = currentUser?.uid

  val isSignedIn: Boolean
    get() = currentUser != null

  fun authStateFlow(): Flow<FirebaseUser?> {
    val auth = firebaseAuth ?: return flowOf(null)
    return callbackFlow {
      val listener = FirebaseAuth.AuthStateListener { firebaseAuthInstance ->
        trySend(firebaseAuthInstance.currentUser)
      }
      auth.addAuthStateListener(listener)
      awaitClose { auth.removeAuthStateListener(listener) }
    }
  }

  suspend fun attemptAutoSignIn(context: Context): Result<FirebaseUser?> {
    val credentialManager = CredentialManager.create(context)
    val webClientId = context.getString(R.string.default_web_client_id)

    val googleIdOption = GetGoogleIdOption.Builder()
      .setServerClientId(webClientId)
      .setFilterByAuthorizedAccounts(true)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    return try {
      val result = credentialManager.getCredential(context, request)
      Result.success(firebaseSignInWithGoogle(result))
    } catch (e: NoCredentialException) {
      Result.success(null)
    } catch (e: GetCredentialException) {
      Log.w("AuthService", "Auto sign-in skipped", e)
      Result.failure(e)
    }
  }

  suspend fun signInWithGoogle(context: Context): Result<FirebaseUser?> {
    val credentialManager = CredentialManager.create(context)
    val webClientId = context.getString(R.string.default_web_client_id)

    val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    return try {
      val result = credentialManager.getCredential(context, request)
      Result.success(firebaseSignInWithGoogle(result))
    } catch (e: GetCredentialCancellationException) {
      Result.success(null)
    } catch (e: GetCredentialException) {
      Log.e("AuthService", "Google sign-in failed", e)
      Result.failure(e)
    }
  }

  private suspend fun firebaseSignInWithGoogle(result: GetCredentialResponse): FirebaseUser? {
    val credential = result.credential
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      val firebaseCredential =
        GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
      val auth = checkNotNull(firebaseAuth) { "FirebaseAuth is not initialized" }
      return auth.signInWithCredential(firebaseCredential).await().user
    }
    error("Unexpected credential type: ${credential.type}")
  }

  suspend fun signOut(context: Context = appContext) {
    runCatching { firebaseAuth?.signOut() }
    runCatching {
      CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
  }
}
