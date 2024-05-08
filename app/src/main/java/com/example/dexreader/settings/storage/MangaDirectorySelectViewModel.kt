package com.example.dexreader.settings.storage

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.local.data.LocalStorageManager
import javax.inject.Inject

@HiltViewModel
class MangaDirectorySelectViewModel @Inject constructor(
	private val storageManager: LocalStorageManager,
	private val settings: AppSettings,
) : BaseViewModel() {

	val items = MutableStateFlow(emptyList<DirectoryModel>())
	val onDismissDialog = MutableEventFlow<Unit>()
	val onPickDirectory = MutableEventFlow<Unit>()

	init {
		refresh()
	}

	fun onItemClick(item: DirectoryModel) {
		if (item.file != null) {
			settings.mangaStorageDir = item.file
			onDismissDialog.call(Unit)
		} else {
			onPickDirectory.call(Unit)
		}
	}

	fun onCustomDirectoryPicked(uri: Uri) {
		launchJob(Dispatchers.Default) {
			storageManager.takePermissions(uri)
			val dir = requireNotNull(storageManager.resolveUri(uri)) {
				"Cannot resolve file name of \"$uri\""
			}
			if (!dir.canWrite()) {
				throw AccessDeniedException(dir)
			}
			if (dir !in storageManager.getApplicationStorageDirs()) {
				settings.mangaStorageDir = dir
				storageManager.setDirIsNoMedia(dir)
			}
			onDismissDialog.call(Unit)
		}
	}

	fun refresh() {
		launchJob(Dispatchers.Default) {
			val defaultValue = storageManager.getDefaultWriteableDir()
			val available = storageManager.getWriteableDirs()
			items.value = buildList(available.size + 1) {
				available.mapTo(this) { dir ->
					DirectoryModel(
						title = storageManager.getDirectoryDisplayName(dir, isFullPath = false),
						titleRes = 0,
						file = dir,
						isChecked = dir == defaultValue,
						isAvailable = true,
						isRemovable = false,
					)
				}
				this += DirectoryModel(
					title = null,
					titleRes = R.string.pick_custom_directory,
					file = null,
					isChecked = false,
					isAvailable = true,
					isRemovable = false,
				)
			}
		}
	}
}
