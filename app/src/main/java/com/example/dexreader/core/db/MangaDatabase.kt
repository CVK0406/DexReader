package com.example.dexreader.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.example.dexreader.bookmarks.data.BookmarkEntity
import com.example.dexreader.bookmarks.data.BookmarksDao
import com.example.dexreader.core.db.dao.MangaDao
import com.example.dexreader.core.db.dao.MangaSourcesDao
import com.example.dexreader.core.db.dao.PreferencesDao
import com.example.dexreader.core.db.dao.TagsDao
import com.example.dexreader.core.db.dao.TrackLogsDao
import com.example.dexreader.core.db.entity.MangaEntity
import com.example.dexreader.core.db.entity.MangaPrefsEntity
import com.example.dexreader.core.db.entity.MangaSourceEntity
import com.example.dexreader.core.db.entity.MangaTagsEntity
import com.example.dexreader.core.db.entity.TagEntity
import com.example.dexreader.core.db.migrations.Migration10To11
import com.example.dexreader.core.db.migrations.Migration11To12
import com.example.dexreader.core.db.migrations.Migration12To13
import com.example.dexreader.core.db.migrations.Migration13To14
import com.example.dexreader.core.db.migrations.Migration14To15
import com.example.dexreader.core.db.migrations.Migration15To16
import com.example.dexreader.core.db.migrations.Migration16To17
import com.example.dexreader.core.db.migrations.Migration17To18
import com.example.dexreader.core.db.migrations.Migration18To19
import com.example.dexreader.core.db.migrations.Migration19To20
import com.example.dexreader.core.db.migrations.Migration1To2
import com.example.dexreader.core.db.migrations.Migration20To21
import com.example.dexreader.core.db.migrations.Migration2To3
import com.example.dexreader.core.db.migrations.Migration3To4
import com.example.dexreader.core.db.migrations.Migration4To5
import com.example.dexreader.core.db.migrations.Migration5To6
import com.example.dexreader.core.db.migrations.Migration6To7
import com.example.dexreader.core.db.migrations.Migration7To8
import com.example.dexreader.core.db.migrations.Migration8To9
import com.example.dexreader.core.db.migrations.Migration9To10
import com.example.dexreader.core.util.ext.processLifecycleScope
import com.example.dexreader.favourites.data.FavouriteCategoriesDao
import com.example.dexreader.favourites.data.FavouriteCategoryEntity
import com.example.dexreader.favourites.data.FavouriteEntity
import com.example.dexreader.favourites.data.FavouritesDao
import com.example.dexreader.history.data.HistoryDao
import com.example.dexreader.history.data.HistoryEntity
import com.example.dexreader.stats.data.StatsDao
import com.example.dexreader.stats.data.StatsEntity

import com.example.dexreader.tracker.data.TrackEntity
import com.example.dexreader.tracker.data.TrackLogEntity
import com.example.dexreader.tracker.data.TracksDao
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val DATABASE_VERSION = 21

@Database(
	entities = [
		MangaEntity::class, TagEntity::class, HistoryEntity::class, MangaTagsEntity::class,
		FavouriteCategoryEntity::class, FavouriteEntity::class, MangaPrefsEntity::class,
		TrackEntity::class, TrackLogEntity::class, BookmarkEntity::class,
		MangaSourceEntity::class, StatsEntity::class,
	],
	version = DATABASE_VERSION,
)
abstract class MangaDatabase : RoomDatabase() {

	abstract fun getHistoryDao(): HistoryDao

	abstract fun getTagsDao(): TagsDao

	abstract fun getMangaDao(): MangaDao

	abstract fun getFavouritesDao(): FavouritesDao

	abstract fun getPreferencesDao(): PreferencesDao

	abstract fun getFavouriteCategoriesDao(): FavouriteCategoriesDao

	abstract fun getTracksDao(): TracksDao

	abstract fun getTrackLogsDao(): TrackLogsDao

	abstract fun getBookmarksDao(): BookmarksDao

	abstract fun getSourcesDao(): MangaSourcesDao

	abstract fun getStatsDao(): StatsDao
}

fun getDatabaseMigrations(context: Context): Array<Migration> = arrayOf(
	Migration1To2(),
	Migration2To3(),
	Migration3To4(),
	Migration4To5(),
	Migration5To6(),
	Migration6To7(),
	Migration7To8(),
	Migration8To9(),
	Migration9To10(),
	Migration10To11(),
	Migration11To12(),
	Migration12To13(),
	Migration13To14(),
	Migration14To15(),
	Migration15To16(),
	Migration16To17(context),
	Migration17To18(),
	Migration18To19(),
	Migration19To20(),
	Migration20To21(),
)

fun MangaDatabase(context: Context): MangaDatabase = Room
	.databaseBuilder(context, MangaDatabase::class.java, "dexreader-db")
	.addMigrations(*getDatabaseMigrations(context))
	.addCallback(DatabasePrePopulateCallback(context.resources))
	.build()

fun InvalidationTracker.removeObserverAsync(observer: InvalidationTracker.Observer) {
	val scope = processLifecycleScope
	if (scope.isActive) {
		processLifecycleScope.launch(Dispatchers.Default, CoroutineStart.ATOMIC) {
			removeObserver(observer)
		}
	}
}
