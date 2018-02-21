package com.ramotion.navigationtoolbar.example.simple

import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.TextView
import com.ramotion.navigationtoolbar.HeaderLayout
import java.util.*

class HeaderItem(view: View) : HeaderLayout.ViewHolder(view) {

    private companion object {
        val mRandom = Random()
    }

    internal val mTitle = view.findViewById<TextView>(R.id.title)

    fun setContent() {
        mTitle.text = "Title ${mPosition}"
        view.setBackgroundColor(Color.rgb(mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255)))
    }

    fun clearContent() {
    }

}