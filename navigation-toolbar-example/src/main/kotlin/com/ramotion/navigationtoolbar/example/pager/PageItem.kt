package com.ramotion.navigationtoolbar.example.pager

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.ramotion.navigationtoolbar.example.R

sealed class PageItem(view: View) : RecyclerView.ViewHolder(view) {

    fun clearContent() {}

}

class ItemUser(view: View) : PageItem(view) {

    fun setContent(/* content data class */) {}

}

class ItemImage(view: View) : PageItem(view) {

    val imageView = view.findViewById<ImageView>(R.id.page_image)

    fun setImage(imgId: Int) {
        imageView.setImageResource(imgId)
    }

}
