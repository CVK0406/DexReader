package com.example.dexreader.core.ui.util

import com.example.dexreader.core.prefs.AppSettings
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BaseActivityEntryPoint {
	val settings: AppSettings
}
