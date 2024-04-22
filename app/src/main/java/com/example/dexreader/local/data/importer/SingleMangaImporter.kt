package com.example.dexreader.local.data.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.dexreader.core.exceptions.UnsupportedFileException
import com.example.dexreader.core.util.ext.resolveName
import com.example.dexreader.core.util.ext.writeAllCancellable
import com.example.dexreader.local.data.LocalStorageChanges
import com.example.dexreader.local.data.LocalStorageManager
import com.example.dexreader.local.data.hasCbzExtension
import com.example.dexreader.local.data.input.LocalMangaInput
import com.example.dexreader.local.domain.model.LocalManga
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject

@Reusable
class SingleMangaImporter @Inject constructor(
	@ApplicationContext private val context: Context,
	private val storageManager: LocalStorageManager,
	@LocalStorageChanges private val localStorageChanges: MutableSharedFlow<LocalManga?>,
) {

	private val contentResolver = context.contentResolver

	suspend fun import(uri: Uri): LocalManga {
		val result = if (isDirectory(uri)) {
			importDirectory(uri)
		} else {
			importFile(uri)
		}
		localStorageChanges.emit(result)
		return result
	}

	private suspend fun importFile(uri: Uri): LocalManga = withContext(Dispatchers.IO) {
		val contentResolver = storageManager.contentResolver
		val name = contentResolver.resolveName(uri) ?: throw IOException("Cannot fetch name from uri: $uri")
		if (!hasCbzExtension(name)) {
			throw UnsupportedFileException("Unsupported file $name on $uri")
		}
		val dest = File(getOutputDir(), name)
		runInterruptible {
			contentResolver.openInputStream(uri)
		}?.use { source ->
			dest.sink().buffer().use { output ->
				output.writeAllCancellable(source.source())
			}
		} ?: throw IOException("Cannot open input stream: $uri")
		LocalMangaInput.of(dest).getManga()
	}

	private suspend fun importDirectory(uri: Uri): LocalManga {
		val root = requireNotNull(DocumentFile.fromTreeUri(context, uri)) {
			"Provided uri $uri is not a tree"
		}
		val dest = File(getOutputDir(), root.requireName())
		dest.mkdir()
		for (docFile in root.listFiles()) {
			docFile.copyTo(dest)
		}
		return LocalMangaInput.of(dest).getManga()
	}

	/**
	 * TODO: progress
	 */
	private suspend fun DocumentFile.copyTo(destDir: File) {
		if (isDirectory) {
			val subDir = File(destDir, requireName())
			subDir.mkdir()
			for (docFile in listFiles()) {
				docFile.copyTo(subDir)
			}
		} else {
			inputStream().source().use { input ->
				File(destDir, requireName()).sink().buffer().use { output ->
					output.writeAllCancellable(input)
				}
			}
		}
	}

	private suspend fun getOutputDir(): File {
		return storageManager.getDefaultWriteableDir() ?: throw IOException("External files dir unavailable")
	}

	private suspend fun DocumentFile.inputStream() = runInterruptible(Dispatchers.IO) {
		contentResolver.openInputStream(uri) ?: throw IOException("Cannot open input stream: $uri")
	}

	private fun DocumentFile.requireName(): String {
		return name ?: throw IOException("Cannot fetch name from uri: $uri")
	}

	private fun isDirectory(uri: Uri): Boolean {
		return runCatching {
			DocumentFile.fromTreeUri(context, uri)
		}.isSuccess
	}
}