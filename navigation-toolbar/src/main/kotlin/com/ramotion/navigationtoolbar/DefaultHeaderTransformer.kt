package com.ramotion.navigationtoolbar

import android.graphics.PointF
import android.util.Log
import kotlin.math.max
import kotlin.math.min

open class DefaultHeaderTransformer
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

    protected var mCurrentRatio = 0f; private set
    protected var mCurrentRatioWork = 0f; private set
    protected var mCurrentRatioTopHalf = 0f; private set
    protected var mCurrentRatioBottomHalf = 0f; private set

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
        val prevRatio = mCurrentRatio
        val prevRatioTopHalf = mCurrentRatioTopHalf
        val prevRatioBottomHalf = mCurrentRatioBottomHalf

        val prevItemCount = mPrevItemCount ?: 0
        val curItemCount = mHeaderLayout?.childCount ?: 0
        mPrevItemCount = curItemCount

        updateRatios(headerBottom)

        val nothingChanged = prevRatio == mCurrentRatio && prevItemCount == curItemCount
        if (nothingChanged) {
            Log.d("D", "transform| nothing changed")
            return
        }

        var transformed = false

        // On scroll from top (top half) to bottom (bottom half)
        val expandedToTopOfBottomHalf = mCurrentRatioTopHalf == 1f
                && prevRatioTopHalf < mCurrentRatioTopHalf && prevRatioTopHalf != 0f
        if (expandedToTopOfBottomHalf) {
            transformBottomHalf()
            updatePoints(false)
            transformed = true
        }

        // On scroll from top to bottom
        val expandedToBottomOfBottomHalf = mCurrentRatioBottomHalf == 1f
                && prevRatioBottomHalf < mCurrentRatioBottomHalf && prevRatioBottomHalf != 0f
        if (expandedToBottomOfBottomHalf) {
            transformBottomHalf()
            clearPoints()
            transformed = true
        }

        // On scroll from bottom to top
        val collapsedToTopOfBottomHalf = mCurrentRatioBottomHalf == 0f
                && prevRatioBottomHalf > mCurrentRatioBottomHalf
        if (collapsedToTopOfBottomHalf) {
            transformBottomHalf()
            transformTopHalf()
            mHeaderLayout?.let { mLayoutManager?.fill(it) }
            updatePoints(false)
            transformed = true
        }

        val collapsedToTopOfTopHalf = mCurrentRatioTopHalf == 0f
                && prevRatioTopHalf > mCurrentRatioTopHalf && prevRatioTopHalf != 0f
        if (collapsedToTopOfTopHalf) {
            transformTopHalf()
            clearPoints()
            transformed = true
        }

        if (!transformed) {
            val isAtBottomHalf = mCurrentRatioBottomHalf > 0f && mCurrentRatioBottomHalf < 1f
            if (isAtBottomHalf) {
                transformBottomHalf()
            } else {
                transformTopHalf()
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
        Log.d("D", "updatePoints| ${if(up) "up" else " down"}")

        val lm = mLayoutManager ?: return
        val header = mHeaderLayout ?: return

        val index = if (up) {
            mClickedItemIndex ?: throw RuntimeException("No vertical (clicked) item index")
        } else {
            lm.getAnchorView(header)?.let { header.indexOfChild(it) }
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
        Log.d("D", "clearPoints")
        mHPoints.clear()
        mVPoints.clear()
    }

    private fun transformTopHalf() {
    }

    private fun transformBottomHalf() {
        val lm = mLayoutManager ?: return
        val header = mHeaderLayout ?: return

        val hw = lm.mHorizontalTabWidth
        val hh = lm.mHorizontalTabHeight
        val vw = lm.mVerticalTabWidth
        val vh = lm.mVerticalTabHeight

        val newWidth = hw - (hw - vw) * mCurrentRatioBottomHalf
        val newHeight = hh - (hh - vh) * mCurrentRatioBottomHalf

        for (i in 0 until mHPoints.size) {
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