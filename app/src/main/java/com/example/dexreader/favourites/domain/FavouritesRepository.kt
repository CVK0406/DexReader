package com.example.dexreader.favourites.domain

import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import com.example.dexreader.core.db.MangaDatabase
import com.example.dexreader.core.db.entity.toEntities
import com.example.dexreader.core.db.entity.toEntity
import com.example.dexreader.core.model.FavouriteCategory
import com.example.dexreader.core.ui.util.ReversibleHandle
import com.example.dexreader.core.util.ext.mapItems
import com.example.dexreader.favourites.data.FavouriteCategoryEntity
import com.example.dexreader.favourites.data.FavouriteEntity
import com.example.dexreader.favourites.data.toFavouriteCategory
import com.example.dexreader.favourites.data.toManga
import com.example.dexreader.favourites.data.toMangaList
import com.example.dexreader.favourites.domain.model.Cover
import com.example.dexreader.list.domain.ListSortOrder
import org.example.dexreader.parsers.model.Manga
import com.example.dexreader.tracker.work.TrackerNotificationChannels
import javax.inject.Inject

@Reusable
class FavouritesRepository @Inject constructor(
	private val db: MangaDatabase,
	private val channels: TrackerNotificationChannels,
) {

	suspend fun getAllManga(): List<Manga> {
		val entities = db.getFavouritesDao().findAll()
		return entities.toMangaList()
	}

	suspend fun getLastManga(limit: Int): List<Manga> {
		val entities = db.getFavouritesDao().findLast(limit)
		return entities.toMangaList()
	}

	fun observeAll(order: ListSortOrder): Flow<List<Manga>> {
		return db.getFavouritesDao().observeAll(order)
			.mapItems { it.toManga() }
	}

	suspend fun getManga(categoryId: Long): List<Manga> {
		val entities = db.getFavouritesDao().findAll(categoryId)
		return entities.toMangaList()
	}

	fun observeAll(categoryId: Long, order: ListSortOrder): Flow<List<Manga>> {
		return db.getFavouritesDao().observeAll(categoryId, order)
			.mapItems { it.toManga() }
	}

	fun observeAll(categoryId: Long): Flow<List<Manga>> {
		return observeOrder(categoryId)
			.flatMapLatest { order -> observeAll(categoryId, order) }
	}

	fun observeMangaCount(): Flow<Int> {
		return db.getFavouritesDao().observeMangaCount()
			.distinctUntilChanged()
	}

	fun observeCategories(): Flow<List<FavouriteCategory>> {
		return db.getFavouriteCategoriesDao().observeAll().mapItems {
			it.toFavouriteCategory()
		}.distinctUntilChanged()
	}

	fun observeCategoriesForLibrary(): Flow<List<FavouriteCategory>> {
		return db.getFavouriteCategoriesDao().observeAllForLibrary().mapItems {
			it.toFavouriteCategory()
		}.distinctUntilChanged()
	}

	fun observeCategoriesWithCovers(): Flow<Map<FavouriteCategory, List<Cover>>> {
		return db.getFavouriteCategoriesDao().observeAll()
			.map {
				db.withTransaction {
					val res = LinkedHashMap<FavouriteCategory, List<Cover>>()
					for (entity in it) {
						val cat = entity.toFavouriteCategory()
						res[cat] = db.getFavouritesDao().findCovers(
							categoryId = cat.id,
							order = cat.order,
						)
					}
					res
				}
			}
	}

	suspend fun getAllFavoritesCovers(order: ListSortOrder, limit: Int): List<Cover> {
		return db.getFavouritesDao().findCovers(order, limit)
	}

	fun observeCategory(id: Long): Flow<FavouriteCategory?> {
		return db.getFavouriteCategoriesDao().observe(id)
			.map { it?.toFavouriteCategory() }
	}

	fun observeCategoriesIds(mangaId: Long): Flow<Set<Long>> {
		return db.getFavouritesDao().observeIds(mangaId).map { it.toSet() }
	}

	suspend fun getCategory(id: Long): FavouriteCategory {
		return db.getFavouriteCategoriesDao().find(id.toInt()).toFavouriteCategory()
	}

	suspend fun getCategoriesIds(mangaIds: Collection<Long>): Set<Long> {
		return db.getFavouritesDao().findCategoriesIds(mangaIds).toSet()
	}

	suspend fun createCategory(
		title: String,
		sortOrder: ListSortOrder,
		isTrackerEnabled: Boolean,
		isVisibleOnShelf: Boolean,
	): FavouriteCategory {
		val entity = FavouriteCategoryEntity(
			title = title,
			createdAt = System.currentTimeMillis(),
			sortKey = db.getFavouriteCategoriesDao().getNextSortKey(),
			categoryId = 0,
			order = sortOrder.name,
			track = isTrackerEnabled,
			deletedAt = 0L,
			isVisibleInLibrary = isVisibleOnShelf,
		)
		val id = db.getFavouriteCategoriesDao().insert(entity)
		val category = entity.toFavouriteCategory(id)
		channels.createChannel(category)
		return category
	}

	suspend fun updateCategory(
		id: Long,
		title: String,
		sortOrder: ListSortOrder,
		isTrackerEnabled: Boolean,
		isVisibleOnShelf: Boolean,
	) {
		db.getFavouriteCategoriesDao().update(id, title, sortOrder.name, isTrackerEnabled, isVisibleOnShelf)
	}

	suspend fun updateCategory(id: Long, isVisibleInLibrary: Boolean) {
		db.getFavouriteCategoriesDao().updateLibVisibility(id, isVisibleInLibrary)
	}

	suspend fun updateCategoryTracking(id: Long, isTrackingEnabled: Boolean) {
		db.getFavouriteCategoriesDao().updateTracking(id, isTrackingEnabled)
	}

	suspend fun removeCategories(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().deleteAll(id)
				db.getFavouriteCategoriesDao().delete(id)
			}
		}
		// run after transaction success
		for (id in ids) {
			channels.deleteChannel(id)
		}
	}

	suspend fun setCategoryOrder(id: Long, order: ListSortOrder) {
		db.getFavouriteCategoriesDao().updateOrder(id, order.name)
	}

	suspend fun reorderCategories(orderedIds: List<Long>) {
		val dao = db.getFavouriteCategoriesDao()
		db.withTransaction {
			for ((i, id) in orderedIds.withIndex()) {
				dao.updateSortKey(id, i)
			}
		}
	}

	suspend fun addToCategory(categoryId: Long, mangas: Collection<Manga>) {
		db.withTransaction {
			for (manga in mangas) {
				val tags = manga.tags.toEntities()
				db.getTagsDao().upsert(tags)
				db.getMangaDao().upsert(manga.toEntity(), tags)
				val entity = FavouriteEntity(
					mangaId = manga.id,
					categoryId = categoryId,
					createdAt = System.currentTimeMillis(),
					sortKey = 0,
					deletedAt = 0L,
				)
				db.getFavouritesDao().insert(entity)
			}
		}
	}

	suspend fun removeFromFavourites(ids: Collection<Long>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().delete(mangaId = id)
			}
		}
		return ReversibleHandle { recoverToFavourites(ids) }
	}

	suspend fun removeFromCategory(categoryId: Long, ids: Collection<Long>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().delete(categoryId = categoryId, mangaId = id)
			}
		}
		return ReversibleHandle { recoverToCategory(categoryId, ids) }
	}

	private fun observeOrder(categoryId: Long): Flow<ListSortOrder> {
		return db.getFavouriteCategoriesDao().observe(categoryId)
			.filterNotNull()
			.map { x -> ListSortOrder(x.order, ListSortOrder.NEWEST) }
			.distinctUntilChanged()
	}

	private suspend fun recoverToFavourites(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().recover(mangaId = id)
			}
		}
	}

	private suspend fun recoverToCategory(categoryId: Long, ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().recover(mangaId = id, categoryId = categoryId)
			}
		}
	}
}