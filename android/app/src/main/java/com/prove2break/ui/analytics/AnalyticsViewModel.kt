package com.prove2break.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.AnalyticsRepository
import com.prove2break.data.repository.LocalAnalyticsRepository
import com.prove2break.domain.model.AnalyticsOverview
import com.prove2break.domain.model.LocalStudyStats
import com.prove2break.domain.model.TopicPerformanceSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val localStats: LocalStudyStats = LocalStudyStats.EMPTY,
    val quizOverview: AnalyticsOverview = AnalyticsOverview.EMPTY,
    val topicPerformance: List<TopicPerformanceSummary> = emptyList(),
    val weakTopics: List<TopicPerformanceSummary> = emptyList(),
    val errorMessage: String? = null,
)

/**
 * Combines Android-local aggregation (Room: focus sessions/breaks) with
 * backend-owned quiz/topic-performance analytics (docs/ANALYTICS.md), per the
 * data-split architecture decision (docs/PROCESS.md "Data split for
 * analytics"): the two sources are never merged server-side, only in this
 * ViewModel/UI layer.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val localAnalyticsRepository: LocalAnalyticsRepository,
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                coroutineScope {
                    val localStatsDeferred = async { localAnalyticsRepository.getLocalStudyStats() }
                    val overviewDeferred = async { analyticsRepository.getOverview() }
                    val topicsDeferred = async { analyticsRepository.getTopicPerformance() }
                    val weakTopicsDeferred = async { analyticsRepository.getWeakTopics() }
                    AnalyticsUiState(
                        isLoading = false,
                        localStats = localStatsDeferred.await(),
                        quizOverview = overviewDeferred.await(),
                        topicPerformance = topicsDeferred.await(),
                        weakTopics = weakTopicsDeferred.await(),
                    )
                }
            }.onSuccess { newState ->
                _uiState.value = newState
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load analytics",
                )
            }
        }
    }
}
