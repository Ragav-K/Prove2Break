package com.prove2break.data.repository

import com.prove2break.data.auth.TokenStore
import com.prove2break.data.remote.HealthResponseDto
import com.prove2break.data.remote.Prove2BreakApiService
import com.prove2break.data.remote.dto.AccessTokenResponseDto
import com.prove2break.data.remote.dto.AttemptStartResponseDto
import com.prove2break.data.remote.dto.LoginRequest
import com.prove2break.data.remote.dto.QuizGenerateRequest
import com.prove2break.data.remote.dto.QuizResponseDto
import com.prove2break.data.remote.dto.QuizResultResponseDto
import com.prove2break.data.remote.dto.QuizSubmitRequestDto
import com.prove2break.data.remote.dto.QuizSubmitResponseDto
import com.prove2break.data.remote.dto.RefreshRequest
import com.prove2break.data.remote.dto.RegisterRequest
import com.prove2break.data.remote.dto.RegisterResponseDto
import com.prove2break.data.remote.dto.StudyMaterialResponseDto
import com.prove2break.data.remote.dto.StudyMaterialStatusResponseDto
import com.prove2break.data.remote.dto.SubjectCreateRequest
import com.prove2break.data.remote.dto.SubjectListItemDto
import com.prove2break.data.remote.dto.SubjectResponseDto
import com.prove2break.data.remote.dto.SubjectUpdateRequest
import com.prove2break.data.remote.dto.TokenResponseDto
import com.prove2break.data.remote.dto.TopicResponseDto
import com.prove2break.data.remote.dto.TopicUpdateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

/** In-memory [TokenStore] fake — no Android/DataStore dependency, matches the project's fake-based test style. */
private class FakeTokenStore : TokenStore {
    private val state = MutableStateFlow<String?>(null)
    var refreshToken: String? = null
        private set

    override val isAuthenticatedFlow: Flow<Boolean> get() = state.map { !it.isNullOrBlank() }

    override suspend fun getAccessToken(): String? = state.value
    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        state.value = accessToken
        this.refreshToken = refreshToken
    }

    override suspend fun updateAccessToken(accessToken: String) {
        state.value = accessToken
    }

    override suspend fun clear() {
        state.value = null
        refreshToken = null
    }
}

/** Minimal fake of the (large) Retrofit interface: only auth methods are wired per test. */
private class FakeAuthApiService(
    var registerResponse: Response<RegisterResponseDto>? = null,
    var loginResponse: Response<TokenResponseDto>? = null,
) : Prove2BreakApiService {
    var lastLoginRequest: LoginRequest? = null
    var loginCallCount = 0

    override suspend fun health(): Response<HealthResponseDto> = unsupported()

    override suspend fun register(body: RegisterRequest): Response<RegisterResponseDto> =
        registerResponse ?: unsupported()

    override suspend fun login(body: LoginRequest): Response<TokenResponseDto> {
        loginCallCount++
        lastLoginRequest = body
        return loginResponse ?: unsupported()
    }

    override suspend fun refresh(body: RefreshRequest): Response<AccessTokenResponseDto> = unsupported()
    override suspend fun listSubjects(): Response<List<SubjectListItemDto>> = unsupported()
    override suspend fun createSubject(body: SubjectCreateRequest): Response<SubjectResponseDto> = unsupported()
    override suspend fun getSubject(subjectId: String): Response<SubjectResponseDto> = unsupported()
    override suspend fun updateSubject(subjectId: String, body: SubjectUpdateRequest): Response<SubjectResponseDto> =
        unsupported()

    override suspend fun deleteSubject(subjectId: String): Response<Unit> = unsupported()
    override suspend fun uploadMaterial(
        subjectId: RequestBody,
        file: MultipartBody.Part,
    ): Response<StudyMaterialResponseDto> = unsupported()

    override suspend fun getMaterial(materialId: String): Response<StudyMaterialResponseDto> = unsupported()
    override suspend fun getMaterialStatus(materialId: String): Response<StudyMaterialStatusResponseDto> =
        unsupported()

    override suspend fun listTopics(subjectId: String): Response<List<TopicResponseDto>> = unsupported()
    override suspend fun updateTopic(topicId: String, body: TopicUpdateRequest): Response<TopicResponseDto> =
        unsupported()

    override suspend fun generateQuiz(body: QuizGenerateRequest): Response<QuizResponseDto> = unsupported()
    override suspend fun startQuizAttempt(quizId: String): Response<AttemptStartResponseDto> = unsupported()
    override suspend fun submitQuiz(quizId: String, body: QuizSubmitRequestDto): Response<QuizSubmitResponseDto> =
        unsupported()

    override suspend fun getQuizResult(quizId: String): Response<QuizResultResponseDto> = unsupported()

    override suspend fun getAnalyticsOverview(): Response<com.prove2break.data.remote.dto.AnalyticsOverviewResponseDto> = unsupported()

    override suspend fun getTopicPerformance(): Response<com.prove2break.data.remote.dto.TopicPerformanceListResponseDto> = unsupported()

    override suspend fun getWeakTopics(): Response<com.prove2break.data.remote.dto.WeakTopicsResponseDto> = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Not stubbed for this test")
}

