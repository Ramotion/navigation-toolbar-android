package com.ramotion.navigationtoolbar

import android.content.Context
import android.graphics.Rect
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout

/**
 * Header views container and producer with cache (recycler).
 */
class HeaderLayout : FrameLayout {

    companion object {
        const val INVALID_POSITION = -1

        fun getChildLayoutParams(child: View) = child.layoutParams as? LayoutParams

        fun getChildViewHolder(child: View) = getChildLayoutParams(child)?.viewHolder

        fun getChildPosition(child: View) = getChildViewHolder(child)?.position ?: INVALID_POSITION
    }

    internal interface ScrollListener {
        fun onItemClick(header: HeaderLayout, viewHolder: ViewHolder): Boolean
        fun onHeaderDown(header: HeaderLayout): Boolean
        fun onHeaderUp(header: HeaderLayout): Unit
        fun onHeaderHorizontalScroll(header: HeaderLayout, distance: Float): Boolean
        fun onHeaderVerticalScroll(header: HeaderLayout, distance: Float): Boolean
        fun onHeaderHorizontalFling(header: HeaderLayout, velocity: Float): Boolean
        fun onHeaderVerticalFling(header: HeaderLayout, velocity: Float): Boolean
    }

    private val gestureDetector: GestureDetectorCompat

    internal val recycler = Recycler()

    internal var isHorizontalScrollEnabled = false
    internal var isVerticalScrollEnabled = false

    internal var scrollListener: ScrollListener? = null

    var adapter: Adapter<ViewHolder>? = null; private set

    private inner class TouchGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val listener = scrollListener ?: return false

            val rect = Rect()
            val location = IntArray(2)

            for (i in 0 until childCount) {
                val child = getChildAt(i)
                child.getDrawingRect(rect)
                child.getLocationOnScreen(location)
                rect.offset(location[0], location[1])
                val contains = rect.contains(e.rawX.toInt(), e.rawY.toInt())
                if (contains) {
                    return getChildViewHolder(child)
                            ?.let { listener.onItemClick(this@HeaderLayout, it) }
                            ?: throw RuntimeException("View holder not found")
                }
            }
            return false
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return scrollListener
                    ?.onHeaderDown(this@HeaderLayout)
                    ?: false
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            return scrollListener?.run {
                when {
                    isHorizontalScrollEnabled -> onHeaderHorizontalScroll(this@HeaderLayout, distanceX)
                    isVerticalScrollEnabled -> onHeaderVerticalScroll(this@HeaderLayout, distanceY)
                    else -> false
                }
            } ?: false
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            return scrollListener?.run {
                when {
                    isHorizontalScrollEnabled -> onHeaderHorizontalFling(this@HeaderLayout, velocityX)
                    isVerticalScrollEnabled -> onHeaderVerticalFling(this@HeaderLayout, velocityY)
                    else -> false
                }
            } ?: false
        }
    }

    open class ViewHolder(val view: View) {

        var position: Int = INVALID_POSITION
            internal set

    }

    open class LayoutParams : FrameLayout.LayoutParams {

        internal val decorRect = Rect()

        internal var decorRectValid = false
        internal var viewHolder: ViewHolder? = null

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: ViewGroup.MarginLayoutParams) : super(source)

        constructor(source: ViewGroup.LayoutParams) : super(source)

        constructor(source: LayoutParams) : super(source as ViewGroup.LayoutParams)
    }

    abstract class Adapter<VH : ViewHolder> {

        abstract fun getItemCount(): Int

        abstract fun onCreateViewHolder(parent: ViewGroup): VH

        abstract fun onBindViewHolder(holder: VH, position: Int)

        open fun onViewRecycled(holder: VH) {}

        fun createViewHolder(parent: ViewGroup): VH {
            val holder = onCreateViewHolder(parent)
            holder.view.outlineProvider = ViewOutlineProvider.BOUNDS
            return holder
        }

        fun bindViewHolder(holder: VH, position: Int) {
            holder.position = position
            onBindViewHolder(holder, position)

            val lp = holder.view.layoutParams
            val hlp = when (lp) {
                null -> LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                !is LayoutParams -> LayoutParams(lp)
                else -> lp
            }

            hlp.viewHolder = holder
            hlp.decorRectValid = false
            holder.view.layoutParams = hlp
        }

        fun recycleView(holder: VH) = onViewRecycled(holder)

    }

    internal inner class Recycler {

        private val viewCache = mutableListOf<View>()

        fun getViewForPosition(position: Int): View {
            val adapter = adapter ?: throw RuntimeException("No adapter set")
            val holder = viewCache.firstOrNull()
                    ?.let { viewCache.remove(it); getChildViewHolder(it) }
                    ?: adapter.createViewHolder(this@HeaderLayout)
            bindViewToPosition(holder, position)
            return holder.view
        }

        fun recycleView(view: View, cache: Boolean = true) {
            val adapter = adapter ?: throw RuntimeException("No adapter set")
            val lp = getChildLayoutParams(view) ?: throw RuntimeException("Invalid layout paramsr")
            val holder = lp.viewHolder ?: throw RuntimeException("No view holder")
            adapter.recycleView(holder)
            this@HeaderLayout.removeView(view)
            if (cache) {
                lp.decorRectValid = false
                holder.position = INVALID_POSITION
                viewCache.add(holder.view)
            }
        }

        internal fun markItemDecorInsetsDirty() {
            viewCache.forEach { view ->
                HeaderLayout.getChildLayoutParams(view)
                        ?.let { it.decorRectValid = false }
            }
        }

        private fun bindViewToPosition(holder: ViewHolder, position: Int) {
            val adapter = adapter ?: throw RuntimeException("No adapter set")
            adapter.bindViewHolder(holder, position)
        }

    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        gestureDetector = GestureDetectorCompat(context, TouchGestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val res = gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) {
            scrollListener?.onHeaderUp(this)
        }
        return res
    }

    override fun onDetachedFromWindow() {
        while (childCount > 0) {
            recycler.recycleView(getChildAt(0), false)
        }
        super.onDetachedFromWindow()
    }

    internal fun detachView(child: View) = detachViewFromParent(child)

    internal fun attachView(child: View) = attachViewToParent(child, -1, child.layoutParams)

    internal fun setAdapter(adapter: Adapter<out ViewHolder>) {
        this.adapter = adapter as Adapter<ViewHolder>
    }

}