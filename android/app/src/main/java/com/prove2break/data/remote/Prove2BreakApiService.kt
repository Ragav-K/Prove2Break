package com.prove2break.data.remote

import com.prove2break.data.remote.dto.AccessTokenResponseDto
import com.prove2break.data.remote.dto.AnalyticsOverviewResponseDto
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
import com.prove2break.data.remote.dto.TopicPerformanceListResponseDto
import com.prove2break.data.remote.dto.TopicResponseDto
import com.prove2break.data.remote.dto.TopicUpdateRequest
import com.prove2break.data.remote.dto.WeakTopicsResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Retrofit interface for the backend API described in docs/API.md.
 * Real endpoints (auth, subjects, materials, quizzes, analytics) are added
 * as those features are implemented; this proves the network layer wiring.
 *
 * Phase 4 (Study Management) additions: subjects/materials/topics endpoints,
 * matching backend/app/api/{subjects,materials,topics}.py exactly.
 */
interface Prove2BreakApiService {
    @GET("health")
    suspend fun health(): Response<HealthResponseDto>

    // ---- Auth (backend/app/api/auth.py) ----

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponseDto>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponseDto>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<AccessTokenResponseDto>

    // ---- Subjects (backend/app/api/subjects.py) ----

    @GET("api/subjects")
    suspend fun listSubjects(): Response<List<SubjectListItemDto>>

    @POST("api/subjects")
    suspend fun createSubject(@Body body: SubjectCreateRequest): Response<SubjectResponseDto>

    @GET("api/subjects/{subjectId}")
    suspend fun getSubject(@Path("subjectId") subjectId: String): Response<SubjectResponseDto>

    @PUT("api/subjects/{subjectId}")
    suspend fun updateSubject(
        @Path("subjectId") subjectId: String,
        @Body body: SubjectUpdateRequest,
    ): Response<SubjectResponseDto>

    @DELETE("api/subjects/{subjectId}")
    suspend fun deleteSubject(@Path("subjectId") subjectId: String): Response<Unit>

    // ---- Materials (backend/app/api/materials.py) ----

    @Multipart
    @POST("api/materials/upload")
    suspend fun uploadMaterial(
        @Part("subject_id") subjectId: RequestBody,
        @Part file: MultipartBody.Part,
    ): Response<StudyMaterialResponseDto>

    @GET("api/materials/{materialId}")
    suspend fun getMaterial(@Path("materialId") materialId: String): Response<StudyMaterialResponseDto>

    @GET("api/materials/{materialId}/status")
    suspend fun getMaterialStatus(
        @Path("materialId") materialId: String,
    ): Response<StudyMaterialStatusResponseDto>

    // ---- Topics (backend/app/api/topics.py) ----

    @GET("api/subjects/{subjectId}/topics")
    suspend fun listTopics(@Path("subjectId") subjectId: String): Response<List<TopicResponseDto>>

    @PUT("api/topics/{topicId}/completion")
    suspend fun updateTopic(
        @Path("topicId") topicId: String,
        @Body body: TopicUpdateRequest,
    ): Response<TopicResponseDto>

    // ---- Quiz (backend/app/api/quiz.py) ----

    @POST("api/quiz/generate")
    suspend fun generateQuiz(@Body body: QuizGenerateRequest): Response<QuizResponseDto>

    @POST("api/quiz/{quizId}/attempt")
    suspend fun startQuizAttempt(@Path("quizId") quizId: String): Response<AttemptStartResponseDto>

    @POST("api/quiz/{quizId}/submit")
    suspend fun submitQuiz(
        @Path("quizId") quizId: String,
        @Body body: QuizSubmitRequestDto,
    ): Response<QuizSubmitResponseDto>

    @GET("api/quiz/{quizId}/result")
    suspend fun getQuizResult(@Path("quizId") quizId: String): Response<QuizResultResponseDto>

    // ---- Analytics (backend/app/api/analytics.py) ----

    @GET("api/analytics/overview")
    suspend fun getAnalyticsOverview(): Response<AnalyticsOverviewResponseDto>

    @GET("api/analytics/topics")
    suspend fun getTopicPerformance(): Response<TopicPerformanceListResponseDto>

    @GET("api/analytics/weak-topics")
    suspend fun getWeakTopics(): Response<WeakTopicsResponseDto>
}

data class HealthResponseDto(
    val status: String,
)
