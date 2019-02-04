package com.ramotion.navigationtoolbar.example.pager

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        userName.setText(content.userName)
        status.setText(content.status)
        avatar.setImageResource(content.avatar)

        Glide.with(avatar).load(content.avatar).into(avatar)
    }
}

class ItemImage(view: View) : PageItem(view) {
    private val imageView = view.findViewById<ImageView>(R.id.page_image)

    fun setImage(imgId: Int) {
        Glide.with(imageView).load(imgId).into(imageView)
    }
}
