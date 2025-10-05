package at.mcbabo.authenticator.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import at.mcbabo.authenticator.R
import at.mcbabo.authenticator.internal.BiometricAuthManager
import at.mcbabo.authenticator.ui.components.PreferenceSubtitle
import at.mcbabo.authenticator.ui.components.PreferenceSwitch
import at.mcbabo.authenticator.ui.viewmodel.PreferenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PreferenceViewModel = hiltViewModel()
) {
    val scrollBehavior = exitUntilCollapsedScrollBehavior()

    val useDynamicColors by viewModel.useDynamicColors.collectAsState()
    val lockEnabled by viewModel.lockEnabled.collectAsState()

    val context = LocalContext.current
    val activity = LocalActivity.current as AppCompatActivity
    val promptManager by lazy {
        BiometricAuthManager(activity)
    }

    val biometricResult = promptManager.promptResults.collectAsState(null)

    biometricResult.let { result ->
        when (result.value) {
            is BiometricAuthManager.BiometricResult.AuthenticationSuccess -> {
                viewModel.setLockEnabled(true)
            }

            else -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            item {
                PreferenceSubtitle(stringResource(R.string.appearance))
                PreferenceSwitch(
                    title = stringResource(R.string.dynamic_color),
                    description = stringResource(R.string.dynamic_color_desc),
                    isChecked = useDynamicColors,
                ) { viewModel.setUseDynamicColors(!useDynamicColors) }
            }

            item {
                PreferenceSubtitle(stringResource(R.string.security))
                PreferenceSwitch(
                    title = stringResource(R.string.privacy_feature),
                    description = stringResource(R.string.privacy_feature_desc),
                    isChecked = lockEnabled,
                ) {
                    when (lockEnabled) {
                        true -> viewModel.setLockEnabled(false)
                        false -> {
                            promptManager.showBiometricPrompt(
                                title = context.getString(R.string.biometric_auth_title),
                                description = context.getString(R.string.biometric_auth_desc)
                            )
                        }
                    }
                }
            }
        }
    }
}
