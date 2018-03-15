package com.ramotion.navigationtoolbar.example.pager

import android.support.v4.view.PagerAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ramotion.navigationtoolbar.example.R
import java.util.*


class ViewPagerAdapter(private val content: Array<Int>,
                       private val size: Int) : PagerAdapter() {

    private companion object {
        val random = Random()
    }

    override fun getCount(): Int = size

    override fun isViewFromObject(view: View, key: Any): Boolean = view == key

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context).inflate(R.layout.pager_item, container, false)
        initRecyclerView(view as RecyclerView, position)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, key: Any) {
        container.removeView(key as View)
    }

    override fun getPageTitle(position: Int): CharSequence = position.toString()

    private fun initRecyclerView(recyclerView: RecyclerView, position: Int) {
        val adapter = PageAdapter(random.nextInt(10) + 5, content[position % content.size])
        recyclerView.adapter = adapter
    }

}