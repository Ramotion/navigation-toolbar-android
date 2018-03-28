package com.ramotion.navigationtoolbar.example.header

import android.support.constraint.ConstraintLayout
import android.view.View
import com.ramotion.navigationtoolbar.DefaultItemTransformer
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import kotlinx.android.synthetic.main.header_item.view.*
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

        if (!checkForChanges(header, headerBottom)) {
            return
        }

        val isAtTopHalf = currentRatioTopHalf in 0f..1f && currentRatioBottomHalf == 0f
        val doTransform = (if (isAtTopHalf) ::transformTopHalf else ::transformBottomHalf)
        doTransform(lm, header, headerBottom)
    }

    private fun checkForChanges(header: HeaderLayout, headerBottom: Int): Boolean {
        val childCount = header.childCount
        if (childCount == 0) {
            return false
        }

        val (hs, vs) = header.getChildAt(0).let { it.left to it.top }
        val nothingChanged =
                hs == prevHScrollOffset && vs == prevVScrollOffset
                && childCount == prevChildCount
                && prevHeaderBottom == headerBottom
        if (nothingChanged) {
            return false
        }

        prevChildCount = childCount
        prevHScrollOffset = hs
        prevVScrollOffset = vs
        prevHeaderBottom = headerBottom

        return true
    }

    private fun getElevation(header: HeaderLayout): Float {
        return this.elevation ?: run {
            val z = header.takeIf { it.childCount > 0 }?.getChildAt(0)?.elevation?: 0f
            this.elevation = z
            z
        }
    }

    private fun transformTopHalf(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        val childCount = header.childCount
        val invertedRatio = 1f - currentRatioTopHalf

        var curZ = childCount / 2f + getElevation(header)
        var prevZDiff = Int.MAX_VALUE

        for (i in 0 until childCount) {
            val item = header.getChildAt(i)

            val headerCenter = header.width / 2
            val itemCenter = item.left + item.width / 2
            val headerCenterDiff = itemCenter - headerCenter

            val hcDiff = abs(headerCenterDiff)
            curZ += if (prevZDiff > hcDiff) { prevZDiff = hcDiff; -1f } else { prevZDiff = hcDiff; 1f }
            item.z = curZ

            val title = item.title
            val itemNewCenter = headerCenter + headerCenterDiff * horizontalCenterOffsetRatio
            val itemNewCenterDiff = itemNewCenter - itemCenter
            val titleInitialLeft = item.width / 2 - title.width / 2

            val titleNewLeft = titleInitialLeft + itemNewCenterDiff * currentRatioTopHalf
            val titleNewTop = horizontalTopOffset * invertedRatio
            val ratio = 1.5f - min(headerCenter.toFloat(), abs(headerCenter - itemNewCenter)) / headerCenter
            val animate = currentRatioTopHalf > 0.5f && !topTitlesAnimated

            transformTitle(title, ratio, titleNewLeft, titleNewTop, animate)
        }

        topTitlesAnimated = true
    }

    private fun transformBottomHalf(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        topTitlesAnimated = false

        for (i in 0 until header.childCount) {
            val item = header.getChildAt(i)

            item.z = getElevation(header)

            val title = item.title
            val titleInitialLeft = item.width / 2 - title.width / 2
            val titleNewLeft = titleInitialLeft - abs(titleInitialLeft - verticalLeftOffset) * currentRatioBottomHalf
            transformTitle(title, 1f, titleNewLeft)
        }
    }

    private fun transformTitle(title: View, ratio: Float, x: Float, y: Float? = null, animate: Boolean = false) {
        title.alpha = ratio
        title.scaleX = min(1f, ratio)
        title.scaleY = title.scaleX

        if (animate) {
            title.animate().x(x).start()
        } else {
            title.x = x
        }

        y?.let {
            val lp = title.layoutParams as ConstraintLayout.LayoutParams
            lp.topMargin = it.toInt()
            title.requestLayout()
        }
    }
}