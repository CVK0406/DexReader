package com.example.dexreader.favourites.ui.container

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import coil.ImageLoader
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.ui.BaseFragment
import com.example.dexreader.core.ui.util.ActionModeListener
import com.example.dexreader.core.ui.util.ReversibleActionObserver
import com.example.dexreader.core.util.ext.addMenuProvider
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.recyclerView
import com.example.dexreader.core.util.ext.setTabsEnabled
import com.example.dexreader.core.util.ext.setTextAndVisible
import com.example.dexreader.databinding.FragmentFavouritesContainerBinding
import com.example.dexreader.databinding.ItemEmptyStateBinding
import com.example.dexreader.favourites.ui.categories.FavouriteCategoriesActivity
import javax.inject.Inject

@AndroidEntryPoint
class FavouritesContainerFragment : BaseFragment<FragmentFavouritesContainerBinding>(), ActionModeListener,
	ViewStub.OnInflateListener, View.OnClickListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel: FavouritesContainerViewModel by viewModels()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentFavouritesContainerBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentFavouritesContainerBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val pagerAdapter = FavouritesContainerAdapter(this)
		binding.pager.adapter = pagerAdapter
		binding.pager.offscreenPageLimit = 1
		binding.pager.recyclerView?.isNestedScrollingEnabled = false
		TabLayoutMediator(
			binding.tabs,
			binding.pager,
			FavouritesTabConfigurationStrategy(pagerAdapter, viewModel),
		).attach()
		binding.stubEmpty.setOnInflateListener(this)
		actionModeDelegate.addListener(this)
		viewModel.categories.observe(viewLifecycleOwner, pagerAdapter)
		viewModel.isEmpty.observe(viewLifecycleOwner, ::onEmptyStateChanged)
		addMenuProvider(FavouritesContainerMenuProvider(binding.root.context))
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(binding.pager))
	}

	override fun onDestroyView() {
		actionModeDelegate.removeListener(this)
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding?.tabs?.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onActionModeStarted(mode: ActionMode) {
		viewBinding?.run {
			pager.isUserInputEnabled = false
			tabs.setTabsEnabled(false)
		}
	}

	override fun onActionModeFinished(mode: ActionMode) {
		viewBinding?.run {
			pager.isUserInputEnabled = true
			tabs.setTabsEnabled(true)
		}
	}

	override fun onInflate(stub: ViewStub?, inflated: View) {
		val stubBinding = ItemEmptyStateBinding.bind(inflated)
		stubBinding.icon.newImageRequest(viewLifecycleOwner, R.drawable.ic_empty_favourites)?.enqueueWith(coil)
		stubBinding.textPrimary.setText(R.string.text_empty_holder_primary)
		stubBinding.textSecondary.setTextAndVisible(R.string.empty_favourite_categories)
		stubBinding.buttonRetry.setTextAndVisible(R.string.manage)
		stubBinding.buttonRetry.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> startActivity(
				FavouriteCategoriesActivity.newIntent(v.context),
			)
		}
	}

	private fun onEmptyStateChanged(isEmpty: Boolean) {
		viewBinding?.run {
			pager.isGone = isEmpty
			tabs.isGone = isEmpty
			stubEmpty.isVisible = isEmpty
		}
	}
}
