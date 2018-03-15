package com.ramotion.navigationtoolbar.example.header

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.example.R

class HeaderItem(view: View) : HeaderLayout.ViewHolder(view) {

    private val background = view.findViewById<ImageView>(R.id.background)

    internal val title = view.findViewById<TextView>(R.id.title)

    fun setContent(imgId: Int) {
        background.setImageResource(imgId)
        title.text = "Title $position"
    }

    fun clearContent() {
        Log.d("D", "clearContent| position: $position")
    }

}