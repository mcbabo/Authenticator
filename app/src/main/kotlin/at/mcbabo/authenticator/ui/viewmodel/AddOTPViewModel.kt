package at.mcbabo.authenticator.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.mcbabo.authenticator.data.repository.OtpAccountRepository
import at.mcbabo.authenticator.internal.crypto.Algorithm
import at.mcbabo.authenticator.internal.crypto.AuthType
import at.mcbabo.authenticator.internal.crypto.OtpAuthData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddOTPUiState {
    object Loading : AddOTPUiState()
    data class Success(val accounts: List<OtpAccountWithCode>) : AddOTPUiState()
    data class Error(val message: String) : AddOTPUiState()
}

sealed class AddOTPEvent {
    data class ShowError(val message: String) : AddOTPEvent()
    data class ShowSuccess(val message: String) : AddOTPEvent()
}

@HiltViewModel
class AddOTPViewModel @Inject constructor(
    private val repository: OtpAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddOTPUiState>(AddOTPUiState.Loading)
    val uiState: StateFlow<AddOTPUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddOTPEvent>()
    val events: SharedFlow<AddOTPEvent> = _events.asSharedFlow()

    fun addAccount(
        accountName: String,
        issuer: String,
        secret: String,
        algorithm: Algorithm = Algorithm.SHA1,
        digits: Int = 6,
        period: Int = 30,
        type: AuthType = AuthType.TOTP
    ) = viewModelScope.launch {
        try {
            repository.addAccount(accountName, issuer, secret, algorithm, digits, period, type)
            _events.emit(AddOTPEvent.ShowSuccess("Account added successfully"))
        } catch (e: Exception) {
            Log.e("AddOTPViewModel", "Error adding account", e)
            _events.emit(AddOTPEvent.ShowError("Failed to add account: ${e.message}"))
        }
    }

    fun addMultipleAccounts(accounts: List<OtpAuthData>) = viewModelScope.launch {
        accounts.forEach { account ->
            try {
                repository.addAccount(
                    accountName = account.accountName,
                    issuer = account.issuer,
                    secret = account.secret,
                    algorithm = account.algorithm,
                    digits = account.digits,
                    period = account.period,
                    type = account.type
                )
                _events.emit(AddOTPEvent.ShowSuccess("Account ${account.accountName} added successfully"))
            } catch (e: Exception) {
                Log.e("AddOTPViewModel", "Error adding account ${account.accountName}", e)
                _events.emit(AddOTPEvent.ShowError("Failed to add account ${account.accountName}: ${e.message}"))
            }
        }
    }
}
