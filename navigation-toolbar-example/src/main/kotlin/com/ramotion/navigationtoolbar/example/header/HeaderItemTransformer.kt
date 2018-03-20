package com.ramotion.navigationtoolbar.example.header

import android.support.constraint.ConstraintLayout
import android.view.View
import com.ramotion.navigationtoolbar.DefaultItemTransformer
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import kotlin.math.abs
import kotlin.math.min

class HeaderItemTransformer(
        private val horizontalTopOffset: Int,
        private val verticalLeftOffset: Int,
        private val horizontalCenterOffsetRatio: Float) : DefaultItemTransformer() {

    private var prevChildCount = Int.MIN_VALUE
    private var prevHScrollOffset = Int.MIN_VALUE
    private var prevVScrollOffset = Int.MIN_VALUE
    private var prevHeaderBottom = Int.MIN_VALUE

    private var topTitlesAnimated = false
    private var elevation: Float? = null

    override fun transform(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        super.transform(lm, header, headerBottom)

        val childCount = header.childCount
        if (childCount == 0) {
            return
        }

        val child0 = header.getChildAt(0)
        val hScrollOffset = child0.left
        val vScrollOffset = child0.top
        val nothingChanged = hScrollOffset == prevHScrollOffset && vScrollOffset == prevVScrollOffset
                && childCount == prevChildCount && prevHeaderBottom == headerBottom
        if (nothingChanged) {
            return
        }

        prevChildCount = childCount
        prevHScrollOffset = hScrollOffset
        prevVScrollOffset = vScrollOffset
        prevHeaderBottom = headerBottom

        val initialZ = this.elevation ?: run { child0.elevation.also { this.elevation = it }}

        if (currentRatioTopHalf in 0f..1f && currentRatioBottomHalf == 0f) {
            val invertedRatio = 1f - currentRatioTopHalf

            var curZ = childCount / 2f + initialZ
            var prevZDiff = Int.MAX_VALUE
            for (i in 0 until childCount) {
                val item = header.getChildAt(i)
                val holder = HeaderLayout.getChildViewHolder(item) as HeaderItem

                val headerCenter = header.width / 2
                val itemCenter = item.left + item.width / 2
                val headerCenterDiff = itemCenter - headerCenter
                val itemNewCenter = headerCenter + headerCenterDiff * horizontalCenterOffsetRatio
                val itemNewCenterDiff = itemNewCenter - itemCenter

                val hcDiff = abs(headerCenterDiff)
                curZ += if (prevZDiff > hcDiff) { prevZDiff = hcDiff; -1f } else { prevZDiff = hcDiff; 1f }
                item.z = curZ

                val titleInitialLeft = item.width / 2 - holder.title.width / 2
                val titleNewLeft = titleInitialLeft + itemNewCenterDiff * currentRatioTopHalf
                val ratio = 1.5f - min(headerCenter.toFloat(), abs(headerCenter - itemNewCenter)) / headerCenter

                holder.title.also {
                    if (currentRatioTopHalf > 0.5f && !topTitlesAnimated) {
                        it.animate().x(titleNewLeft).start()
                    } else {
                        it.x = titleNewLeft
                    }

                    val lp = it.layoutParams as ConstraintLayout.LayoutParams
                    lp.topMargin = (horizontalTopOffset * invertedRatio).toInt()
                    it.requestLayout()

                    transformTitle(it, ratio)
                }
            }
            topTitlesAnimated = true
        } else if (currentRatioBottomHalf in 0f .. 1f && currentRatioTopHalf == 1f) {
            for (i in 0 until childCount) {
                val item = header.getChildAt(i)
                val holder = HeaderLayout.getChildViewHolder(item) as HeaderItem

                item.z = initialZ

                val itemWidth = item.width
                val titleInitialLeft = itemWidth / 2 - holder.title.width / 2
                val titleNewLeft = titleInitialLeft - abs(titleInitialLeft - verticalLeftOffset) * currentRatioBottomHalf

                holder.title.also {
                    it.x = titleNewLeft
                    transformTitle(it, 1f)
                }
            }
            topTitlesAnimated = false
        }
    }

    private fun transformTitle(view: View, ratio: Float) {
        view.alpha = ratio
        view.scaleX = min(1f, ratio)
        view.scaleY = view.scaleX
    }
}