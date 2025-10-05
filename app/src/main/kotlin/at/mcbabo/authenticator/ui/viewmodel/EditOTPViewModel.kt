package at.mcbabo.authenticator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.mcbabo.authenticator.data.db.OtpAccount
import at.mcbabo.authenticator.data.repository.OtpAccountRepository
import at.mcbabo.authenticator.internal.crypto.OTPGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditOTPUiState {
    object Loading : EditOTPUiState()
    data class Success(val account: OtpAccount) : EditOTPUiState()
    data class Error(val message: String) : EditOTPUiState()
}

sealed class EditOTPEvent {
    data class ShowError(val message: String) : EditOTPEvent()
    data class ShowSuccess(val message: String) : EditOTPEvent()
}

@HiltViewModel
class EditOTPViewModel @Inject constructor(
    private val repository: OtpAccountRepository,
    private val otpGenerator: OTPGenerator
) : ViewModel() {
    private val _uiState = MutableStateFlow<EditOTPUiState>(EditOTPUiState.Loading)
    val uiState: StateFlow<EditOTPUiState> = _uiState.asStateFlow()

    fun loadAccount(accountId: Long) {
        viewModelScope.launch {
            try {
                val account = repository.getAccountById(accountId)
                if (account != null) {
                    _uiState.value = EditOTPUiState.Success(account)
                } else {
                    _uiState.value = EditOTPUiState.Error("Account not found")
                }
            } catch (e: Exception) {
                _uiState.value = EditOTPUiState.Error("Failed to load account: ${e.message}")
            }
        }
    }

    fun updateAccount(account: OtpAccount) {
        viewModelScope.launch {
            try {
                repository.updateAccount(account)
                _uiState.value = EditOTPUiState.Success(account)
            } catch (e: Exception) {
                _uiState.value = EditOTPUiState.Error("Failed to update account: ${e.message}")
            }
        }
    }

    fun deleteAccount(account: OtpAccount) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(account)
                _uiState.value = EditOTPUiState.Loading
            } catch (e: Exception) {
                _uiState.value = EditOTPUiState.Error("Failed to delete account: ${e.message}")
            }
        }
    }
}