private fun errorBody(detail: String): okhttp3.ResponseBody =
    """{"detail":"$detail"}""".toResponseBody("application/json".toMediaType())

class AuthRepositoryImplTest {

    @Test
    fun `login success persists access and refresh tokens`() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApiService(
            loginResponse = Response.success(
                TokenResponseDto(accessToken = "access-1", refreshToken = "refresh-1", tokenType = "bearer", expiresIn = 1800),
            ),
        )
        val repository = AuthRepositoryImpl(api, tokenStore)

        repository.login("user@example.com", "password123")

        assertEquals("access-1", tokenStore.getAccessToken())
        assertEquals("refresh-1", tokenStore.getRefreshToken())
        assertEquals("user@example.com", api.lastLoginRequest?.email)
    }

    @Test
    fun `login failure with 401 surfaces invalid credentials message and stores nothing`() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApiService(
            loginResponse = Response.error(401, errorBody("Invalid email or password")),
        )
        val repository = AuthRepositoryImpl(api, tokenStore)

        val error = try {
            repository.login("user@example.com", "wrong")
            fail("expected ApiException")
            null
        } catch (e: ApiException) {
            e
        }

        assertEquals(401, error?.code)
        assertEquals("Invalid email or password", error?.message)
        assertNull(tokenStore.getAccessToken())
    }

    @Test
    fun `register success auto-logs-in and persists tokens`() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApiService(
            registerResponse = Response.success(RegisterResponseDto(id = "user-1", email = "new@example.com")),
            loginResponse = Response.success(
                TokenResponseDto(accessToken = "access-2", refreshToken = "refresh-2", tokenType = "bearer", expiresIn = 1800),
            ),
        )
        val repository = AuthRepositoryImpl(api, tokenStore)

        repository.register("new@example.com", "password123")

        assertEquals(1, api.loginCallCount)
        assertEquals("access-2", tokenStore.getAccessToken())
        assertEquals("refresh-2", tokenStore.getRefreshToken())
    }

    @Test
    fun `register failure with 409 surfaces duplicate-email message and never calls login`() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApiService(
            registerResponse = Response.error(409, errorBody("Email already registered")),
        )
        val repository = AuthRepositoryImpl(api, tokenStore)

        val error = try {
            repository.register("dup@example.com", "password123")
            fail("expected ApiException")
            null
        } catch (e: ApiException) {
            e
        }

        assertEquals(409, error?.code)
        assertTrue(error?.message?.contains("already exists") == true)
        assertEquals(0, api.loginCallCount)
    }

    @Test
    fun `logout clears the token store`() = runBlocking {
        val tokenStore = FakeTokenStore()
        tokenStore.saveTokens("access", "refresh")
        val repository = AuthRepositoryImpl(FakeAuthApiService(), tokenStore)

        repository.logout()

        assertNull(tokenStore.getAccessToken())
        assertNull(tokenStore.getRefreshToken())
    }

    @Test
    fun `observeIsAuthenticated reflects token store state`() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repository = AuthRepositoryImpl(FakeAuthApiService(), tokenStore)

        assertFalse(repository.observeIsAuthenticated().first())

        tokenStore.saveTokens("access", "refresh")

        assertTrue(repository.observeIsAuthenticated().first())
    }
}
