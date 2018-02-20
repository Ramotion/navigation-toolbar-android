package com.ramotion.navigationtoolbar

import android.util.Log
import kotlin.math.max
import kotlin.math.min

open class DefaultHeaderTransformer(private val mLayoutManager: HeaderLayoutManager) : HeaderLayoutManager.ItemTransformer {
    private var mRatioWork = 0f
    private var mRatioTopHalf = 0f
    private var mRatioBottomHalf = 0f

    protected var mCurrentRatio = 0f; private set
    protected var mCurrentRatioWork = 0f; private set
    protected var mCurrentRatioTopHalf = 0f; private set
    protected var mCurrentRatioBottomHalf = 0f; private set

    init {
        mRatioWork = mLayoutManager.mWorkHeight / mLayoutManager.mScreenHeight.toFloat()
        mRatioTopHalf = mLayoutManager.mToolBarHeight / mLayoutManager.mScreenHeight.toFloat()
        mRatioBottomHalf = mLayoutManager.mScreenHalf / mLayoutManager.mScreenHeight.toFloat()
    }

    override fun transform(header: HeaderLayout, headerBottom: Int) {
        Log.d("D", "transform called: $headerBottom")
        updateRatios(headerBottom)


    }

    private fun updateRatios(headerBottom: Int) {
        mCurrentRatio = max(0f, headerBottom / mLayoutManager.mScreenHeight.toFloat())
        mCurrentRatioWork = max(0f, (headerBottom - mLayoutManager.mToolBarHeight) / mLayoutManager.mWorkHeight.toFloat())
        mCurrentRatioTopHalf = max(0f, 1 - (mRatioBottomHalf - min(max(mCurrentRatio, mRatioTopHalf), mRatioBottomHalf)) / (mRatioBottomHalf - mRatioTopHalf))
        mCurrentRatioBottomHalf = max(0f, (mCurrentRatio - mRatioBottomHalf) / mRatioBottomHalf)

        Log.d("D", "r: $mCurrentRatio, rw: $mCurrentRatioWork, rth: $mCurrentRatioTopHalf, rbh: $mCurrentRatioBottomHalf")
    }

}