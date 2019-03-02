package com.ramotion.navigationtoolbar.example.header

import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.ramotion.navigationtoolbar.DefaultItemTransformer
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class HeaderItemTransformer(
        private val headerOverlay: FrameLayout,
        private val titleLeftOffset: Int,
        private val lineRightOffset: Int,
        private val lineBottomOffset: Int,
        private val lineTitleOffset: Int) : DefaultItemTransformer() {

    private var prevChildCount = Int.MIN_VALUE
    private var prevHScrollOffset = Int.MIN_VALUE
    private var prevVScrollOffset = Int.MIN_VALUE
    private var prevHeaderBottom = Int.MIN_VALUE

    private var isOverlayLaidout = false

    override fun transform(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        super.transform(lm, header, headerBottom)

        if (!isOverlayLaidout) {
            headerOverlay.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    headerOverlay.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    isOverlayLaidout = true
                    transformOverlay(header)
                }
            })
            return
        }

        if (!checkForChanges(header, headerBottom)) {
            return
        }

        transformOverlay(header)
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

    private fun transformOverlay(header: HeaderLayout) {
        val invertedBottomRatio = 1f - currentRatioBottomHalf
        val headerCenter = header.width / 2f

        val lineAlpha = (abs((min(0.8f, max(0.2f, currentRatioBottomHalf)) - 0.2f) / 0.6f - 0.5f) / 0.5f).pow(11)

        for (i in 0 until header.childCount) {
            val card = header.getChildAt(i)
            val holder = HeaderLayout.getChildViewHolder(card) as HeaderItem

            val cardWidth = card.width
            val cardHeight = card.height
            val cardCenterX = card.x + cardWidth / 2
            val cardCenterY = card.y + cardHeight / 2

            val ratioHorizontalPosition = (card.x / cardWidth) * invertedBottomRatio
            val ratioHorizontalOffset = (1f - min(headerCenter, abs(headerCenter - cardCenterX)) / headerCenter * invertedBottomRatio)
            val alphaTitle = 0.7f + 0.3f * ratioHorizontalOffset

            if (holder.overlayTitle?.text?.isNotEmpty() == true && holder.overlayLine?.width == 0)
                holder.overlayTitle?.requestLayout()
            transformTitle(holder, card, cardCenterX, cardCenterY, ratioHorizontalPosition, invertedBottomRatio, ratioHorizontalOffset, alphaTitle)

            if (holder.overlayLine?.width == 0 || holder.overlayLine?.height == 0)
                holder.overlayLine?.requestLayout()
            transformLine(holder, card, cardCenterX, cardCenterY, ratioHorizontalPosition, lineAlpha, alphaTitle)

            val background = holder.backgroundLayout
            if (currentRatioBottomHalf != 0f) {
                background.translationX = 0f
                background.alpha = 1f
                card.outlineProvider = ViewOutlineProvider.BOUNDS
            } else {
                card.outlineProvider = null
                if (ratioHorizontalPosition <= -1f || ratioHorizontalPosition >= 1f) {
                    background.translationX = cardWidth * ratioHorizontalPosition
                    background.alpha = 0f
                } else if (ratioHorizontalPosition == 0f) {
                    background.translationX = cardWidth * ratioHorizontalPosition
                    background.alpha = 1f
                } else {
                    background.translationX = cardWidth * -ratioHorizontalPosition
                    background.alpha = 1f - abs(ratioHorizontalPosition)
                }
            }
        }
    }

    private fun transformTitle(holder: HeaderItem, card: View,
                               cardCenterX: Float, cardCenterY: Float,
                               ratioHorizontalPosition: Float, invertedBottomRatio: Float,
                               ratioHorizontalOffset: Float, alphaTitle: Float) {

        if (holder.overlayTitle?.text?.isNotEmpty() == true && holder.overlayLine?.width == 0) {
            holder.overlayTitle?.visibility = View.INVISIBLE
            holder.overlayTitle?.postDelayed({
                transformTitle(holder, card, cardCenterX, cardCenterY, ratioHorizontalPosition, invertedBottomRatio, ratioHorizontalOffset, alphaTitle)
            }, 50)
            return
        }

        holder.overlayTitle?.also { title ->
            holder.overlayTitle?.visibility = View.VISIBLE
            val titleLeft = card.x + titleLeftOffset
            val titleCenter = cardCenterX - title.width / 2
            val titleCurrentLeft = titleLeft + (titleCenter - titleLeft) * invertedBottomRatio
            val titleTop = cardCenterY - title.height / 2
            val titleOffset = (-ratioHorizontalPosition * card.width / 2) * currentRatioTopHalf

            title.x = titleCurrentLeft + titleOffset
            title.y = titleTop
            title.alpha = alphaTitle
            title.scaleX = min(1f, 0.8f + 0.2f * ratioHorizontalOffset)
            title.scaleY = title.scaleX
        }
    }

    private fun transformLine(holder: HeaderItem, card: View,
                              cardCenterX: Float, cardCenterY: Float,
                              ratioHorizontalPosition: Float,
                              lineAlpha: Float, alphaTitle: Float) {

        if (holder.overlayLine?.width == 0 || holder.overlayLine?.height == 0) {
            holder.overlayLine?.visibility = View.INVISIBLE
            holder.overlayLine?.postDelayed({
                transformLine(holder, card, cardCenterX, cardCenterY, ratioHorizontalPosition, lineAlpha, alphaTitle)
            }, 50)
            return
        }

        holder.overlayLine?.also { line ->
            holder.overlayLine?.visibility = View.VISIBLE
            val lineWidth = line.width
            val lineHeight = line.height
            val lineLeft = cardCenterX - lineWidth / 2
            val lineTop = cardCenterY + (holder.overlayTitle?.let { it.height / 2 + lineTitleOffset } ?: 0)
            val hBottomOffset = ((card.right - lineRightOffset - lineWidth) - lineLeft) * currentRatioBottomHalf
            val hTopOffset = -ratioHorizontalPosition * card.width / 1.1f * (1f - currentRatioTopHalf)
            val vOffset = ((card.bottom - lineBottomOffset - lineHeight) - lineTop) * currentRatioBottomHalf
            line.x = lineLeft + hBottomOffset + hTopOffset
            line.y = lineTop + vOffset
            line.alpha = if (currentRatioTopHalf == 1f) lineAlpha else alphaTitle
        }
    }
}