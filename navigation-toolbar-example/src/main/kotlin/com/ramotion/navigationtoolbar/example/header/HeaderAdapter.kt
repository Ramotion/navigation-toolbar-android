package com.ramotion.navigationtoolbar.example.header

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.example.HeaderDataSet
import com.ramotion.navigationtoolbar.example.R

class HeaderAdapter(
        private val count: Int,
        private val dataSet: HeaderDataSet,
        overlay: FrameLayout) : HeaderLayout.Adapter<HeaderItem>() {

    private val textsLayout = overlay.findViewById<FrameLayout>(R.id.texts)
    private val linesLayout = overlay.findViewById<FrameLayout>(R.id.lines)

    override fun getItemCount() = count

    override fun onCreateViewHolder(parent: ViewGroup): HeaderItem {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.header_item, parent, false)
        return HeaderItem(view)
    }

    override fun onBindViewHolder(holder: HeaderItem, position: Int) {
        holder.setContent(dataSet.getItemData(position), getNextOverlayTitle(), getNextOverlayLine())
    }

    override fun onViewRecycled(holder: HeaderItem) {
        holder.clearContent()
    }

    private fun getNextOverlayTitle(): TextView? {
        for (i in 0 until textsLayout.childCount) {
            val child = textsLayout.getChildAt(i)
            if (child is TextView && child.getTag() == null) {
                return child
            }
        }
        return null
    }

    private fun getNextOverlayLine(): View? {
        for (i in 0 until linesLayout.childCount) {
            val child = linesLayout.getChildAt(i)
            if (child.getTag() == null) {
                return child
            }
        }
        return null
    }
}