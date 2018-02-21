package com.ramotion.navigationtoolbar

import android.graphics.PointF
import kotlin.math.max
import kotlin.math.min

open class DefaultItemTransformer
    : HeaderLayoutManager.ItemTransformer, HeaderLayoutManager.ItemClickListener {

    private val mHPoints: MutableList<PointF> = mutableListOf()
    private val mVPoints: MutableList<PointF> = mutableListOf()

    private var mLayoutManager: HeaderLayoutManager? = null
    private var mHeaderLayout: HeaderLayout? = null

    private var mRatioWork = 0f
    private var mRatioTopHalf = 0f
    private var mRatioBottomHalf = 0f

    private var mClickedItemIndex: Int? = null
    private var mPrevItemCount: Int? = null

    protected var mCurrentRatio = -1f; private set
    protected var mCurrentRatioWork = -1f; private set
    protected var mCurrentRatioTopHalf = -1f; private set
    protected var mCurrentRatioBottomHalf = -1f; private set

    override fun attach(lm: HeaderLayoutManager, header: HeaderLayout) {
        mLayoutManager = lm
        mHeaderLayout = header

        mRatioWork = lm.mWorkHeight / lm.mScreenHeight.toFloat()
        mRatioTopHalf = lm.mToolBarHeight / lm.mScreenHeight.toFloat()
        mRatioBottomHalf = lm.mScreenHalf / lm.mScreenHeight.toFloat()

        lm.addItemClickListener(this)
    }

    override fun detach() {
        mLayoutManager?.removeItemClickListener(this)

        mLayoutManager = null
        mHeaderLayout = null
    }

    override fun transform(headerBottom: Int) {
        val lm = mLayoutManager ?: return
        val header = mHeaderLayout ?: return

        val prevRatio = mCurrentRatio
        val prevRatioTopHalf = mCurrentRatioTopHalf
        val prevRatioBottomHalf = mCurrentRatioBottomHalf

        val prevItemCount = mPrevItemCount ?: 0
        val curItemCount = mHeaderLayout?.childCount ?: 0
        mPrevItemCount = curItemCount

        updateRatios(headerBottom)

        val nothingChanged = prevRatio == mCurrentRatio && prevItemCount == curItemCount
        if (nothingChanged) {
            return
        }

        var transformed = false

        // On scroll from top (top half) to bottom (bottom half)
        val expandedToTopOfBottomHalf = mCurrentRatioTopHalf == 1f
                && prevRatioTopHalf < mCurrentRatioTopHalf && prevRatioTopHalf != -1f
        if (expandedToTopOfBottomHalf) {
            transformTopHalf(lm, header, headerBottom)
            updatePoints(false)
            transformBottomHalf(lm, header)
            transformed = true
        } else {
            // On scroll from top to bottom
            val expandedToBottomOfBottomHalf = mCurrentRatioBottomHalf == 1f
                    && prevRatioBottomHalf <= mCurrentRatioBottomHalf
            if (expandedToBottomOfBottomHalf) {
                transformBottomHalf(lm, header)
                clearPoints()
                transformed = true
        } else {
            // On scroll from bottom to top
            val collapsedToTopOfBottomHalf = mCurrentRatioBottomHalf == 0f
                    && prevRatioBottomHalf > mCurrentRatioBottomHalf
            if (collapsedToTopOfBottomHalf) {
                transformBottomHalf(lm, header)
                transformTopHalf(lm, header, headerBottom)
                mHeaderLayout?.let { mLayoutManager?.fill(it) }
                updatePoints(false)
                transformed = true
        } else {
            val collapsedToTopOfTopHalf = mCurrentRatioTopHalf == 0f
                    && prevRatioTopHalf > mCurrentRatioTopHalf && prevRatioTopHalf != -1f
            if (collapsedToTopOfTopHalf) {
                transformTopHalf(lm, header, headerBottom)
                clearPoints()
                transformed = true
            }
        }}}

        if (!transformed) {
            val isAtBottomHalf = mCurrentRatioBottomHalf > 0f && mCurrentRatioBottomHalf < 1f
            if (isAtBottomHalf) {
                transformBottomHalf(lm, header)
            } else {
                transformTopHalf(lm, header, headerBottom)
            }
        }
    }

    override fun onItemClick(viewHolder: HeaderLayout.ViewHolder) {
        mClickedItemIndex = mHeaderLayout?.indexOfChild(viewHolder.view)
        updatePoints(true)
    }

    private fun updateRatios(headerBottom: Int) {
        val lm = mLayoutManager ?: return

        mCurrentRatio = max(0f, headerBottom / lm.mScreenHeight.toFloat())
        mCurrentRatioWork = max(0f, (headerBottom - lm.mToolBarHeight) / lm.mWorkHeight.toFloat())
        mCurrentRatioTopHalf = max(0f, 1 - (mRatioBottomHalf - min(max(mCurrentRatio, mRatioTopHalf), mRatioBottomHalf)) / (mRatioBottomHalf - mRatioTopHalf))
        mCurrentRatioBottomHalf = max(0f, (mCurrentRatio - mRatioBottomHalf) / mRatioBottomHalf)
    }

    private fun updatePoints(up: Boolean) {
        val lm = mLayoutManager ?: return
        val header = mHeaderLayout ?: return

        val index = if (up) {
            mClickedItemIndex ?: throw RuntimeException("No vertical (clicked) item index")
        } else {
            lm.getHorizontalAnchorView(header)?.let { header.indexOfChild(it) }
                    ?: throw RuntimeException("No horizontal item index")
        }

        clearPoints()

        if (up) {
            val left = -index * lm.mHorizontalTabWidth
            val (x, y) = lm.getPoints().first.run { x to y }

            for (i in 0 until header.childCount) {
                mVPoints.add(header.getChildAt(i).let { PointF(it.x, it.y) })
                mHPoints.add(PointF(x + left + i * lm.mHorizontalTabWidth, y))
            }
        } else {
            val top = -index * lm.mVerticalTabHeight
            val (x, y) = lm.getPoints().second.run { x to y }

            for (i in 0 until header.childCount) {
                mHPoints.add(header.getChildAt(i).let { PointF(it.x, it.y) })
                mVPoints.add(PointF(x, y + top + i * lm.mVerticalTabHeight))
            }
        }
    }

    private fun clearPoints() {
        mHPoints.clear()
        mVPoints.clear()
    }

    private fun transformTopHalf(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        val top = header.height - headerBottom
        (0 until header.childCount)
                .map { header.getChildAt(it) }
                .forEach { lm.layoutChild(it, it.left, top, it.width, headerBottom) }
    }

    private fun transformBottomHalf(lm: HeaderLayoutManager, header: HeaderLayout) {
        val hw = lm.mHorizontalTabWidth
        val hh = lm.mHorizontalTabHeight
        val vw = lm.mVerticalTabWidth
        val vh = lm.mVerticalTabHeight

        val newWidth = hw - (hw - vw) * mCurrentRatioBottomHalf
        val newHeight = hh - (hh - vh) * mCurrentRatioBottomHalf

        val count = min(header.childCount, mHPoints.size)
        for (i in 0 until count) {
            val hp = mHPoints[i]
            val vp = mVPoints[i]
            val hDiff = (vp.x - hp.x) * mCurrentRatioBottomHalf
            val vDiff = (vp.y - hp.y) * mCurrentRatioBottomHalf

            val x = (hp.x + hDiff).toInt()
            val y = (hp.y + vDiff).toInt()
            lm.layoutChild(header.getChildAt(i), x, y, newWidth.toInt(), newHeight.toInt())
        }
    }

}