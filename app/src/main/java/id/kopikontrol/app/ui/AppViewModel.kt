package id.kopikontrol.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.kopikontrol.app.data.Account
import id.kopikontrol.app.data.ApiException
import id.kopikontrol.app.data.KopiKontrolApi
import id.kopikontrol.app.data.OnboardingDraft
import id.kopikontrol.app.data.SessionStore
import id.kopikontrol.app.data.WorkspaceData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppState {
    data object Loading : AppState
    data class SignedOut(val error: String = "") : AppState
    data class Ready(val account: Account, val workspace: WorkspaceData?, val loadingData: Boolean = false, val error: String = "") : AppState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val api = KopiKontrolApi(SessionStore(application))
    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init { restoreSession() }

    fun restoreSession() = viewModelScope.launch {
        _state.value = AppState.Loading
        val account = runCatching { api.restoreSession() }.getOrNull()
        if (account == null) _state.value = AppState.SignedOut() else loadWorkspace(account)
    }

    fun login(whatsapp: String, password: String) = authenticate { api.login(whatsapp, password) }

    fun signup(name: String, whatsapp: String, password: String) = authenticate { api.signup(name, whatsapp, password) }

    fun handleOauth(uri: Uri) {
        val payload = uri.getQueryParameter("payload").orEmpty()
        if (payload.isBlank()) {
            _state.value = AppState.SignedOut("Sesi Google tidak ditemukan. Silakan coba lagi.")
            return
        }
        authenticate { api.completeGoogleLogin(payload) }
    }

    fun reloadWorkspace() {
        val account = (_state.value as? AppState.Ready)?.account ?: return
        loadWorkspace(account)
    }

    fun finishOnboarding(draft: OnboardingDraft) = viewModelScope.launch {
        val ready = _state.value as? AppState.Ready ?: return@launch
        _state.value = ready.copy(loadingData = true, error = "")
        try {
            _state.value = ready.copy(workspace = api.finishOnboarding(draft), loadingData = false)
        } catch (error: Exception) {
            _state.value = ready.copy(loadingData = false, error = error.userMessage())
        }
    }

    fun logout() = viewModelScope.launch {
        api.logout()
        _state.value = AppState.SignedOut()
    }

    fun clearError() {
        _state.value = when (val current = _state.value) {
            is AppState.SignedOut -> current.copy(error = "")
            is AppState.Ready -> current.copy(error = "")
            else -> current
        }
    }

    private fun authenticate(block: suspend () -> Account) = viewModelScope.launch {
        _state.value = AppState.Loading
        try { loadWorkspace(block()) }
        catch (error: Exception) { _state.value = AppState.SignedOut(error.userMessage()) }
    }

    private fun loadWorkspace(account: Account) = viewModelScope.launch {
        _state.value = AppState.Ready(account, null, loadingData = true)
        try { _state.value = AppState.Ready(account, api.loadWorkspace()) }
        catch (error: Exception) {
            if (error is ApiException && error.statusCode == 401) _state.value = AppState.SignedOut("Sesi berakhir. Silakan masuk kembali.")
            else _state.value = AppState.Ready(account, null, error = error.userMessage())
        }
    }

    private fun Exception.userMessage() = message?.takeIf { it.isNotBlank() } ?: "Koneksi ke KopiKontrol gagal."
}
