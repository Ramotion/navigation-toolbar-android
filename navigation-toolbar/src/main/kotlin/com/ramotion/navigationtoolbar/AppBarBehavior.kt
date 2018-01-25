package com.ramotion.navigationtoolbar

import android.support.design.widget.AppBarLayout

class AppBarBehavior() : AppBarLayout.Behavior() {

    internal var mCanDrag = true

    init {
        setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout) = mCanDrag
        })
    }

}