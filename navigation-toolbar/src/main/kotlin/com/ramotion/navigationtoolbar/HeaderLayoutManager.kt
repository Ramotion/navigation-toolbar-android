package com.ramotion.navigationtoolbar

import android.content.Context
import android.graphics.PointF
import android.os.Looper
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View

/**
 * Moves header's views
 */
class HeaderLayoutManager(private val context: Context, attrs: AttributeSet?)
    : CoordinatorLayout.Behavior<HeaderLayout>(context, attrs), AppBarLayout.OnOffsetChangedListener {

    private companion object {
        val TAB_ON_SCREEN_COUNT = 5
        val TAB_OFF_SCREEN_COUNT = 1
        val VERTICAL_TAB_HEIGHT_RATIO = 1f / TAB_ON_SCREEN_COUNT
        val VERTICAL_TAB_WIDTH_RATIO = 4f / 5f
        val SCROLL_STOP_CHECK_DELAY = 300L
    }

    private enum class Orientation {
        HORIZONTAL, VERTICAL
    }

    private val mScreenWidth = context.resources.displayMetrics.widthPixels
    private val mScreenHeight = context.resources.displayMetrics.heightPixels
    private val mScreenHalf = mScreenHeight / 2f

    // TODO: init in constructor from attr
    private val mTabOffsetCount = TAB_OFF_SCREEN_COUNT
    private val mTabOnScreenCount = TAB_ON_SCREEN_COUNT
    private val mTabCount = mTabOnScreenCount + mTabOffsetCount * 2

    private val mHorizontalTabWidth = mScreenWidth
    private val mHorizontalTabHeight = mScreenHalf.toInt()
    private val mVerticalTabHeight = (mScreenHeight * VERTICAL_TAB_HEIGHT_RATIO).toInt()
    private val mVerticalTabWidth = (mScreenWidth * VERTICAL_TAB_WIDTH_RATIO).toInt()
    private val mCenterIndex = mTabOnScreenCount % 2 + mTabOffsetCount

    private val mHPoints = mutableListOf<PointF>()
    private val mVPoints = mutableListOf<PointF>()
    private val mViewCache = SparseArray<View?>()

    internal val mAppBarBehavior = AppBarBehavior()

    private lateinit var mAppBar: AppBarLayout
    private lateinit var mHeaderLayout: HeaderLayout

    private var mCanDrag = true
    private var mOffsetChanged = false
    private var mIsCheckingScrollStop =false

    init {
        Looper.myQueue().addIdleHandler {
            if (mOffsetChanged && !mIsCheckingScrollStop) {
                checkIfScrollStopped()
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

            initPoints(header) // TODO: remove header parameter
            fill(header) // TODO: remove header parameter

            return true
        }

        return super.onLayoutChild(parent, header, layoutDirection)
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, header: HeaderLayout, dependency: View): Boolean {
        header.y = (dependency.bottom - header.height).toFloat()

        return true
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        mOffsetChanged = true
        val offset = verticalOffset
        Log.d("D", "onOffsetChanged| offset: $offset")
    }

    private fun initPoints(header: HeaderLayout) {
        val hx = 0f
        val hy = mScreenHalf
        val vx = (header.width - mVerticalTabWidth).toFloat()
        val vy = ((1f * mScreenHeight) / mTabOnScreenCount) * mCenterIndex

        for (i in 0 .. mTabCount) {
            val diff = i - mCenterIndex
            mHPoints += PointF(hx + diff * mHorizontalTabWidth, hy)
            mVPoints += PointF(vx, vy + diff * mVerticalTabHeight)
        }
    }

    private fun getPositionRatio(): Float {
        return Math.max(0f, mAppBar.bottom / mScreenHeight.toFloat())
    }

    private fun getOrientation(ratio: Float): Orientation {
        return if (ratio > 0.5f) Orientation.VERTICAL else Orientation.HORIZONTAL
    }

    private fun getAnchorPos(header: HeaderLayout, points: List<PointF>): Int {
        val currentPosition = header.mAdapter?.mCurrentPosition ?: HeaderLayout.INVALID_POSITION
        if (currentPosition != HeaderLayout.INVALID_POSITION) {
            return currentPosition
        } else {
            val centerLeft = points[mCenterIndex].x

            var result = HeaderLayout.INVALID_POSITION
            var lastDiff = Int.MAX_VALUE

            for (i in 0 until header.childCount) {
                val child = header.getChildAt(i)
                val diff = Math.abs(child.left - centerLeft).toInt()
                if (diff < lastDiff) {
                    lastDiff = diff
                    result = header.getAdapterPosition(child)
                }
            }

            return result
        }
    }

    private fun fill(header: HeaderLayout) {
        mViewCache.clear()

        for (i in 0 until header.childCount) {
            val view = header.getChildAt(i)
            val pos = header.getAdapterPosition(view)
            mViewCache.put(pos, view)
        }

        for (i in 0 until mViewCache.size()) {
            header.detachView(mViewCache.valueAt(i)!!)
        }

        when (getOrientation(getPositionRatio())) {
            Orientation.HORIZONTAL -> {
                val pos = getAnchorPos(header, mHPoints)
                fillLeft(header, pos)
                fillRight(header, pos)
            }
            Orientation.VERTICAL -> {
                val pos = getAnchorPos(header, mVPoints)
                Log.d("D", "fill vertical| anchor pos: $pos")
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

        val top = mHPoints[mCenterIndex].y.toInt()
        var left = mHPoints[mCenterIndex].x.toInt() -(mCenterIndex - 1) * mHorizontalTabWidth
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

        val maxPos = Math.min(header.mAdapter?.getItemCount() ?: 0, anchorPos + (mHPoints.size - mCenterIndex))
        val top = mHPoints[mCenterIndex].y.toInt()
        var left = mHPoints[mCenterIndex].x.toInt()
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

        val left = mVPoints[mCenterIndex].x.toInt()
        var pos = Math.max(0, anchorPos - mCenterIndex - mTabOffsetCount)
        var top = mVPoints[mCenterIndex].y.toInt() -(anchorPos - pos) * mVerticalTabHeight

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

        val maxPos = Math.min(header.mAdapter?.run { getItemCount() - 1 } ?: 0, anchorPos + mCenterIndex + 1 + mTabOffsetCount)
        val left = mVPoints[mCenterIndex].x.toInt()
        var top = mVPoints[mCenterIndex].y.toInt()
        var pos = anchorPos

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
        val ws = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY)
        val hs = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        view.measure(ws, hs)
        view.layout(x, y, x + w, y + h)
        header.addView(view)
        return view
    }

    private fun checkIfScrollStopped() {
        mOffsetChanged = false
        mIsCheckingScrollStop = true

        val startOffset = mAppBarBehavior.topAndBottomOffset
        mHeaderLayout.postOnAnimationDelayed({
            mIsCheckingScrollStop = false
            val currentOffset = mAppBarBehavior.topAndBottomOffset
            val scrollStopped = currentOffset == startOffset
            if (scrollStopped) {
                onScrollStopped(currentOffset)
            }
            Log.d("D","checkIfScrollStopped| startOffset: $startOffset, currentOffset: $currentOffset, scrollStopped: $scrollStopped")
        }, SCROLL_STOP_CHECK_DELAY)
    }

    private fun onScrollStopped(offset: Int) {
        Log.d("D","onScrollStopped| offset: $offset")
        var hScroll = false
        var vScroll = false
        if (offset == 0) {
            vScroll = true
            // TODO: disable dragging
        } else if (offset == mScreenHalf.toInt()) {
            hScroll = true
        } else {
            // TODO: check if near and offset (scroll) header if needed
        }

        mHeaderLayout.mEnableHorizontalScroll = hScroll
        mHeaderLayout.mEnableVerticalScroll = vScroll
    }

    inner class AppBarBehavior : AppBarLayout.Behavior() {

        init {
            setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout) = mCanDrag
            })
        }

    }

}