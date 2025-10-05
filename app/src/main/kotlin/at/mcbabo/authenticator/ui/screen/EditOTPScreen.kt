package at.mcbabo.authenticator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import at.mcbabo.authenticator.R
import at.mcbabo.authenticator.ui.viewmodel.EditOTPUiState
import at.mcbabo.authenticator.ui.viewmodel.EditOTPViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditOTPScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditOTPViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var newIssuer by remember { mutableStateOf<String?>(null) }
    var newAccountName by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAccount(accountId)
    }

    when (uiState) {
        is EditOTPUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }

        is EditOTPUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(uiState as EditOTPUiState.Error).message}")
        }

        is EditOTPUiState.Success -> {
            val account = (uiState as EditOTPUiState.Success).account

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(R.string.edit_otp)) },
                        navigationIcon = {
                            IconButton(onClick = { onNavigateBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    showDeleteDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.delete_account)
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (uiState is EditOTPUiState.Success) {
                        val account = (uiState as EditOTPUiState.Success).account
                        if ((newAccountName != null && newAccountName != account.accountName) || (newIssuer != null && newIssuer != account.issuer)) {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.updateAccount(
                                        account.copy(
                                            issuer = newIssuer ?: account.issuer,
                                            accountName = newAccountName ?: account.accountName
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = stringResource(R.string.save)
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newIssuer ?: account.issuer,
                            onValueChange = { newIssuer = it },
                            modifier = Modifier
                                .fillMaxWidth(),
                            label = { Text(stringResource(R.string.issuer)) },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newAccountName ?: account.accountName,
                            onValueChange = { newAccountName = it },
                            modifier = Modifier
                                .fillMaxWidth(),
                            label = { Text(stringResource(R.string.account_name)) },
                            singleLine = true
                        )
                    }
                }
            }
            if (showDeleteDialog) {
                BasicAlertDialog(
                    onDismissRequest = { showDeleteDialog = false }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.delete_account),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.delete_account),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(modifier = Modifier.fillMaxWidth())

                            Text(stringResource(R.string.delete_account_desc))

                            HorizontalDivider(modifier = Modifier.fillMaxWidth())

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteAccount(account)
                                        showDeleteDialog = false
                                        onNavigateBack()
                                    }
                                ) {
                                    Text(stringResource(R.string.delete_account))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
