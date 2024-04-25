package com.example.dexreader.details.ui.related

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.databinding.ActivityContainerBinding
import com.example.dexreader.main.ui.owners.AppBarOwner
import org.example.dexreader.parsers.model.Manga

@AndroidEntryPoint
class RelatedMangaActivity : BaseActivity<ActivityContainerBinding>(), AppBarOwner {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				setReorderingAllowed(true)
				replace(R.id.container, RelatedListFragment::class.java, intent.extras)
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	companion object {

		fun newIntent(context: Context, seed: Manga) = Intent(context, RelatedMangaActivity::class.java)
			.putExtra(MangaIntent.KEY_MANGA, ParcelableManga(seed))
	}
}
