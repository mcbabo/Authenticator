package at.mcbabo.authenticator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.mcbabo.authenticator.data.store.GestureType
import at.mcbabo.authenticator.data.store.SortType
import at.mcbabo.authenticator.data.store.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferenceViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    val useDynamicColors: StateFlow<Boolean> =
        userPreferences.useDynamicColors.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val firstLaunch: StateFlow<Boolean> =
        userPreferences.firstLaunch.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val sortType: StateFlow<SortType> =
        userPreferences.sortType.stateIn(viewModelScope, SharingStarted.Eagerly, SortType.ID)

    val lockEnabled: StateFlow<Boolean> =
        userPreferences.lockEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val gestureType: StateFlow<GestureType> =
        userPreferences.gestureType.stateIn(viewModelScope, SharingStarted.Eagerly, GestureType.TAP_TO_COPY)

    fun setUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setUseDynamicColors(enabled)
        }
    }

    fun setFirstLaunch(isFirstLaunch: Boolean) {
        viewModelScope.launch {
            userPreferences.setFirstLaunch(isFirstLaunch)
        }
    }

    fun setSortType(sortType: SortType) {
        viewModelScope.launch {
            userPreferences.setSortType(sortType)
        }
    }

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setLockEnabled(enabled)
        }
    }

    fun setGestureType(gestureType: GestureType) {
        viewModelScope.launch {
            userPreferences.setGestureType(gestureType)
        }
    }
}
