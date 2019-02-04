package com.ramotion.navigationtoolbar.example.pager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.ramotion.navigationtoolbar.example.R
import com.ramotion.navigationtoolbar.example.ViewPagerDataSet
import java.util.*


class ViewPagerAdapter(private val count: Int,
                       private val dataSet: ViewPagerDataSet) : PagerAdapter() {

    private companion object {
        val random = Random()
    }

    override fun getCount(): Int = count

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
        val adapter = PageAdapter(random.nextInt(10) + 5, dataSet.getPageData(position))
        recyclerView.adapter = adapter
    }

}