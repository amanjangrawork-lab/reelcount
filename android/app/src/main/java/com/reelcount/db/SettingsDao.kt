package com.reelcount.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun get(): Settings?

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getBlocking(): Settings?

    @Upsert
    suspend fun upsert(settings: Settings)
}
