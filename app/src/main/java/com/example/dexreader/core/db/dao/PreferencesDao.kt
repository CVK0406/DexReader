package com.example.dexreader.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.dexreader.core.db.entity.MangaPrefsEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PreferencesDao {

	@Query("SELECT * FROM preferences WHERE manga_id = :mangaId")
	abstract suspend fun find(mangaId: Long): MangaPrefsEntity?

	@Query("SELECT * FROM preferences WHERE manga_id = :mangaId")
	abstract fun observe(mangaId: Long): Flow<MangaPrefsEntity?>

	@Upsert
	abstract suspend fun upsert(pref: MangaPrefsEntity)
}
