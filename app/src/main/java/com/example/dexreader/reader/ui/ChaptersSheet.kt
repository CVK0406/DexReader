package com.example.dexreader.reader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dexreader.R
import com.example.dexreader.core.model.MangaHistory
import com.example.dexreader.core.model.findById
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.sheet.BaseAdaptiveSheet
import com.example.dexreader.core.util.RecyclerViewScrollCallback
import com.example.dexreader.core.util.ext.showDistinct
import com.example.dexreader.databinding.SheetChaptersBinding
import com.example.dexreader.details.ui.adapter.ChaptersAdapter
import com.example.dexreader.details.ui.mapChapters
import com.example.dexreader.details.ui.model.ChapterListItem
import com.example.dexreader.details.ui.pager.chapters.ChapterGridSpanHelper
import com.example.dexreader.details.ui.withVolumeHeaders
import com.example.dexreader.history.data.PROGRESS_NONE
import com.example.dexreader.list.ui.adapter.TypedListSpacingDecoration
import dagger.hilt.android.AndroidEntryPoint
import org.example.dexreader.parsers.model.MangaChapter
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class ChaptersSheet : BaseAdaptiveSheet<SheetChaptersBinding>(),
	OnListItemClickListener<ChapterListItem> {

	@Inject
	lateinit var settings: AppSettings

	private val viewModel: ReaderViewModel by activityViewModels()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = SheetChaptersBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: SheetChaptersBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val manga = viewModel.manga
		if (manga == null) {
			dismissAllowingStateLoss()
			return
		}
		val state = viewModel.getCurrentState()
		val currentChapter = state?.let { manga.allChapters.findById(it.chapterId) }
		val chapters = manga.mapChapters(
			history = state?.let {
				MangaHistory(
					createdAt = Instant.now(),
					updatedAt = Instant.now(),
					chapterId = it.chapterId,
					page = it.page,
					scroll = it.scroll,
					percent = PROGRESS_NONE,
				)
			},
			newCount = 0,
			branch = currentChapter?.branch,
			bookmarks = listOf(),
			isGrid = settings.isChaptersGridView,
		).withVolumeHeaders(binding.root.context)
		if (chapters.isEmpty()) {
			dismissAllowingStateLoss()
			return
		}
		val currentPosition = if (currentChapter != null) {
			chapters.indexOfFirst { it is ChapterListItem && it.chapter.id == currentChapter.id }
		} else {
			-1
		}
		binding.recyclerView.addItemDecoration(TypedListSpacingDecoration(binding.recyclerView.context, true))
		binding.recyclerView.adapter = ChaptersAdapter(this).also { adapter ->
			if (currentPosition >= 0) {
				val targetPosition = (currentPosition - 1).coerceAtLeast(0)
				val offset =
					(resources.getDimensionPixelSize(R.dimen.chapter_list_item_height) * 0.6).roundToInt()
				adapter.setItems(
					chapters, RecyclerViewScrollCallback(binding.recyclerView, targetPosition, offset),
				)
			} else {
				adapter.items = chapters
			}
		}
		ChapterGridSpanHelper.attach(binding.recyclerView)
		binding.recyclerView.layoutManager = if (settings.isChaptersGridView) {
			GridLayoutManager(context, ChapterGridSpanHelper.getSpanCount(binding.recyclerView)).apply {
				spanSizeLookup = ChapterGridSpanHelper.SpanSizeLookup(binding.recyclerView)
			}
		} else {
			LinearLayoutManager(context)
		}
	}

	override fun onItemClick(item: ChapterListItem, view: View) {
		((parentFragment as? OnChapterChangeListener)
			?: (activity as? OnChapterChangeListener))?.let {
			dismiss()
			it.onChapterChanged(item.chapter)
		}
	}

	fun interface OnChapterChangeListener {

		fun onChapterChanged(chapter: MangaChapter)
	}

	companion object {

		private const val TAG = "ChaptersBottomSheet"

		fun show(fm: FragmentManager) = ChaptersSheet().showDistinct(fm, TAG)
	}
}
