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

    private var mClickedItem: Int? = null

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

        updateRatios(headerBottom)

        val nothingChanged = prevRatio == mCurrentRatio
        if (nothingChanged) {
            Log.d("D", "transform| nothing changed")
            return
        }

        // On scroll from top (top half) to bottom (bottom half)
        val expandedToTopOfBottomHalf = mCurrentRatioTopHalf == 1f
                && prevRatioTopHalf < mCurrentRatioTopHalf && prevRatioTopHalf != 0f
        if (expandedToTopOfBottomHalf) {
            Log.d("D", "------------------------------------")
            Log.d("D", "|transform| reached middle from top|")
            Log.d("D", "------------------------------------")
            updatePoints(false)
            transformBottomHalf()
        }

        Log.d("D", "transform| mCurrentRatioBottomHalf: $mCurrentRatioBottomHalf")
        // On scroll from top to bottom
        val expandedToBottomOfBottomHalf = mCurrentRatioBottomHalf == 1f
                && prevRatioBottomHalf < mCurrentRatioBottomHalf && prevRatioBottomHalf != 0f
        if (expandedToBottomOfBottomHalf) {
            Log.d("D", "---------------------------")
            Log.d("D", "|transform| reached bottom|")
            Log.d("D", "---------------------------")
            transformBottomHalf()
            clearPoints()
        }

        // On scroll from bottom to top
        val collapsedToTopOfBottomHalf = mCurrentRatioBottomHalf == 0f
                && prevRatioBottomHalf > mCurrentRatioBottomHalf
        if (collapsedToTopOfBottomHalf) {
            Log.d("D", "---------------------------------------")
            Log.d("D", "|transform| reached middle from bottom|")
            Log.d("D", "---------------------------------------")
            transformBottomHalf()
            transformTopHalf()
            clearPoints()
        }

        val collapsedToTopOfTopHalf = mCurrentRatioTopHalf == 0f
                && prevRatioTopHalf > mCurrentRatioTopHalf && prevRatioTopHalf != 0f
        if (collapsedToTopOfTopHalf) {
            Log.d("D", "------------------------")
            Log.d("D", "|transform| reached top|")
            Log.d("D", "------------------------")
            transformTopHalf()
        }

        val transformed = expandedToBottomOfBottomHalf || expandedToTopOfBottomHalf
                || collapsedToTopOfBottomHalf || collapsedToTopOfTopHalf
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
        mClickedItem = mHeaderLayout?.indexOfChild(viewHolder.view)
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
        val index = lm.getAnchorView(header)?.let { header.indexOfChild(it) } ?: return

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
        Log.d("D", "transformTopHalf")
    }

    private fun transformBottomHalf() {
        Log.d("D", "transformBottomHalf")
    }

}