package com.minis.glassh.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.minis.glassh.model.HostConfig
import com.minis.glassh.model.HostStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "glassh")

private val KEY_HOSTS = stringPreferencesKey("hosts_v1")

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class HostRepository(private val context: Context) {

    val hostsFlow: Flow<List<HostConfig>> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOSTS]?.let { runCatching { json.decodeFromString(HostStore.serializer(), it) }.getOrNull() }
            ?.hosts.orEmpty()
    }

    suspend fun upsert(host: HostConfig) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_HOSTS]
                ?.let { runCatching { json.decodeFromString(HostStore.serializer(), it) }.getOrNull() }
                ?.hosts.orEmpty()
            val replaced = current.toMutableList().apply {
                val idx = indexOfFirst { it.id == host.id }
                if (idx >= 0) set(idx, host) else add(host)
            }
            prefs[KEY_HOSTS] = json.encodeToString(HostStore.serializer(), HostStore(replaced))
        }
    }

    suspend fun delete(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_HOSTS]
                ?.let { runCatching { json.decodeFromString(HostStore.serializer(), it) }.getOrNull() }
                ?.hosts.orEmpty()
            prefs[KEY_HOSTS] = json.encodeToString(
                HostStore.serializer(),
                HostStore(current.filterNot { it.id == id })
            )
        }
    }

    suspend fun touch(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_HOSTS]
                ?.let { runCatching { json.decodeFromString(HostStore.serializer(), it) }.getOrNull() }
                ?.hosts.orEmpty()
            val updated = current.map { if (it.id == id) it.copy(lastUsed = System.currentTimeMillis()) else it }
            prefs[KEY_HOSTS] = json.encodeToString(HostStore.serializer(), HostStore(updated))
        }
    }
}
