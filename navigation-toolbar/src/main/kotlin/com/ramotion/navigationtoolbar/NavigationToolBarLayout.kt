package com.ramotion.navigationtoolbar

import android.content.Context
import android.support.annotation.AttrRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewStub
import android.widget.ImageView
import com.ramotion.navigationtoolbar.HeaderLayoutManager.*
import kotlin.math.min


class NavigationToolBarLayout : CoordinatorLayout {

    abstract class ItemTransformer : HeaderChangeListener, HeaderUpdateListener {
        abstract fun attach(ntl: NavigationToolBarLayout)

        abstract fun detach()

        abstract fun transform(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int)

        final override fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) =
                transform(lm, header, headerBottom)

        final override fun onHeaderUpdated(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) =
                transform(lm, header, headerBottom)
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
        mHeaderLayoutManager = HeaderLayoutManager(context, attrs)
        (mHeaderLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior = mHeaderLayoutManager

        mAppBarLayout = findViewById(R.id.com_ramotion_app_bar)
        mAppBarLayout.outlineProvider = null
        mAppBarLayout.addOnOffsetChangedListener(mHeaderLayoutManager)
        (mAppBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior = mHeaderLayoutManager.mAppBarBehavior

        attrs?.also {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.NavigationToolBarr, defStyleAttr, 0)
            try {
                val imgResID = a.getResourceId(R.styleable.NavigationToolBarr_headerBackgroundSrc, -1)
                if (imgResID != -1) {
                    val imageView = findViewById<ViewStub>(R.id.com_ramotion_background_stub).inflate() as ImageView
                    imageView.setImageResource(imgResID)
                    addHeaderChangeListener(object : HeaderChangeListener {
                        val maxRatio = 0.8f
                        override fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
                            val ratio = 1f - headerBottom / (mHeaderLayout.height + 1f)
                            val alpha = (maxRatio - min(maxRatio, ratio)) / maxRatio
                            imageView.alpha = alpha
                        }
                    })
                }
            } finally {
                a.recycle()
            }
        }

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

    fun addHeaderChangeListener(listener: HeaderChangeListener) {
        mHeaderLayoutManager.mHeaderChangeListener += listener
    }

    fun removeHeaderChangeListener(listener: HeaderChangeListener) {
        mHeaderLayoutManager.mHeaderChangeListener -= listener
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