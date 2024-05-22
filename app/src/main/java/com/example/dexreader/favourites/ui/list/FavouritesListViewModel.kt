package com.example.dexreader.favourites.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.observeAsFlow
import com.example.dexreader.core.ui.util.ReversibleAction
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.download.ui.worker.DownloadWorker
import com.example.dexreader.favourites.domain.FavouritesRepository
import com.example.dexreader.favourites.ui.list.FavouritesListFragment.Companion.ARG_CATEGORY_ID
import com.example.dexreader.favourites.ui.list.FavouritesListFragment.Companion.NO_ID
import com.example.dexreader.history.domain.MarkAsReadUseCase
import com.example.dexreader.list.domain.ListExtraProvider
import com.example.dexreader.list.domain.ListSortOrder
import com.example.dexreader.list.ui.MangaListViewModel
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.list.ui.model.LoadingState
import com.example.dexreader.list.ui.model.toErrorState
import com.example.dexreader.list.ui.model.toUi
import org.example.dexreader.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class FavouritesListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val repository: FavouritesRepository,
	private val listExtraProvider: ListExtraProvider,
	private val markAsReadUseCase: MarkAsReadUseCase,
	settings: AppSettings,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler) {

	val categoryId: Long = savedStateHandle[ARG_CATEGORY_ID] ?: NO_ID
	private val refreshTrigger = MutableStateFlow(Any())

	override val listMode = settings.observeAsFlow(AppSettings.KEY_LIST_MODE_FAVORITES) { favoritesListMode }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, settings.favoritesListMode)

	val sortOrder: StateFlow<ListSortOrder?> = if (categoryId == NO_ID) {
		settings.observeAsFlow(AppSettings.KEY_FAVORITES_ORDER) {
			allFavoritesSortOrder
		}
	} else {
		repository.observeCategory(categoryId)
			.map { it?.order }
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	override val content = combine(
		if (categoryId == NO_ID) {
			sortOrder.filterNotNull().flatMapLatest {
				repository.observeAll(it)
			}
		} else {
			repository.observeAll(categoryId)
		},
		listMode,
		refreshTrigger,
	) { list, mode, _ ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_favourites,
					textPrimary = R.string.text_empty_holder_primary,
					textSecondary = if (categoryId == NO_ID) {
						R.string.you_have_not_favourites_yet
					} else {
						R.string.favourites_category_empty
					},
					actionStringRes = 0,
				),
			)

			else -> list.toUi(mode, listExtraProvider)
		}
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() {
		refreshTrigger.value = Any()
	}

	override fun onRetry() = Unit

	fun markAsRead(items: Set<Manga>) {
		launchLoadingJob(Dispatchers.Default) {
			markAsReadUseCase(items)
			onRefresh()
		}
	}

	fun removeFromFavourites(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = if (categoryId == NO_ID) {
				repository.removeFromFavourites(ids)
			} else {
				repository.removeFromCategory(categoryId, ids)
			}
			onActionDone.call(ReversibleAction(R.string.removed_from_favourites, handle))
		}
	}

	fun setSortOrder(order: ListSortOrder) {
		if (categoryId == NO_ID) {
			return
		}
		launchJob {
			repository.setCategoryOrder(categoryId, order)
		}
	}
}
