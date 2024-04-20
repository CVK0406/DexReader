package com.example.dexreader.explore.ui.adapter

import android.view.View
import com.example.dexreader.list.ui.adapter.ListHeaderClickListener
import com.example.dexreader.list.ui.adapter.ListStateHolderListener

interface ExploreListEventListener : ListStateHolderListener, View.OnClickListener, ListHeaderClickListener
