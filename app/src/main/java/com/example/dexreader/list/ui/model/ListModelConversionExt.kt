package com.example.dexreader.list.ui.model

import com.example.dexreader.R
import com.example.dexreader.core.exceptions.resolve.ExceptionResolver
import com.example.dexreader.core.prefs.ListMode
import com.example.dexreader.core.ui.widgets.ChipsView
import com.example.dexreader.core.util.ext.getDisplayIcon
import com.example.dexreader.core.util.ext.ifZero
import com.example.dexreader.history.data.PROGRESS_NONE
import com.example.dexreader.list.domain.ListExtraProvider
import org.example.dexreader.parsers.model.Manga

suspend fun Manga.toListModel(
	extraProvider: ListExtraProvider?
) = MangaListModel(
	id = id,
	title = title,
	subtitle = tags.joinToString(", ") { it.title },
	coverUrl = coverUrl,
	manga = this,
	counter = extraProvider?.getCounter(id) ?: 0,
	progress = extraProvider?.getProgress(id) ?: PROGRESS_NONE,
)

suspend fun Manga.toListDetailedModel(
	extraProvider: ListExtraProvider?,
) = MangaListDetailedModel(
	id = id,
	title = title,
	subtitle = altTitle,
	coverUrl = coverUrl,
	manga = this,
	counter = extraProvider?.getCounter(id) ?: 0,
	progress = extraProvider?.getProgress(id) ?: PROGRESS_NONE,
	tags = tags.map {
		ChipsView.ChipModel(
			tint = extraProvider?.getTagTint(it) ?: 0,
			title = it.title,
			icon = 0,
			isCheckable = false,
			isChecked = false,
			data = it,
		)
	},
)

suspend fun Manga.toGridModel(
	extraProvider: ListExtraProvider?,
) = MangaGridModel(
	id = id,
	title = title,
	coverUrl = coverUrl,
	manga = this,
	counter = extraProvider?.getCounter(id) ?: 0,
	progress = extraProvider?.getProgress(id) ?: PROGRESS_NONE,
)

suspend fun List<Manga>.toUi(
	mode: ListMode,
	extraProvider: ListExtraProvider,
): List<MangaItemModel> = if (isEmpty()) {
	emptyList()
} else {
	toUi(ArrayList(size), mode, extraProvider)
}

suspend fun <C : MutableCollection<in MangaItemModel>> List<Manga>.toUi(
	destination: C,
	mode: ListMode,
	extraProvider: ListExtraProvider,
): C = when (mode) {
	ListMode.LIST -> mapTo(destination) { it.toListModel(extraProvider) }
	ListMode.DETAILED_LIST -> mapTo(destination) { it.toListDetailedModel(extraProvider) }
	ListMode.GRID -> mapTo(destination) { it.toGridModel(extraProvider) }
}

fun Throwable.toErrorState(canRetry: Boolean = true) = ErrorState(
	exception = this,
	icon = getDisplayIcon(),
	canRetry = canRetry,
	buttonText = ExceptionResolver.getResolveStringId(this).ifZero { R.string.try_again },
)

fun Throwable.toErrorFooter() = ErrorFooter(
	exception = this,
)
