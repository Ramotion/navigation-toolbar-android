package com.ramotion.navigationtoolbar

import android.content.Context
import android.support.annotation.AttrRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.LayoutInflater


class NavigationToolBarLayout : CoordinatorLayout {

    val mToolBar: Toolbar // TODO: remove

    private val mHeaderLayout: HeaderLayout
    private val mHeaderLayoutManager: HeaderLayoutManager
    private val mAppBarLayout: AppBarLayout

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.navigation_layout, this, true)

        mToolBar = findViewById(R.id.com_ramotion_toolbar)
        mHeaderLayout = findViewById(R.id.com_ramotion_header_layout)

        mHeaderLayoutManager = (mHeaderLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior as HeaderLayoutManager
        mHeaderLayoutManager.mItemsTransformer = HeaderTransformer()

        mAppBarLayout = findViewById(R.id.com_ramotion_app_bar)
        mAppBarLayout.addOnOffsetChangedListener(mHeaderLayoutManager)
        (mAppBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior = mHeaderLayoutManager.mAppBarBehavior
    }

    fun setAdapter(adapter: HeaderLayout.Adapter<out HeaderLayout.ViewHolder>) {
        mHeaderLayout.setAdapter(adapter)
    }

    fun setCurrentPosition(pos: Int) {
        mHeaderLayoutManager.scrollToPosition(pos)
    }

    // TODO: set header items transformer

}