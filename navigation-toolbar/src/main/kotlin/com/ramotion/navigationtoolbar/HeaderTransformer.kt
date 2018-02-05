package com.ramotion.navigationtoolbar

import android.graphics.PointF
import android.util.Log
import com.ramotion.navigationtoolbar.HeaderLayoutManager.Orientation


class HeaderTransformer : HeaderLayoutManager.DefaultItemsTransformer() {

    private var mIsEmptyHeader = false
    private var mOffset: Float = 0f
    private var mHPoints: List<PointF> = emptyList()
    private var mVPoints: List<PointF> = mHPoints

    override fun onOffsetChangeStarted(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {
        super.onOffsetChangeStarted(header, lm, ratio, orientRatio)
        Log.d("D", "onOffsetChangeStarted| ratio: $ratio, orientRatio: $orientRatio")

        mIsEmptyHeader = header.childCount == 0
        if (mIsEmptyHeader) {
            return
        }

        lm.getPoints().let {
            mHPoints = it.first
            mVPoints = it.second
        }

        if (mStartOrientation == Orientation.VERTICAL) {
            val clickIndex = lm.getClickedChildIndex()
            val index = if (clickIndex != HeaderLayout.INVALID_POSITION) {
                clickIndex
            } else {
                lm.getVerticalAnchorView(header)?.let { header.indexOfChild(it) } ?: HeaderLayout.INVALID_POSITION
            }

            if (index == HeaderLayout.INVALID_POSITION) {
                mIsEmptyHeader = true
                return
            }

            // TODO: get offset of index view position to... to points[mCenterIndex]
            mOffset = mVPoints[lm.getCenterIndex()].y - header.getChildAt(clickIndex).y
            Log.d("D", "mOffset: $mOffset")
        }

        if (mStartOrientation == Orientation.HORIZONTAL) {
            // TODO: implement
        }
    }

    override fun onOffsetChangeStopped(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {
        super.onOffsetChangeStopped(header, lm, ratio, orientRatio)
        Log.d("D", "onOffsetChangeStopped| r: $ratio, or: $orientRatio, sp: $mStartOrientation")

        mOffset = 0f
        mHPoints = emptyList()
        mVPoints = mHPoints

        if (mIsEmptyHeader) {
            return
        }
    }

    override fun onOffsetChanged(header: HeaderLayout, lm: HeaderLayoutManager, ratio: Float, orientRatio: Float) {
        super.onOffsetChanged(header, lm, ratio, orientRatio)
        Log.d("D", "onOffsetChanged| ratio: $ratio, orientRatio: $orientRatio")

        if (mIsEmptyHeader) {
            return
        }
    }

}