package com.ramotion.navigationtoolbar.example.simple

import android.view.View
import com.ramotion.navigationtoolbar.DefaultItemTransformer
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import kotlin.math.abs
import kotlin.math.min

class HeaderItemTransformer(
        private val mVerticalLeftOffset: Int,
        private val mHorizontalCenterOffsetRatio: Float)
    : DefaultItemTransformer() {

    private var mLayoutManager: HeaderLayoutManager? = null
    private var mHeaderLayout: HeaderLayout? = null

    override fun attach(lm: HeaderLayoutManager, header: HeaderLayout) {
        super.attach(lm, header)

        mLayoutManager = lm
        mHeaderLayout = header
    }

    override fun transform(headerBottom: Int) {
        super.transform(headerBottom)

        val header = mHeaderLayout ?: return
        val childCount = header.childCount
        if (childCount == 0) {
            return
        }

        if (mCurrentRatioTopHalf in 0f..1f && mCurrentRatioBottomHalf == 0f) {
            var curZ = childCount / 2f
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
                transformTitle(holder.mTitle, titleNewLeft, ratio)

//                Log.d("D", "i: $i, z: ${item.z}, ic: $itemCenter, hcd: $headerCenterDiff, inc: $itemNewCenter, incd: $itemNewCenterDiff, a: $ratio")
            }
        } else if (mCurrentRatioBottomHalf in 0f .. 1f && mCurrentRatioTopHalf == 1f) {
            for (i in 0 until childCount) {
                val item = header.getChildAt(i)
                val holder = HeaderLayout.getChildViewHolder(item) as HeaderItem

                item.z = 0f

                val itemWidth = item.width
                val titleInitialLeft = itemWidth / 2 - holder.mTitle.width / 2
                val titleNewLeft = titleInitialLeft - abs(titleInitialLeft - mVerticalLeftOffset) * mCurrentRatioBottomHalf

                transformTitle(holder.mTitle, titleNewLeft,1f)
            }
        }
    }

    private fun transformTitle(view: View, x: Float, ratio: Float) {
        view.x = x
        view.alpha = ratio
        view.scaleX = min(1f, ratio)
        view.scaleY = view.scaleX
    }
}