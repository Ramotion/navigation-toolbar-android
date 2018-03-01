package com.ramotion.navigationtoolbar.example.simple

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.ramotion.navigationtoolbar.HeaderLayout
import com.ramotion.navigationtoolbar.HeaderLayoutManager
import com.ramotion.navigationtoolbar.NavigationToolBarLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mItemCount = 20
    private val mPictures = intArrayOf(R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5).toTypedArray()

    private lateinit var mViewPager: ViewPager
    private lateinit var mHeader: NavigationToolBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(navigation_toolbar_layout.mToolBar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

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

    private fun initViewPager() {
        mViewPager = findViewById<ViewPager>(R.id.pager)
        mViewPager.adapter = ViewPagerAdapter(mItemCount)
        mViewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mHeader.smoothScrollToPosition(position)
            }
        })
    }

    private fun initHeader() {
        mHeader = findViewById<NavigationToolBarLayout>(R.id.navigation_toolbar_layout)
        mHeader.setItemTransformer(HeaderItemTransformer(-50, 0.35f))
        mHeader.setAdapter(HeaderAdapter(mPictures, mItemCount))

        mHeader.addItemChangeListener(object : HeaderLayoutManager.ItemChangeListener {
            override fun onItemChanged(position: Int) {
                mViewPager.currentItem = position
            }
        })

        mHeader.addItemClickListener(object : HeaderLayoutManager.ItemClickListener {
            override fun onItemClicked(viewHolder: HeaderLayout.ViewHolder) {
                mViewPager.currentItem = viewHolder.mPosition
            }
        })

        SimpleSnapHelper().attach(mHeader)
    }

}
