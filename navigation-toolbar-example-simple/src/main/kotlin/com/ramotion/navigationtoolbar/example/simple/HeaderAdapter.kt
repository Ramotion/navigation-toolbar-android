package com.ramotion.navigationtoolbar.example.simple

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ramotion.navigationtoolbar.HeaderLayout

class HeaderAdapter(private val content: Array<Int>,
                    private val count: Int) : HeaderLayout.Adapter<HeaderItem>() {

    override fun getItemCount() = count

    override fun onCreateViewHolder(parent: ViewGroup): HeaderItem {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.header_item, parent, false)
        return HeaderItem(view)
    }

    override fun onBindViewHolder(holder: HeaderItem, position: Int) {
        holder.setContent(content[position % content.size])
    }

    override fun onViewRecycled(holder: HeaderItem) {
        holder.clearContent()
    }
}