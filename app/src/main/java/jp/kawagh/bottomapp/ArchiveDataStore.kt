package jp.kawagh.bottomapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArchiveDataStore(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("archives")
        val KEY = stringSetPreferencesKey("archives_key")
    }

    val getValue: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY] ?: emptySet()
    }

    suspend fun saveValue(packageName: String) {
        context.dataStore.edit { preferences ->
            if (preferences[KEY] == null) {
                preferences[KEY] = setOf(packageName)
            } else {
                preferences[KEY] = preferences[KEY]!!.toMutableSet().plus(packageName)
            }
        }
    }

    suspend fun removeValue(packageName: String) {
        context.dataStore.edit { pref ->
            if (pref[KEY] == null) {
                pref[KEY] = emptySet()
            } else {
                pref[KEY] = pref[KEY]!!.toMutableSet().minus(packageName)
            }
        }
    }
}