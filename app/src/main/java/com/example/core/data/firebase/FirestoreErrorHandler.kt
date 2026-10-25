package com.example.core.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
  CREATE,
  UPDATE,
  DELETE,
  GET,
  LIST,
  WRITE
}

fun handleFirestoreError(
  error: Throwable,
  operationType: OperationType,
  path: String?
): Nothing {
  val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
  val currentUser = auth?.currentUser

  val providerInfoArray = JSONArray()
  currentUser?.providerData?.forEach { userInfo ->
    val providerObj = JSONObject().apply {
      put("providerId", userInfo.providerId)
      put("displayName", userInfo.displayName)
      put("email", userInfo.email)
      put("photoUrl", userInfo.photoUrl?.toString())
    }
    providerInfoArray.put(providerObj)
  }

  val authInfo = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("isAnonymous", currentUser?.isAnonymous)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", providerInfoArray)
  }

  val errInfo = JSONObject().apply {
    put("error", error.message ?: error.toString())
    put("operationType", operationType.name.lowercase())
    put("path", path)
    put("authInfo", authInfo)
  }

  val jsonString = errInfo.toString()
  Log.e("FirestoreError", jsonString, error)
  throw RuntimeException(jsonString, error)
}
