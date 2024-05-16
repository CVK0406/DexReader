package com.example.dexreader.favourites.ui.container

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.R
import com.example.dexreader.core.model.FavouriteCategory
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.observeAsFlow
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.util.ReversibleAction
import com.example.dexreader.core.ui.util.ReversibleHandle
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.favourites.domain.FavouritesRepository
import com.example.dexreader.favourites.ui.list.FavouritesListFragment.Companion.NO_ID
import javax.inject.Inject

@HiltViewModel
class FavouritesContainerViewModel @Inject constructor(
	private val settings: AppSettings,
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val onActionDone = MutableEventFlow<ReversibleAction>()

	private val categoriesStateFlow = favouritesRepository.observeCategoriesForLibrary()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val categories = combine(
		categoriesStateFlow.filterNotNull(),
		observeAllFavouritesVisibility(),
	) { list, showAll ->
		list.toUi(showAll)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val isEmpty = categoriesStateFlow.map {
		it?.isEmpty() == true
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	private fun List<FavouriteCategory>.toUi(showAll: Boolean): List<FavouriteTabModel> {
		if (isEmpty()) {
			return emptyList()
		}
		val result = ArrayList<FavouriteTabModel>(if (showAll) size + 1 else size)
		if (showAll) {
			result.add(FavouriteTabModel(NO_ID, null))
		}
		mapTo(result) { FavouriteTabModel(it.id, it.title) }
		return result
	}

	fun hide(categoryId: Long) {
		launchJob(Dispatchers.Default) {
			if (categoryId == NO_ID) {
				settings.isAllFavouritesVisible = false
			} else {
				favouritesRepository.updateCategory(categoryId, isVisibleInLibrary = false)
				val reverse = ReversibleHandle {
					favouritesRepository.updateCategory(categoryId, isVisibleInLibrary = true)
				}
				onActionDone.call(ReversibleAction(R.string.category_hidden_done, reverse))
			}
		}
	}

	fun deleteCategory(categoryId: Long) {
		launchJob(Dispatchers.Default) {
			favouritesRepository.removeCategories(setOf(categoryId))
		}
	}

	private fun observeAllFavouritesVisibility() = settings.observeAsFlow(
		key = AppSettings.KEY_ALL_FAVOURITES_VISIBLE,
		valueProducer = { isAllFavouritesVisible },
	)
}
