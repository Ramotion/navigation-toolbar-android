package com.ramotion.navigationtoolbar.example.simple

import com.ramotion.navigationtoolbar.DefaultItemTransformer
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import kotlin.math.abs

class HeaderItemTransformer(private val mVerticalOffset: Int) : DefaultItemTransformer() {

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

        for (i in 0 until header.childCount) {
            val item = header.getChildAt(i)
            val holder = HeaderLayout.getChildViewHolder(item) as HeaderItem

            val headerWidth = header.width
            val titleWidth = holder.mTitle.width
            val titleInitialLeft = headerWidth / 2 - titleWidth / 2
            val titleNewLeft = titleInitialLeft - abs(titleInitialLeft - mVerticalOffset) * mCurrentRatioBottomHalf
            holder.mTitle.left = titleNewLeft.toInt()
        }
    }

}