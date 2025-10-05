package at.mcbabo.authenticator.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.mcbabo.authenticator.R
import at.mcbabo.authenticator.internal.BiometricAuthManager

@Composable
fun AuthScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current as AppCompatActivity
    val promptManager by lazy {
        BiometricAuthManager(activity)
    }

    val biometricResult = promptManager.promptResults.collectAsState(null)

    LaunchedEffect(Unit) {
        promptManager.showBiometricPrompt(
            title = context.getString(R.string.biometric_auth_title),
            description = context.getString(R.string.biometric_auth_desc)
        )
    }

    biometricResult.let { result ->
        when (result.value) {
            is BiometricAuthManager.BiometricResult.AuthenticationSuccess -> {
                onSuccess()
            }

            else -> Unit
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(256.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    promptManager.showBiometricPrompt(
                        title = context.getString(R.string.biometric_auth_title),
                        description = context.getString(R.string.biometric_auth_desc)
                    )
                }
            ) {
                Text(text = stringResource(R.string.use_biometric_authentication))
            }
        }
    }
}
