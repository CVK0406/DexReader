package com.example.dexreader.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.dexreader.tracker.data.TrackLogEntity
import com.example.dexreader.tracker.data.TrackLogWithManga
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackLogsDao {

	@Transaction
	@Query("SELECT * FROM track_logs ORDER BY created_at DESC LIMIT :limit OFFSET 0")
	fun observeAll(limit: Int): Flow<List<TrackLogWithManga>>

	@Query("DELETE FROM track_logs")
	suspend fun clear()

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(entity: TrackLogEntity): Long

	@Query("DELETE FROM track_logs WHERE manga_id = :mangaId")
	suspend fun removeAll(mangaId: Long)

	@Query("DELETE FROM track_logs WHERE manga_id NOT IN (SELECT manga_id FROM tracks)")
	suspend fun gc()

	@Query("SELECT COUNT(*) FROM track_logs")
	suspend fun count(): Int
}
