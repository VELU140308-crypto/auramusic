package com.aura.music.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.music.data.local.entities.PlaybackHistoryEntity
import com.aura.music.data.local.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN (
            SELECT songId, MAX(playedAt) as maxPlayedAt
            FROM playback_history
            GROUP BY songId
        ) h ON s.id = h.songId
        WHERE s.isAvailable = 1
        ORDER BY h.maxPlayedAt DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayed(limit: Int = 30): Flow<List<SongEntity>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
