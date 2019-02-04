package com.ramotion.navigationtoolbar.example.pager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ramotion.navigationtoolbar.example.PageDataSet
import com.ramotion.navigationtoolbar.example.R


class PageAdapter(private val count: Int,
                  private val dataSet: PageDataSet) : RecyclerView.Adapter<PageItem>() {

    private enum class ItemType(val value: Int) {
        USER(1),
        IMAGE(2);

        companion object {
            private val map = ItemType.values().associateBy(ItemType::value)
            fun fromInt(type: Int, defaultValue: ItemType = USER) = map.getOrElse(type) {defaultValue}
        }
    }

    override fun getItemCount() = count

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItem {
        return when (ItemType.fromInt(viewType)) {
            ItemType.USER -> createItemUser(parent)
            ItemType.IMAGE -> createItemImage(parent)
        }
    }

    override fun onBindViewHolder(holder: PageItem, position: Int) {
        when (holder) {
            is ItemUser -> { holder.setContent(dataSet.getItemData(position)) }
            is ItemImage -> { holder.setImage(dataSet.secondItemImage) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return (if (position == 1) ItemType.IMAGE else ItemType.USER).value
    }

    override fun onViewRecycled(holder: PageItem) {
        super.onViewRecycled(holder)
        holder.clearContent()
    }

    private fun createItemUser(parent: ViewGroup): ItemUser {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_user, parent, false)
        return ItemUser(view)
    }

    private fun createItemImage(parent: ViewGroup): ItemImage {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_image, parent, false)
        return ItemImage(view)
    }
}