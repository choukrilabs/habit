package com.example.feature.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.firebase.AuthService
import com.example.core.di.appContainer
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Facade delegating to [AuthService] provided by [com.example.core.di.AppContainer].
 */
object GoogleAuthManager {

  fun authStateFlow(context: Context): Flow<FirebaseUser?> =
    context.appContainer.authService.authStateFlow()

  suspend fun attemptAutoSignIn(context: Context): Result<FirebaseUser?> =
    context.appContainer.authService.attemptAutoSignIn(context)

  suspend fun onGoogleSignInClicked(context: Context): Result<FirebaseUser?> =
    context.appContainer.authService.signInWithGoogle(context)

  suspend fun signOut(context: Context) {
    context.appContainer.authService.signOut(context)
  }
}

@Composable
fun GoogleAuthScreen(
  modifier: Modifier = Modifier,
  authService: AuthService = LocalContext.current.appContainer.authService,
  onContinueWithoutSignIn: () -> Unit = { authService.continueWithoutSignIn() }
) {
  val context = LocalContext.current
  val colors = LocalHabitRadarColors.current
  val scope = rememberCoroutineScope()

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(authService) {
    isLoading = true
    authService.attemptAutoSignIn(context)
    isLoading = false
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(colors.bg)
      .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(
        modifier = Modifier
          .size(68.dp)
          .clip(CircleShape)
          .background(colors.accent.copy(alpha = 0.16f))
          .border(1.dp, colors.accent.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.CloudDone,
          contentDescription = "Cloud Sync",
          tint = colors.accent,
          modifier = Modifier.size(32.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Every small mark counts.",
        fontFamily = InstrumentSerifFamily,
        fontStyle = FontStyle.Italic,
        fontSize = 36.sp,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Sign in with Google to keep your habits, streaks, and daily check-ins safely backed up and synced in the cloud.",
        fontFamily = PlusJakartaSansFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp)
      )

      Spacer(modifier = Modifier.height(28.dp))

      HabitRadarCard(
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = colors.accent,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Zero-trust user-owned Firestore storage",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = colors.textSecondary
            )
          }

          Spacer(modifier = Modifier.height(18.dp))

          Button(
            onClick = {
              errorMessage = null
              isLoading = true
              scope.launch {
                val res = authService.signInWithGoogle(context)
                isLoading = false
                res.exceptionOrNull()?.let { err ->
                  errorMessage = err.localizedMessage ?: "Sign-in could not be completed. Please try again."
                }
              }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = Color(0xFF0E0F12)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("google_sign_in_button")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                color = Color(0xFF0E0F12),
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Connecting...",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
              )
            } else {
              Text(
                text = "Continue with Google",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          androidx.compose.material3.OutlinedButton(
            onClick = {
              authService.continueWithoutSignIn()
              onContinueWithoutSignIn()
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(999.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surface2),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = colors.textPrimary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("continue_without_sign_in_button")
          ) {
            Text(
              text = "Continue without signing in",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 15.sp,
              fontWeight = FontWeight.Medium
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Habits & widgets work offline. Cloud backup & restore stay disabled until you sign in.",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
          )

          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = errorMessage!!,
              fontFamily = PlusJakartaSansFamily,
              fontSize = 13.sp,
              color = Color(0xFFEE9A9A),
              textAlign = TextAlign.Center,
              modifier = Modifier.testTag("auth_error_text")
            )
          }
        }
      }
    }
  }
}
