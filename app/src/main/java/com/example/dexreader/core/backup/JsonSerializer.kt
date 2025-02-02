package com.example.dexreader.core.backup

import com.example.dexreader.bookmarks.data.BookmarkEntity
import com.example.dexreader.core.db.entity.MangaEntity
import com.example.dexreader.core.db.entity.MangaSourceEntity
import com.example.dexreader.core.db.entity.TagEntity
import com.example.dexreader.favourites.data.FavouriteCategoryEntity
import com.example.dexreader.favourites.data.FavouriteEntity
import com.example.dexreader.history.data.HistoryEntity
import org.json.JSONObject

class JsonSerializer private constructor(private val json: JSONObject) {

	constructor(e: FavouriteEntity) : this(
		JSONObject().apply {
			put("manga_id", e.mangaId)
			put("category_id", e.categoryId)
			put("sort_key", e.sortKey)
			put("created_at", e.createdAt)
		},
	)

	constructor(e: FavouriteCategoryEntity) : this(
		JSONObject().apply {
			put("category_id", e.categoryId)
			put("created_at", e.createdAt)
			put("sort_key", e.sortKey)
			put("title", e.title)
			put("order", e.order)
			put("track", e.track)
			put("show_in_lib", e.isVisibleInLibrary)
		},
	)

	constructor(e: HistoryEntity) : this(
		JSONObject().apply {
			put("manga_id", e.mangaId)
			put("created_at", e.createdAt)
			put("updated_at", e.updatedAt)
			put("chapter_id", e.chapterId)
			put("page", e.page)
			put("scroll", e.scroll)
			put("percent", e.percent)
			put("chapters", e.chaptersCount)
		},
	)

	constructor(e: TagEntity) : this(
		JSONObject().apply {
			put("id", e.id)
			put("title", e.title)
			put("key", e.key)
			put("source", e.source)
		},
	)

	constructor(e: MangaEntity) : this(
		JSONObject().apply {
			put("id", e.id)
			put("title", e.title)
			put("alt_title", e.altTitle)
			put("url", e.url)
			put("public_url", e.publicUrl)
			put("rating", e.rating)
			put("nsfw", e.isNsfw)
			put("cover_url", e.coverUrl)
			put("large_cover_url", e.largeCoverUrl)
			put("state", e.state)
			put("author", e.author)
			put("source", e.source)
		},
	)

	constructor(e: BookmarkEntity) : this(
		JSONObject().apply {
			put("manga_id", e.mangaId)
			put("page_id", e.pageId)
			put("chapter_id", e.chapterId)
			put("page", e.page)
			put("scroll", e.scroll)
			put("image_url", e.imageUrl)
			put("created_at", e.createdAt)
			put("percent", e.percent)
		},
	)

	constructor(e: MangaSourceEntity) : this(
		JSONObject().apply {
			put("source", e.source)
			put("enabled", e.isEnabled)
			put("sort_key", e.sortKey)
		},
	)

	constructor(m: Map<String, *>) : this(
		JSONObject(m),
	)

	fun toJson(): JSONObject = json
}
