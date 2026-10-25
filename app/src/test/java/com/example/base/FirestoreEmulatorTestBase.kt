package com.example.base

import android.os.Looper
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
abstract class FirestoreEmulatorTestBase {

  lateinit var db: FirebaseFirestore
  lateinit var auth: FirebaseAuth
  lateinit var databaseId: String

  private val host = "127.0.0.1"
  private val firestorePort = 8085
  private val authPort = 9099

  @Before
  open fun setUp() {
    ShadowLog.stream = System.out
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    val projectId = FirebaseApp.getInstance().options.projectId
      ?: throw IllegalStateException("Project ID not found in google-services.json")
    databaseId = context.getString(R.string.firestore_database_id)

    db = FirebaseFirestore.getInstance(databaseId)
    auth = FirebaseAuth.getInstance()

    try {
      db.useEmulator(host, firestorePort)
      auth.useEmulator(host, authPort)

      val settings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
        .build()
      db.firestoreSettings = settings
    } catch (e: IllegalStateException) {
      // Emulator already configured on singleton instance
    }

    clearFirestoreEmulator(projectId, databaseId)
    clearAuthEmulator(projectId)
  }

  @After
  open fun tearDown() {
    runEmulatorTest {
      try {
        auth.signOut()
      } catch (e: Exception) {
        // Ignore cleanup errors
      }
    }
  }

  /**
   * Executes a suspend test body on [Dispatchers.IO] while continuously draining Robolectric's
   * main looper so Firebase Auth and Firestore main-thread callbacks complete without deadlock.
   */
  protected fun runEmulatorTest(block: suspend CoroutineScope.() -> Unit) {
    val errorRef = AtomicReference<Throwable?>(null)
    val done = AtomicBoolean(false)
    val job = CoroutineScope(Dispatchers.IO).launch {
      try {
        block()
      } catch (t: Throwable) {
        errorRef.set(t)
      } finally {
        done.set(true)
      }
    }
    val shadowMainLooper = Shadows.shadowOf(Looper.getMainLooper())
    val deadline = System.currentTimeMillis() + 45_000L
    while (!done.get() && System.currentTimeMillis() < deadline) {
      shadowMainLooper.idle()
      Thread.sleep(10)
    }
    shadowMainLooper.idle()
    if (!done.get()) {
      job.cancel()
      throw AssertionError("Emulator test timed out after 45 seconds")
    }
    errorRef.get()?.let { throw it }
  }

  protected suspend fun signInTestUser(email: String, pass: String = "password123"): String {
    return try {
      auth.createUserWithEmailAndPassword(email, pass).await().user!!.uid
    } catch (e: Exception) {
      auth.signInWithEmailAndPassword(email, pass).await().user!!.uid
    }
  }

  private fun clearFirestoreEmulator(projectId: String, databaseId: String) {
    val url = URL("http://$host:$firestorePort/emulator/v1/projects/$projectId/databases/$databaseId/documents")
    val conn = url.openConnection() as HttpURLConnection
    try {
      conn.connectTimeout = 5000
      conn.readTimeout = 5000
      conn.requestMethod = "DELETE"
      if (conn.responseCode != 200) {
        Log.w("FirestoreTestBase", "Failed to clear Firestore ($databaseId): ${conn.responseMessage}")
      }
    } finally {
      conn.disconnect()
    }
  }

  private fun clearAuthEmulator(projectId: String) {
    val url = URL("http://$host:$authPort/emulator/v1/projects/$projectId/accounts")
    val conn = url.openConnection() as HttpURLConnection
    try {
      conn.connectTimeout = 5000
      conn.readTimeout = 5000
      conn.requestMethod = "DELETE"
      if (conn.responseCode != 200) {
        Log.w("FirestoreTestBase", "Failed to clear Auth: ${conn.responseMessage}")
      }
    } finally {
      conn.disconnect()
    }
  }
}
