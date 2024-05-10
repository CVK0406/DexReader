package com.example.dexreader.favourites.data

import com.example.dexreader.core.db.entity.toManga
import com.example.dexreader.core.db.entity.toMangaTags
import com.example.dexreader.core.model.FavouriteCategory
import com.example.dexreader.list.domain.ListSortOrder
import java.time.Instant

fun FavouriteCategoryEntity.toFavouriteCategory(id: Long = categoryId.toLong()) = FavouriteCategory(
	id = id,
	title = title,
	sortKey = sortKey,
	order = ListSortOrder(order, ListSortOrder.NEWEST),
	createdAt = Instant.ofEpochMilli(createdAt),
	isTrackingEnabled = track,
	isVisibleInLibrary = isVisibleInLibrary,
)

fun FavouriteManga.toManga() = manga.toManga(tags.toMangaTags())

fun Collection<FavouriteManga>.toMangaList() = map { it.toManga() }
