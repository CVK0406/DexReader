package com.example.dexreader.history.data

import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.example.dexreader.core.db.MangaDatabase
import com.example.dexreader.core.db.entity.toEntity
import com.example.dexreader.core.db.entity.toManga
import com.example.dexreader.core.db.entity.toMangaTag
import com.example.dexreader.core.db.entity.toMangaTags
import com.example.dexreader.core.model.MangaHistory
import com.example.dexreader.core.model.findById
import com.example.dexreader.core.model.isLocal
import com.example.dexreader.core.model.isNsfw
import com.example.dexreader.core.parser.MangaDataRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.util.ReversibleHandle
import com.example.dexreader.core.util.ext.mapItems
import com.example.dexreader.history.domain.model.MangaWithHistory
import com.example.dexreader.list.domain.ListSortOrder
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaTag
import com.example.dexreader.tracker.domain.TrackingRepository
import javax.inject.Inject

const val PROGRESS_NONE = -1f

@Reusable
class HistoryRepository @Inject constructor(
	private val db: MangaDatabase,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	private val mangaRepository: MangaDataRepository,
) {

	suspend fun getList(offset: Int, limit: Int): List<Manga> {
		val entities = db.getHistoryDao().findAll(offset, limit)
		return entities.map { it.manga.toManga(it.tags.toMangaTags()) }
	}

	suspend fun getLastOrNull(): Manga? {
		val entity = db.getHistoryDao().findAll(0, 1).firstOrNull() ?: return null
		return entity.manga.toManga(entity.tags.toMangaTags())
	}

	fun observeLast(): Flow<Manga?> {
		return db.getHistoryDao().observeAll(1).map {
			val first = it.firstOrNull()
			first?.manga?.toManga(first.tags.toMangaTags())
		}
	}

	fun observeAll(): Flow<List<Manga>> {
		return db.getHistoryDao().observeAll().mapItems {
			it.manga.toManga(it.tags.toMangaTags())
		}
	}

	fun observeAll(limit: Int): Flow<List<Manga>> {
		return db.getHistoryDao().observeAll(limit).mapItems {
			it.manga.toManga(it.tags.toMangaTags())
		}
	}

	fun observeAllWithHistory(order: ListSortOrder): Flow<List<MangaWithHistory>> {
		return db.getHistoryDao().observeAll(order).mapItems {
			MangaWithHistory(
				it.manga.toManga(it.tags.toMangaTags()),
				it.history.toMangaHistory(),
			)
		}
	}

	fun observeOne(id: Long): Flow<MangaHistory?> {
		return db.getHistoryDao().observe(id).map {
			it?.toMangaHistory()
		}
	}

	fun observeHasItems(): Flow<Boolean> {
		return db.getHistoryDao().observeCount()
			.map { it > 0 }
			.distinctUntilChanged()
	}

	suspend fun addOrUpdate(manga: Manga, chapterId: Long, page: Int, scroll: Int, percent: Float, force: Boolean) {
		if (!force && shouldSkip(manga)) {
			return
		}
		assert(manga.chapters != null)
		db.withTransaction {
			mangaRepository.storeManga(manga)
			db.getHistoryDao().upsert(
				HistoryEntity(
					mangaId = manga.id,
					createdAt = System.currentTimeMillis(),
					updatedAt = System.currentTimeMillis(),
					chapterId = chapterId,
					page = page,
					scroll = scroll.toFloat(),
					percent = percent,
					chaptersCount = manga.chapters?.size ?: -1,
					deletedAt = 0L,
				),
			)
			trackingRepository.syncWithHistory(manga, chapterId)
		}
	}

	suspend fun getOne(manga: Manga): MangaHistory? {
		return db.getHistoryDao().find(manga.id)?.recoverIfNeeded(manga)?.toMangaHistory()
	}

	suspend fun getProgress(mangaId: Long): Float {
		return db.getHistoryDao().findProgress(mangaId) ?: PROGRESS_NONE
	}

	suspend fun clear() {
		db.getHistoryDao().clear()
	}

	suspend fun delete(manga: Manga) {
		db.getHistoryDao().delete(manga.id)
	}

	suspend fun deleteAfter(minDate: Long) {
		db.getHistoryDao().deleteAfter(minDate)
	}

	suspend fun delete(ids: Collection<Long>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.getHistoryDao().delete(id)
			}
		}
		return ReversibleHandle {
			recover(ids)
		}
	}

	suspend fun deleteOrSwap(manga: Manga, alternative: Manga?) {
		if (alternative == null || db.getMangaDao().update(alternative.toEntity()) <= 0) {
			db.getHistoryDao().delete(manga.id)
		}
	}

	suspend fun getPopularTags(limit: Int): List<MangaTag> {
		return db.getHistoryDao().findPopularTags(limit).map { x -> x.toMangaTag() }
	}

	fun shouldSkip(manga: Manga): Boolean {
		return (manga.source.isNsfw() || manga.isNsfw) && settings.isHistoryExcludeNsfw
	}

	fun observeShouldSkip(manga: Manga): Flow<Boolean> {
		return settings.observe()
			.filter { key -> key == AppSettings.KEY_HISTORY_EXCLUDE_NSFW }
			.onStart { emit("") }
			.map { shouldSkip(manga) }
			.distinctUntilChanged()
	}

	private suspend fun recover(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.getHistoryDao().recover(id)
			}
		}
	}

	private suspend fun HistoryEntity.recoverIfNeeded(manga: Manga): HistoryEntity {
		val chapters = manga.chapters
		if (manga.isLocal || chapters.isNullOrEmpty() || chapters.findById(chapterId) != null) {
			return this
		}
		val newChapterId = chapters.getOrNull(
			(chapters.size * percent).toInt(),
		)?.id ?: return this
		val newEntity = copy(chapterId = newChapterId)
		db.getHistoryDao().update(newEntity)
		return newEntity
	}
}
