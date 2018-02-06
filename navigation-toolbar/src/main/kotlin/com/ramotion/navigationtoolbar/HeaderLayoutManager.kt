package com.ramotion.navigationtoolbar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PointF
import android.os.Looper
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import kotlin.math.max

/**
 * Moves header's views
 */
class HeaderLayoutManager(private val context: Context, attrs: AttributeSet?)
    : CoordinatorLayout.Behavior<HeaderLayout>(context, attrs), AppBarLayout.OnOffsetChangedListener, HeaderLayout.ScrollListener {

    interface ItemsTransformer {
        fun transform(header: HeaderLayout, layoutManager: HeaderLayoutManager, ratio: Float)
    }

    abstract class DefaultItemsTransformer: ItemsTransformer {
        private var mOffsetChangeStarted = false

        var mStartOrientation = Orientation.TRANSITIONAL
            private set

        override fun transform(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float) {
            val orientRatio = max(0f, (ratio - 0.5f) / 0.5f)
            val isAtBorder = orientRatio == 0f || orientRatio == 1f

            if (!isAtBorder && mStartOrientation == Orientation.TRANSITIONAL) {
                onOffsetChangeStarted(header, lm, ratio, orientRatio)
                onOffsetChanged(header, lm, ratio, orientRatio)
            } else  if (isAtBorder && mStartOrientation != Orientation.TRANSITIONAL) {
                onOffsetChanged(header, lm, ratio, orientRatio)
                onOffsetChangeStopped(header, lm, ratio, orientRatio)
            } else if (mOffsetChangeStarted) {
                onOffsetChanged(header, lm, ratio, orientRatio)
            }
        }

        open fun onOffsetChangeStarted(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {
            mOffsetChangeStarted = true
            mStartOrientation = if (orientRatio >= 0.5) Orientation.VERTICAL else Orientation.HORIZONTAL
        }

        open fun onOffsetChangeStopped(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {
            mOffsetChangeStarted = false
            mStartOrientation = Orientation.TRANSITIONAL
        }

        open fun onOffsetChanged(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {}
    }

    enum class Orientation {
        HORIZONTAL, VERTICAL, TRANSITIONAL
    }

    internal companion object {
        const val TAB_ON_SCREEN_COUNT = 5
        const val TAB_OFF_SCREEN_COUNT = 1
        const val VERTICAL_TAB_HEIGHT_RATIO = 1f / TAB_ON_SCREEN_COUNT
        const val VERTICAL_TAB_WIDTH_RATIO = 4f / 5f
        const val SCROLL_STOP_CHECK_DELAY = 300L
        const val SCROLL_UP_ANIMATION_DURATION = 1000L
    }

    private val mScreenWidth = context.resources.displayMetrics.widthPixels
    private val mScreenHeight = context.resources.displayMetrics.heightPixels
    private val mScreenHalf = mScreenHeight / 2f

    // TODO: init in constructor from attr
    private val mTabOffsetCount = TAB_OFF_SCREEN_COUNT
    private val mTabOnScreenCount = TAB_ON_SCREEN_COUNT
    private val mTabCount = mTabOnScreenCount + mTabOffsetCount * 2

    // TODO: add getters
    val mHorizontalTabWidth = mScreenWidth
    val mHorizontalTabHeight = mScreenHalf.toInt()
    val mVerticalTabHeight = (mScreenHeight * VERTICAL_TAB_HEIGHT_RATIO).toInt()
    val mVerticalTabWidth = (mScreenWidth * VERTICAL_TAB_WIDTH_RATIO).toInt()

    private val mViewCache = SparseArray<View?>()
    private val mOffsetAnimator = ValueAnimator() // TODO: add duration attribute
    private val mCenterIndex = mTabOnScreenCount % 2 + mTabOffsetCount

    internal val mAppBarBehavior = AppBarBehavior()

    private lateinit var mAppBar: AppBarLayout
    private lateinit var mHeaderLayout: HeaderLayout
    private lateinit var mHPoint: PointF // TODO: replace with data class
    private lateinit var mVPoint: PointF // TODO: replace with data class

    private var mInitialized = false
    private var mCanDrag = true
    private var mOffsetChanged = false
    private var mIsCheckingScrollStop =false

    private var mScrollToPosition = HeaderLayout.INVALID_POSITION
    private var mClickedChildIndex = HeaderLayout.INVALID_POSITION // TODO: set on click

    internal var mItemsTransformer: ItemsTransformer? = null

    init {
        Looper.myQueue().addIdleHandler {
            if (mOffsetChanged && !mIsCheckingScrollStop) {
                checkIfOffsetChangingStopped()
            }
            true
        }
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: HeaderLayout, dependency: View): Boolean {
        return dependency is AppBarLayout
    }

    override fun onLayoutChild(parent: CoordinatorLayout, header: HeaderLayout, layoutDirection: Int): Boolean {
        if (!parent.isLaidOut) {
            parent.onLayoutChild(header, layoutDirection)

            mAppBar = parent.findViewById(R.id.com_ramotion_app_bar)
            mHeaderLayout = header
            mHeaderLayout.mScrollListener = this

            initPoints(header)
            fill(header)

            mInitialized = true
        }

        return true
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, header: HeaderLayout, dependency: View): Boolean {
        // Offset header on collapsing
        header.y = (dependency.bottom - header.height).toFloat()

        // Transform header items
        mItemsTransformer?.transform(header, this, getPositionRatio())

        return true
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        mOffsetChanged = true
    }

    override fun onItemClick(header: HeaderLayout, viewHolder: HeaderLayout.ViewHolder): Boolean {
        mClickedChildIndex = header.indexOfChild(viewHolder.view)
        smoothOffset(mScreenHalf.toInt())
        return true
    }

    override fun onHeaderDown(header: HeaderLayout): Boolean {
        if (header.childCount == 0) {
            return false
        }

        header.mScroller.forceFinished(true);
        ViewCompat.postInvalidateOnAnimation(header);
        return true
    }

    override fun onHeaderHorizontalScroll(header: HeaderLayout, distance: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        mScrollToPosition = HeaderLayout.INVALID_POSITION

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onHeaderVerticalScroll(header: HeaderLayout, distance: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        mScrollToPosition = HeaderLayout.INVALID_POSITION

        val scrollUp = distance >= 0
        val offset = if (scrollUp) {
            val lastBottom = header.getChildAt(childCount - 1).bottom
            val newBottom = lastBottom - distance
            if (newBottom > header.height) distance.toInt() else lastBottom - header.height
        } else {
            val firstTop = header.getChildAt(0).top
            if (firstTop > 0) { // TODO: firstTop > border, border - center or systemBar height
                0
            } else {
                val newTop = firstTop - distance
                if (newTop < 0) distance.toInt() else firstTop
            }
        }

        for (i in 0 until childCount) {
            header.getChildAt(i).offsetTopAndBottom(-offset)
        }

        fill(header)
        return true
    }

    override fun onHeaderHorizontalFling(header: HeaderLayout, velocity: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onHeaderVerticalFling(header: HeaderLayout, velocity: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        val itemCount = header.mAdapter?.getItemCount() ?: return false
        val first = header.getChildAt(0)
        val firstPos = HeaderLayout.getChildViewHolder(first)!!.mPosition
        val start = first.top - firstPos * mVerticalTabHeight
        val min = -itemCount * mVerticalTabHeight + header.height
        val max = 0

        header.mScroller.apply {
            forceFinished(true)
            fling(0, start, 0, velocity.toInt(), 0, 0, min, max)
        }
        ViewCompat.postInvalidateOnAnimation(header)

        return true
    }

    override fun computeScroll(header: HeaderLayout) {
        val x = header.mScroller.currX
        val y = header.mScroller.currY

        if (!header.mScroller.computeScrollOffset()) {
            return
        }

        for (i in 0 until header.childCount) {
            val diffX = header.mScroller.currX - x
            val diffY = header.mScroller.currY - y
            val child = header.getChildAt(i)
            child.offsetLeftAndRight(diffX)
            child.offsetTopAndBottom(diffY)
        }

        fill(header)

        ViewCompat.postInvalidateOnAnimation(header)
    }

    fun scrollToPosition(pos: Int) {
        if (!mInitialized) {
            return
        }

        if (pos < 0 || mHeaderLayout.mAdapter?.run { pos >= getItemCount() } == true) {
            mScrollToPosition = pos
            fill(mHeaderLayout)
        }
    }

    fun getCenterIndex(): Int = mCenterIndex

    fun getPoints(): Pair<PointF, PointF> = Pair(PointF(mHPoint.x, mHPoint.y), PointF(mVPoint.x, mVPoint.y))

    fun getHorizontalAnchorView(header: HeaderLayout): View? {
        val centerLeft = mHPoint.x

        var result: View? = null
        var lastDiff = Int.MAX_VALUE

        for (i in 0 until header.childCount) {
            val child = header.getChildAt(i)
            val diff = Math.abs(child.left - centerLeft).toInt()
            if (diff < lastDiff) {
                lastDiff = diff
                result = child
            }
        }

        return result
    }

    fun getVerticalAnchorView(header: HeaderLayout): View? {
        val centerTop = mVPoint.y

        var result: View? = null
        var lastDiff = Int.MAX_VALUE

        for (i in 0 until header.childCount) {
            val child = header.getChildAt(i)
            val diff = Math.abs(child.top - centerTop).toInt()
            if (diff < lastDiff) {
                lastDiff = diff
                result = child
            }
        }

        return result
    }

    private fun getHorizontalAnchorPos(header: HeaderLayout): Int {
        return if (mScrollToPosition != HeaderLayout.INVALID_POSITION) {
            mScrollToPosition
        } else {
            getHorizontalAnchorView(header)?.let { header.getAdapterPosition(it) } ?: 0
        }
    }

    private fun getVerticalAnchorPos(header: HeaderLayout): Int {
        return if (mScrollToPosition != HeaderLayout.INVALID_POSITION) {
            mScrollToPosition
        } else {
            getVerticalAnchorView(header)?.let { header.getAdapterPosition(it) } ?: 0
        }
    }

    fun getClickedChildIndex() = mClickedChildIndex

    fun layoutChild(child: View, x: Int, y: Int, w: Int, h: Int) {
        val ws = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.AT_MOST)
        val hs = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST)
        child.measure(ws, hs)
        child.layout(x, y, x + w, y + h)
    }

    private fun initPoints(header: HeaderLayout) {
        val hx = 0f
        val hy = mScreenHalf
        val vx = (header.width - mVerticalTabWidth).toFloat()
        val vy = ((1f * mScreenHeight) / mTabOnScreenCount) * mCenterIndex

        mHPoint = PointF(hx, hy)
        mVPoint = PointF(vx, vy)
    }

    private fun getPositionRatio(): Float {
        return Math.max(0f, mAppBar.bottom / mScreenHeight.toFloat())
    }

    private fun getOrientation(ratio: Float): Orientation {
        return when {
            ratio <= 0.5f -> Orientation.HORIZONTAL
            ratio < 1 -> Orientation.TRANSITIONAL
            else -> Orientation.VERTICAL
        }
    }

    private fun fill(header: HeaderLayout) {
        // TODO: optimize
        val orientation = getOrientation(getPositionRatio())
        val pos = when (orientation) {
            Orientation.HORIZONTAL -> getHorizontalAnchorPos(header)
            Orientation.VERTICAL -> getVerticalAnchorPos(header)
            Orientation.TRANSITIONAL -> return
        }

        mViewCache.clear()

        for (i in 0 until header.childCount) {
            val view = header.getChildAt(i)
            mViewCache.put(header.getAdapterPosition(view), view)
        }

        for (i in 0 until mViewCache.size()) {
            header.detachView(mViewCache.valueAt(i)!!)
        }

        when (getOrientation(getPositionRatio())) {
            Orientation.HORIZONTAL -> {
                fillLeft(header, pos)
                fillRight(header, pos)
            }
            Orientation.VERTICAL -> {
                fillTop(header, pos)
                fillBottom(header, pos)
            }
        }

        for (i in 0 until mViewCache.size()) {
            header.mRecycler.recycleView(mViewCache.valueAt(i)!!)
        }
    }

    private fun fillLeft(header: HeaderLayout, anchorPos: Int) {
        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        val top = mHPoint.y.toInt()
        var left = mHPoint.x.toInt() -(mCenterIndex - 1) * mHorizontalTabWidth
        var pos = Math.max(0, anchorPos - mCenterIndex - 1)

        while (pos < anchorPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, mHorizontalTabWidth, mHorizontalTabHeight)
            left = view.right
            pos++
        }
    }

    private fun fillRight(header: HeaderLayout, anchorPos: Int) {
        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        val maxPos = Math.min(header.mAdapter?.getItemCount() ?: 0, anchorPos + (mTabCount - mCenterIndex))
        val top = mHPoint.y.toInt()
        var left = mHPoint.x.toInt()
        var pos = anchorPos

        while (pos <  maxPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, mHorizontalTabWidth, mHorizontalTabHeight)
            left = view.right
            pos++
        }
    }

    private fun fillTop(header: HeaderLayout, anchorPos: Int) {
        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        val topDiff = mVPoint.y.toInt() - (mViewCache.get(anchorPos)?.top ?: 0)
        val left = mVPoint.x.toInt()

        var pos = Math.max(0, anchorPos - mCenterIndex - mTabOffsetCount)
        var top = (mVPoint.y.toInt() -(anchorPos - pos) * mVerticalTabHeight) - topDiff

        while (pos < anchorPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, mVerticalTabWidth, mVerticalTabHeight)
            top = view.bottom
            pos++
        }
    }

    private fun fillBottom(header: HeaderLayout, anchorPos: Int) {
        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        val maxPos = Math.min(header.mAdapter?.run { getItemCount() } ?: 0, anchorPos + mCenterIndex + 1 + mTabOffsetCount)
        val left = mVPoint.x.toInt()
        var pos = anchorPos

        var top  = if (header.childCount > 0) {
            header.getChildAt(header.childCount - 1).bottom
        } else {
            mVPoint.y.toInt()
        }

        while (pos <  maxPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, mVerticalTabWidth, mVerticalTabHeight)
            top = view.bottom
            pos++
        }
    }

    private fun getPlacedChildForPosition(header: HeaderLayout, pos: Int, x: Int, y: Int, w: Int, h: Int): View {
        val cacheView = mViewCache.get(pos)
        if (cacheView != null) {
            header.attachView(cacheView)
            mViewCache.remove(pos)
            return cacheView
        }

        val view = header.mRecycler.getViewForPosition(pos)
        header.addView(view)
        layoutChild(view, x, y, w, h)
        return view
    }

    private fun checkIfOffsetChangingStopped() {
        mOffsetChanged = false
        mIsCheckingScrollStop = true

        val startOffset = mAppBarBehavior.topAndBottomOffset
        mHeaderLayout.postOnAnimationDelayed({
            mIsCheckingScrollStop = false
            val currentOffset = mAppBarBehavior.topAndBottomOffset
            val scrollStopped = currentOffset == startOffset
            if (scrollStopped) {
                onOffsetChangingStopped(currentOffset)
            }
        }, SCROLL_STOP_CHECK_DELAY)
    }

    private fun onOffsetChangingStopped(offset: Int) {
        var hScrollEnable = false
        var vScrollEnable = false
        if (offset == 0) {
            vScrollEnable = true
            mCanDrag = false
        } else if (offset == mScreenHalf.toInt()) {
            hScrollEnable = true
        } else {
            // TODO: check if near and offset (scroll) header if needed
        }

        mHeaderLayout.mIsHorizontalScrollEnabled = hScrollEnable
        mHeaderLayout.mIsVerticalScrollEnabled = vScrollEnable
    }

    private fun smoothOffset(offset: Int) {
        mOffsetAnimator.cancel()
        mOffsetAnimator.setDuration(SCROLL_UP_ANIMATION_DURATION)
        mOffsetAnimator.setIntValues(mAppBarBehavior.topAndBottomOffset, -offset)
        mOffsetAnimator.addUpdateListener {
            val value = it.animatedValue as Int
            mAppBarBehavior.topAndBottomOffset = value
        }
        mOffsetAnimator.start()
    }

    inner class AppBarBehavior : AppBarLayout.Behavior() {

        init {
            setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout) = mCanDrag
            })
        }

    }

}