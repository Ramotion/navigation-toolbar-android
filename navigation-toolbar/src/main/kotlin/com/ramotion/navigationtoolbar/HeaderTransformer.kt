package com.ramotion.navigationtoolbar

import android.graphics.PointF
import com.ramotion.navigationtoolbar.HeaderLayoutManager.Orientation


class HeaderTransformer : HeaderLayoutManager.DefaultItemsTransformer() {

    private var mIsEmptyHeader = false

    private var mIsTransformTop = false
    private var mIsTransformMiddle = false

    private var mMiddleStartOrientation = Orientation.TRANSITIONAL
    private var mHPoints: MutableList<PointF> = mutableListOf()
    private var mVPoints: MutableList<PointF> = mutableListOf()

    private var mAppBarBottom: Int = 0

    override fun transform(header: HeaderLayout, lm: HeaderLayoutManager, appBarBottom: Int) {
        super.transform(header, lm, appBarBottom)
        mAppBarBottom = appBarBottom
    }

    override fun onOffsetChanged(header: HeaderLayout, lm: HeaderLayoutManager) {
        mIsEmptyHeader = header.childCount == 0
        if (mIsEmptyHeader) {
            return
        }

        val isTransformMiddleStarted = !mIsTransformMiddle && mCurrentRatioMiddle > 0f
        val isTransformMiddleStopped = mIsTransformMiddle && (mCurrentRatioMiddle == 0f || mCurrentRatioMiddle == 1f)
        val isTransformTopStarted = !mIsTransformTop && mCurrentRatioTop > 0f && mCurrentRatioTop < 1f
        val isTransformTopStopped = mIsTransformTop && (mCurrentRatioTop == 0f || mCurrentRatioTop == 1f)

        if (isTransformMiddleStopped) onTransformMiddleStopped(header, lm)
        else if (isTransformTopStopped) onTransformTopStopped(header, lm)

        if (isTransformMiddleStarted) onTransformMiddleStarted(header, lm)
        else if (isTransformTopStarted) onTransformTopStarted(header, lm)

        if (mIsTransformMiddle || mIsTransformTop) onTransform(header, lm)
    }

    private fun onTransformMiddleStarted(header: HeaderLayout, lm: HeaderLayoutManager) {
        mIsTransformMiddle = true
        mMiddleStartOrientation = if (mCurrentRatioMiddle >= 0.5) Orientation.VERTICAL else Orientation.HORIZONTAL

        if (mMiddleStartOrientation == Orientation.VERTICAL) {
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
        } else if (mMiddleStartOrientation == Orientation.HORIZONTAL) {
            /*  TODO:
                Fix for last adapter item - it map to vertical center, but must be mapped to bottom.
                Start smooth scroll down or map to bottom.
              */
            val index = getHorizontalAnchorChildIndex(header, lm)
            if (index == HeaderLayout.INVALID_POSITION) {
                mIsEmptyHeader = true
                return
            }

            val top = -index * lm.mVerticalTabHeight
            val vp = lm.getPoints().second
            for (i in 0 until header.childCount) {
                mHPoints.add(header.getChildAt(i).let { PointF(it.x, it.y) })
                mVPoints.add(PointF(vp.x, vp.y + top + i * lm.mVerticalTabHeight))
            }
        }
    }

    private fun onTransformMiddleStopped(header: HeaderLayout, lm: HeaderLayoutManager) {
        if (mCurrentRatioMiddle == 1f) { onTransform(header, lm) }
        mIsTransformMiddle = false
        mHPoints.clear()
        mVPoints.clear()
        lm.fill(header)
    }

    private fun onTransformTopStarted(header: HeaderLayout, lm: HeaderLayoutManager) {
        mIsTransformTop = true
    }

    private fun onTransformTopStopped(header: HeaderLayout, lm: HeaderLayoutManager) {
        if (mCurrentRatioTop == 0f) { onTransform(header, lm) }
        mIsTransformTop = false
    }

    private fun onTransform(header: HeaderLayout, lm: HeaderLayoutManager) {
        if (mIsEmptyHeader) {
            return
        }

        if (mIsTransformTop) {
            (0 until header.childCount)
                    .map { header.getChildAt(it) }
                    .forEach { lm.layoutChild(it, it.left, header.height - mAppBarBottom, it.width, mAppBarBottom) }
        } else if (mIsTransformMiddle) {
            val hw = lm.mHorizontalTabWidth
            val hh = lm.mHorizontalTabHeight
            val vw = lm.mVerticalTabWidth
            val vh = lm.mVerticalTabHeight

            val newWidth = hw - (hw - vw) * mCurrentRatioMiddle
            val newHeight = hh - (hh - vh) * mCurrentRatioMiddle

            for (i in 0 until mHPoints.size) {
                val hp = mHPoints[i]
                val vp = mVPoints[i]
                val hDiff = (vp.x - hp.x) * mCurrentRatioMiddle
                val vDiff = (vp.y - hp.y) * mCurrentRatioMiddle

                val x = (hp.x + hDiff).toInt()
                val y = (hp.y + vDiff).toInt()
                lm.layoutChild(header.getChildAt(i), x, y, newWidth.toInt(), newHeight.toInt())
            }
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

    private fun getHorizontalAnchorChildIndex(header: HeaderLayout, lm: HeaderLayoutManager): Int {
        return lm.getHorizontalAnchorView(header)?.let { header.indexOfChild(it) } ?: HeaderLayout.INVALID_POSITION
    }

}