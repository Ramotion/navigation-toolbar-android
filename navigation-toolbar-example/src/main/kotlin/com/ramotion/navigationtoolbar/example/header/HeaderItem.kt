package com.ramotion.navigationtoolbar.example.header

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.example.HeaderDataSet
import com.ramotion.navigationtoolbar.example.R

class HeaderItem(view: View) : HeaderLayout.ViewHolder(view) {

    private val gradient = view.findViewById<View>(R.id.gradient)
    private val background = view.findViewById<ImageView>(R.id.background)

    internal val title = view.findViewById<TextView>(R.id.title)

    fun setContent(content: HeaderDataSet.ItemData) {
        gradient.setBackgroundResource(content.gradient)
        title.setText(content.title)
        Glide.with(background).load(content.background).into(background)
    }

    fun clearContent() {}

}