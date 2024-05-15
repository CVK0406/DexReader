package com.example.dexreader.favourites.ui.categories

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.R
import com.example.dexreader.core.model.FavouriteCategory
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.observeAsFlow
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.util.ext.requireValue
import com.example.dexreader.favourites.domain.FavouritesRepository
import com.example.dexreader.favourites.domain.model.Cover
import com.example.dexreader.favourites.ui.categories.adapter.AllCategoriesListModel
import com.example.dexreader.favourites.ui.categories.adapter.CategoryListModel
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingState
import javax.inject.Inject

@HiltViewModel
class FavouritesCategoriesViewModel @Inject constructor(
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private var commitJob: Job? = null

	val content = combine(
		repository.observeCategoriesWithCovers(),
		observeAllCategories(),
		settings.observeAsFlow(AppSettings.KEY_ALL_FAVOURITES_VISIBLE) { isAllFavouritesVisible },
	) { cats, all, showAll ->
		cats.toUiList(all, showAll)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	fun deleteCategories(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			repository.removeCategories(ids)
		}
	}

	fun setAllCategoriesVisible(isVisible: Boolean) {
		settings.isAllFavouritesVisible = isVisible
	}

	fun isEmpty(): Boolean = content.value.none { it is CategoryListModel }

	fun saveOrder(snapshot: List<ListModel>) {
		val prevJob = commitJob
		commitJob = launchJob {
			prevJob?.cancelAndJoin()
			val ids = snapshot.mapNotNullTo(ArrayList(snapshot.size)) {
				(it as? CategoryListModel)?.category?.id
			}
			if (ids.isNotEmpty()) {
				repository.reorderCategories(ids)
			}
		}
	}

	fun setIsVisible(ids: Set<Long>, isVisible: Boolean) {
		launchJob(Dispatchers.Default) {
			for (id in ids) {
				repository.updateCategory(id, isVisible)
			}
		}
	}

	fun getCategories(ids: Set<Long>): ArrayList<FavouriteCategory> {
		val items = content.requireValue()
		return items.mapNotNullTo(ArrayList(ids.size)) { item ->
			(item as? CategoryListModel)?.category?.takeIf { it.id in ids }
		}
	}

	private fun Map<FavouriteCategory, List<Cover>>.toUiList(
		allFavorites: Pair<Int, List<Cover>>,
		showAll: Boolean
	): List<ListModel> {
		if (isEmpty()) {
			return listOf(
				EmptyState(
					icon = R.drawable.ic_empty_favourites,
					textPrimary = R.string.text_empty_holder_primary,
					textSecondary = R.string.empty_favourite_categories,
					actionStringRes = 0,
				),
			)
		}
		val result = ArrayList<ListModel>(size + 1)
		result.add(
			AllCategoriesListModel(
				mangaCount = allFavorites.first,
				covers = allFavorites.second,
				isVisible = showAll,
			),
		)
		mapTo(result) { (category, covers) ->
			CategoryListModel(
				mangaCount = covers.size,
				covers = covers.take(3),
				category = category,
				isTrackerEnabled = settings.isTrackerEnabled && AppSettings.TRACK_FAVOURITES in settings.trackSources,
			)
		}
		return result
	}

	private fun observeAllCategories(): Flow<Pair<Int, List<Cover>>> {
		return settings.observeAsFlow(AppSettings.KEY_FAVORITES_ORDER) {
			allFavoritesSortOrder
		}.mapLatest { order ->
			repository.getAllFavoritesCovers(order, limit = 3)
		}.combine(repository.observeMangaCount()) { covers, count ->
			count to covers
		}
	}
}
