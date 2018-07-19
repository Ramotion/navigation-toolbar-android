package com.ramotion.navigationtoolbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.min


/**
 * Responsible from moving and placing HeaderLayout's cards.
 */
class HeaderLayoutManager(context: Context, attrs: AttributeSet?)
    : CoordinatorLayout.Behavior<HeaderLayout>(context, attrs), AppBarLayout.OnOffsetChangedListener {

    /**
     * Defines header orientation states.
     * HORIZONTAL - horizontal orientation, when cards placed horizontally.
     * VERTICAL - vertical orientation, when cards placed vertically.
     * TRANSITIONAL - state when when header is changing from vertical to horizontal and vice versa.
     */
    enum class Orientation {
        /**
         * Horizontal orientation, when cards placed horizontally.
         */
        HORIZONTAL,
        /**
         * Vertical orientation, when cards placed vertically.
         */
        VERTICAL,
        /**
         * state when when header is changing from vertical to horizontal and vice versa.
         */
        TRANSITIONAL
    }

    /**
     * Defines header scroll states.
     */
    enum class ScrollState {
        IDLE, DRAGGING, FLING
    }

    enum class VerticalGravity(val value: Int) {
        LEFT(-1),
        CENTER(-2),
        RIGHT(-3);

        companion object {
            private val map = VerticalGravity.values().associateBy(VerticalGravity::value)
            fun fromInt(type: Int, defaultValue: VerticalGravity = RIGHT) = map.getOrElse(type) {defaultValue}
        }
    }

    data class Point(val x: Int, val y: Int)

    class State : View.BaseSavedState {
        companion object {
            @JvmField
            @Suppress("unused")
            val CREATOR = object : Parcelable.Creator<State> {
                override fun createFromParcel(parcel: Parcel): State = State(parcel)
                override fun newArray(size: Int): Array<State?> = arrayOfNulls(size)
            }
        }

        val anchorPos: Int

        constructor(superState: Parcelable, anchorPos: Int) : super(superState) {
            this.anchorPos = anchorPos
        }

        private constructor(parcel: Parcel) : super(parcel) {
            this.anchorPos = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel, i: Int) {
            parcel.writeInt(anchorPos)
        }

        override fun describeContents(): Int = 0
    }

    internal companion object {
        const val TAB_ON_SCREEN_COUNT = 5
        const val TAB_OFF_SCREEN_COUNT = 1
        const val VERTICAL_TAB_WIDTH_RATIO = 4f / 5f
        const val SCROLL_STOP_CHECK_DELAY = 100L
        const val COLLAPSING_BY_SELECT_DURATION = 500
        const val SNAP_ANIMATION_DURATION = 300L
        const val MAX_SCROLL_DURATION = 600L
    }

    /**
     * Observes header's change (expand / collapse).
     */
    interface HeaderChangeListener {
        /**
         * Invoked whenever header is changed (expanded / collapsed).
         * @param lm HeaderLayoutManager.
         * @param header HeaderLayout.
         * @param headerBottom HeaderLayout bottom position.
         * @see HeaderLayoutManager
         * @see HeaderLayout
         */
        fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int)
    }

    /**
     * Observes header's update (redraw).
     */
    interface HeaderUpdateListener {
        /**
         * Invoked whenever header is updated (filled / redrawn).
         * @param lm HeaderLayoutManager.
         * @param header HeaderLayout.
         * @param headerBottom HeaderLayout bottom position.
         * @see HeaderLayoutManager
         * @see HeaderLayout
         */
        fun onHeaderUpdated(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int)
    }

    /**
     * Enhanced HeaderChangeListener which observes header's position states: collapsed, middle, expanded.
     */
    abstract class HeaderChangeStateListener : HeaderChangeListener {
        /**
         * Invoked on header's position state change to: collapsed, middle, expanded.
         * @param lm HeaderLayoutManager.
         * @param header HeaderLayout.
         * @param headerBottom HeaderLayout bottom position.
         * @see HeaderLayoutManager
         * @see HeaderLayout
         */
        final override fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
            when (headerBottom) {
                lm.workTop -> onCollapsed()
                lm.workMiddle -> onMiddle()
                lm.workBottom -> onExpanded()
            }
        }

        /**
         * Invoked when header is collapsed.
         */
        open fun onCollapsed() {}

        /**
         * Invoked when header passed or stopped in the middle.
         */
        open fun onMiddle() {}

        /**
         * Invoked when header is expanded.
         */
        open fun onExpanded() {}
    }

    /**
     * Observes card's click events.
     */
    interface ItemClickListener {
        /**
         * Invoked when header card is clicked.
         * @param viewHolder ViewHolder of clicked card.
         * @see HeaderLayout.ViewHolder
         */
        fun onItemClicked(viewHolder: HeaderLayout.ViewHolder)
    }

    /**
     * Observes header scrolling.
     */
    interface ItemChangeListener {
        /**
         * Invoked when header scroll started
         * @param position Current anchor position.
         * @see getAnchorPos
         */
        fun onItemChangeStarted(position: Int) {}

        /**
         * Invoked when header item (card) position is changed.
         * @param position new anchor position.
         * @see getAnchorPos
         */
        fun onItemChanged(position: Int)
    }

    /**
     * Observes header scroll states
     */
    interface ScrollStateListener {
        /**
         * Invoked when scroll state is changed.
         * @param state Current scroll state.
         * @see HeaderLayoutManager.ScrollState
         */
        fun onScrollStateChanged(state: HeaderLayoutManager.ScrollState)
    }

    /**
     * Used for cards decoration.
     */
    interface ItemDecoration {
        /**
         * Retrieve any offsets for the given item. Each field of outRect specifies the number of pixels that the item view should be inset by, similar to padding or margin.
         * @param outRect Rect to receive the output.
         * @param viewHolder Card ViewHolder.
         */
        fun getItemOffsets(outRect: Rect, viewHolder: HeaderLayout.ViewHolder)
    }

    private val tabOffsetCount = TAB_OFF_SCREEN_COUNT
    private val screenWidth = context.resources.displayMetrics.widthPixels
    private val viewCache = SparseArray<View?>()
    private val viewFlinger = ViewFlinger(context)
    private val verticalGravity: VerticalGravity
    private val tempDecorRect = Rect()
    private val itemDecorations = mutableListOf<ItemDecoration>()
    private val scrollListener = HeaderLayoutScrollListener()
    private val adapterChangeListener = AdapterChangeListener()

    private val verticalScrollTopBorder: Int
    private val collapsingBySelectDuration: Int
    private val tabOnScreenCount: Int
    private val centerIndex: Int
    private val topSnapDistance: Int
    private val bottomSnapDistance: Int

    val workBottom = context.resources.displayMetrics.heightPixels
    val workMiddle = workBottom / 2
    val workTop: Int
    val workHeight: Int

    /**
     * Width of card in horizontal orientation.
     */
    val horizontalTabWidth = screenWidth
    /**
     * Height of card in horizontal orientation.
     */
    val horizontalTabHeight = workMiddle
    /**
     * Width of card in vertical orientation.
     */
    val verticalTabWidth: Int
    /**
     * Height of card in vertical orientation.
     */
    val verticalTabHeight: Int

    internal val appBarBehavior = AppBarBehavior()
    internal val changeListener = mutableListOf<HeaderChangeListener>()
    internal val updateListener = mutableListOf<HeaderUpdateListener>()
    internal val itemClickListeners = mutableListOf<ItemClickListener>()
    internal val itemChangeListeners = mutableListOf<ItemChangeListener>()
    internal val scrollStateListeners = mutableListOf<ScrollStateListener>()

    private var offsetAnimator: ValueAnimator? = null
    private var appBar: AppBarLayout? = null
    private var headerLayout: HeaderLayout? = null

    private var isCanDrag = true
    private var isOffsetChanged = false
    private var isCheckingScrollStop =false
    private var isHeaderChanging = false

    private var scrollState = ScrollState.IDLE
    private var curOrientation: Orientation? = null
    private var prevOffset: Int = Int.MAX_VALUE
    private var restoredAnchorPosition: Int? = null

    private var hPoint: Point? = null
    private var vPoint: Point? = null

    internal inner class AppBarBehavior : AppBarLayout.Behavior() {
        init {
            setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout) = isCanDrag
            })
        }
    }

    private inner class HeaderLayoutScrollListener : HeaderLayout.ScrollListener {
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

    private inner class AdapterChangeListener : HeaderLayout.AdapterChangeListener {
        override fun onAdapterChanged(header: HeaderLayout) {
            updatePoints(header)
        }
    }

    private inner class ViewFlinger(context: Context) : Runnable {
        private val scroller = OverScroller(context, LinearInterpolator())

        override fun run() {
            val header = headerLayout ?: return

            val x = scroller.currX
            val y = scroller.currY

            if (!scroller.computeScrollOffset()) {
                setScrollState(ScrollState.IDLE)
                return
            }

            val diffX = scroller.currX - x
            val diffY = scroller.currY - y

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
            scroller.forceFinished(true)
            scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
            ViewCompat.postOnAnimation(headerLayout, this)
        }

        fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            setScrollState(ScrollState.FLING)
            scroller.forceFinished(true)
            scroller.startScroll(startX, startY, dx, dy, duration)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                scroller.computeScrollOffset()
            }
            ViewCompat.postOnAnimation(headerLayout, this)
        }

        fun stop() {
            if (!scroller.isFinished) {
                setScrollState(ScrollState.IDLE)
                scroller.abortAnimation()
            }
        }
    }

    init {
        Looper.myQueue().addIdleHandler {
            if (isOffsetChanged && !isCheckingScrollStop) {
                checkIfOffsetChangingStopped()
            }
            true
        }

        var itemCount = TAB_ON_SCREEN_COUNT
        var verticalItemWidth = screenWidth * VERTICAL_TAB_WIDTH_RATIO
        var gravity = VerticalGravity.RIGHT
        var collapsingDuration = COLLAPSING_BY_SELECT_DURATION
        var vScrollTopBorder = false

        attrs?.also {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.NavigationToolBarr, 0, 0)
            try {
                itemCount = a.getInteger(R.styleable.NavigationToolBarr_headerOnScreenItemCount, -1)
                        .let { if (it <= 0) TAB_ON_SCREEN_COUNT else it }
                gravity = VerticalGravity.fromInt(a.getInteger(R.styleable.NavigationToolBarr_headerVerticalGravity, VerticalGravity.RIGHT.value))
                collapsingDuration = a.getInteger(R.styleable.NavigationToolBarr_headerCollapsingBySelectDuration, COLLAPSING_BY_SELECT_DURATION)
                vScrollTopBorder = a.getBoolean(R.styleable.NavigationToolBarr_headerTopBorderAtSystemBar, false)

                if (a.hasValue(R.styleable.NavigationToolBarr_headerVerticalItemWidth)) {
                    verticalItemWidth = if (a.getType(R.styleable.NavigationToolBarr_headerVerticalItemWidth) == TypedValue.TYPE_DIMENSION) {
                        a.getDimension(R.styleable.NavigationToolBarr_headerVerticalItemWidth, verticalItemWidth)
                    } else {
                        screenWidth.toFloat()
                    }
                }
            } finally {
                a.recycle()
            }
        }

        tabOnScreenCount = itemCount
        centerIndex = tabOnScreenCount / 2
        verticalTabHeight = (workBottom * (1f / tabOnScreenCount)).toInt()
        verticalTabWidth = verticalItemWidth.toInt()
        verticalGravity = gravity
        collapsingBySelectDuration = collapsingDuration

        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else 0

        val actionBarSize: Int
        val styledAttributes = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        try {
            actionBarSize = styledAttributes.getDimension(0, 0f).toInt()
        } finally {
            styledAttributes.recycle()
        }

        workTop = actionBarSize + statusBarHeight
        workHeight = workBottom - workTop

        topSnapDistance = (workTop + (workMiddle - workTop) / 2)
        bottomSnapDistance = (workMiddle + workMiddle / 2)

        verticalScrollTopBorder = if (vScrollTopBorder) statusBarHeight else 0
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: HeaderLayout, dependency: View): Boolean {
        return dependency is AppBarLayout
    }

    override fun onLayoutChild(parent: CoordinatorLayout, header: HeaderLayout, layoutDirection: Int): Boolean {
        if (!parent.isLaidOut) {
            parent.onLayoutChild(header, layoutDirection)

            appBar = parent.findViewById(R.id.com_ramotion_app_bar)

            headerLayout = header
            header.scrollListener = scrollListener
            header.adapterChangeListener = adapterChangeListener

            updatePoints(header)
            fill(header)
        }

        return true
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, header: HeaderLayout, dependency: View): Boolean {
        val headerBottom = dependency.bottom
        header.y = (headerBottom - header.height).toFloat() // Offset header on collapsing
        curOrientation = null
        isHeaderChanging = true
        viewFlinger.stop()
        changeListener.forEach { it.onHeaderChanged(this, header, headerBottom) }
        return true
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        isOffsetChanged = true
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, header: HeaderLayout): Parcelable {
        val anchorPos = getAnchorPos(header)
        val superParcel = super.onSaveInstanceState(parent, header)
        return State(superParcel, anchorPos)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: HeaderLayout, state: Parcelable?) {
        super.onRestoreInstanceState(parent, child, state)
        if (state is State) {
            restoredAnchorPosition = state.anchorPos
        }
    }

    // TODO: fun scroll(distance)

    /**
     * Scroll header to specified position.
     * @param pos Position where to scroll.
     */
    fun scrollToPosition(pos: Int) {
        scrollToPosition(pos) { header, _, offset, horizontal ->
            when(horizontal) {
                true -> onHeaderHorizontalScroll(header, offset.toFloat())
                else -> onHeaderVerticalScroll(header, offset.toFloat())
            }
        }
    }

    /**
     * Smooth scroll header to specified position.
     * @param pos Position where to scroll.
     */
    fun smoothScrollToPosition(pos: Int) {
        scrollToPosition(pos) { _, anchorView, offset, horizontal ->
            if (horizontal) {
                val startX = getStartX(anchorView)
                val delta = abs(offset) / horizontalTabWidth
                val duration = min((delta + 1) * 100, MAX_SCROLL_DURATION.toInt())
                viewFlinger.startScroll(startX, 0, -offset, 0, duration)
            } else {
                val startY = getStartY(anchorView)
                val delta = abs(offset) / verticalTabHeight
                val duration = min((delta + 1) * 100, MAX_SCROLL_DURATION.toInt())
                viewFlinger.startScroll(0, startY, 0, -offset, duration)
            }
        }
    }

    /**
     * Returns point of left top corner where clicked card get placed when header change orientation
     * from vertical to horizontal.
     * @return Point(x, y) of left top corner where card get placed when header change orientation
     * from vertical to horizontal.
     */
    fun getHorizontalPoint(): Point {
        return hPoint ?: throw RuntimeException("Layout manager not initialized yet")
    }

    /**
     * Returns point of left top corner where anchor card get placed when header change orientation
     * from horizontal to vertical.
     * @return Point(x, y) of left top corner where anchor card get placed when header change orientation
     * from horizontal to vertical.
     */
    fun getVerticalPoint(): Point {
        return vPoint ?: throw RuntimeException("Layout manager not initialized yet")
    }

    /**
     * Returns horizontal center card.
     * @return View or null if card not found.
     */
    fun getHorizontalAnchorView(header: HeaderLayout): View? {
        val centerLeft = hPoint?.x ?: return null

        var result: View? = null
        var lastDiff = Int.MAX_VALUE

        for (i in 0 until header.childCount) {
            val child = header.getChildAt(i)
            val diff = Math.abs(getDecoratedLeft(child) - centerLeft)
            if (diff < lastDiff) {
                lastDiff = diff
                result = child
            }
        }

        return result
    }

    /**
     * Returns vertical center card.
     * @return View or null if card not found.
     */
    fun getVerticalAnchorView(header: HeaderLayout): View? {
        val centerTop = vPoint?.y ?: return null

        var result: View? = null
        var lastDiff = Int.MAX_VALUE

        for (i in 0 until header.childCount) {
            val child = header.getChildAt(i)
            val diff = Math.abs(getDecoratedTop(child) - centerTop)
            if (diff < lastDiff) {
                lastDiff = diff
                result = child
            }
        }

        return result
    }

    /**
     * Lays out view at specified position.
     * @param child View to lay out.
     * @param x Left position, relative to parent.
     * @param y Top position, relative to parent.
     * @param w View width.
     * @param h View height.
     */
    fun layoutChild(child: View, x: Int, y: Int, w: Int, h: Int) {
        val inset = getDecorInsetsForChild(child)

        val wPadding = inset.left + inset.right
        val hPadding = inset.top + inset.bottom
        val pw = w - wPadding
        val ph = h - hPadding

        val ws = View.MeasureSpec.makeMeasureSpec(pw, View.MeasureSpec.EXACTLY)
        val hs = View.MeasureSpec.makeMeasureSpec(ph, View.MeasureSpec.EXACTLY)
        child.measure(ws, hs)

        val l = x + inset.left
        val t = y + inset.top
        child.layout(l, t, l + pw, t + ph)
    }

    /**
     * Fill and redraws all cards in header.
     * @param header HeaderLayout.
     */
    fun fill(header: HeaderLayout) {
        val orientation = getOrientation(::getPositionRatio)
        if (orientation == Orientation.TRANSITIONAL) {
            return
        }

        val anchorPos = getAnchorPos(header)

        viewCache.clear()

        for (i in 0 until header.childCount) {
            val view = header.getChildAt(i)
            viewCache.put(HeaderLayout.getChildPosition(view), view)
        }

        for (i in 0 until viewCache.size()) {
            viewCache.valueAt(i)?.also { header.detachView(it) }
        }

        if (orientation == Orientation.HORIZONTAL) {
            fillLeft(header, anchorPos)
            fillRight(header, anchorPos)
        } else{
            fillTop(header, anchorPos)
            fillBottom(header, anchorPos)
        }

        for (i in 0 until viewCache.size()) {
            viewCache.valueAt(i)?.also { header.recycler.recycleView(it) }
        }

        val headerBottom = (header.y + header.height).toInt()
        updateListener.forEach { it.onHeaderUpdated(this, header, headerBottom) }
    }

    /**
     * Returns current center card, of current orientation.
     * @param header HeaderLayout.
     * @return View or null if card not found.
     */
    fun getAnchorView(header: HeaderLayout): View? {
        val orientation = getOrientation(::getPositionRatio)
        return when (orientation) {
            Orientation.HORIZONTAL -> getHorizontalAnchorView(header)
            Orientation.VERTICAL -> getVerticalAnchorView(header)
            Orientation.TRANSITIONAL -> null
        }
    }

    /**
     * Returns current center card, adapter position.
     * @param header HeaderLayout.
     * @return Current center card, adapter position or HeaderLayout.INVALID_POSITION if card not found.
     * @see HeaderLayout.INVALID_POSITION
     */
    fun getAnchorPos(header: HeaderLayout): Int {
        return restoredAnchorPosition
                ?.also { restoredAnchorPosition = null }
                ?: getAnchorView(header)
                ?.let { HeaderLayout.getChildPosition(it) }
                ?: HeaderLayout.INVALID_POSITION
    }

    /**
     * Returns child (card) decorated width.
     * @param child Header's card which decorated width needed.
     * @return child width + decorators width offsets.
     * @link ItemDecoration
     */
    fun getDecoratedWidth(child: View): Int {
        val width = child.width
        val inset = getDecorInsetsForChild(child)
        return width + inset.left + inset.right
    }

    /**
     * Returns child (card) decorated height.
     * @param child Header's card which decorated width needed.
     * @return child height + decorators height offsets.
     * @link ItemDecoration
     */
    fun getDecoratedHeight(child: View): Int {
        val height = child.height
        val inset = getDecorInsetsForChild(child)
        return height + inset.top + inset.bottom
    }

    /**
     * Returns child (card) decorated left position.
     * @param child Header's card which decorated left position needed.
     * @return child left - decorators left offsets.
     * @link ItemDecoration
     */
    fun getDecoratedLeft(child: View): Int {
        return child.left - getDecorInsetsForChild(child).left
    }

    /**
     * Returns child (card) decorated right position.
     * @param child Header's card which decorated right position needed.
     * @return child right + decorators right offsets.
     * @link ItemDecoration
     */
    fun getDecoratedRight(child: View): Int {
        return child.right + getDecorInsetsForChild(child).right
    }

    /**
     * Returns child (card) decorated top position.
     * @param child Header's card which decorated top position needed.
     * @return child top - decorators top offsets.
     * @link ItemDecoration
     */
    fun getDecoratedTop(child: View): Int {
        return child.top - getDecorInsetsForChild(child).top
    }

    /**
     * Returns child (card) decorated bottom position.
     * @param child Header's card which decorated bottom position needed.
     * @return child bottom + decorators bottom offsets.
     * @link ItemDecoration
     */
    fun getDecoratedBottom(child: View): Int {
        return child.bottom + getDecorInsetsForChild(child).bottom
    }

    internal fun addItemDecoration(decoration: ItemDecoration) {
        itemDecorations += decoration
        markItemDecorInsetsDirty()
    }

    internal fun removeItemDecoration(decoration: ItemDecoration) {
        itemDecorations -= decoration
        markItemDecorInsetsDirty()
    }

    internal fun onHeaderItemClick(header: HeaderLayout, viewHolder: HeaderLayout.ViewHolder): Boolean {
        return when {
            header.isHorizontalScrollEnabled -> {
                smoothScrollToPosition(viewHolder.position)
                itemClickListeners.forEach { it.onItemClicked(viewHolder) }
                true
            }
            header.isVerticalScrollEnabled -> {
                smoothOffset(workMiddle)
                itemClickListeners.forEach { it.onItemClicked(viewHolder) }
                true
            }
            else -> false
        }
    }

    private fun scrollToPosition(pos: Int,
                                 task: (header: HeaderLayout,
                                        anchorView: View,
                                        offset: Int,
                                        horizontal: Boolean) -> Unit)
    {
        if (offsetAnimator?.isRunning == true) {
            return
        }

        val hx = hPoint?.x ?: return
        val vy = vPoint?.y ?: return

        val header = headerLayout ?: return
        if (header.childCount == 0) {
            return
        }

        val itemCount = header.adapter?.getItemCount() ?: -1
        if (pos < 0 || pos > itemCount) {
            return
        }

        val anchorView = getAnchorView(header) ?: return
        val anchorPos = HeaderLayout.getChildPosition(anchorView)
        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        itemChangeListeners.forEach { it.onItemChanged(pos) }

        val offset = when {
            header.isHorizontalScrollEnabled -> {
                val childWidth = getDecoratedWidth(anchorView)
                (pos - anchorPos) * childWidth + (getDecoratedLeft(anchorView) - hx)
            }
            header.isVerticalScrollEnabled -> {
                val childHeight = getDecoratedHeight(anchorView)
                (pos - anchorPos) * childHeight + (getDecoratedTop(anchorView) - vy)
            }
            else -> 0
        }

        if (offset == 0) {
            return
        }

        if (pos == anchorPos && header.isVerticalScrollEnabled) {
            val lastChild = header.getChildAt(header.childCount - 1)
            val lastChildPos = HeaderLayout.getChildPosition(lastChild)
            val lastChildIsLastItem = lastChildPos == itemCount - 1
            if (lastChildIsLastItem && lastChild.bottom <= header.height) {
                return
            }
        }

        task(header, anchorView, offset, header.isHorizontalScrollEnabled)
    }

    private fun markItemDecorInsetsDirty() {
        headerLayout?.also { header ->
            val childCount = header.childCount
            for (i in 0 until childCount) {
                HeaderLayout.getChildLayoutParams(header.getChildAt(i))
                        ?.let { it.decorRectValid = false }
            }
            header.recycler.markItemDecorInsetsDirty()
        }
    }

    private fun onHeaderDown(header: HeaderLayout): Boolean {
        if (header.childCount == 0) {
            return false
        }

        viewFlinger.stop()
        return true
    }

    private fun onHeaderUp(header: HeaderLayout) {
        if (scrollState != ScrollState.FLING) {
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
            val lastRight = getDecoratedRight(header.getChildAt(childCount - 1))
            val newRight = lastRight - distance
            if (newRight > header.width) distance.toInt() else lastRight - header.width
        } else {
            val firstLeft = getDecoratedLeft(header.getChildAt(0))
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
            val lastBottom = getDecoratedBottom(header.getChildAt(childCount - 1))
            if (lastBottom < header.height) {
                0
            } else {
                val newBottom = lastBottom - distance
                if (newBottom > header.height) distance.toInt() else lastBottom - header.height
            }
        } else {
            val firstTop = getDecoratedTop(header.getChildAt(0))
            if (firstTop > verticalScrollTopBorder) {
                0
            } else {
                val newTop = firstTop - distance
                if (newTop < verticalScrollTopBorder) distance.toInt() else firstTop - verticalScrollTopBorder
            }
        }

        for (i in 0 until childCount) {
            header.getChildAt(i).offsetTopAndBottom(-offset)
        }

        fill(header)
        return true
    }

    private fun onHeaderHorizontalFling(header: HeaderLayout, velocity: Float): Boolean {
        val startX = getAnchorView(header)?.left?.takeIf { it != 0 } ?: return false
        val (min, max) = if (startX < 0) (-horizontalTabWidth to 0) else (0 to horizontalTabWidth)
        viewFlinger.fling(startX, 0, velocity.toInt(), 0, min, max, 0, 0)
        return true
    }

    private fun onHeaderVerticalFling(header: HeaderLayout, velocity: Float): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        val flingUp = velocity < 0
        val itemCount = header.adapter?.getItemCount() ?: return false
        val startY = getStartY(header.getChildAt(0))
        val min = -itemCount * verticalTabHeight + header.height
        val max = if (flingUp) header.height else 0

        viewFlinger.fling(0, startY, 0, velocity.toInt(), 0, 0, min, max)

        return true
    }

    private fun getStartX(view: View): Int {
        return HeaderLayout.getChildViewHolder(view)
                ?.let { getDecoratedLeft(view) - it.position * horizontalTabWidth }
                ?: throw RuntimeException("View holder not found")
    }

    private fun getStartY(view: View): Int {
        return HeaderLayout.getChildViewHolder(view)
                ?.let { getDecoratedTop(view) - it.position * verticalTabHeight }
                ?: throw RuntimeException("View holder not found")
    }

    private fun updatePoints(header: HeaderLayout) {
        val vx = when (verticalGravity) {
            HeaderLayoutManager.VerticalGravity.LEFT -> 0
            HeaderLayoutManager.VerticalGravity.CENTER -> header.width - verticalTabWidth / 2
            HeaderLayoutManager.VerticalGravity.RIGHT -> header.width - verticalTabWidth
        }

        val totalHeight = (header.adapter?.getItemCount() ?: 0) * verticalTabHeight
        val vy = if (totalHeight > workHeight) {
            (workBottom / tabOnScreenCount) * centerIndex + verticalScrollTopBorder
        } else {
            (header.height - totalHeight) / 2
        }

        hPoint = Point(0, workMiddle)
        vPoint = Point(vx, vy)
    }

    private fun getPositionRatio() = appBar?.let { Math.max(0f, it.bottom / workBottom.toFloat()) } ?: 0f

    private tailrec fun getOrientation(getRatio: () -> Float, force: Boolean = false): Orientation {
        return if (force) {
            val ratio = getRatio()
            when {
                ratio <= 0.5f -> Orientation.HORIZONTAL
                ratio < 1 -> Orientation.TRANSITIONAL
                else -> Orientation.VERTICAL
            }.also {
                curOrientation = it
            }
        } else {
            curOrientation ?: getOrientation(getRatio, true)
        }
    }

    private fun fillLeft(header: HeaderLayout, anchorPos: Int) {
        val (hx, hy) = hPoint ?: return

        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        val top = appBar?.let { header.height - it.bottom } ?: hy
        val bottom = appBar?.bottom ?: horizontalTabHeight
        val leftDiff = hx - (viewCache.get(anchorPos)?.let { getDecoratedLeft(it) } ?: 0)

        var pos = Math.max(0, anchorPos - centerIndex - tabOffsetCount)
        var left = (hx - (anchorPos - pos) * horizontalTabWidth) - leftDiff

        while (pos < anchorPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, horizontalTabWidth, bottom)
            left = getDecoratedRight(view)
            pos++
        }
    }

    private fun fillRight(header: HeaderLayout, anchorPos: Int) {
        val (hx, hy) = hPoint ?: return

        val count = header.adapter?.getItemCount() ?: 0
        if (count == 0) {
            return
        }

        val startPos = when (anchorPos) {
            HeaderLayout.INVALID_POSITION -> 0
            else -> anchorPos
        }

        val top = appBar?.let { header.height - it.bottom } ?: hy
        val bottom = appBar?.bottom ?: horizontalTabHeight
        val maxPos = Math.min(count, startPos + centerIndex + 1 + tabOffsetCount)

        var pos = startPos
        var left  = if (header.childCount > 0) {
            getDecoratedRight(header.getChildAt(header.childCount - 1))
        } else {
            hx
        }

        while (pos <  maxPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, horizontalTabWidth, bottom)
            left = getDecoratedRight(view)
            pos++
        }
    }

    private fun fillTop(header: HeaderLayout, anchorPos: Int) {
        val (left, vy) = vPoint ?: return

        if (anchorPos == HeaderLayout.INVALID_POSITION) {
            return
        }

        var pos = Math.max(0, anchorPos - centerIndex - tabOffsetCount)
        val topDiff = viewCache.get(anchorPos)?.let { vy - getDecoratedTop(it) } ?: 0
        var top = (vy - (anchorPos - pos) * verticalTabHeight) - topDiff

        while (pos < anchorPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, verticalTabWidth, verticalTabHeight)
            top = getDecoratedBottom(view)
            pos++
        }
    }

    private fun fillBottom(header: HeaderLayout, anchorPos: Int) {
        val (left, vy) = vPoint ?: return

        val count = header.adapter?.getItemCount() ?: 0
        if (count == 0) {
            return
        }

        val startPos = when (anchorPos) {
            HeaderLayout.INVALID_POSITION -> 0
            else -> anchorPos
        }

        val maxPos = Math.min(count, startPos + centerIndex + 1 + tabOffsetCount)
        var pos = startPos

        var top  = if (header.childCount > 0) {
            getDecoratedBottom(header.getChildAt(header.childCount - 1))
        } else {
            vy
        }

        while (pos <  maxPos) {
            val view = getPlacedChildForPosition(header, pos, left, top, verticalTabWidth, verticalTabHeight)
            top = getDecoratedBottom(view)
            pos++
        }
    }

    private fun getPlacedChildForPosition(header: HeaderLayout, pos: Int, x: Int, y: Int, w: Int, h: Int): View {
        val cacheView = viewCache.get(pos)
        if (cacheView != null) {
            header.attachView(cacheView)
            viewCache.remove(pos)
            return cacheView
        }

        val view = header.recycler.getViewForPosition(pos)
        header.addView(view)
        layoutChild(view, x, y, w, h)
        return view
    }

    private fun checkIfOffsetChangingStopped() {
        val header = headerLayout ?: return

        isOffsetChanged = false
        isCheckingScrollStop = true

        val startOffset = appBarBehavior.topAndBottomOffset
        header.postOnAnimationDelayed({
            isCheckingScrollStop = false
            val currentOffset = appBarBehavior.topAndBottomOffset
            val scrollStopped = currentOffset == startOffset
            if (scrollStopped) {
                onOffsetChangingStopped(currentOffset)
            }
        }, SCROLL_STOP_CHECK_DELAY)
    }

    private fun onOffsetChangingStopped(offset: Int) {
        isHeaderChanging = false

        if (offset == prevOffset) {
            return
        }
        prevOffset = offset

        val header = headerLayout ?: return
        val appBar = appBar ?: return

        var hScrollEnable = false
        var vScrollEnable = false

        val invertedOffset = workBottom + offset
        when (invertedOffset) {
            workBottom -> {
                vScrollEnable = true
                isCanDrag = false
            }
            workMiddle -> {
                hScrollEnable = true
                isCanDrag = false
                header.postOnAnimation { smoothScrollToPosition(getAnchorPos(header)) }
            }
            workTop -> {
                hScrollEnable = true
                isCanDrag = true
                header.postOnAnimation { smoothScrollToPosition(getAnchorPos(header)) }
            }
            in workTop..(topSnapDistance - 1) -> {
                appBar.setExpanded(false, true)
            }
            in topSnapDistance..(bottomSnapDistance - 1) -> {
                smoothOffset(workMiddle, SNAP_ANIMATION_DURATION)
            }
            else -> {
                appBar.setExpanded(true, true)
            }
        }

        header.isHorizontalScrollEnabled = hScrollEnable
        header.isVerticalScrollEnabled = vScrollEnable
    }

    private fun smoothOffset(offset: Int, duration: Long = collapsingBySelectDuration.toLong()) {
        val header = headerLayout ?: return

        offsetAnimator?.cancel()

        offsetAnimator = ValueAnimator().also { animator ->
            animator.duration = duration
            animator.setIntValues(appBarBehavior.topAndBottomOffset, -offset)
            animator.addUpdateListener {
                val value = it.animatedValue as Int
                appBarBehavior.topAndBottomOffset = value
                appBar?.also { it.postOnAnimation { it.requestLayout() }}
            }
            animator.addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                    header.isHorizontalScrollEnabled = false
                    header.isVerticalScrollEnabled = false
                }
                override fun onAnimationEnd(animation: Animator?) {
                    this@HeaderLayoutManager.onOffsetChangingStopped(-offset)
                }
            })
            animator.start()
        }
    }

    private fun setScrollState(state: ScrollState) {
        if (scrollState == state) {
            return
        }

        scrollStateListeners.forEach { it.onScrollStateChanged(state) }
        scrollState = state
    }

    private fun getDecorInsetsForChild(child: View): Rect {
        val lp = HeaderLayout.getChildLayoutParams(child)
                ?: throw RuntimeException("Invalid layout params")

        val vh = lp.viewHolder
                ?: throw RuntimeException("No view holder")

        val decorRect = lp.decorRect
        if (!isHeaderChanging && lp.decorRectValid) {
            return decorRect
        }

        decorRect.set(0, 0, 0, 0)
        for (decoration in itemDecorations) {
            tempDecorRect.set(0, 0, 0, 0)
            decoration.getItemOffsets(tempDecorRect, vh)
            decorRect.left += tempDecorRect.left
            decorRect.top += tempDecorRect.top
            decorRect.right += tempDecorRect.right
            decorRect.bottom += tempDecorRect.bottom
        }
        lp.decorRectValid = true

        return decorRect
    }
}