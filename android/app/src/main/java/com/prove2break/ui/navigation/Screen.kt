package com.prove2break.ui.navigation

/** Sealed route inventory — one entry per screen in UX-UI.md's Screen Inventory. */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Dashboard : Screen("dashboard")
    data object Analytics : Screen("analytics")
    data object Subjects : Screen("subjects")
    data object SubjectCreate : Screen("subject_create")
    data object SubjectEdit : Screen("subject_edit/{subjectId}") {
        fun createRoute(subjectId: String) = "subject_edit/$subjectId"
    }
    data object SubjectDetail : Screen("subject_detail/{subjectId}") {
        fun createRoute(subjectId: String) = "subject_detail/$subjectId"
    }
    data object TopicList : Screen("topic_list/{subjectId}") {
        fun createRoute(subjectId: String) = "topic_list/$subjectId"
    }
    data object FocusSetup : Screen("focus_setup")
    data object FocusMode : Screen("focus_mode/{sessionId}") {
        fun createRoute(sessionId: Long) = "focus_mode/$sessionId"
    }
    data object BreakActive : Screen("break_active/{sessionId}") {
        fun createRoute(sessionId: Long) = "break_active/$sessionId"
    }
    data object BreakTopicChecklist : Screen("break_topics/{sessionId}/{subjectId}") {
        fun createRoute(sessionId: Long, subjectId: String) = "break_topics/$sessionId/$subjectId"
    }
    data object Quiz : Screen("quiz/{sessionId}/{subjectId}/{topicIds}") {
        fun createRoute(sessionId: Long, subjectId: String, topicIds: List<String>) =
            "quiz/$sessionId/$subjectId/${topicIds.joinToString(",")}"
    }
    data object AppRestrictionRationale : Screen("app_restriction_rationale")
    data object AppSelection : Screen("app_selection")
    data object Settings : Screen("settings")
}
