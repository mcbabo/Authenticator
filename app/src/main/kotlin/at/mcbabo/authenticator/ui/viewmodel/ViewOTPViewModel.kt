package at.mcbabo.authenticator.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.mcbabo.authenticator.data.crypto.AuthType
import at.mcbabo.authenticator.data.crypto.OTPGenerator
import at.mcbabo.authenticator.data.db.OtpAccount
import at.mcbabo.authenticator.data.repository.OtpAccountRepository
import at.mcbabo.authenticator.data.store.SortType
import at.mcbabo.authenticator.data.store.UserPreferences
import at.mcbabo.authenticator.internal.levenshtein
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtpAccountWithCode(
    val account: OtpAccount,
    val currentCode: String?,
    val remainingSeconds: Int,
    val isGenerating: Boolean = false
)

sealed class ViewOTPUiState {
    object Loading : ViewOTPUiState()
    data class Success(val accounts: List<OtpAccountWithCode>) : ViewOTPUiState()
    data class Error(val message: String) : ViewOTPUiState()
}

sealed class ViewOTPEvent {
    data class ShowError(val message: String) : ViewOTPEvent()
    data class ShowSuccess(val message: String) : ViewOTPEvent()
    data class CodeCopied(val code: String) : ViewOTPEvent()
}

@HiltViewModel
class ViewOTPViewModel @Inject constructor(
    private val repository: OtpAccountRepository,
    private val otpGenerator: OTPGenerator,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<ViewOTPUiState>(ViewOTPUiState.Loading)
    val uiState: StateFlow<ViewOTPUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ViewOTPEvent>()
    val events: SharedFlow<ViewOTPEvent> = _events.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allAccountsWithCodes = MutableStateFlow<List<OtpAccountWithCode>>(emptyList())

    private val _sortType = MutableStateFlow(SortType.ISSUER)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    init {
        startUpdater()
        observeSearchQuery()
        viewModelScope.launch {
            userPreferences.sortType.collect {
                _sortType.value = it
            }
        }
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    fun deleteAccount(accountId: Long) = viewModelScope.launch {
        try {
            repository.deleteAccount(accountId)
            _events.emit(ViewOTPEvent.ShowSuccess("Account deleted"))
        } catch (e: Exception) {
            _events.emit(ViewOTPEvent.ShowError("Failed to delete account: ${e.message}"))
        }
    }

    fun copyCode(code: String) = viewModelScope.launch {
        _events.emit(ViewOTPEvent.CodeCopied(code))
    }

    fun searchAccounts(query: String) {
        _searchQuery.value = query
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            combine(_searchQuery, _allAccountsWithCodes, _sortType) { query, accounts, sort ->
                val filtered = if (query.isBlank()) {
                    accounts
                } else {
                    accounts.filter {
                        levenshtein(it.account.accountName.lowercase(), query.lowercase()) <= 3 ||
                                levenshtein(it.account.issuer.lowercase(), query.lowercase()) <= 3 ||
                                it.account.accountName.contains(query, ignoreCase = true) ||
                                it.account.issuer.contains(query, ignoreCase = true)
                    }
                }

                when (sort) {
                    SortType.ISSUER -> filtered.sortedBy { it.account.issuer.lowercase() }
                    SortType.ID -> filtered.sortedBy { it.account.id }
                }
            }
                .distinctUntilChanged()
                .collect { sorted ->
                    _uiState.value = ViewOTPUiState.Success(sorted)
                }
        }
    }

    private fun startUpdater() {
        viewModelScope.launch {
            loop@ while (true) {
                try {
                    val accounts = repository.getAllAccounts()
                    val now = System.currentTimeMillis() / 1000

                    val hotpCodeMap = _allAccountsWithCodes.value
                        .filter { it.account.type == AuthType.HOTP }
                        .associate { it.account.id to it.currentCode }

                    val accountsWithCodes = accounts.map { account ->
                        val secret = repository.getDecryptedSecret(account.id).getOrNull()

                        if (secret == null) {
                            OtpAccountWithCode(
                                account = account,
                                currentCode = "ERROR",
                                remainingSeconds = 0
                            )
                        } else

                            when (account.type) {
                                AuthType.TOTP -> {
                                    val code = otpGenerator.generateTOTP(
                                        base32Secret = secret,
                                        timeStepSeconds = account.period.toLong(),
                                        digits = account.digits,
                                        algorithm = account.algorithm
                                    )
                                    OtpAccountWithCode(
                                        account = account,
                                        currentCode = code,
                                        remainingSeconds = account.period - (now % account.period).toInt()
                                    )
                                }

                                AuthType.HOTP -> {
                                    generateHOTP(account, false)
                                    OtpAccountWithCode(
                                        account = account,
                                        currentCode = hotpCodeMap[account.id] ?: "______",
                                        remainingSeconds = 0
                                    )
                                }
                            }
                    }

                    _allAccountsWithCodes.value = accountsWithCodes
                } catch (e: Exception) {
                    Log.e("ViewOTPViewModel", "Error updating OTP codes", e)
                    _uiState.value = ViewOTPUiState.Error(e.message ?: "Unknown error")
                }

                delay(1000)
            }
        }
    }

    fun generateHOTP(account: OtpAccount, updatedCounter: Boolean = true) = viewModelScope.launch {
        try {
            val secret = repository.getDecryptedSecret(account.id).getOrThrow()

            val code = otpGenerator.generateHOTP(
                base32Secret = secret,
                counter = account.counter,
                digits = account.digits,
                algorithm = account.algorithm
            )

            if (updatedCounter) {
                repository.updateCounter(account)
            }

            _allAccountsWithCodes.update { list ->
                list.map {
                    if (it.account.id == account.id) it.copy(currentCode = code) else it
                }
            }

            _events.emit(ViewOTPEvent.ShowSuccess("HOTP code generated"))
        } catch (e: Exception) {
            _events.emit(ViewOTPEvent.ShowError("Failed to generate HOTP: ${e.message}"))
        }
    }
}
