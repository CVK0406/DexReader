package com.example.dexreader.details.domain

import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.util.ext.printStackTraceDebug
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.util.runCatchingCancellable
import javax.inject.Inject

class RelatedMangaUseCase @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(seed: Manga) = runCatchingCancellable {
		mangaRepositoryFactory.create(seed.source).getRelated(seed)
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrNull()
}
