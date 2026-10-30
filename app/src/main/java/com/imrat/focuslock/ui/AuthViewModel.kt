package com.imrat.focuslock.ui

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imrat.focuslock.R
import com.imrat.focuslock.data.AuthRepository
import com.imrat.focuslock.data.ProfileResult
import com.imrat.focuslock.data.UserProfile
import com.imrat.focuslock.data.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class SignedOut(val errorRes: Int? = null) : AuthUiState
    data class Ready(val profile: UserProfile) : AuthUiState
    data object OtherDevice : AuthUiState
}

class AuthViewModel(
    private val auth: AuthRepository,
    private val users: UserRepository,
    private val deviceId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            auth.userFlow.collect { user ->
                profileJob?.cancel()
                if (user == null) {
                    if (_state.value !is AuthUiState.SignedOut) _state.value = AuthUiState.SignedOut()
                } else {
                    profileJob = launch { loadProfile(user) }
                }
            }
        }
    }

    private suspend fun loadProfile(user: com.google.firebase.auth.FirebaseUser) {
        _state.value = AuthUiState.Loading
        _state.value = try {
            when (val r = users.ensureProfile(user, deviceId)) {
                is ProfileResult.Ready -> AuthUiState.Ready(r.profile)
                ProfileResult.OtherDevice -> AuthUiState.OtherDevice
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthUiState.SignedOut(R.string.error_profile)
        }
    }

    fun signIn(activityContext: Context) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                auth.signIn(activityContext) // userFlow then drives the state
            } catch (e: GetCredentialCancellationException) {
                _state.value = AuthUiState.SignedOut()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthUiState.SignedOut(R.string.error_sign_in)
            }
        }
    }

    fun signOut(context: Context) {
        // AUTH-3: never while a lock is active on this phone
        val now = android.os.SystemClock.elapsedRealtime()
        if (com.imrat.focuslock.lock.SessionCache(context).get().any { it.endElapsedMs > now }) return
        viewModelScope.launch { auth.signOut(context) }
    }

    class Factory(
        private val auth: AuthRepository,
        private val users: UserRepository,
        private val deviceId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(auth, users, deviceId) as T
    }
}
