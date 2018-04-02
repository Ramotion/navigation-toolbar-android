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

    internal var titleText: String? = null

    internal var _overlayTitle: TextView? = null

    fun setContent(content: HeaderDataSet.ItemData, overlayTitle: TextView?) {
        gradient.setBackgroundResource(content.gradient)
        Glide.with(background).load(content.background).into(background)

        titleText = content.title
        title.setText(content.title)

        _overlayTitle = overlayTitle
        _overlayTitle?.also {
            it.setTag(position)
            it.setText(content.title)
        }
    }

    fun clearContent() {
        titleText = null

        _overlayTitle?.also {
            it.setText("unused")
            it.setTag(null)
        }
        _overlayTitle = null
    }

}