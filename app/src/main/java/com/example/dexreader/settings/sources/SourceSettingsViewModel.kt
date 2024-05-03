package com.example.dexreader.settings.sources

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.HttpUrl
import com.example.dexreader.R
import com.example.dexreader.core.network.cookies.MutableCookieJar
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.parser.RemoteMangaRepository
import com.example.dexreader.core.prefs.SourceSettings
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.util.ReversibleAction
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.core.util.ext.require
import com.example.dexreader.explore.data.MangaSourcesRepository
import org.example.dexreader.parsers.exception.AuthRequiredException
import org.example.dexreader.parsers.model.MangaSource
import javax.inject.Inject

@HiltViewModel
class SourceSettingsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	private val cookieJar: MutableCookieJar,
	private val mangaSourcesRepository: MangaSourcesRepository,
) : BaseViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

	val source = savedStateHandle.require<MangaSource>(SourceSettingsFragment.EXTRA_SOURCE)
	val repository = mangaRepositoryFactory.create(source) as RemoteMangaRepository

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val username = MutableStateFlow<String?>(null)
	val isEnabled = mangaSourcesRepository.observeIsEnabled(source)
	private var usernameLoadJob: Job? = null

	init {
		repository.getConfig().subscribe(this)
		loadUsername()
	}

	override fun onCleared() {
		repository.getConfig().unsubscribe(this)
		super.onCleared()
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key != SourceSettings.KEY_SLOWDOWN && key != SourceSettings.KEY_SORT_ORDER) {
			repository.invalidateCache()
		}
	}

	fun onResume() {
		if (usernameLoadJob?.isActive != true) {
			loadUsername()
		}
	}

	fun clearCookies() {
		launchLoadingJob(Dispatchers.Default) {
			val url = HttpUrl.Builder()
				.scheme("https")
				.host(repository.domain)
				.build()
			cookieJar.removeCookies(url, null)
			onActionDone.call(ReversibleAction(R.string.cookies_cleared, null))
			loadUsername()
		}
	}

	fun setEnabled(value: Boolean) {
		launchJob(Dispatchers.Default) {
			mangaSourcesRepository.setSourceEnabled(source, value)
		}
	}

	private fun loadUsername() {
		launchLoadingJob(Dispatchers.Default) {
			try {
				username.value = null
				username.value = repository.getAuthProvider()?.getUsername()
			} catch (_: AuthRequiredException) {
			}
		}
	}
}
