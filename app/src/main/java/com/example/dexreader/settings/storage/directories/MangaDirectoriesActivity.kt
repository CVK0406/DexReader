package com.example.dexreader.settings.storage.directories

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.tryLaunch
import com.example.dexreader.databinding.ActivityMangaDirectoriesBinding
import com.example.dexreader.settings.storage.DirectoryDiffCallback
import com.example.dexreader.settings.storage.DirectoryModel
import com.example.dexreader.settings.storage.RequestStorageManagerPermissionContract

@AndroidEntryPoint
class MangaDirectoriesActivity : BaseActivity<ActivityMangaDirectoriesBinding>(),
	OnListItemClickListener<DirectoryModel>, View.OnClickListener {

	private val viewModel: MangaDirectoriesViewModel by viewModels()
	private val pickFileTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
		if (it != null) viewModel.onCustomDirectoryPicked(it)
	}
	private val permissionRequestLauncher = registerForActivityResult(
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			RequestStorageManagerPermissionContract()
		} else {
			ActivityResultContracts.RequestPermission()
		},
	) {
		if (it) {
			viewModel.updateList()
			if (!pickFileTreeLauncher.tryLaunch(null)) {
				Snackbar.make(
					viewBinding.recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
				).show()
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMangaDirectoriesBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = AsyncListDifferDelegationAdapter(DirectoryDiffCallback(), directoryConfigAD(this))
		viewBinding.recyclerView.adapter = adapter
		viewBinding.fabAdd.setOnClickListener(this)
		viewModel.items.observe(this) { adapter.items = it }
		viewModel.isLoading.observe(this) { viewBinding.progressBar.isVisible = it }
	}

	override fun onItemClick(item: DirectoryModel, view: View) {
		viewModel.onRemoveClick(item.file ?: return)
	}

	override fun onClick(v: View?) {
		if (!permissionRequestLauncher.tryLaunch(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			Snackbar.make(
				viewBinding.recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
			).show()
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.fabAdd.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			rightMargin = topMargin + insets.right
			leftMargin = topMargin + insets.left
			bottomMargin = topMargin + insets.bottom
		}
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, MangaDirectoriesActivity::class.java)
	}
}
