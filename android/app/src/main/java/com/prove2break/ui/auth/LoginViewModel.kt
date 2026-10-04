package com.prove2break.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedIn: Boolean = false,
) {
    val isValid: Boolean get() = email.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun login() {
        val state = _uiState.value
        val validationError = validate(state.email, state.password)
        if (validationError != null) {
            _uiState.value = state.copy(errorMessage = validationError)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.login(state.email.trim(), state.password) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, loggedIn = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Login failed. Please try again",
                    )
                }
        }
    }
}

/**
 * Shared by Login/Register — kept as a plain function (no Android framework dependency, e.g.
 * `android.util.Patterns`) so it runs as a fast, real JVM unit test instead of needing
 * Robolectric/instrumentation.
 */
private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

internal fun validate(email: String, password: String): String? {
    if (email.isBlank()) return "Email is required"
    if (!EMAIL_REGEX.matches(email)) return "Enter a valid email address"
    if (password.isBlank()) return "Password is required"
    return null
}
