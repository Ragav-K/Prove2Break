package com.prove2break.di

import android.content.Context
import androidx.room.Room
import com.prove2break.data.local.BlockedAppDao
import com.prove2break.data.local.BreakBudgetDao
import com.prove2break.data.local.EmergencyUnlockDao
import com.prove2break.data.local.FocusSessionDao
import com.prove2break.data.local.Prove2BreakDatabase
import com.prove2break.data.local.ScheduledBreakDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Prove2BreakDatabase {
        return Room.databaseBuilder(
            context,
            Prove2BreakDatabase::class.java,
            Prove2BreakDatabase.DATABASE_NAME,
        )
            // Pre-release schema: no migration path is published yet, so a
            // version bump simply recreates the local database rather than
            // crashing existing installs. Revisit once the app ships.
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFocusSessionDao(database: Prove2BreakDatabase): FocusSessionDao {
        return database.focusSessionDao()
    }

    @Provides
    fun provideBlockedAppDao(database: Prove2BreakDatabase): BlockedAppDao {
        return database.blockedAppDao()
    }

    @Provides
    fun provideEmergencyUnlockDao(database: Prove2BreakDatabase): EmergencyUnlockDao {
        return database.emergencyUnlockDao()
    }

    @Provides
    fun provideScheduledBreakDao(database: Prove2BreakDatabase): ScheduledBreakDao {
        return database.scheduledBreakDao()
    }

    @Provides
    fun provideBreakBudgetDao(database: Prove2BreakDatabase): BreakBudgetDao {
        return database.breakBudgetDao()
    }
}
