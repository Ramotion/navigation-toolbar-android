package com.ramotion.navigationtoolbar.example

import android.animation.ObjectAnimator
import android.graphics.Rect
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.view.Menu
import android.view.MenuItem
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import com.ramotion.navigationtoolbar.NavigationToolBarLayout
import com.ramotion.navigationtoolbar.SimpleSnapHelper
import com.ramotion.navigationtoolbar.example.header.HeaderAdapter
import com.ramotion.navigationtoolbar.example.header.HeaderItemTransformer
import com.ramotion.navigationtoolbar.example.pager.ViewPagerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.ceil
import kotlin.math.max


class MainActivity : AppCompatActivity() {

    private val itemCount = 4
    private val dataSet = ExampleDataSet()

    private lateinit var viewPager: ViewPager
    private lateinit var header: NavigationToolBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        initActionBar()
        initViewPager()
        initHeader()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initActionBar() {
        val toolbar = navigation_toolbar_layout.toolBar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initViewPager() {
        viewPager = findViewById(R.id.pager)
        viewPager.adapter = ViewPagerAdapter(itemCount, dataSet.viewPagerDataSet)
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                header.smoothScrollToPosition(position)
            }
        })
    }

    private fun initHeader() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0

        header = findViewById(R.id.navigation_toolbar_layout)
        header.setItemTransformer(HeaderItemTransformer(statusBarHeight, -50, 0.45f))
        header.setAdapter(HeaderAdapter(itemCount, dataSet.headerDataSet))

        header.addItemChangeListener(object : HeaderLayoutManager.ItemChangeListener {
            override fun onItemChanged(position: Int) {
                viewPager.currentItem = position
            }
        })

        header.addItemClickListener(object : HeaderLayoutManager.ItemClickListener {
            override fun onItemClicked(viewHolder: HeaderLayout.ViewHolder) {
                viewPager.currentItem = viewHolder.position
            }
        })

        val drawerArrow = DrawerArrowDrawable(this)
        drawerArrow.color = resources.getColor(android.R.color.white)
        header.toolBar.navigationIcon = drawerArrow
        header.addHeaderChangeStateListener(object : HeaderLayoutManager.HeaderChangeStateListener() {
            override fun onCollapsed() {
                ObjectAnimator.ofFloat(drawerArrow, "progress", 1f).start()
            }

            override fun onExpanded() {
                ObjectAnimator.ofFloat(drawerArrow, "progress", 0f).start()
            }
        })

        val decorator = object :
                HeaderLayoutManager.ItemDecoration,
                HeaderLayoutManager.HeaderChangeListener {

            private val dp5 = resources.getDimensionPixelSize(R.dimen.decor_bottom)

            private var bottomOffset = dp5

            override fun onHeaderChanged(lm: HeaderLayoutManager, header: HeaderLayout, headerBottom: Int) {
                val ratio = max(0f, headerBottom.toFloat() / header.height - 0.5f) / 0.5f
                bottomOffset = ceil(dp5 * ratio).toInt()
            }

            override fun getItemOffsets(outRect: Rect, viewHolder: HeaderLayout.ViewHolder) {
                outRect.bottom = bottomOffset
            }
        }

        header.addItemDecoration(decorator)
        header.addHeaderChangeListener(decorator)

        SimpleSnapHelper().attach(header)
    }

}
