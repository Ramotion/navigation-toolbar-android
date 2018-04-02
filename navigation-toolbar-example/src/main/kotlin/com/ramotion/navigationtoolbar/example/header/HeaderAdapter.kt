package com.ramotion.navigationtoolbar.example.header

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.example.HeaderDataSet
import com.ramotion.navigationtoolbar.example.R

class HeaderAdapter(
        private val count: Int,
        private val dataSet: HeaderDataSet,
        private val overlay: FrameLayout) : HeaderLayout.Adapter<HeaderItem>() {

    override fun getItemCount() = count

    override fun onCreateViewHolder(parent: ViewGroup): HeaderItem {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.header_item, parent, false)
        return HeaderItem(view)
    }

    override fun onBindViewHolder(holder: HeaderItem, position: Int) {
        holder.setContent(dataSet.getItemData(position), getNextOverlayTitle())
    }

    override fun onViewRecycled(holder: HeaderItem) {
        holder.clearContent()
    }

    private fun getNextOverlayTitle(): TextView? {
        for (i in 0 until overlay.childCount) {
            val child = overlay.getChildAt(i)
            if (child is TextView && child.getTag() == null) {
                return child
            }
        }
        return null
    }
}