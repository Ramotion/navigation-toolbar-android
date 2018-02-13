package com.ramotion.navigationtoolbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import kotlin.math.min

/**
 * Moves header's views
 */
class HeaderLayoutManager(private val context: Context, attrs: AttributeSet?)
    : CoordinatorLayout.Behavior<HeaderLayout>(context, attrs), AppBarLayout.OnOffsetChangedListener {

    interface ItemsTransformer {
        fun transform(header: HeaderLayout, lm: HeaderLayoutManager, appBarBottom: Int)
    }

    abstract class DefaultItemsTransformer: ItemsTransformer {
        private var mIsInitialized = false
        private var mOffsetChangeStarted = false
        private var mRatioWork = 0f
        private var mRatioMiddle = 0f
        private var mRatioTop = 0f

        var mCurrentOrientation = Orientation.TRANSITIONAL
            private set

        var mCurrentRatio = 0f
            private  set

        var mCurrentRatioWork = 0f
            private  set

        var mCurrentRatioMiddle = 0f
            private  set

        var mCurrentRatioTop = 0f
            private  set

        override fun transform(header: HeaderLayout, lm: HeaderLayoutManager, appBarBottom: Int) {
            if (!mIsInitialized) {
                mRatioWork = lm.mWorkHeight / lm.mScreenHeight.toFloat()
                mRatioMiddle = lm.mScreenHalf / lm.mScreenHeight.toFloat()
                mRatioTop = lm.mToolBarHeight / lm.mScreenHeight.toFloat()
                mIsInitialized = true
            }

            mCurrentRatio = max(0f, appBarBottom / lm.mScreenHeight.toFloat())
            mCurrentRatioWork = max(0f, (appBarBottom - lm.mToolBarHeight) / lm.mWorkHeight.toFloat())
            mCurrentRatioMiddle = max(0f, (mCurrentRatio - mRatioMiddle) / mRatioMiddle)
            mCurrentRatioTop = max(0f, 1 - (mRatioMiddle - min(max(mCurrentRatio, mRatioTop), mRatioMiddle)) / (mRatioMiddle - mRatioTop))

            val isAtBorder = mCurrentRatioWork == 0f || mCurrentRatioWork == 1f
            if (!isAtBorder && mCurrentOrientation == Orientation.TRANSITIONAL) {
                mOffsetChangeStarted = true
                mCurrentOrientation = if (mRatioMiddle >= 0.5) Orientation.VERTICAL else Orientation.HORIZONTAL
                onOffsetChangeStarted(header, lm)
                onOffsetChanged(header, lm)
            } else if (isAtBorder && mCurrentOrientation != Orientation.TRANSITIONAL) {
                onOffsetChanged(header, lm)
                onOffsetChangeStopped(header, lm)
                mOffsetChangeStarted = false
                mCurrentOrientation = Orientation.TRANSITIONAL
            } else if (mOffsetChangeStarted) {
                onOffsetChanged(header, lm)
            }
        }

        open fun onOffsetChangeStarted(header: HeaderLayout, lm: HeaderLayoutManager) {}
        open fun onOffsetChanged(header: HeaderLayout, lm: HeaderLayoutManager) {}
        open fun onOffsetChangeStopped(header: HeaderLayout, lm: HeaderLayoutManager) {}
    }

    inner class AppBarBehavior : AppBarLayout.Behavior() {

        init {
            setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout) = mCanDrag
            })
        }

    }

    inner class HeaderScrollListener : HeaderLayout.ScrollListener {
        override fun onItemClick(header: HeaderLayout, viewHolder: HeaderLayout.ViewHolder) =
                this@HeaderLayoutManager.onHeaderItemClick(header, viewHolder)

        override fun onHeaderDown(header: HeaderLayout) =
                this@HeaderLayoutManager.onHeaderDown(header)

        override fun onHeaderHorizontalScroll(header: HeaderLayout, distance: Float) =
                this@HeaderLayoutManager.onHeaderHorizontalScroll(header, distance)

        override fun onHeaderVerticalScroll(header: HeaderLayout, distance: Float) =
                this@HeaderLayoutManager.onHeaderVerticalScroll(header, distance)

        override fun onHeaderHorizontalFling(header: HeaderLayout, velocity: Float) =
                this@HeaderLayoutManager.onHeaderHorizontalFling(header, velocity)

        override fun onHeaderVerticalFling(header: HeaderLayout, velocity: Float) =
                this@HeaderLayoutManager.onHeaderVerticalFling(header, velocity)

        override fun computeScroll(header: HeaderLayout) =
                this@HeaderLayoutManager.computeScroll(header)
    }

    enum class Orientation {
        HORIZONTAL, VERTICAL, TRANSITIONAL
    }

    internal companion object {
        const val TAB_ON_SCREEN_COUNT = 5
        const val TAB_OFF_SCREEN_COUNT = 1
        const val VERTICAL_TAB_HEIGHT_RATIO = 1f / TAB_ON_SCREEN_COUNT
        const val VERTICAL_TAB_WIDTH_RATIO = 4f / 5f
        const val SCROLL_STOP_CHECK_DELAY = 100L
        const val SCROLL_UP_ANIMATION_DURATION = 1000L
        const val SNAP_ANIMATION_DURATION = 300L

    }

    // TODO: init in constructor from attr
    private val mTabOffsetCount = TAB_OFF_SCREEN_COUNT
    private val mTabOnScreenCount = TAB_ON_SCREEN_COUNT
    private val mTabCount = mTabOnScreenCount + mTabOffsetCount * 2
    private val mScrollUpAnimationDuration = SCROLL_UP_ANIMATION_DURATION

    val mScreenWidth = context.resources.displayMetrics.widthPixels
    val mScreenHeight = context.resources.displayMetrics.heightPixels
    val mScreenHalf = mScreenHeight / 2f
    val mStatusBarHeight: Int
    val mToolBarHeight: Int
    val mWorkHeight: Int

    val mTopSnapDistance: Int
    val mBottomSnapDistnace: Int

    // TODO: add getters
    val mHorizontalTabWidth = mScreenWidth
    val mHorizontalTabHeight = mScreenHalf.toInt()
    val mVerticalTabHeight = (mScreenHeight * VERTICAL_TAB_HEIGHT_RATIO).toInt()
    val mVerticalTabWidth = (mScreenWidth * VERTICAL_TAB_WIDTH_RATIO).toInt()

    private val mViewCache = SparseArray<View?>()
    private val mOffsetAnimator = ValueAnimator() // TODO: add duration attribute
    private val mCenterIndex = mTabOnScreenCount % 2 + mTabOffsetCount

    internal val mAppBarBehavior = AppBarBehavior()
    internal val mHeaderScrollListener = HeaderScrollListener()

    private var mAppBar: AppBarLayout? = null
    private var mHeaderLayout: HeaderLayout? = null

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

        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        mStatusBarHeight = if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else 0

        val styledAttributes = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarSize = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()

        mToolBarHeight = actionBarSize + mStatusBarHeight
        mWorkHeight = mScreenHeight - mToolBarHeight

        mTopSnapDistance = (mToolBarHeight + (mScreenHalf - mToolBarHeight) / 2).toInt()
        mBottomSnapDistnace = (mScreenHalf + mScreenHalf / 2).toInt()
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: HeaderLayout, dependency: View): Boolean {
        return dependency is AppBarLayout
    }

    override fun onLayoutChild(parent: CoordinatorLayout, header: HeaderLayout, layoutDirection: Int): Boolean {
        if (!parent.isLaidOut) {
            parent.onLayoutChild(header, layoutDirection)

            mAppBar = parent.findViewById(R.id.com_ramotion_app_bar)

            mHeaderLayout = header
            header.mScrollListener = mHeaderScrollListener

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
        mItemsTransformer?.transform(header, this, dependency.bottom)

        return true
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        mOffsetChanged = true
    }

    fun scrollToPosition(pos: Int) {
        val header = mHeaderLayout ?: return
        if (pos < 0 || header.mAdapter?.run { pos >= getItemCount() } == true) {
            mScrollToPosition = pos
            fill(header)
        }
    }

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

    fun getClickedChildIndex() = mClickedChildIndex

    fun layoutChild(child: View, x: Int, y: Int, w: Int, h: Int) {
        val ws = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY)
        val hs = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        child.measure(ws, hs)
        child.layout(x, y, x + w, y + h)
    }

    fun fill(header: HeaderLayout) {
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

        when (orientation) {
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

    fun scrollHorizontally(distance: Float): Boolean {
        val header = mHeaderLayout ?: return false
        if (!header.mIsHorizontalScrollEnabled) {
            return false
        }

        return onHeaderHorizontalScroll(header, distance)
    }

    fun scrollVertically(distance: Float): Boolean {
        val header = mHeaderLayout ?: return false
        if (!header.mIsVerticalScrollEnabled) {
            return false
        }

        return onHeaderVerticalScroll(header, distance)
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

    private fun onHeaderItemClick(header: HeaderLayout, viewHolder: HeaderLayout.ViewHolder): Boolean {
        mClickedChildIndex = header.indexOfChild(viewHolder.view)
        smoothOffset(mScreenHalf.toInt())
        return true
    }

    private fun onHeaderDown(header: HeaderLayout): Boolean {
        if (header.childCount == 0) {
            return false
        }

        header.mScroller.forceFinished(true);
        ViewCompat.postInvalidateOnAnimation(header);
        return true
    }

    private fun onHeaderHorizontalScroll(header: HeaderLayout, distance: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        mScrollToPosition = HeaderLayout.INVALID_POSITION

        val scrollLeft = distance >= 0
        val offset = if (scrollLeft) {
            val lastRight = header.getChildAt(childCount - 1).right
            val newRight = lastRight - distance
            if (newRight > header.width) distance.toInt() else lastRight - header.width
        } else {
            val firstLeft = header.getChildAt(0).left
            if (firstLeft > 0) { // TODO: firstTop > border, border - center or systemBar height
                0
            } else {
                val newLeft = firstLeft - distance
                if (newLeft < 0) distance.toInt() else firstLeft
            }
        }

        for (i in 0 until childCount) {
            header.getChildAt(i).offsetLeftAndRight(-offset)
        }

        fill(header)
        return true
    }

    private fun onHeaderVerticalScroll(header: HeaderLayout, distance: Float): Boolean {
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

    private fun onHeaderHorizontalFling(header: HeaderLayout, velocity: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        val itemCount = header.mAdapter?.getItemCount() ?: return false
        val first = header.getChildAt(0)
        val firstPos = HeaderLayout.getChildViewHolder(first)!!.mPosition
        val start = first.left - firstPos * mHorizontalTabWidth
        val min = -itemCount * mHorizontalTabWidth + header.width
        val max = 0

        header.mScroller.apply {
            forceFinished(true)
            fling(start, 0, velocity.toInt(), 0, min, max, 0, 0)
        }
        ViewCompat.postInvalidateOnAnimation(header)

        return true
    }

    private fun onHeaderVerticalFling(header: HeaderLayout, velocity: Float): Boolean {
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

    private fun computeScroll(header: HeaderLayout) {
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

    private fun initPoints(header: HeaderLayout) {
        val hx = 0f
        val hy = mScreenHalf // - mStatusBarHeight
        val vx = (header.width - mVerticalTabWidth).toFloat()
        val vy = ((1f * mScreenHeight) / mTabOnScreenCount) * mCenterIndex

        mHPoint = PointF(hx, hy)
        mVPoint = PointF(vx, vy)
    }

    private fun getPositionRatio(): Float =
            mAppBar?.let { Math.max(0f, it.bottom / mScreenHeight.toFloat()) } ?: 0f

    private fun getOrientation(ratio: Float): Orientation {
        return when {
            ratio <= 0.5f -> Orientation.HORIZONTAL
            ratio < 1 -> Orientation.TRANSITIONAL
            else -> Orientation.VERTICAL
        }
    }

    private fun fillLeft(header: HeaderLayout, anchorPos: Int) {
        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        val top = mHPoint.y.toInt()
        val leftDiff = mHPoint.x.toInt() - (mViewCache.get(anchorPos)?.left ?: 0)

        var pos = Math.max(0, anchorPos - mCenterIndex - mTabOffsetCount)
        var left = (mHPoint.x.toInt() -(anchorPos - pos) * mHorizontalTabWidth) - leftDiff

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

        val maxPos = Math.min(header.mAdapter?.run { getItemCount() } ?: 0, anchorPos + mCenterIndex + 1 + mTabOffsetCount)
        val top = mHPoint.y.toInt()

        var pos = anchorPos
        var left  = if (header.childCount > 0) {
            header.getChildAt(header.childCount - 1).right
        } else {
            mHPoint.x.toInt()
        }

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
        val header = mHeaderLayout ?: return

        mOffsetChanged = false
        mIsCheckingScrollStop = true

        val startOffset = mAppBarBehavior.topAndBottomOffset
        header.postOnAnimationDelayed({
            mIsCheckingScrollStop = false
            val currentOffset = mAppBarBehavior.topAndBottomOffset
            val scrollStopped = currentOffset == startOffset
            if (scrollStopped) {
                onOffsetChangingStopped(currentOffset)
            }
        }, SCROLL_STOP_CHECK_DELAY)
    }

    private fun onOffsetChangingStopped(offset: Int) {
        val header = mHeaderLayout ?: return
        val appBar = mAppBar ?: return

        var hScrollEnable = false
        var vScrollEnable = false

        val invertedOffset = mScreenHeight + offset
        if (invertedOffset == mScreenHeight) {
            vScrollEnable = true
            mCanDrag = false
        } else if (invertedOffset == mScreenHalf.toInt()) {
            hScrollEnable = true
            mCanDrag = false
        } else if (invertedOffset == mToolBarHeight) {
            mCanDrag = true
            return
        } else {
            // TODO: check
            if (invertedOffset in mToolBarHeight..(mTopSnapDistance - 1)) {
                appBar.setExpanded(false, true)
                //smoothOffset(mScreenHeight - mToolBarHeight, SNAP_ANIMATION_DURATION)
            } else if (invertedOffset in mTopSnapDistance..(mBottomSnapDistnace - 1)) {
                smoothOffset(mScreenHalf.toInt(), SNAP_ANIMATION_DURATION)
            } else {
                smoothOffset(0, SNAP_ANIMATION_DURATION)
            }
        }

        header.mIsHorizontalScrollEnabled = hScrollEnable
        header.mIsVerticalScrollEnabled = vScrollEnable
    }

    private fun smoothOffset(offset: Int, duration: Long = mScrollUpAnimationDuration) {
        val header = mHeaderLayout ?: return

        mOffsetAnimator.cancel()
        mOffsetAnimator.setDuration(duration)
        mOffsetAnimator.setIntValues(mAppBarBehavior.topAndBottomOffset, -offset)
        mOffsetAnimator.addUpdateListener {
            val value = it.animatedValue as Int
            mAppBarBehavior.topAndBottomOffset = value
        }
        mOffsetAnimator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                header.mIsHorizontalScrollEnabled = false
                header.mIsVerticalScrollEnabled = false
            }
            override fun onAnimationEnd(animation: Animator?) {
                this@HeaderLayoutManager.onOffsetChangingStopped(-offset)
            }
        })
        mOffsetAnimator.start()
    }

}