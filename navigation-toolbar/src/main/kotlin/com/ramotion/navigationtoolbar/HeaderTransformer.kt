package com.ramotion.navigationtoolbar

import android.graphics.PointF
import android.util.Log
import com.ramotion.navigationtoolbar.HeaderLayoutManager.Orientation


class HeaderTransformer : HeaderLayoutManager.DefaultItemsTransformer() {

    private var mIsEmptyHeader = false
    private var mHPoints: MutableList<PointF> = mutableListOf()
    private var mVPoints: MutableList<PointF> = mutableListOf()

    override fun onOffsetChangeStarted(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {
        super.onOffsetChangeStarted(header, lm, ratio, orientRatio)

        mIsEmptyHeader = header.childCount == 0
        if (mIsEmptyHeader) {
            return
        }

        if (mStartOrientation == Orientation.VERTICAL) {
            val index = getVerticalAnchorChildIndex(header, lm)
            if (index == HeaderLayout.INVALID_POSITION) {
                mIsEmptyHeader = true
                return
            }

            val left = -index * lm.mHorizontalTabWidth
            val hp = lm.getPoints().first
            for (i in 0 until header.childCount) {
                mVPoints.add(header.getChildAt(i).let { PointF(it.x, it.y) })
                mHPoints.add(PointF(hp.x + left + i * lm.mHorizontalTabWidth, hp.y))
            }
        }

        if (mStartOrientation == Orientation.HORIZONTAL) {
            // TODO: implement
        }
    }

    override fun onOffsetChangeStopped(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {
        super.onOffsetChangeStopped(header, lm, ratio, orientRatio)

        mHPoints.clear()
        mVPoints.clear()

        if (mIsEmptyHeader) {
            return
        }
    }

    override fun onOffsetChanged(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {
        super.onOffsetChanged(header, lm, ratio, orientRatio)

        if (mIsEmptyHeader) {
            return
        }

        val hw = lm.mHorizontalTabWidth
        val hh = lm.mHorizontalTabHeight
        val vw = lm.mVerticalTabWidth
        val vh = lm.mVerticalTabHeight

        val newWidth = hw - (hw - vw) * orientRatio
        val newHeight = hh - (hh - vh) * orientRatio

        if (mStartOrientation == Orientation.VERTICAL) {
            for (i in 0 until mHPoints.size) {
                val hp = mHPoints[i]
                val vp = mVPoints[i]
                val hDiff = (vp.x - hp.x) * orientRatio
                val vDiff = (vp.y - hp.y) * orientRatio

                val x = (hp.x + hDiff).toInt()
                val y = (hp.y + vDiff).toInt()
                lm.layoutChild(header.getChildAt(i), x, y, newWidth.toInt(), newHeight.toInt())
            }
        }

        if (mStartOrientation == Orientation.HORIZONTAL) {
            // TODO: implement
        }
    }

    private fun getVerticalAnchorChildIndex(header: HeaderLayout, lm: HeaderLayoutManager): Int {
        val clickIndex = lm.getClickedChildIndex()
        return if (clickIndex != HeaderLayout.INVALID_POSITION) {
            clickIndex
        } else {
            lm.getVerticalAnchorView(header)?.let { header.indexOfChild(it) } ?: HeaderLayout.INVALID_POSITION
        }
    }

}