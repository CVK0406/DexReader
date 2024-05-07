package com.example.dexreader.core.model

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import androidx.annotation.StringRes
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import com.example.dexreader.R
import com.example.dexreader.core.util.ext.getDisplayName
import com.example.dexreader.core.util.ext.getThemeColor
import com.example.dexreader.core.util.ext.toLocale
import org.example.dexreader.parsers.model.ContentType
import org.example.dexreader.parsers.model.MangaSource
import com.google.android.material.R as materialR

fun MangaSource(name: String): MangaSource {
	MangaSource.entries.forEach {
		if (it.name == name) return it
	}
	return MangaSource.DUMMY
}

fun MangaSource.isNsfw() = contentType == ContentType.HENTAI

@get:StringRes
val ContentType.titleResId
	get() = when (this) {
		ContentType.MANGA -> R.string.content_type_manga
		ContentType.HENTAI -> R.string.content_type_hentai
		ContentType.COMICS -> R.string.content_type_comics
		ContentType.OTHER -> R.string.content_type_other
	}

fun MangaSource.getSummary(context: Context): String {
	val type = context.getString(contentType.titleResId)
	val locale = locale?.toLocale().getDisplayName(context)
	return context.getString(R.string.source_summary_pattern, type, locale)
}

fun MangaSource.getTitle(context: Context): CharSequence = if (isNsfw()) {
	buildSpannedString {
		append(title)
		append(' ')
		appendNsfwLabel(context)
	}
} else {
	title
}

private fun SpannableStringBuilder.appendNsfwLabel(context: Context) = inSpans(
	ForegroundColorSpan(context.getThemeColor(materialR.attr.colorError, Color.RED)),
	RelativeSizeSpan(0.74f),
	SuperscriptSpan(),
) {
	append(context.getString(R.string.nsfw))
}
