package com.ramotion.navigationtoolbar

import android.content.Context
import android.support.annotation.AttrRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.LayoutInflater
import com.ramotion.navigationtoolbar.HeaderLayoutManager.*
import kotlin.math.min


class NavigationToolBarLayout : CoordinatorLayout {

    private companion object {
        const val maxBackgroundRatio = 0.8f
    }

    abstract class ItemTransformer : HeaderChangeListener, HeaderUpdateListener {
        abstract fun attach(ntl: NavigationToolBarLayout)

        abstract fun detach()

        abstract fun transform(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int)

        final override fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) =
                transform(lm, header, headerBottom)

        final override fun onHeaderUpdated(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) =
                transform(lm, header, headerBottom)
    }

    val toolBar: Toolbar
    val headerLayout: HeaderLayout
    val layoutManager: HeaderLayoutManager
    val appBarLayout: AppBarLayout

    private var itemTransformer: ItemTransformer? = null

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.navigation_layout, this, true)

        toolBar = findViewById(R.id.com_ramotion_toolbar)
        headerLayout = findViewById(R.id.com_ramotion_header_layout)
        layoutManager = HeaderLayoutManager(context, attrs)
        (headerLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior = layoutManager

        appBarLayout = findViewById(R.id.com_ramotion_app_bar)
        appBarLayout.outlineProvider = null
        appBarLayout.addOnOffsetChangedListener(layoutManager)
        (appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior = layoutManager.appBarBehavior

        attrs?.also {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.NavigationToolBarr, defStyleAttr, 0)
            try {
                if (a.hasValue(R.styleable.NavigationToolBarr_headerBackgroundLayout)) {
                    val backgroundId = a.getResourceId(R.styleable.NavigationToolBarr_headerBackgroundLayout, -1)
                    initBackgroundLayout(context, backgroundId)
                }
            } finally {
                a.recycle()
            }
        }

        setItemTransformer(null)
    }

    fun setAdapter(adapter: HeaderLayout.Adapter<out HeaderLayout.ViewHolder>) = headerLayout.setAdapter(adapter)

    fun scrollToPosition(pos: Int) = layoutManager.scrollToPosition(pos)

    fun smoothScrollToPosition(pos: Int) = layoutManager.smoothScrollToPosition(pos)

    fun getAnchorPos(): Int = layoutManager.getAnchorPos(headerLayout)

    fun addItemChangeListener(listener: ItemChangeListener) {
        layoutManager.itemChangeListeners += listener
    }

    fun removeItemChangeListener(listener: ItemChangeListener) {
        layoutManager.itemChangeListeners -= listener
    }

    fun addScrollStateListener(listener: ScrollStateListener) {
        layoutManager.scrollStateListeners += listener
    }

    fun removeScrollStateListener(listener: ScrollStateListener) {
        layoutManager.scrollStateListeners -= listener
    }

    fun addItemClickListener(listener: HeaderLayoutManager.ItemClickListener) {
        layoutManager.itemClickListeners += listener
    }

    fun removeItemClickListener(listener: HeaderLayoutManager.ItemClickListener) {
        layoutManager.itemClickListeners += listener
    }

    fun addHeaderChangeListener(listener: HeaderChangeListener) {
        layoutManager.changeListener += listener
    }

    fun removeHeaderChangeListener(listener: HeaderChangeListener) {
        layoutManager.changeListener -= listener
    }

    fun addItemDecoration(decoration: ItemDecoration) {
        layoutManager.addItemDecoration(decoration)
    }

    fun removeItemDecoration(decoration: ItemDecoration) {
        layoutManager.removeItemDecoration(decoration)
    }

    fun setItemTransformer(newTransformer: ItemTransformer?) {
        itemTransformer?.also {
            layoutManager.changeListener -= it
            layoutManager.updateListener -= it
            it.detach()
        }

        (newTransformer ?: DefaultItemTransformer()).also {
            it.attach(this)
            layoutManager.changeListener += it
            layoutManager.updateListener += it
            itemTransformer = it
        }
    }

    private fun initBackgroundLayout(context: Context, layoutId: Int) {
        val ctl = findViewById<CollapsingToolbarLayout>(R.id.com_ramotion_toolbar_layout)
        val background = LayoutInflater.from(context).inflate(layoutId, ctl, true)
        addHeaderChangeListener(object : HeaderChangeListener {
            override fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
                val ratio = 1f - headerBottom / (headerLayout.height + 1f)
                val alpha = (maxBackgroundRatio - min(maxBackgroundRatio, ratio)) / maxBackgroundRatio
                background.alpha = alpha
            }
        })
    }
}