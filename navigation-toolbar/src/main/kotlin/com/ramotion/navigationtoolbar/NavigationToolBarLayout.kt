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

/**
 * The main class that combines two other main classes: HeaderLayoutManager and HeaderLayout.
 * @see HeaderLayoutManager
 * @see HeaderLayout
 */
class NavigationToolBarLayout : CoordinatorLayout {

    private companion object {
        const val HEADER_HIDE_START = 0.5f
    }

    /**
     * ItemTransformer abstract class can be used as parent for class that will be responsible
     * for transformation of HeaderLayout child, during HeaderLayout change (expand / collapse)
     * or update (redraw / fill).
     * @see DefaultItemTransformer
     * @see setItemTransformer
     */
    abstract class ItemTransformer : HeaderChangeListener, HeaderUpdateListener {
        protected var navigationToolBarLayout: NavigationToolBarLayout? = null
            private set

        /**
         * Called on HeaderLayout change (expand / collapse) or update (redraw / fill).
         * @param lm HeaderLayoutManager
         * @param header HeaderLayout
         * @param headerBottom HeaderLayout bottom position.
         */
        abstract fun transform(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int)

        /**
         * Called on attach ot NavigationToolBarLayout.
         * @see setItemTransformer
         */
        abstract fun onAttach(ntl: NavigationToolBarLayout)

        /**
         * Called on detach from NavigationToolBarLayout.
         * @see setItemTransformer
         */
        abstract fun onDetach()

        internal fun attach(ntl: NavigationToolBarLayout) {
            navigationToolBarLayout = ntl
            ntl.addHeaderChangeListener(this)
            ntl.addHeaderUpdateListener(this)
            onAttach(ntl)
        }

        internal fun detach() {
            onDetach()
            navigationToolBarLayout?.also {
                it.removeHeaderChangeListener(this)
                it.removeHeaderUpdateListener(this)
            }
            navigationToolBarLayout = null
        }

        final override fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) =
                transform(lm, header, headerBottom)

