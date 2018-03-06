package com.ramotion.navigationtoolbar.example.simple

import com.ramotion.navigationtoolbar.HeaderLayoutManager
import com.ramotion.navigationtoolbar.HeaderLayoutManager.ScrollState
import com.ramotion.navigationtoolbar.HeaderLayoutManager.ScrollState.IDLE
import com.ramotion.navigationtoolbar.NavigationToolBarLayout
import java.lang.ref.WeakReference

class SimpleSnapHelper : HeaderLayoutManager.ScrollStateListener {

    private var toolBarRef: WeakReference<NavigationToolBarLayout>? = null

    override fun onScrollStateChanged(state: ScrollState) {
        if (state != IDLE) {
            return
        }

        toolBarRef?.get()?.also { toolbar ->
            toolbar.getAnchorPos()?.also { toolbar.smoothScrollToPosition(it) }
        }
    }

    fun attach(toolbar: NavigationToolBarLayout) {
        toolBarRef = WeakReference(toolbar)
        toolbar.addScrollStateListener(this)
    }

    fun detach(toolbar: NavigationToolBarLayout) {
        toolBarRef = null
        toolbar.removeScrollStateListener(this)
    }

}