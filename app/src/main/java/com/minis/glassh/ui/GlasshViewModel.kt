package com.minis.glassh.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minis.glassh.data.HostRepository
import com.minis.glassh.model.HostConfig
import com.minis.glassh.model.VpsStats
import com.minis.glassh.ssh.SshClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiScreen {
    data object HostList : UiScreen
    data class HostEdit(val hostId: String?) : UiScreen
    data class Dashboard(val hostId: String) : UiScreen
}

data class DashboardState(
    val host: HostConfig? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val stats: VpsStats? = null,
    val previousStats: VpsStats? = null,
    val autoRefresh: Boolean = true,
    val intervalSeconds: Int = 5,
)

class GlasshViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = HostRepository(app)

    val hosts: StateFlow<List<HostConfig>> = repo.hostsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _screen = MutableStateFlow<UiScreen>(UiScreen.HostList)
    val screen: StateFlow<UiScreen> = _screen.asStateFlow()

    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard.asStateFlow()

    private var pollJob: Job? = null

    fun navigate(target: UiScreen) {
        if (target !is UiScreen.Dashboard) {
            stopPolling()
            _dashboard.value = DashboardState()
        }
        _screen.value = target
    }

    fun back() {
        when (_screen.value) {
            is UiScreen.HostList -> Unit
            is UiScreen.HostEdit -> _screen.value = UiScreen.HostList
            is UiScreen.Dashboard -> {
                stopPolling()
                _dashboard.value = DashboardState()
                _screen.value = UiScreen.HostList
            }
        }
    }

    fun saveHost(host: HostConfig) {
        viewModelScope.launch {
            val final = if (host.id.isBlank()) host.copy(id = UUID.randomUUID().toString()) else host
            repo.upsert(final)
            _screen.value = UiScreen.HostList
        }
    }

    fun deleteHost(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun openDashboard(hostId: String) {
        viewModelScope.launch {
            val h = hosts.value.firstOrNull { it.id == hostId } ?: return@launch
            repo.touch(hostId)
            _screen.value = UiScreen.Dashboard(hostId)
            _dashboard.value = DashboardState(host = h, loading = true)
            startPolling(h)
        }
    }

    fun setAutoRefresh(enabled: Boolean) {
        _dashboard.value = _dashboard.value.copy(autoRefresh = enabled)
        val h = _dashboard.value.host ?: return
        if (enabled) startPolling(h) else stopPolling()
    }

    fun setInterval(seconds: Int) {
        _dashboard.value = _dashboard.value.copy(intervalSeconds = seconds.coerceIn(2, 60))
    }

    fun manualRefresh() {
        val h = _dashboard.value.host ?: return
        viewModelScope.launch { fetchOnce(h) }
    }

    private fun startPolling(host: HostConfig) {
        stopPolling()
        pollJob = viewModelScope.launch {
            while (true) {
                fetchOnce(host)
                if (!_dashboard.value.autoRefresh) break
                delay(_dashboard.value.intervalSeconds * 1000L)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun fetchOnce(host: HostConfig) {
        _dashboard.value = _dashboard.value.copy(loading = true, error = null)
        try {
            val client = SshClient(host)
            val stats = client.fetchStats()
            val prev = _dashboard.value.stats
            _dashboard.value = _dashboard.value.copy(
                loading = false,
                error = null,
                stats = stats,
                previousStats = prev,
            )
        } catch (t: Throwable) {
            _dashboard.value = _dashboard.value.copy(
                loading = false,
                error = t.message ?: t::class.java.simpleName,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
