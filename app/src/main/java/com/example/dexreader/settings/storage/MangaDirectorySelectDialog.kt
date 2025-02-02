package com.example.dexreader.settings.storage

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.ui.AlertDialogFragment
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.showDistinct
import com.example.dexreader.databinding.DialogDirectorySelectBinding

@AndroidEntryPoint
class MangaDirectorySelectDialog : AlertDialogFragment<DialogDirectorySelectBinding>(),
	OnListItemClickListener<DirectoryModel> {

	private val viewModel: MangaDirectorySelectViewModel by viewModels()
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
			viewModel.refresh()
			pickFileTreeLauncher.launch(null)
		}
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogDirectorySelectBinding {
		return DialogDirectorySelectBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: DialogDirectorySelectBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = AsyncListDifferDelegationAdapter(DirectoryDiffCallback(), directoryAD(this))
		binding.root.adapter = adapter
		viewModel.items.observe(viewLifecycleOwner) { adapter.items = it }
		viewModel.onDismissDialog.observeEvent(viewLifecycleOwner) { dismiss() }
		viewModel.onPickDirectory.observeEvent(viewLifecycleOwner) { pickCustomDirectory() }
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setCancelable(true)
			.setTitle(R.string.manga_save_location)
			.setNegativeButton(android.R.string.cancel, null)
	}

	override fun onItemClick(item: DirectoryModel, view: View) {
		viewModel.onItemClick(item)
	}

	private fun pickCustomDirectory() {
		permissionRequestLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	}

	companion object {

		private const val TAG = "MangaDirectorySelectDialog"

		fun show(fm: FragmentManager) = MangaDirectorySelectDialog()
			.showDistinct(fm, TAG)
	}
}
