package com.ramotion.navigationtoolbar

import com.ramotion.navigationtoolbar.HeaderLayoutManager.Point
import kotlin.math.max
import kotlin.math.min

/**
 * DefaultItemTransformer - default implementation if ItemTransformer interface.
 * @see NavigationToolBarLayout.ItemTransformer
 * @see NavigationToolBarLayout.setItemTransformer
 */
open class DefaultItemTransformer
    : NavigationToolBarLayout.ItemTransformer(), HeaderLayoutManager.ItemClickListener {

    private val hPoints: MutableList<Point> = mutableListOf()
    private val vPoints: MutableList<Point> = mutableListOf()

    private var ratioWork = 0f
    private var ratioTopHalf = 0f
    private var ratioBottomHalf = 0f

    private var clickedItemIndex: Int? = null
    private var prevItemCount: Int? = null

    /**
     * Current HeaderLayout bottom position form 1f (bottom) to 0.11f (top). -1f if not computed yet.
     */
    protected var currentRatio = -1f; private set
    /**
     * Current HeaderLayout bottom position from 1f (bottom) to 0f (top, bellow ToolBar). -1f if not computed yet.
     */
    protected var currentRatioWork = -1f; private set
    /**
     * Current HeaderLayout bottom position from 1f (middle) to 0f (top, bellow ToolBar). -1f if not computed yet.
     */
    protected var currentRatioTopHalf = -1f; private set
    /**
     * Current HeaderLayout bottom position from 1f (bottom) to 0f (middle). -1f if not computed yet.
     */
    protected var currentRatioBottomHalf = -1f; private set

    override fun onAttach(ntl: NavigationToolBarLayout) {
        ntl.layoutManager.also { lm ->
            ratioWork = lm.workHeight / lm.workBottom.toFloat()
            ratioTopHalf = lm.workTop / lm.workBottom.toFloat()
            ratioBottomHalf = lm.workMiddle / lm.workBottom.toFloat()
        }

        ntl.addItemClickListener(this)
    }

    override fun onDetach() {
        navigationToolBarLayout?.removeItemClickListener(this)
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
            clearPoints()
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
                clearPoints()
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
                val arePointsEmpty = hPoints.isEmpty() || vPoints.isEmpty()
                if (arePointsEmpty) {
                    updatePoints(lm, header, false)
                }
                transformBottomHalf(lm, header)
            } else {
                transformTopHalf(lm, header, headerBottom)
            }
        }
    }

    override fun onItemClicked(viewHolder: HeaderLayout.ViewHolder) {
        navigationToolBarLayout
                ?.takeIf { currentRatioBottomHalf == 1f }
                ?.also { it ->
                    clickedItemIndex = it.headerLayout.indexOfChild(viewHolder.view)
                    updatePoints(it.layoutManager, it.headerLayout, true)
                }
    }

    private fun updateRatios(lm: HeaderLayoutManager, headerBottom: Int) {
        currentRatio = max(0f, headerBottom / lm.workBottom.toFloat())
        currentRatioWork = max(0f, (headerBottom - lm.workTop) / lm.workHeight.toFloat())
        currentRatioTopHalf = max(0f, 1 - (ratioBottomHalf - min(max(currentRatio, ratioTopHalf), ratioBottomHalf)) / (ratioBottomHalf - ratioTopHalf))
        currentRatioBottomHalf = max(0f, (currentRatio - ratioBottomHalf) / ratioBottomHalf)
    }

    private fun updatePoints(lm: HeaderLayoutManager, header: HeaderLayout, up: Boolean) {
        val index = if (up) {
            clickedItemIndex ?: throw RuntimeException("No vertical (clicked) item index")
        } else {
            lm.getHorizontalAnchorView(header)
                    ?.let { header.indexOfChild(it) }
                    ?: throw RuntimeException("No horizontal item index")
        }

        clearPoints()

        if (up) {
            val left = -index * lm.horizontalTabWidth
            val (x, y) = lm.getHorizontalPoint()

            for (i in 0 until header.childCount) {
                vPoints.add(header.getChildAt(i).let { Point(lm.getDecoratedLeft(it), lm.getDecoratedTop(it)) })
                hPoints.add(Point(x + left + i * lm.horizontalTabWidth, y))
            }
        } else {
            val totalHeight = (header.adapter?.getItemCount() ?: 0) * lm.verticalTabHeight
            if (totalHeight > lm.workHeight) {
                val top = -index * lm.verticalTabHeight
                val (x, y) = lm.getVerticalPoint()

                for (i in 0 until header.childCount) {
                    hPoints.add(header.getChildAt(i).let { Point(lm.getDecoratedLeft(it), lm.getDecoratedTop(it)) })
                    vPoints.add(Point(x, y + top + i * lm.verticalTabHeight))
                }
            } else {
                val x = lm.getVerticalPoint().x
                val y = (header.height - totalHeight) / 2

                for (i in 0 until header.childCount) {
                    hPoints.add(header.getChildAt(i).let { Point(lm.getDecoratedLeft(it), lm.getDecoratedTop(it)) })
                    vPoints.add(Point(x, y + i * lm.verticalTabHeight))
                }
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
                .forEach { lm.layoutChild(it, lm.getDecoratedLeft(it), top, lm.getDecoratedWidth(it), headerBottom) }
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