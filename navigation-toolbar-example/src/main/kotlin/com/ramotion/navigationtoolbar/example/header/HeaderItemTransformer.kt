package com.ramotion.navigationtoolbar.example.header

import android.view.ViewOutlineProvider
import com.ramotion.navigationtoolbar.DefaultItemTransformer
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import com.ramotion.navigationtoolbar.NavigationToolBarLayout
import kotlin.math.abs
import kotlin.math.min

class HeaderItemTransformer(
        private val horizontalTopOffset: Int,
        private val titleLeftOffset: Int,
        private val lineRightOffset: Int,
        private val lineBottomOffset: Int) : DefaultItemTransformer() {

    private var maxWidthDiff: Int = 0

    private var prevChildCount = Int.MIN_VALUE
    private var prevHScrollOffset = Int.MIN_VALUE
    private var prevVScrollOffset = Int.MIN_VALUE
    private var prevHeaderBottom = Int.MIN_VALUE

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

    private fun transformOverlay(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        val invertedBottomRatio = 1f - currentRatioBottomHalf
        val headerCenter = header.width / 2f

        for (i in 0 until header.childCount) {
            val card = header.getChildAt(i)
            val holder = HeaderLayout.getChildViewHolder(card) as HeaderItem

            val cardWidth = card.width
            val cardHeight = card.height
            val cardWidthDiff = lm.horizontalTabWidth - cardWidth
            val cardCenterX = card.x + cardWidth / 2
            val cardCenterY = card.y + cardHeight / 2

            val ratioWidth = cardWidthDiff / maxWidthDiff.toFloat()
            val ratioOffset = (card.x / cardWidth) * invertedBottomRatio
            val ratioAlphaScale = 0.8f + 0.2f * (1f - min(headerCenter, abs(headerCenter - cardCenterX)) / headerCenter * invertedBottomRatio)

            holder.overlayTitle?.also { title ->
                val titleLeft = card.x + titleLeftOffset
                val titleCenter = cardCenterX - title.width / 2
                val titleCurrentLeft = titleLeft + (titleCenter - titleLeft) * (1f - ratioWidth)
                val titleTop = cardCenterY - title.height / 2 + horizontalTopOffset / 2 * (1f - currentRatioTopHalf)
                val titleOffset = (-ratioOffset * cardWidth / 2) * currentRatioTopHalf

                title.x = titleCurrentLeft + titleOffset
                title.y = titleTop
                title.alpha = ratioAlphaScale
                title.scaleX = min(1f, ratioAlphaScale)
                title.scaleY = title.scaleX
            }

            holder.overlayLine?.also { line ->
                val lineWidth = line.width
                val lineHeight = line.height
                val lineLeft = cardCenterX - lineWidth / 2
                val lineTop = cardCenterY + (holder.overlayTitle?.let { it.height / 2 } ?: 0)
                val hOffset = ((card.right - lineRightOffset - lineWidth) - lineLeft) * currentRatioBottomHalf
                val vOffset = ((card.bottom - lineBottomOffset - lineHeight) - lineTop) * currentRatioBottomHalf
                line.x = lineLeft + hOffset
                line.y = lineTop + vOffset + horizontalTopOffset / 2 * (1f - currentRatioTopHalf)
            }

            val background = holder.backgroundLayout
            if (currentRatioBottomHalf != 0f) {
                background.translationX = 0f
                background.alpha = 1f
                card.outlineProvider = ViewOutlineProvider.BACKGROUND
            } else {
                card.outlineProvider = null
                if (ratioOffset <= -1f || ratioOffset >= 1f) {
                    background.translationX = card.width * ratioOffset
                    background.alpha = 0f
                } else if (ratioOffset == 0f) {
                    background.translationX = card.width * ratioOffset
                    background.alpha = 1f
                } else {
                    background.translationX = card.width * -ratioOffset
                    background.alpha = 1f - abs(ratioOffset)
                }
            }
        }
    }
}