package at.mcbabo.authenticator.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import at.mcbabo.authenticator.R
import at.mcbabo.authenticator.internal.crypto.AuthType
import at.mcbabo.authenticator.internal.crypto.OtpAuthData
import at.mcbabo.authenticator.internal.crypto.parseOTPAccountData
import at.mcbabo.authenticator.ui.viewmodel.AddOTPEvent
import at.mcbabo.authenticator.ui.viewmodel.AddOTPViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOTPScreen(
    qrCode: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddOTPViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState(null)

    val authData: OtpAuthData? = qrCode?.let { parseOTPAccountData(it) }

    var issuer by remember { mutableStateOf(authData?.issuer.orEmpty()) }
    var accountName by remember { mutableStateOf(authData?.accountName.orEmpty()) }
    var secret by remember { mutableStateOf(TextFieldValue(authData?.secret.orEmpty())) }
    var authType by remember { mutableStateOf(authData?.type ?: AuthType.TOTP) }

    val isIssuerEmpty = issuer.isBlank()
    val isSecretEmpty = secret.text.isBlank()


    var mExpanded by remember { mutableStateOf(false) }
    var mTextFieldSize by remember { mutableStateOf(Size.Zero) }

    val icon = if (mExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_otp_account)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.issuer)) },
                    isError = isIssuerEmpty,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.account_name)) },
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = TextFieldValue(
                        text = secret.text.chunked(4).joinToString(" "),
                        selection = TextRange(secret.text.length * 5 / 4)
                    ),
                    onValueChange = { newValue ->
                        val normalized = newValue.text.replace(" ", "")
                        secret = TextFieldValue(
                            text = normalized,
                            selection = TextRange(normalized.length)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.secret)) },
                    isError = isSecretEmpty,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = authType.name,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            mTextFieldSize = coordinates.size.toSize()
                        },
                    label = { Text(stringResource(R.string.select_type)) },
                    trailingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = "expand",
                            modifier = Modifier.clickable { mExpanded = !mExpanded })
                    },
                    maxLines = 1
                )

                DropdownMenu(
                    expanded = mExpanded,
                    onDismissRequest = { mExpanded = false },
                    modifier = Modifier
                        .width(with(LocalDensity.current) { mTextFieldSize.width.toDp() })
                ) {
                    AuthType.entries.forEach { label ->
                        DropdownMenuItem(
                            text = { Text(text = label.name) },
                            onClick = {
                                authType = label
                                mExpanded = false
                            }
                        )
                    }
                }

                if (events is AddOTPEvent.ShowError) {
                    Text(
                        text = (events as AddOTPEvent.ShowError).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (isIssuerEmpty || isSecretEmpty) return@Button
                    viewModel.addAccount(
                        issuer = issuer,
                        accountName = accountName,
                        secret = secret.text.replace(" ", ""),
                        type = authType
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_otp_account))
            }
        }
    }
}
