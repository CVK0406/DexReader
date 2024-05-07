package com.example.dexreader.core.fs

import android.os.Build
import com.example.dexreader.core.util.iterator.CloseableIterator
import com.example.dexreader.core.util.iterator.MappingIterator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class FileSequence(private val dir: File) : Sequence<File> {

	override fun iterator(): Iterator<File> {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val stream = Files.newDirectoryStream(dir.toPath())
			CloseableIterator(MappingIterator(stream.iterator(), Path::toFile), stream)
		} else {
			dir.listFiles().orEmpty().iterator()
		}
	}
}
