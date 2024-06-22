package com.example.dexreader

import android.content.Context
import android.os.StrictMode
import androidx.fragment.app.strictmode.FragmentStrictMode
import com.example.dexreader.core.BaseApp
import com.example.dexreader.local.data.LocalMangaRepository
import com.example.dexreader.local.data.PagesCache
import org.example.dexreader.parsers.MangaLoaderContext
import com.example.dexreader.reader.domain.PageLoader

class DexReaderApp : BaseApp() {

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		enableStrictMode()
	}

	private fun enableStrictMode() {
		StrictMode.setThreadPolicy(
			StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.penaltyLog()
				.build()
		)
		StrictMode.setVmPolicy(
			StrictMode.VmPolicy.Builder()
				.setClassInstanceLimit(LocalMangaRepository::class.java, 1)
				.setClassInstanceLimit(PagesCache::class.java, 1)
				.setClassInstanceLimit(MangaLoaderContext::class.java, 1)
				.setClassInstanceLimit(PageLoader::class.java, 1)
				.penaltyLog()
				.build()
		)
		FragmentStrictMode.defaultPolicy = FragmentStrictMode.Policy.Builder()
			.penaltyDeath()
			.detectFragmentReuse()
			.detectWrongFragmentContainer()
			.detectRetainInstanceUsage()
			.detectSetUserVisibleHint()
			.detectFragmentTagUsage()
			.build()
	}
}
