package com.example.dexreader.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration20To21 : Migration(20, 21) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("DROP TABLE IF EXISTS scrobblings")
	}
}
