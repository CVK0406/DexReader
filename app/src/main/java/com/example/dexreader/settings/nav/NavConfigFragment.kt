package com.example.dexreader.settings.nav

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.prefs.NavItem
import com.example.dexreader.core.ui.BaseFragment
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.dialog.RecyclerViewAlertDialog
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.util.RecyclerViewOwner
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.databinding.FragmentSettingsSourcesBinding
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.settings.nav.adapter.navAddAD
import com.example.dexreader.settings.nav.adapter.navAvailableAD
import com.example.dexreader.settings.nav.adapter.navConfigAD

@AndroidEntryPoint
class NavConfigFragment : BaseFragment<FragmentSettingsSourcesBinding>(), RecyclerViewOwner,
	OnListItemClickListener<NavItem>, View.OnClickListener {

	private var reorderHelper: ItemTouchHelper? = null
	private val viewModel by viewModels<NavConfigViewModel>()

	override val recyclerView: RecyclerView
		get() = requireViewBinding().recyclerView

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	): FragmentSettingsSourcesBinding {
		return FragmentSettingsSourcesBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(
		binding: FragmentSettingsSourcesBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val navConfigAdapter = BaseListAdapter<ListModel>()
			.addDelegate(ListItemType.NAV_ITEM, navConfigAD(this))
			.addDelegate(ListItemType.FOOTER_LOADING, navAddAD(this))
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = navConfigAdapter
			reorderHelper = ItemTouchHelper(ReorderCallback()).also {
				it.attachToRecyclerView(this)
			}
		}
		viewModel.content.observe(viewLifecycleOwner, navConfigAdapter)
	}

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.main_screen_sections)
	}

	override fun onDestroyView() {
		reorderHelper = null
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		requireViewBinding().recyclerView.updatePadding(
			bottom = insets.bottom,
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onClick(v: View) {
		var dialog: DialogInterface? = null
		val listener = OnListItemClickListener<NavItem> { item, _ ->
			viewModel.addItem(item)
			dialog?.dismiss()
		}
		dialog = RecyclerViewAlertDialog.Builder<NavItem>(v.context)
			.setTitle(R.string.add)
			.addAdapterDelegate(navAvailableAD(listener))
			.setCancelable(true)
			.setItems(viewModel.availableItems)
			.setNegativeButton(android.R.string.cancel, null)
			.create()
			.apply { show() }
	}

	override fun onItemClick(item: NavItem, view: View) {
		viewModel.removeItem(item)
	}

	override fun onItemLongClick(item: NavItem, view: View): Boolean {
		val holder = viewBinding?.recyclerView?.findContainingViewHolder(view) ?: return false
		reorderHelper?.startDrag(holder)
		return true
	}

	private inner class ReorderCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP,
		0,
	) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = target.itemViewType == ListItemType.NAV_ITEM.ordinal

		override fun onMoved(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			fromPos: Int,
			target: RecyclerView.ViewHolder,
			toPos: Int,
			x: Int,
			y: Int,
		) {
			super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
			viewModel.reorder(fromPos, toPos)
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

		override fun isLongPressDragEnabled() = false
	}
}
