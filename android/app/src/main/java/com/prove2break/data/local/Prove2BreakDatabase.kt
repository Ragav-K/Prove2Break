package com.prove2break.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FocusSessionEntity::class,
        BlockedAppEntity::class,
        EmergencyUnlockEntity::class,
        ScheduledBreakEntity::class,
        BreakBudgetEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class Prove2BreakDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun emergencyUnlockDao(): EmergencyUnlockDao
    abstract fun scheduledBreakDao(): ScheduledBreakDao
    abstract fun breakBudgetDao(): BreakBudgetDao

    companion object {
        const val DATABASE_NAME = "prove2break.db"
    }
}
