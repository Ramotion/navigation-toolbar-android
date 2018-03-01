package com.ramotion.navigationtoolbar

import android.content.Context
import android.support.annotation.AttrRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.LayoutInflater
import com.ramotion.navigationtoolbar.HeaderLayoutManager.HeaderChangeListener
import com.ramotion.navigationtoolbar.HeaderLayoutManager.HeaderUpdateListener
import com.ramotion.navigationtoolbar.HeaderLayoutManager.ItemChangeListener
import com.ramotion.navigationtoolbar.HeaderLayoutManager.ScrollStateListener


class NavigationToolBarLayout : CoordinatorLayout {

    abstract class ItemTransformer : HeaderChangeListener, HeaderUpdateListener {
        final override fun onHeaderChanged(headerBottom: Int) = transform(headerBottom)
        final override fun onHeaderUpdated(headerBottom: Int) = transform(headerBottom)
        abstract fun attach(ntl: NavigationToolBarLayout)
        abstract fun detach()
        abstract fun transform(headerBottom: Int)
    }

    val mToolBar: Toolbar
    val mHeaderLayout: HeaderLayout
    val mHeaderLayoutManager: HeaderLayoutManager
    val mAppBarLayout: AppBarLayout

    private var mHeaderItemTransformer: ItemTransformer? = null

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.navigation_layout, this, true)

        mToolBar = findViewById(R.id.com_ramotion_toolbar)
        mHeaderLayout = findViewById(R.id.com_ramotion_header_layout)

        mHeaderLayoutManager = (mHeaderLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior as HeaderLayoutManager

        mAppBarLayout = findViewById(R.id.com_ramotion_app_bar)
        mAppBarLayout.outlineProvider = null
        mAppBarLayout.addOnOffsetChangedListener(mHeaderLayoutManager)
        (mAppBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior = mHeaderLayoutManager.mAppBarBehavior

        setItemTransformer(null)
    }

    fun setAdapter(adapter: HeaderLayout.Adapter<out HeaderLayout.ViewHolder>) = mHeaderLayout.setAdapter(adapter)

    fun scrollToPosition(pos: Int) = mHeaderLayoutManager.scrollToPosition(pos)

    fun smoothScrollToPosition(pos: Int) = mHeaderLayoutManager.smoothScrollToPosition(pos)

    fun getAnchorPos(): Int? = mHeaderLayoutManager.getAnchorPos(mHeaderLayout)

    fun addItemChangeListener(listener: ItemChangeListener) {
        mHeaderLayoutManager.mItemChangeListeners += listener
    }

    fun removeItemChangeListener(listener: ItemChangeListener) {
        mHeaderLayoutManager.mItemChangeListeners -= listener
    }

    fun addScrollStateListener(listener: ScrollStateListener) {
        mHeaderLayoutManager.mScrollStateListeners += listener
    }

    fun removeScrollStateListener(listener: ScrollStateListener) {
        mHeaderLayoutManager.mScrollStateListeners -= listener
    }

    fun addItemClickListener(listener: HeaderLayoutManager.ItemClickListener) {
        mHeaderLayoutManager.mItemClickListeners += listener
    }

    fun removeItemClickListener(listener: HeaderLayoutManager.ItemClickListener) {
        mHeaderLayoutManager.mItemClickListeners += listener
    }

    fun setItemTransformer(newTransformer: ItemTransformer?) {
        mHeaderItemTransformer?.also {
            mHeaderLayoutManager.mHeaderChangeListener -= it
            mHeaderLayoutManager.mHeaderUpdateListener -= it
            it.detach()
        }

        (newTransformer ?: DefaultItemTransformer()).also {
            it.attach(this)
            mHeaderLayoutManager.mHeaderChangeListener += it
            mHeaderLayoutManager.mHeaderUpdateListener += it
            mHeaderItemTransformer = it
        }
    }

}