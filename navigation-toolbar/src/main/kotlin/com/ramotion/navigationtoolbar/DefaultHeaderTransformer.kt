package com.ramotion.navigationtoolbar

import android.util.Log
import kotlin.math.max
import kotlin.math.min

open class DefaultHeaderTransformer
    : HeaderLayoutManager.ItemTransformer, HeaderLayoutManager.ItemClickListener {

    private var mLayoutManager: HeaderLayoutManager? = null
    private var mHeaderLayout: HeaderLayout? = null

    private var mRatioWork = 0f
    private var mRatioTopHalf = 0f
    private var mRatioBottomHalf = 0f

    private var mClickedItem: Int? = 0

    protected var mCurrentRatio = 0f; private set
    protected var mCurrentRatioWork = 0f; private set
    protected var mCurrentRatioTopHalf = 0f; private set
    protected var mCurrentRatioBottomHalf = 0f; private set

    override fun attach(lm: HeaderLayoutManager, header: HeaderLayout) {
        mLayoutManager = lm
        mHeaderLayout = header

        mRatioWork = lm.mWorkHeight / lm.mScreenHeight.toFloat()
        mRatioTopHalf = lm.mToolBarHeight / lm.mScreenHeight.toFloat()
        mRatioBottomHalf = lm.mScreenHalf / lm.mScreenHeight.toFloat()

        lm.addItemClickListener(this)
    }

    override fun detach() {
        mLayoutManager?.removeItemClickListener(this)

        mLayoutManager = null
        mHeaderLayout = null
    }

    override fun transform(headerBottom: Int) {
        Log.d("D", "transform called: $headerBottom")
        updateRatios(headerBottom)
    }

    override fun onItemClick(viewHolder: HeaderLayout.ViewHolder) {
        mClickedItem = mHeaderLayout?.indexOfChild(viewHolder.view)
    }

    private fun updateRatios(headerBottom: Int) {
        val lm = mLayoutManager ?: return

        mCurrentRatio = max(0f, headerBottom / lm.mScreenHeight.toFloat())
        mCurrentRatioWork = max(0f, (headerBottom - lm.mToolBarHeight) / lm.mWorkHeight.toFloat())
        mCurrentRatioTopHalf = max(0f, 1 - (mRatioBottomHalf - min(max(mCurrentRatio, mRatioTopHalf), mRatioBottomHalf)) / (mRatioBottomHalf - mRatioTopHalf))
        mCurrentRatioBottomHalf = max(0f, (mCurrentRatio - mRatioBottomHalf) / mRatioBottomHalf)

        Log.d("D", "r: $mCurrentRatio, rw: $mCurrentRatioWork, rth: $mCurrentRatioTopHalf, rbh: $mCurrentRatioBottomHalf")
    }

}