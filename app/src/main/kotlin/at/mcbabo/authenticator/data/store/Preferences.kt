package at.mcbabo.authenticator.data.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("user_preferences")

enum class SortType {
    ISSUER,
    ID
}

enum class GestureType(val displayName: String) {
    TAP_TO_COPY("Tap to copy"),
    LONG_PRESS_TO_COPY("Long press to copy"),
}

object PreferencesKeys {
    val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    val SORT_TYPE = stringPreferencesKey("sort_type")
    val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
    val GESTURE_TYPE = stringPreferencesKey("gesture_type")
}

class UserPreferences(private val context: Context) {
    val useDynamicColors: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.USE_DYNAMIC_COLORS] ?: true }

    val firstLaunch: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.FIRST_LAUNCH] ?: true }

    val sortType: Flow<SortType> = context.dataStore.data
        .map { enumValueOf<SortType>(it[PreferencesKeys.SORT_TYPE] ?: SortType.ID.name) }

    val lockEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.LOCK_ENABLED] ?: false }

    val gestureType: Flow<GestureType> = context.dataStore.data
        .map { enumValueOf<GestureType>(it[PreferencesKeys.GESTURE_TYPE] ?: GestureType.TAP_TO_COPY.name) }

    suspend fun setUseDynamicColors(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USE_DYNAMIC_COLORS] = enabled
        }
    }

    suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FIRST_LAUNCH] = isFirstLaunch
        }
    }

    suspend fun setSortType(sortType: SortType) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SORT_TYPE] = sortType.name
        }
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LOCK_ENABLED] = enabled
        }
    }

    suspend fun setGestureType(gestureType: GestureType) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.GESTURE_TYPE] = gestureType.name
        }
    }
}