        final override fun onHeaderUpdated(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) =
                transform(lm, header, headerBottom)
    }

    /**
     * Toolbar layout with id `@id/com_ramotion_toolbar`.
     */
    val toolBar: Toolbar
    /**
     * AppBarLayout with id `@id/com_ramotion_app_bar`.
     */
    val appBarLayout: AppBarLayout
    /**
     * HeaderLayout.
     * @see HeaderLayout
     */
    val headerLayout: HeaderLayout
    /**
     * HeaderLayoutManager.
     * @see HeaderLayoutManager
     */
    val layoutManager: HeaderLayoutManager

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

    /**
     * Sets HeaderLayout Adapter.
     * @see HeaderLayout
     * @see HeaderLayout.Adapter
     */
    fun setAdapter(adapter: HeaderLayout.Adapter<out HeaderLayout.ViewHolder>) = headerLayout.setAdapter(adapter)

    /**
     * Scroll header to specified position.
     * @param pos Position where to scroll
     * @see HeaderLayoutManager.scrollToPosition
     */
    fun scrollToPosition(pos: Int) = layoutManager.scrollToPosition(pos)

    /**
     * Smooth scroll header to specified position.
     * @param pos Position where to scroll.
     * @see HeaderLayoutManager.smoothScrollToPosition
     */
    fun smoothScrollToPosition(pos: Int) = layoutManager.smoothScrollToPosition(pos)

    /**
     * Returns current center card, adapter position.
     * @return Current center card, adapter position or HeaderLayout.INVALID_POSITION if card not found.
     * @see HeaderLayout.INVALID_POSITION
     * @see HeaderLayoutManager.getAnchorPos
     */
    fun getAnchorPos(): Int = layoutManager.getAnchorPos(headerLayout)

    /**
     * Adds ItemChangeListener.
     * @param listener ItemChangeListener to add.
     * @see ItemChangeListener
     */
    fun addItemChangeListener(listener: ItemChangeListener) {
        layoutManager.itemChangeListeners += listener
    }

    /**
     * Removes ItemChangeListener.
     * @param listener ItemChangeListener to remove.
     * @see ItemChangeListener
     */
    fun removeItemChangeListener(listener: ItemChangeListener) {
        layoutManager.itemChangeListeners -= listener
    }

    /**
     * Adds ScrollStateListener.
     * @param listener ScrollStateListener to add.
     * @see ScrollStateListener
     */
    fun addScrollStateListener(listener: ScrollStateListener) {
        layoutManager.scrollStateListeners += listener
    }

    /**
     * Removes ScrollStateListener.
     * @param listener ScrollStateListener to remove.
     * @see ScrollStateListener
     */
    fun removeScrollStateListener(listener: ScrollStateListener) {
        layoutManager.scrollStateListeners -= listener
    }

    /**
     * Adds ItemClickListener.
     * @param listener ItemClickListener to add.
     * @see ItemChangeListener
     */
    fun addItemClickListener(listener: ItemClickListener) {
        layoutManager.itemClickListeners += listener
    }

    /**
     * Removes ItemClickListener.
     * @param listener ItemClickListener to remove.
     * @see ItemClickListener
     */
    fun removeItemClickListener(listener: ItemClickListener) {
        layoutManager.itemClickListeners -= listener
    }

    /**
     * Adds HeaderChangeListener
     * @param listener HeaderChangeListener to add.
     * @see HeaderChangeListener
     */
    fun addHeaderChangeListener(listener: HeaderChangeListener) {
        layoutManager.changeListener += listener
    }

    /**
     * Removes HeaderChangeListener
     * @param listener HeaderChangeListener to remove.
     * @see HeaderChangeListener
     */
    fun removeHeaderChangeListener(listener: HeaderChangeListener) {
        layoutManager.changeListener -= listener
    }

    /**
     * Adds HeaderUpdateListener.
     * @param listener HeaderUpdateListener to add.
     * @see HeaderUpdateListener
     */
    fun addHeaderUpdateListener(listener: HeaderUpdateListener) {
        layoutManager.updateListener += listener
    }

    /**
     * Removes HeaderUpdateListener.
     * @param listener HeaderUpdateListener to remove.
     * @see HeaderUpdateListener
     */
    fun removeHeaderUpdateListener(listener: HeaderUpdateListener) {
        layoutManager.updateListener -= listener
    }

    /**
     * Adds ItemDecoration.
     * @param decoration ItemDecoration to add.
     * @see ItemDecoration
     */
    fun addItemDecoration(decoration: ItemDecoration) {
        layoutManager.addItemDecoration(decoration)
    }

    /**
     * Removes ItemDecoration.
     * @param decoration ItemDecoration to remove.
     * @see ItemDecoration
     */
    fun removeItemDecoration(decoration: ItemDecoration) {
        layoutManager.removeItemDecoration(decoration)
    }

    /**
     * Adds HeaderChangeStateListener.
     * @param listener HeaderChangeStateListener to add.
     * @see HeaderChangeStateListener
     */
    fun addHeaderChangeStateListener(listener: HeaderChangeStateListener) {
        layoutManager.changeListener += listener
    }

    /**
     * Removes HeaderChangeStateListener.
     * @param listener HeaderChangeStateListener to remove.
     * @see HeaderChangeStateListener
     */
    fun removeHeaderChangeStateListener(listener: HeaderChangeStateListener) {
        layoutManager.changeListener -= listener
    }

    /**
     * Sets ItemTransformer.
     * @param newTransformer New transformer. Can be null. If null, then DefaultItemTransformer will be used.
     * @see ItemTransformer
     */
    fun setItemTransformer(newTransformer: ItemTransformer?) {
        itemTransformer?.also { it.detach() }

        (newTransformer ?: DefaultItemTransformer()).also {
            it.attach(this)
            itemTransformer = it
        }
    }

    fun collapse() {
        layoutManager.getAnchorView(headerLayout)
                ?.let { HeaderLayout.getChildViewHolder(it) }
                ?.also { layoutManager.onHeaderItemClick(headerLayout, it) }
    }

    fun expand(animate: Boolean) = appBarLayout.setExpanded(true, animate)

    private fun initBackgroundLayout(context: Context, layoutId: Int) {
        val ctl = findViewById<CollapsingToolbarLayout>(R.id.com_ramotion_toolbar_layout)
        val background = LayoutInflater.from(context).inflate(layoutId, ctl, true)
        addHeaderChangeListener(object : HeaderChangeListener {
            override fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
                val ratio = 1f - headerBottom / (headerLayout.height + 1f)
                val headerAlpha = if (ratio >= HEADER_HIDE_START) 0f else 1f
                background.alpha = headerAlpha
            }
        })
    }
}