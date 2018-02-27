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
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.OverScroller
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias ItemChangeListener = (position: Int) -> Unit
typealias ScrollStateListener = (state: HeaderLayoutManager.ScrollState) -> Unit

/**
 * Moves header's views
 */
class HeaderLayoutManager(context: Context, attrs: AttributeSet?)
    : CoordinatorLayout.Behavior<HeaderLayout>(context, attrs), AppBarLayout.OnOffsetChangedListener {

    enum class Orientation {
        HORIZONTAL, VERTICAL, TRANSITIONAL
    }

    enum class ScrollState {
        IDLE, DRAGGING, FLING
    }

    internal companion object {
        const val TAB_ON_SCREEN_COUNT = 5
        const val TAB_OFF_SCREEN_COUNT = 1
        const val VERTICAL_TAB_HEIGHT_RATIO = 1f / TAB_ON_SCREEN_COUNT
        const val VERTICAL_TAB_WIDTH_RATIO = 4f / 5f
        const val SCROLL_STOP_CHECK_DELAY = 100L
        const val SCROLL_UP_ANIMATION_DURATION = 500L
        const val SNAP_ANIMATION_DURATION = 300L
        const val MAX_SCROLL_DURATION = 600L

    }

    interface ItemTransformer {
        fun attach(lm: HeaderLayoutManager, header: HeaderLayout)
        fun detach()
        fun transform(headerBottom: Int)
    }

    interface ItemClickListener {
        fun onItemClick(viewHolder: HeaderLayout.ViewHolder)
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
    private val mCenterIndex = mTabOnScreenCount % 2 + mTabOffsetCount
    private val mViewFlinger = ViewFlinger(context)
    private val mItemClickListeners = mutableListOf<ItemClickListener>()

    internal val mAppBarBehavior = AppBarBehavior()
    internal val mHeaderScrollListener = HeaderScrollListener()

    private var mOffsetAnimator: ValueAnimator? = null // TODO: add duration attribute
    private var mAppBar: AppBarLayout? = null
    private var mHeaderLayout: HeaderLayout? = null

    private var mInitialized = false
    private var mCanDrag = true
    private var mOffsetChanged = false
    private var mIsCheckingScrollStop =false

    private var mScrollState = ScrollState.IDLE
    private var mCurOrientation: Orientation? = null

    private lateinit var mHPoint: PointF // TODO: replace with data class
    private lateinit var mVPoint: PointF // TODO: replace with data class

    internal var mItemTransformer: ItemTransformer? = null
    internal var mItemChangeListener: ItemChangeListener? = null
    internal var mScrollStateListener: ScrollStateListener? = null

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

        override fun onHeaderUp(header: HeaderLayout)  =
                this@HeaderLayoutManager.onHeaderUp(header)

        override fun onHeaderHorizontalScroll(header: HeaderLayout, distance: Float) =
                this@HeaderLayoutManager.onHeaderHorizontalScroll(header, distance)

        override fun onHeaderVerticalScroll(header: HeaderLayout, distance: Float) =
                this@HeaderLayoutManager.onHeaderVerticalScroll(header, distance)

        override fun onHeaderHorizontalFling(header: HeaderLayout, velocity: Float) =
                this@HeaderLayoutManager.onHeaderHorizontalFling(header, velocity)

        override fun onHeaderVerticalFling(header: HeaderLayout, velocity: Float) =
                this@HeaderLayoutManager.onHeaderVerticalFling(header, velocity)
    }

    private inner class ViewFlinger(context: Context) : Runnable {
        private val mScroller = OverScroller(context)

        override fun run() {
            val header = mHeaderLayout ?: return

            val x = mScroller.currX
            val y = mScroller.currY

            if (!mScroller.computeScrollOffset()) {
                setScrollState(ScrollState.IDLE)
                return
            }

            val diffX = mScroller.currX - x
            val diffY = mScroller.currY - y

            if (diffX == 0 && diffY == 0) {
                ViewCompat.postOnAnimation(header, this)
                return
            }

            for (i in 0 until header.childCount) {
                val child = header.getChildAt(i)
                child.offsetLeftAndRight(diffX)
                child.offsetTopAndBottom(diffY)
            }

            fill(header)

            ViewCompat.postOnAnimation(header, this)
        }

        fun fling(startX: Int, startY: Int, velocityX: Int, velocityY: Int, minX: Int, maxX: Int, minY: Int, maxY: Int) {
            setScrollState(ScrollState.FLING)
            mScroller.forceFinished(true)
            mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
            ViewCompat.postOnAnimation(mHeaderLayout, this)
        }

        fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            setScrollState(ScrollState.FLING)
            mScroller.forceFinished(true)
            mScroller.startScroll(startX, startY, dx, dy, duration)
            ViewCompat.postOnAnimation(mHeaderLayout, this)
        }

        fun stop() {
            if (!mScroller.isFinished) {
                setScrollState(ScrollState.IDLE)
                mScroller.abortAnimation()
            }
        }
    }

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
        mViewFlinger.stop()
        header.y = (dependency.bottom - header.height).toFloat() // Offset header on collapsing
        mCurOrientation = null
        mItemTransformer?.transform(dependency.bottom)
        return true
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        mOffsetChanged = true
    }

    // TODO: fun scroll(distance)

    fun scrollToPosition(pos: Int) {
        val header = mHeaderLayout ?: return

        if (header.childCount == 0) {
            return
        }

        val itemCount = header.mAdapter?.getItemCount() ?: -1
        if (pos < 0 || pos > itemCount) {
            return
        }

        if (header.mIsHorizontalScrollEnabled) {
            val anchorPos = getHorizontalAnchorPos(header)
            if (anchorPos == pos) {
                return
            }

            val offset = (pos - anchorPos) * header.getChildAt(0).width
            onHeaderHorizontalScroll(header, offset.toFloat())
        } else if (header.mIsVerticalScrollEnabled) {
            val anchorPos = getVerticalAnchorPos(header)
            if (anchorPos == pos) {
                return
            }

            val offset = (pos - anchorPos) * header.getChildAt(0).height
            onHeaderVerticalScroll(header, offset.toFloat())
        }

        mItemChangeListener?.invoke(pos)
    }

    fun smoothScrollToPosition(pos: Int) {
        if (mOffsetAnimator?.isRunning == true) {
            return
        }

        val header = mHeaderLayout ?: return
        if (header.childCount == 0) {
            return
        }

        val itemCount = header.mAdapter?.getItemCount() ?: -1
        if (pos < 0 || pos > itemCount) {
            return
        }

        mItemChangeListener?.invoke(pos)

        if (header.mIsHorizontalScrollEnabled) {
            val anchorPos = getHorizontalAnchorPos(header)
            if (anchorPos == HeaderLayout.INVALID_POSITION) {
                return
            }

            val anchorView = getHorizontalAnchorView(header) ?: return
            val childWidth = anchorView.width
            val offset = ((pos - anchorPos) * childWidth + (anchorView.left - mHPoint.x)).toInt()
            if (offset == 0) {
                return
            }

            val startX = getStartX(header.getChildAt(0))
            val delta = abs(offset) / childWidth.toFloat()
            val duration = min(((delta + 1) * 100).toInt(), MAX_SCROLL_DURATION.toInt())
            mViewFlinger.startScroll(startX, 0, -offset, 0, duration)
        } else if (header.mIsVerticalScrollEnabled) {
            val anchorPos = getVerticalAnchorPos(header)
            if (anchorPos == HeaderLayout.INVALID_POSITION) {
                return
            }

            val anchorView = getVerticalAnchorView(header) ?: return
            val childHeight = anchorView.height
            val offset = ((pos - anchorPos) * childHeight + (anchorView.top - mVPoint.y)).toInt()
            if (offset == 0) {
                return
            }

            val startY = getStartY(header.getChildAt(0))
            val delta = abs(offset) / childHeight.toFloat()
            val duration = min(((delta + 1) * 100).toInt(), MAX_SCROLL_DURATION.toInt())
            mViewFlinger.startScroll(0, startY, 0, -offset, duration)
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

    fun layoutChild(child: View, x: Int, y: Int, w: Int, h: Int) {
        val ws = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY)
        val hs = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        child.measure(ws, hs)
        child.layout(x, y, x + w, y + h)
    }

    fun fill(header: HeaderLayout) {
        val orientation = getOrientation(::getPositionRatio)
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

        mAppBar?.bottom?.let { mItemTransformer?.transform(it) }
    }

    fun getAnchorView(header: HeaderLayout): View? {
        val orientation = getOrientation(::getPositionRatio)
        return when (orientation) {
            Orientation.HORIZONTAL -> getHorizontalAnchorView(header)
            Orientation.VERTICAL -> getVerticalAnchorView(header)
            Orientation.TRANSITIONAL -> null
        }
    }

    fun getAnchorPos(header: HeaderLayout): Int? {
        return getAnchorView(header)?.let { HeaderLayout.getChildViewHolder(it) }?.mPosition
    }

    fun addItemClickListener(listener: HeaderLayoutManager.ItemClickListener) = mItemClickListeners.add(listener)

    fun removeItemClickListener(listener: HeaderLayoutManager.ItemClickListener) = mItemClickListeners.remove(listener)

    private fun getHorizontalAnchorPos(header: HeaderLayout): Int {
        return getHorizontalAnchorView(header)?.let { header.getAdapterPosition(it) } ?: HeaderLayout.INVALID_POSITION
    }

    private fun getVerticalAnchorPos(header: HeaderLayout): Int {
        return getVerticalAnchorView(header)?.let { header.getAdapterPosition(it) } ?: HeaderLayout.INVALID_POSITION
    }

    private fun onHeaderItemClick(header: HeaderLayout, viewHolder: HeaderLayout.ViewHolder): Boolean {
        return when {
            header.mIsHorizontalScrollEnabled -> {
                smoothScrollToPosition(viewHolder.mPosition)
                mItemClickListeners.forEach { it.onItemClick(viewHolder) }
                true
            }
            header.mIsVerticalScrollEnabled -> {
                smoothOffset(mScreenHalf.toInt())
                mItemClickListeners.forEach { it.onItemClick(viewHolder) }
                true
            }
            else -> false
        }
    }

    private fun onHeaderDown(header: HeaderLayout): Boolean {
        if (header.childCount == 0) {
            return false
        }

        mViewFlinger.stop()
        return true
    }

    private fun onHeaderUp(header: HeaderLayout): Unit {
        if (mScrollState != ScrollState.FLING) {
            setScrollState(ScrollState.IDLE)
        }
    }

    private fun onHeaderHorizontalScroll(header: HeaderLayout, distance: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        setScrollState(ScrollState.DRAGGING)

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

        setScrollState(ScrollState.DRAGGING)

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
        val startX = getStartX(header.getChildAt(0))
        val min = -itemCount * mHorizontalTabWidth + header.width
        val max = 0

        mViewFlinger.fling(startX, 0, velocity.toInt(), 0, min, max, 0, 0)

        return true
    }

    private fun onHeaderVerticalFling(header: HeaderLayout, velocity: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        val itemCount = header.mAdapter?.getItemCount() ?: return false
        val startY = getStartY(header.getChildAt(0))
        val min = -itemCount * mVerticalTabHeight + header.height
        val max = 0

        mViewFlinger.fling(0, startY, 0, velocity.toInt(), 0, 0, min, max)

        return true
    }

    private fun getStartX(firstView: View): Int {
        val firstPos = HeaderLayout.getChildViewHolder(firstView)!!.mPosition
        return firstView.left - firstPos * mHorizontalTabWidth
    }

    private fun getStartY(topView: View): Int {
        val firstPos = HeaderLayout.getChildViewHolder(topView)!!.mPosition
        return topView.top - firstPos * mVerticalTabHeight
    }

    private fun initPoints(header: HeaderLayout) {
        val hx = 0f
        val hy = mScreenHalf // - mStatusBarHeight
        val vx = (header.width - mVerticalTabWidth).toFloat()
        val vy = ((1f * mScreenHeight) / mTabOnScreenCount) * mCenterIndex

        mHPoint = PointF(hx, hy)
        mVPoint = PointF(vx, vy)
    }

    private fun getPositionRatio() = mAppBar?.let { Math.max(0f, it.bottom / mScreenHeight.toFloat()) } ?: 0f

    private fun getOrientation(getRatio: () -> Float, force: Boolean = false): Orientation {
        return if (force) {
            val ratio = getRatio()
            mCurOrientation = when {
                ratio <= 0.5f -> Orientation.HORIZONTAL
                ratio < 1 -> Orientation.TRANSITIONAL
                else -> Orientation.VERTICAL
            }
            mCurOrientation!!
        } else {
            mCurOrientation ?: getOrientation(getRatio, true)
        }
    }

    private fun fillLeft(header: HeaderLayout, anchorPos: Int) {
        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        val top = mAppBar?.let { header.height - it.bottom } ?: mHPoint.y.toInt()
        val bottom = mAppBar?.bottom ?: mHorizontalTabHeight
        val leftDiff = mHPoint.x.toInt() - (mViewCache.get(anchorPos)?.left ?: 0)

        var pos = Math.max(0, anchorPos - mCenterIndex - mTabOffsetCount)
        var left = (mHPoint.x.toInt() -(anchorPos - pos) * mHorizontalTabWidth) - leftDiff

        while (pos < anchorPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, mHorizontalTabWidth, bottom)
            left = view.right
            pos++
        }
    }

    private fun fillRight(header: HeaderLayout, anchorPos: Int) {
        if (header.mAdapter?.run { getItemCount() == 0 } != false) {
            return
        }

        val startPos = when (anchorPos) {
            HeaderLayout.INVALID_POSITION -> 0
            else -> anchorPos
        }

        val top = mAppBar?.let { header.height - it.bottom } ?: mHPoint.y.toInt()
        val bottom = mAppBar?.bottom ?: mHorizontalTabHeight
        val maxPos = Math.min(header.mAdapter?.run { getItemCount() } ?: 0, startPos + mCenterIndex + 1 + mTabOffsetCount)

        var pos = startPos
        var left  = if (header.childCount > 0) {
            header.getChildAt(header.childCount - 1).right
        } else {
            mHPoint.x.toInt()
        }

        while (pos <  maxPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, mHorizontalTabWidth, bottom)
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
        if (header.mAdapter?.run { getItemCount() == 0 } != false) {
            return
        }

        val startPos = when (anchorPos) {
            HeaderLayout.INVALID_POSITION -> 0
            else -> anchorPos
        }

        val maxPos = Math.min(header.mAdapter?.run { getItemCount() } ?: 0, startPos + mCenterIndex + 1 + mTabOffsetCount)
        val left = mVPoint.x.toInt()
        var pos = startPos

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
        when (invertedOffset) {
            mScreenHeight -> {
                vScrollEnable = true
                mCanDrag = false
            }
            mScreenHalf.toInt() -> {
                hScrollEnable = true
                mCanDrag = false
            }
            mToolBarHeight -> {
                hScrollEnable = true
                mCanDrag = true
            }
            in mToolBarHeight..(mTopSnapDistance - 1) -> {
                appBar.setExpanded(false, true) // or smoothOffset(mScreenHeight - mToolBarHeight, SNAP_ANIMATION_DURATION)
            }
            in mTopSnapDistance..(mBottomSnapDistnace - 1) -> {
                smoothOffset(mScreenHalf.toInt(), SNAP_ANIMATION_DURATION)
            }
            else -> {
                smoothOffset(0, SNAP_ANIMATION_DURATION)
            }
        }

        header.mIsHorizontalScrollEnabled = hScrollEnable
        header.mIsVerticalScrollEnabled = vScrollEnable
    }

    private fun smoothOffset(offset: Int, duration: Long = mScrollUpAnimationDuration) {
        val header = mHeaderLayout ?: return

        mOffsetAnimator?.cancel()

        mOffsetAnimator = ValueAnimator().also { animator ->
            animator.duration = duration
            animator.setIntValues(mAppBarBehavior.topAndBottomOffset, -offset)
            animator.addUpdateListener {
                val value = it.animatedValue as Int
                mAppBarBehavior.topAndBottomOffset = value
            }
            animator.addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                    header.mIsHorizontalScrollEnabled = false
                    header.mIsVerticalScrollEnabled = false
                }
                override fun onAnimationEnd(animation: Animator?) {
                    this@HeaderLayoutManager.onOffsetChangingStopped(-offset)
                }
            })
            animator.start()
        }
    }

    private fun setScrollState(state: ScrollState) {
        if (mScrollState == state) {
            return
        }

        mScrollStateListener?.invoke(state)
        mScrollState = state
    }
}