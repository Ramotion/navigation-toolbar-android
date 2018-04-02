package com.ramotion.navigationtoolbar.example.header

import android.support.constraint.ConstraintLayout
import android.view.View
import android.widget.FrameLayout
import com.ramotion.navigationtoolbar.DefaultItemTransformer
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import com.ramotion.navigationtoolbar.NavigationToolBarLayout
import kotlinx.android.synthetic.main.header_item.view.*
import kotlin.math.abs
import kotlin.math.min

class HeaderItemTransformer(
        private val overlay: FrameLayout,
        private val horizontalTopOffset: Int,
        private val verticalLeftOffset: Int,
        private val horizontalCenterOffsetRatio: Float) : DefaultItemTransformer() {

    private var maxWidthDiff: Int = 0

    private var prevChildCount = Int.MIN_VALUE
    private var prevHScrollOffset = Int.MIN_VALUE
    private var prevVScrollOffset = Int.MIN_VALUE
    private var prevHeaderBottom = Int.MIN_VALUE

    private var topTitlesAnimated = false
    private var elevation: Float? = null

    override fun onAttach(ntl: NavigationToolBarLayout) {
        super.onAttach(ntl)
        val lm = ntl.layoutManager
        maxWidthDiff = lm.horizontalTabWidth - lm.verticalTabWidth
    }

    override fun transform(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        super.transform(lm, header, headerBottom)

        if (!checkForChanges(header, headerBottom)) {
            return
        }

        val isAtTopHalf = currentRatioTopHalf in 0f..1f && currentRatioBottomHalf == 0f
        val transformItems = (if (isAtTopHalf) ::transformTopHalf else ::transformBottomHalf)
        transformItems(lm, header, headerBottom)
        transformOverlay(lm, header, headerBottom)
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

    private fun transformOverlay(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        val invertedBottomRatio = 1f - currentRatioBottomHalf
        val headerCenter = header.width / 2f

        for (i in 0 until header.childCount) {
            val card = header.getChildAt(i)
            val holder = HeaderLayout.getChildViewHolder(card) as HeaderItem
            holder._overlayTitle?.also { title ->
                val cardWidth = card.width
                val cardWidthDiff = lm.horizontalTabWidth - cardWidth
                val widthRatio = cardWidthDiff / maxWidthDiff.toFloat()

                val cardCenter = card.x + cardWidth / 2
                val titleLeft = card.x + verticalLeftOffset
                val titleCenter = cardCenter - title.width / 2
                val titleCurrentLeft = titleLeft + (titleCenter - titleLeft) * (1f - widthRatio)
                val titleTop = card.y + card.height / 2 - title.height / 2 + horizontalTopOffset / 2 * (1f - currentRatioTopHalf)

                val ratioOffsetDateTime = (card.x / card.width) * invertedBottomRatio
                val ratioAlphaScale = 0.8f + 0.2f * (1f - min(headerCenter, abs(headerCenter - cardCenter)) / headerCenter * invertedBottomRatio)

                val titleOffset = (-ratioOffsetDateTime * cardWidth / 2)

                title.x = titleCurrentLeft + titleOffset
                title.y = titleTop
                title.alpha = ratioAlphaScale
                title.scaleX = min(1f, ratioAlphaScale)
                title.scaleY = title.scaleX
            }
        }
    }
}