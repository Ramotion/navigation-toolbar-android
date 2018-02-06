package com.ramotion.navigationtoolbar

import android.content.Context
import android.graphics.Rect
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.OverScroller

/**
 * Header views container and producer with cache (recycler).
 */
class HeaderLayout : FrameLayout {

    companion object {
        const val INVALID_POSITION = -1

        fun getChildViewHolder(child: View): ViewHolder? = (child.layoutParams as LayoutParams).mViewHolder
    }

    internal interface ScrollListener {
        fun onItemClick(header: HeaderLayout, viewHolder: ViewHolder): Boolean
        fun onHeaderDown(header: HeaderLayout): Boolean
        fun onHeaderHorizontalScroll(header: HeaderLayout, distance: Float): Boolean
        fun onHeaderVerticalScroll(header: HeaderLayout, distance: Float): Boolean
        fun onHeaderHorizontalFling(header: HeaderLayout, velocity: Float): Boolean
        fun onHeaderVerticalFling(header: HeaderLayout, velocity: Float): Boolean
        fun computeScroll(header: HeaderLayout)
    }

    private val mTouchGestureDetector: GestureDetectorCompat

    internal val mScroller: OverScroller
    internal val mRecycler = Recycler()

    internal var mIsHorizontalScrollEnabled = false
    internal var mIsVerticalScrollEnabled = false

    internal var mScrollListener: ScrollListener? = null

    var mAdapter: Adapter<ViewHolder>? = null
        private set

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        mTouchGestureDetector = GestureDetectorCompat(context, TouchGestureListener())
        mScroller = OverScroller(context)
    }

    // Do nothing here. Layout children in HeaderLayoutManager
    //override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {}

    // TODO: performClick stuff
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mTouchGestureDetector.onTouchEvent(event)
    }

    override fun computeScroll() {
        super.computeScroll()
        mScrollListener?.computeScroll(this)
    }

    fun getAdapterPosition(view: View) = (view.layoutParams as LayoutParams).getViewAdapterPosition()

    internal fun detachView(child: View) = detachViewFromParent(child)

    internal fun attachView(child: View) = attachViewToParent(child, -1, child.layoutParams)

    internal fun setAdapter(adapter: Adapter<out ViewHolder>) {
        mAdapter = adapter as Adapter<ViewHolder> // TODO: fix?
    }

    private inner class TouchGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val listener = mScrollListener ?: return false

            val rect = Rect()
            val location = IntArray(2)

            for (i in 0 until childCount) {
                val child = getChildAt(i)
                child.getDrawingRect(rect)
                child.getLocationOnScreen(location)
                rect.offset(location[0], location[1])
                val contains = rect.contains(e.rawX.toInt(), e.rawY.toInt())
                if (contains) {
                    return listener.onItemClick(this@HeaderLayout, getChildViewHolder(child)!!)
                }
            }
            return false
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return mScrollListener?.onHeaderDown(this@HeaderLayout) ?: false
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            return mScrollListener?.run {
                if (mIsHorizontalScrollEnabled) {
                    onHeaderHorizontalScroll(this@HeaderLayout, distanceX)
                } else if (mIsVerticalScrollEnabled) {
                    onHeaderVerticalScroll(this@HeaderLayout, distanceY)
                } else {
                    false
                }
            } ?: false
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            return mScrollListener?.run {
                if (mIsHorizontalScrollEnabled) {
                    onHeaderHorizontalFling(this@HeaderLayout, velocityX)
                } else if (mIsVerticalScrollEnabled) {
                    onHeaderVerticalFling(this@HeaderLayout, velocityY)
                } else {
                    false
                }
            } ?: false
        }
    }

    open class ViewHolder(val view: View) {

        var mPosition: Int = INVALID_POSITION
            internal set

    }

    open class LayoutParams : FrameLayout.LayoutParams {

        internal var mViewHolder: ViewHolder? = null

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: ViewGroup.MarginLayoutParams) : super(source)

        constructor(source: ViewGroup.LayoutParams): super(source)

        constructor(source: LayoutParams): super(source as ViewGroup.LayoutParams)

        fun getViewAdapterPosition() = mViewHolder?.mPosition ?: INVALID_POSITION;

    }

    // TODO: use ViewHolder pattern
    abstract class Adapter<VH : ViewHolder> {

        abstract fun getItemCount(): Int

        abstract fun onCreateViewHolder(parent: ViewGroup): VH

        abstract fun onBindViewHolder(holder: VH, position: Int)

        fun createViewHolder(parent: ViewGroup): VH = onCreateViewHolder(parent)

        fun bindViewHolder(holder: VH, position: Int) {
            holder.mPosition = position
            onBindViewHolder(holder, position)

            val lp = holder.view.layoutParams
            val hlp = when (lp) {
                null -> LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                !is LayoutParams -> LayoutParams(lp)
                else -> lp
            }

            hlp.mViewHolder = holder
            holder.view.layoutParams = hlp
        }

        fun onViewRecycled(holder: VH) {}

    }

    // TODO: use ViewHolder pattern
    internal inner class Recycler {

        fun getViewForPosition(position: Int): View {
            // TODO: try get from cache
            val holder = mAdapter!!.createViewHolder(this@HeaderLayout)
            mAdapter!!.bindViewHolder(holder, position)
            return holder.view
        }

        /* TODO: use with cache
        fun bindViewToPosition(view: View, position: Int) {
            mAdapter!!.bindViewHolder(getChildViewHolder(view)!!, position)
        }
        */

        fun recycleView(view: View) {
            // TODO: cache
            mAdapter?.onViewRecycled(getChildViewHolder(view)!!)
            this@HeaderLayout.removeView(view)
        }
    }

}