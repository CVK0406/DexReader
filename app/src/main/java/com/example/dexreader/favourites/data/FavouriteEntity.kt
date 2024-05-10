package com.example.dexreader.favourites.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.example.dexreader.core.db.TABLE_FAVOURITES
import com.example.dexreader.core.db.entity.MangaEntity

@Entity(
	tableName = TABLE_FAVOURITES,
	primaryKeys = ["manga_id", "category_id"],
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = FavouriteCategoryEntity::class,
			parentColumns = ["category_id"],
			childColumns = ["category_id"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
data class FavouriteEntity(
	@ColumnInfo(name = "manga_id", index = true) val mangaId: Long,
	@ColumnInfo(name = "category_id", index = true) val categoryId: Long,
	@ColumnInfo(name = "sort_key") val sortKey: Int,
	@ColumnInfo(name = "created_at") val createdAt: Long,
	@ColumnInfo(name = "deleted_at") val deletedAt: Long,
)
