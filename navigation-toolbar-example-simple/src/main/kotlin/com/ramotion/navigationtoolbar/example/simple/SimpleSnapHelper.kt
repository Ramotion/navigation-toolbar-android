package com.ramotion.navigationtoolbar.example.simple

import com.ramotion.navigationtoolbar.HeaderLayoutManager.ScrollState
import com.ramotion.navigationtoolbar.HeaderLayoutManager.ScrollState.*
import com.ramotion.navigationtoolbar.NavigationToolBarLayout
import com.ramotion.navigationtoolbar.ScrollStateListener
import java.lang.ref.WeakReference

class SimpleSnapHelper {

    private var mToolBarRef: WeakReference<NavigationToolBarLayout>? = null

    private val mScrollStateListener = object : ScrollStateListener {
        override fun invoke(state: ScrollState) {
            if (state != IDLE) {
                return
            }

            mToolBarRef?.get()?.also { toolbar ->
                toolbar.getAnchorPos()?.also { toolbar.smoothScrollToPosition(it) }
            }
        }
    }

    fun attach(toolbar: NavigationToolBarLayout) {
        mToolBarRef = WeakReference(toolbar)
        toolbar.addScrollStateListener(mScrollStateListener)
    }

    fun detach(toolbar: NavigationToolBarLayout) {
        mToolBarRef = null
        toolbar.removeScrollStateListener(mScrollStateListener)
    }

}