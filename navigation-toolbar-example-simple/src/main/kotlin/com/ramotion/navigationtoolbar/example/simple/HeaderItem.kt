package com.ramotion.navigationtoolbar.example.simple

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.ramotion.navigationtoolbar.HeaderLayout

class HeaderItem(view: View) : HeaderLayout.ViewHolder(view) {

    private val mBackground = view.findViewById<ImageView>(R.id.background)

    internal val mTitle = view.findViewById<TextView>(R.id.title)

    fun setContent(imgId: Int) {
        mBackground.setImageResource(imgId)
        mTitle.text = "Title ${mPosition}"
    }

    fun clearContent() {
        Log.d("D", "clearContent| position: $mPosition")
    }

}