package com.ramotion.navigationtoolbar.example.header

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.example.HeaderDataSet
import com.ramotion.navigationtoolbar.example.R

class HeaderAdapter(
        private val count: Int,
        private val dataSet: HeaderDataSet) : HeaderLayout.Adapter<HeaderItem>() {

    override fun getItemCount() = count

    override fun onCreateViewHolder(parent: ViewGroup): HeaderItem {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.header_item, parent, false)
        return HeaderItem(view)
    }

    override fun onBindViewHolder(holder: HeaderItem, position: Int) {
        holder.setContent(dataSet.getItemData(position))
    }

    override fun onViewRecycled(holder: HeaderItem) {
        holder.clearContent()
    }
}