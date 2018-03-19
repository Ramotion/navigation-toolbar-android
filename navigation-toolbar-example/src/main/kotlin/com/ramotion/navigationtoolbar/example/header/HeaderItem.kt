package com.ramotion.navigationtoolbar.example.header

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.example.HeaderDataSet
import com.ramotion.navigationtoolbar.example.R

class HeaderItem(view: View) : HeaderLayout.ViewHolder(view) {

    private val gradient = view.findViewById<View>(R.id.gradient)
    private val background = view.findViewById<ImageView>(R.id.background)

    internal val title = view.findViewById<TextView>(R.id.title)

    fun setContent(data: HeaderDataSet.ItemData) {
        this.gradient.setBackgroundResource(data.gradient)
        this.background.setImageResource(data.background)
        this.title.text = data.title
    }

    fun clearContent() {
        Log.d("D", "clearContent| position: $position")
    }

}