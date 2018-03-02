package com.ramotion.navigationtoolbar.example.simple

import android.view.View
import com.ramotion.navigationtoolbar.DefaultItemTransformer
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import com.ramotion.navigationtoolbar.NavigationToolBarLayout
import kotlin.math.abs
import kotlin.math.min

class HeaderItemTransformer(
        private val mVerticalLeftOffset: Int,
        private val mHorizontalCenterOffsetRatio: Float)
    : DefaultItemTransformer() {

    private var mPrevChildCount = Int.MIN_VALUE
    private var mPrevHScrollOffset = Int.MIN_VALUE
    private var mPrevVScrollOffset = Int.MIN_VALUE
    private var mPrevHeaderBottom = Int.MIN_VALUE

    private var mTopTitlesAnimated = false
    private var mElevation: Float? = null

    override fun transform(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        super.transform(lm, header, headerBottom)

        val childCount = header.childCount
        if (childCount == 0) {
            return
        }

        val child0 = header.getChildAt(0)
        val hScrollOffset = child0.left
        val vScrollOffset = child0.top
        val nothingChanged = hScrollOffset == mPrevHScrollOffset && vScrollOffset == mPrevVScrollOffset
                && childCount == mPrevChildCount && mPrevHeaderBottom == headerBottom
        if (nothingChanged) {
            return
        }

        mPrevChildCount = childCount
        mPrevHScrollOffset = hScrollOffset
        mPrevVScrollOffset = vScrollOffset
        mPrevHeaderBottom = headerBottom

        val elevation = mElevation ?: run { mElevation = child0.elevation; mElevation!! }

        if (mCurrentRatioTopHalf in 0f..1f && mCurrentRatioBottomHalf == 0f) {
            var curZ = childCount / 2f + elevation
            var prevZDiff = Int.MAX_VALUE
            for (i in 0 until childCount) {
                val item = header.getChildAt(i)
                val holder = HeaderLayout.getChildViewHolder(item) as HeaderItem

                val headerCenter = header.width / 2
                val itemCenter = item.left + item.width / 2
                val headerCenterDiff = itemCenter - headerCenter
                val itemNewCenter = headerCenter + headerCenterDiff * mHorizontalCenterOffsetRatio
                val itemNewCenterDiff = itemNewCenter - itemCenter

                val hcDiff = abs(headerCenterDiff)
                curZ += if (prevZDiff > hcDiff) { prevZDiff = hcDiff; -1f } else { prevZDiff = hcDiff; 1f }
                item.z = curZ

                val titleInitialLeft = item.width / 2 - holder.mTitle.width / 2
                val titleNewLeft = titleInitialLeft + itemNewCenterDiff * mCurrentRatioTopHalf
                val ratio = 1.5f - min(headerCenter.toFloat(), abs(headerCenter - itemNewCenter)) / headerCenter

                holder.mTitle.also {
                    if (mCurrentRatioTopHalf > 0.5f && !mTopTitlesAnimated) {
                        it.animate().x(titleNewLeft).start()
                    } else {
                        it.x = titleNewLeft
                    }
                    transformTitle(it, ratio)
                }
            }
            mTopTitlesAnimated = true
        } else if (mCurrentRatioBottomHalf in 0f .. 1f && mCurrentRatioTopHalf == 1f) {
            for (i in 0 until childCount) {
                val item = header.getChildAt(i)
                val holder = HeaderLayout.getChildViewHolder(item) as HeaderItem

                item.z = elevation

                val itemWidth = item.width
                val titleInitialLeft = itemWidth / 2 - holder.mTitle.width / 2
                val titleNewLeft = titleInitialLeft - abs(titleInitialLeft - mVerticalLeftOffset) * mCurrentRatioBottomHalf

                holder.mTitle.also {
                    it.x = titleNewLeft
                    transformTitle(it, 1f)
                }
            }
            mTopTitlesAnimated = false
        }
    }

    private fun transformTitle(view: View, ratio: Float) {
        view.alpha = ratio
        view.scaleX = min(1f, ratio)
        view.scaleY = view.scaleX
    }
}