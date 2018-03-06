package com.ramotion.navigationtoolbar

import android.graphics.PointF
import kotlin.math.max
import kotlin.math.min

open class DefaultItemTransformer
    : NavigationToolBarLayout.ItemTransformer(), HeaderLayoutManager.ItemClickListener {

    private val hPoints: MutableList<PointF> = mutableListOf()
    private val vPoints: MutableList<PointF> = mutableListOf()

    private var navigationToolBarLayout: NavigationToolBarLayout? = null

    private var ratioWork = 0f
    private var ratioTopHalf = 0f
    private var ratioBottomHalf = 0f

    private var clickedItemIndex: Int? = null
    private var prevItemCount: Int? = null

    protected var currentRatio = -1f; private set
    protected var currentRatioWork = -1f; private set
    protected var currentRatioTopHalf = -1f; private set
    protected var currentRatioBottomHalf = -1f; private set

    override fun attach(ntl: NavigationToolBarLayout) {
        ntl.layoutManager.also { lm ->
            ratioWork = lm.workHeight / lm.screenHeight.toFloat()
            ratioTopHalf = lm.topBorder / lm.screenHeight.toFloat()
            ratioBottomHalf = lm.screenHalf / lm.screenHeight.toFloat()
        }

        navigationToolBarLayout = ntl.also {
            it.addItemClickListener(this)
        }
    }

    override fun detach() {
        navigationToolBarLayout?.removeItemClickListener(this)
        navigationToolBarLayout = null
    }

    override fun transform(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        val prevRatio = currentRatio
        val prevRatioTopHalf = currentRatioTopHalf
        val prevRatioBottomHalf = currentRatioBottomHalf

        val prevItemCount = prevItemCount ?: 0
        val curItemCount = header.childCount
        this.prevItemCount = curItemCount

        updateRatios(lm, headerBottom)

        val nothingChanged = prevRatio == currentRatio && prevItemCount == curItemCount
        if (nothingChanged) {
            return
        }

        var transformed = false

        // On scroll from top (top half) to bottom (bottom half)
        val expandedToTopOfBottomHalf = currentRatioTopHalf == 1f
                && prevRatioTopHalf < currentRatioTopHalf && prevRatioTopHalf != -1f
        if (expandedToTopOfBottomHalf) {
            transformTopHalf(lm, header, headerBottom)
            updatePoints(lm, header, false)
            transformBottomHalf(lm, header)
            transformed = true
        } else {
            // On scroll from top to bottom
            val expandedToBottomOfBottomHalf = currentRatioBottomHalf == 1f
                    && prevRatioBottomHalf <= currentRatioBottomHalf
            if (expandedToBottomOfBottomHalf) {
                transformBottomHalf(lm, header)
                clearPoints()
                transformed = true
        } else {
            // On scroll from bottom to top
            val collapsedToTopOfBottomHalf = currentRatioBottomHalf == 0f
                    && prevRatioBottomHalf > currentRatioBottomHalf
            if (collapsedToTopOfBottomHalf) {
                transformBottomHalf(lm, header)
                transformTopHalf(lm, header, headerBottom)
                lm.fill(header)
                updatePoints(lm, header, false)
                transformed = true
        } else {
            val collapsedToTopOfTopHalf = currentRatioTopHalf == 0f
                    && prevRatioTopHalf > currentRatioTopHalf && prevRatioTopHalf != -1f
            if (collapsedToTopOfTopHalf) {
                transformTopHalf(lm, header, headerBottom)
                clearPoints()
                transformed = true
            }
        }}}

        if (!transformed) {
            val isAtBottomHalf = currentRatioBottomHalf > 0f && currentRatioBottomHalf < 1f
            if (isAtBottomHalf) {
                transformBottomHalf(lm, header)
            } else {
                transformTopHalf(lm, header, headerBottom)
            }
        }
    }

    override fun onItemClicked(viewHolder: HeaderLayout.ViewHolder) {
        navigationToolBarLayout?.also { it ->
            clickedItemIndex = it.headerLayout.indexOfChild(viewHolder.view)
            updatePoints(it.layoutManager, it.headerLayout, true)
        }
    }

    private fun updateRatios(lm: HeaderLayoutManager, headerBottom: Int) {
        currentRatio = max(0f, headerBottom / lm.screenHeight.toFloat())
        currentRatioWork = max(0f, (headerBottom - lm.topBorder) / lm.workHeight.toFloat())
        currentRatioTopHalf = max(0f, 1 - (ratioBottomHalf - min(max(currentRatio, ratioTopHalf), ratioBottomHalf)) / (ratioBottomHalf - ratioTopHalf))
        currentRatioBottomHalf = max(0f, (currentRatio - ratioBottomHalf) / ratioBottomHalf)
    }

    private fun updatePoints(lm: HeaderLayoutManager, header: HeaderLayout, up: Boolean) {
        val index = if (up) {
            clickedItemIndex ?: throw RuntimeException("No vertical (clicked) item index")
        } else {
            lm.getHorizontalAnchorView(header)?.let { header.indexOfChild(it) }
                    ?: throw RuntimeException("No horizontal item index")
        }

        clearPoints()

        if (up) {
            val left = -index * lm.horizontalTabWidth
            val (x, y) = lm.getPoints().first.run { x to y }

            for (i in 0 until header.childCount) {
                vPoints.add(header.getChildAt(i).let { PointF(it.x, it.y) })
                hPoints.add(PointF(x + left + i * lm.horizontalTabWidth, y))
            }
        } else {
            val top = -index * lm.verticalTabHeight
            val (x, y) = lm.getPoints().second.run { x to y }

            for (i in 0 until header.childCount) {
                hPoints.add(header.getChildAt(i).let { PointF(it.x, it.y) })
                vPoints.add(PointF(x, y + top + i * lm.verticalTabHeight))
            }
        }
    }

    private fun clearPoints() {
        hPoints.clear()
        vPoints.clear()
    }

    private fun transformTopHalf(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
        val top = header.height - headerBottom
        (0 until header.childCount)
                .map { header.getChildAt(it) }
                .forEach { lm.layoutChild(it, it.left, top, it.width, headerBottom) }
    }

    private fun transformBottomHalf(lm: HeaderLayoutManager, header: HeaderLayout) {
        val hw = lm.horizontalTabWidth
        val hh = lm.horizontalTabHeight
        val vw = lm.verticalTabWidth
        val vh = lm.verticalTabHeight

        val newWidth = hw - (hw - vw) * currentRatioBottomHalf
        val newHeight = hh - (hh - vh) * currentRatioBottomHalf

        val count = min(header.childCount, hPoints.size)
        for (i in 0 until count) {
            val hp = hPoints[i]
            val vp = vPoints[i]
            val hDiff = (vp.x - hp.x) * currentRatioBottomHalf
            val vDiff = (vp.y - hp.y) * currentRatioBottomHalf

            val x = (hp.x + hDiff).toInt()
            val y = (hp.y + vDiff).toInt()
            lm.layoutChild(header.getChildAt(i), x, y, newWidth.toInt(), newHeight.toInt())
        }
    }

}