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

    private final val mText: TextView = view.findViewById(R.id.text)

    fun setContent() {
        mText.text = mPosition.toString()
        view.setBackgroundColor(Color.rgb(mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255)))
    }

    fun clearContent() {
        Log.d("D", "item clearContent: $mPosition")
    }

}