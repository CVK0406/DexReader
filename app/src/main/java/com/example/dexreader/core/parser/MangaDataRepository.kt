package com.example.dexreader.core.parser

import androidx.core.net.toUri
import androidx.room.withTransaction
import com.example.dexreader.core.db.MangaDatabase
import com.example.dexreader.core.db.entity.MangaPrefsEntity
import com.example.dexreader.core.db.entity.toEntities
import com.example.dexreader.core.db.entity.toEntity
import com.example.dexreader.core.db.entity.toManga
import com.example.dexreader.core.db.entity.toMangaTags
import com.example.dexreader.core.model.isLocal
import com.example.dexreader.core.prefs.ReaderMode
import com.example.dexreader.core.util.ext.toFileOrNull
import com.example.dexreader.reader.domain.ReaderColorFilter
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaTag
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class MangaDataRepository @Inject constructor(
	private val db: MangaDatabase,
	private val resolverProvider: Provider<MangaLinkResolver>,
) {

	suspend fun saveReaderMode(manga: Manga, mode: ReaderMode) {
		db.withTransaction {
			storeManga(manga)
			val entity = db.getPreferencesDao().find(manga.id) ?: newEntity(manga.id)
			db.getPreferencesDao().upsert(entity.copy(mode = mode.id))
		}
	}

	suspend fun saveColorFilter(manga: Manga, colorFilter: ReaderColorFilter?) {
		db.withTransaction {
			storeManga(manga)
			val entity = db.getPreferencesDao().find(manga.id) ?: newEntity(manga.id)
			db.getPreferencesDao().upsert(
				entity.copy(
					cfBrightness = colorFilter?.brightness ?: 0f,
					cfContrast = colorFilter?.contrast ?: 0f,
					cfInvert = colorFilter?.isInverted ?: false,
				),
			)
		}
	}

	suspend fun getReaderMode(mangaId: Long): ReaderMode? {
		return db.getPreferencesDao().find(mangaId)?.let { ReaderMode.valueOf(it.mode) }
	}

	suspend fun getColorFilter(mangaId: Long): ReaderColorFilter? {
		return db.getPreferencesDao().find(mangaId)?.getColorFilterOrNull()
	}

	fun observeColorFilter(mangaId: Long): Flow<ReaderColorFilter?> {
		return db.getPreferencesDao().observe(mangaId)
			.map { it?.getColorFilterOrNull() }
			.distinctUntilChanged()
	}

	suspend fun findMangaById(mangaId: Long): Manga? {
		return db.getMangaDao().find(mangaId)?.toManga()
	}

	suspend fun findMangaByPublicUrl(publicUrl: String): Manga? {
		return db.getMangaDao().findByPublicUrl(publicUrl)?.toManga()
	}

	suspend fun resolveIntent(intent: MangaIntent): Manga? = when {
		intent.manga != null -> intent.manga
		intent.mangaId != 0L -> findMangaById(intent.mangaId)
		intent.uri != null -> resolverProvider.get().resolve(intent.uri)
		else -> null
	}

	suspend fun storeManga(manga: Manga) {
		db.withTransaction {
			// avoid storing local manga if remote one is already stored
			val existing = if (manga.isLocal) {
				db.getMangaDao().find(manga.id)?.manga
			} else {
				null
			}
			if (existing == null || existing.source == manga.source.name) {
				val tags = manga.tags.toEntities()
				db.getTagsDao().upsert(tags)
				db.getMangaDao().upsert(manga.toEntity(), tags)
			}
		}
	}

	suspend fun findTags(source: MangaSource): Set<MangaTag> {
		return db.getTagsDao().findTags(source.name).toMangaTags()
	}

	suspend fun cleanupLocalManga() {
		val dao = db.getMangaDao()
		val broken = dao.findAllBySource(MangaSource.LOCAL.name)
			.filter { x -> x.manga.url.toUri().toFileOrNull()?.exists() == false }
		if (broken.isNotEmpty()) {
			dao.delete(broken.map { it.manga })
		}
	}

	private fun MangaPrefsEntity.getColorFilterOrNull(): ReaderColorFilter? {
		return if (cfBrightness != 0f || cfContrast != 0f || cfInvert || cfGrayscale) {
			ReaderColorFilter(cfBrightness, cfContrast, cfInvert, cfGrayscale)
		} else {
			null
		}
	}

	private fun newEntity(mangaId: Long) = MangaPrefsEntity(
		mangaId = mangaId,
		mode = -1,
		cfBrightness = 0f,
		cfContrast = 0f,
		cfInvert = false,
		cfGrayscale = false,
	)
}
