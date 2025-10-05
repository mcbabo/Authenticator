package at.mcbabo.authenticator.ui.screen

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import at.mcbabo.authenticator.R
import at.mcbabo.authenticator.data.crypto.AuthType
import at.mcbabo.authenticator.data.store.SortType
import at.mcbabo.authenticator.ui.components.OTPAccountItem
import at.mcbabo.authenticator.ui.components.SearchBarInput
import at.mcbabo.authenticator.ui.viewmodel.ViewOTPUiState
import at.mcbabo.authenticator.ui.viewmodel.ViewOTPViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ViewOTPScreen(
    onAddQRCode: () -> Unit,
    onAddManual: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: ViewOTPViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboard.current
    val focusManager = LocalFocusManager.current

    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val fabEntries = listOf(
        FabEntry(Icons.Outlined.QrCode, stringResource(R.string.scan_qr_code)) { onAddQRCode() },
        FabEntry(Icons.Outlined.Keyboard, stringResource(R.string.enter_key_manually)) { onAddManual() }
    )

    val showFab by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    LaunchedEffect(searchQuery) {
        if (textFieldState.text.toString() != searchQuery) {
            textFieldState.setTextAndPlaceCursorAtEnd(searchQuery)
        }
    }

    LaunchedEffect(textFieldState.text) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { text ->
                if (text != searchQuery) {
                    viewModel.searchAccounts(text)
                }
            }
    }

    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Collapsed) {
            textFieldState.clearText()
            focusManager.clearFocus()
            viewModel.searchAccounts("")
        }
    }

    val inputField = @Composable {
        SearchBarInput(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onBack = {
                textFieldState.clearText()
                focusManager.clearFocus()
                coroutineScope.launch { searchBarState.animateToCollapsed() }
            }
        )
    }

    when (uiState) {
        is ViewOTPUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }

        is ViewOTPUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${(uiState as ViewOTPUiState.Error).message}")
        }

        is ViewOTPUiState.Success -> {
            val accounts = (uiState as? ViewOTPUiState.Success)?.accounts.orEmpty()

            Scaffold(
                topBar = {
                    AppBarWithSearch(
                        scrollBehavior = scrollBehavior,
                        state = searchBarState,
                        inputField = inputField,
                        navigationIcon = {
                            IconButton(onClick = onNavigateSettings) {
                                Icon(Icons.Outlined.Settings, contentDescription = null)
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    val newSort = when (sortType) {
                                        SortType.ID -> SortType.ISSUER
                                        SortType.ISSUER -> SortType.ID
                                    }
                                    viewModel.setSortType(newSort)
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Sort,
                                    contentDescription = stringResource(R.string.sort)
                                )
                            }
                        }
                    )
                    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
                        LazyColumn(
                            state = searchListState
                        ) {
                            items(
                                items = accounts,
                                key = { it.account.id }
                            ) { accountWithCode ->
                                OTPAccountItem(
                                    modifier = Modifier,
                                    account = accountWithCode,
                                    onClick = { onAccountClick(accountWithCode.account.id) },
                                    onLongClick = {
                                        coroutineScope.launch {
                                            if (!accountWithCode.currentCode.isNullOrEmpty()) {
                                                val clipData =
                                                    ClipData.newPlainText(
                                                        context.getString(R.string.otp_code),
                                                        accountWithCode.currentCode
                                                    )
                                                coroutineScope.launch {
                                                    clipboardManager.setClipEntry(
                                                        clipData.toClipEntry()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    if (accountWithCode.account.type == AuthType.HOTP)
                                        viewModel.generateHOTP(accountWithCode.account)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(showFab) {
                        FloatingActionButtonMenu(
                            expanded = fabMenuExpanded,
                            button = {
                                ToggleFloatingActionButton(
                                    checked = fabMenuExpanded,
                                    onCheckedChange = { fabMenuExpanded = !fabMenuExpanded }
                                ) {
                                    val imageVector by remember {
                                        derivedStateOf { if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add }
                                    }
                                    Icon(
                                        painter = rememberVectorPainter(imageVector),
                                        contentDescription = stringResource(R.string.add_otp_account),
                                        modifier = Modifier.animateIcon({ checkedProgress })
                                    )
                                }
                            },
                        ) {
                            fabEntries.forEach { item ->
                                FloatingActionButtonMenuItem(
                                    onClick = {
                                        fabMenuExpanded = false
                                        item.onClick.invoke()
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    text = { Text(text = item.label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    items(
                        items = accounts,
                        key = { it.account.id }
                    ) { accountWithCode ->
                        OTPAccountItem(
                            modifier = Modifier,
                            account = accountWithCode,
                            onClick = { onAccountClick(accountWithCode.account.id) },
                            onLongClick = {
                                coroutineScope.launch {
                                    if (!accountWithCode.currentCode.isNullOrEmpty()) {
                                        val clipData = ClipData.newPlainText(
                                            context.getString(R.string.otp_code),
                                            accountWithCode.currentCode
                                        )
                                        coroutineScope.launch {
                                            clipboardManager.setClipEntry(
                                                clipData.toClipEntry()
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            if (accountWithCode.account.type == AuthType.HOTP)
                                viewModel.generateHOTP(accountWithCode.account)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

data class FabEntry(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)
