package com.ramotion.navigationtoolbar

import android.content.Context
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Header views container and producer with cache (recycler).
 */
class HeaderLayout : FrameLayout {

    companion object {
        val INVALID_POSITION = -1

        fun getChildViewHolder(child: View): ViewHolder? {
            val lp = child.layoutParams
            return when (lp) {
                is LayoutParams -> lp.mViewHolder
                else -> null
            }
        }
    }

    private val mTouchGestureDetector: GestureDetectorCompat
    private val mInterceptGestureDetector: GestureDetectorCompat

    internal val mRecycler = Recycler()

    internal var mGestureListener: GestureDetector.SimpleOnGestureListener? = null

    var mAdapter: Adapter<ViewHolder>? = null
        private set

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        mInterceptGestureDetector = GestureDetectorCompat(context, InterceptGestureListener())
        mTouchGestureDetector = GestureDetectorCompat(context, TouchGestureListener())
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams) = LayoutParams(lp)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // Do nothing here. Layout children in HeaderLayoutManager
    }

    fun getAdapterPosition(view: View) = (view.layoutParams as LayoutParams).getViewAdapterPosition()

    internal fun detachView(child: View) = detachViewFromParent(child)

    internal fun attachView(child: View) = attachViewToParent(child, -1, child.layoutParams)

    internal fun setAdapter(adapter: Adapter<out ViewHolder>) {
        mAdapter = adapter as Adapter<ViewHolder> // TODO: fix?
    }

    private inner class InterceptGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean = true
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean = true
        override fun onDown(e: MotionEvent?): Boolean {
            mGestureListener?.onDown(e)
            return super.onDown(e)
        }
    }

    private inner class TouchGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            mGestureListener?.onScroll(e1, e2, distanceX, distanceY)
            return false
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            mGestureListener?.onFling(e1, e2, velocityX, velocityY)
            return false
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

        internal var mCurrentPosition: Int = INVALID_POSITION

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


        internal fun setCurrentPosition(pos: Int) {
            if (pos in 0 .. getItemCount()) {
                mCurrentPosition = pos
            }
        }

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