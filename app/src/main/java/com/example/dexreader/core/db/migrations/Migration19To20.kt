package com.example.dexreader.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration19To20 : Migration(19, 20) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("DROP TABLE IF EXISTS suggestions")
	}
}
