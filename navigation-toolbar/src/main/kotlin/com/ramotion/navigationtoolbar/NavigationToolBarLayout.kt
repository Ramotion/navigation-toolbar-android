package com.ramotion.navigationtoolbar

import android.content.Context
import android.support.annotation.AttrRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.LayoutInflater


class NavigationToolBarLayout : CoordinatorLayout {

    val mToolBar: Toolbar
    val mHeaderLayout: HeaderLayout
    val mHeaderLayoutManager: HeaderLayoutManager
    val mAppBarLayout: AppBarLayout

    private val mItemChangeListeners = mutableListOf<ItemChangeListener>()
    private val mScrollStateListeners = mutableListOf<ScrollStateListener>()

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.navigation_layout, this, true)

        mToolBar = findViewById(R.id.com_ramotion_toolbar)
        mHeaderLayout = findViewById(R.id.com_ramotion_header_layout)

        mHeaderLayoutManager = (mHeaderLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior as HeaderLayoutManager
        mHeaderLayoutManager.mItemsTransformer = HeaderTransformer()
        mHeaderLayoutManager.mItemChangeListener = { pos -> mItemChangeListeners.forEach { it.invoke(pos) }}
        mHeaderLayoutManager.mScrollStateListener = { state -> mScrollStateListeners.forEach { it.invoke(state) }}

        mAppBarLayout = findViewById(R.id.com_ramotion_app_bar)
        mAppBarLayout.outlineProvider = null
        mAppBarLayout.addOnOffsetChangedListener(mHeaderLayoutManager)
        (mAppBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior = mHeaderLayoutManager.mAppBarBehavior

        setItemTransformer(null)
    }

    fun setAdapter(adapter: HeaderLayout.Adapter<out HeaderLayout.ViewHolder>) = mHeaderLayout.setAdapter(adapter)

    fun scrollToPosition(pos: Int) = mHeaderLayoutManager.scrollToPosition(pos)

    fun smoothScrollToPosition(pos: Int) = mHeaderLayoutManager.smoothScrollToPosition(pos)

    fun getAnchorPos(): Int? = mHeaderLayoutManager.getAnchorPos()

    fun addItemClickListener(listener: HeaderLayoutManager.ItemClickListener) {
        mHeaderLayoutManager.mItemClickListeners += listener
    }

    fun removeItemClickListener(listener: HeaderLayoutManager.ItemClickListener) {
        mHeaderLayoutManager.mItemClickListeners -= listener
    }

    fun addItemChangeListener(listener: ItemChangeListener) {
        mItemChangeListeners += listener
    }

    fun removeItemChangeListener(listener: ItemChangeListener) {
        mItemChangeListeners -= listener
    }

    fun addScrollStateListener(listener: ScrollStateListener) {
        mScrollStateListeners += listener
    }

    fun removeScrollStateListener(listener: ScrollStateListener) {
        mScrollStateListeners -= listener
    }

    fun setItemTransformer(transformer: HeaderLayoutManager.ItemTransformer?) {
        mHeaderLayoutManager.mItemTransformer = transformer ?: DefaultHeaderTransformer(mHeaderLayoutManager)
    }

}