package com.ramotion.navigationtoolbar.example.pager

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.ramotion.navigationtoolbar.example.PageDataSet
import com.ramotion.navigationtoolbar.example.R

sealed class PageItem(view: View) : RecyclerView.ViewHolder(view) {
    fun clearContent() {}
}

class ItemUser(view: View) : PageItem(view) {
    private val avatar = view.findViewById<ImageView>(R.id.avatar)
    private val userName = view.findViewById<TextView>(R.id.user_name)
    private val status = view.findViewById<TextView>(R.id.status)

    fun setContent(content: PageDataSet.ItemData) {
        avatar.setImageResource(content.avatar)
        userName.setText(content.userName)
        status.setText(content.status)
    }
}

class ItemImage(view: View) : PageItem(view) {
    val imageView = view.findViewById<ImageView>(R.id.page_image)

    fun setImage(imgId: Int) = imageView.setImageResource(imgId)
}
