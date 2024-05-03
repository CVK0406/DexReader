package com.example.dexreader.details.domain

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runInterruptible
import okio.IOException
import com.example.dexreader.core.model.isLocal
import com.example.dexreader.core.parser.MangaDataRepository
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.util.ext.peek
import com.example.dexreader.core.util.ext.sanitize
import com.example.dexreader.details.data.MangaDetails
import com.example.dexreader.explore.domain.RecoverMangaUseCase
import com.example.dexreader.local.data.LocalMangaRepository
import org.example.dexreader.parsers.exception.NotFoundException
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.util.recoverNotNull
import org.example.dexreader.parsers.util.runCatchingCancellable
import javax.inject.Inject

class DetailsLoadUseCase @Inject constructor(
	private val mangaDataRepository: MangaDataRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val recoverUseCase: RecoverMangaUseCase,
	private val imageGetter: Html.ImageGetter,
) {

	operator fun invoke(intent: MangaIntent): Flow<MangaDetails> = channelFlow {
		val manga = requireNotNull(mangaDataRepository.resolveIntent(intent)) {
			"Cannot resolve intent $intent"
		}
		val local = if (!manga.isLocal) {
			async {
				localMangaRepository.findSavedManga(manga)
			}
		} else {
			null
		}
		send(MangaDetails(manga, null, null, false))
		try {
			val details = getDetails(manga)
			send(MangaDetails(details, local?.peek(), details.description?.parseAsHtml(withImages = false), false))
			send(MangaDetails(details, local?.await(), details.description?.parseAsHtml(withImages = true), true))
		} catch (e: IOException) {
			local?.await()?.manga?.also { localManga ->
				send(MangaDetails(localManga, null, localManga.description?.parseAsHtml(withImages = false), true))
			} ?: throw e
		}
	}

	private suspend fun getDetails(seed: Manga) = runCatchingCancellable {
		val repository = mangaRepositoryFactory.create(seed.source)
		repository.getDetails(seed)
	}.recoverNotNull { e ->
		if (e is NotFoundException) {
			recoverUseCase(seed)
		} else {
			null
		}
	}.getOrThrow()

	private suspend fun String.parseAsHtml(withImages: Boolean): CharSequence? {
		return if (withImages) {
			runInterruptible(Dispatchers.IO) {
				parseAsHtml(imageGetter = imageGetter)
			}.filterSpans()
		} else {
			runInterruptible(Dispatchers.Default) {
				parseAsHtml()
			}.filterSpans().sanitize()
		}.takeUnless { it.isBlank() }
	}

	private fun Spanned.filterSpans(): Spanned {
		val spannable = SpannableString.valueOf(this)
		val spans = spannable.getSpans<ForegroundColorSpan>()
		for (span in spans) {
			spannable.removeSpan(span)
		}
		return spannable
	}
}
