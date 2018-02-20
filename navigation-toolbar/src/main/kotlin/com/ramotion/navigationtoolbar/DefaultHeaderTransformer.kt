package com.ramotion.navigationtoolbar

import android.util.Log

class DefaultHeaderTransformer() : HeaderLayoutManager.ItemTransformer {

    override fun onFill(header: HeaderLayout) {
        Log.d("D", "onFill called")
        transform(header)
    }

    override fun onHeaderOffsetChange(header: HeaderLayout, offset: Int) {
        Log.d("D", "onHeaderOffsetChange called")
        transform(header)
    }

    private fun transform(header: HeaderLayout) {
        Log.d("D", "transform called")
    }

}