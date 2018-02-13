package com.ramotion.navigationtoolbar.example.simple

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.ramotion.navigationtoolbar.NavigationToolBarLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private companion object {
        const val ITEM_COUNT = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(navigation_toolbar_layout.mToolBar)
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
        val pager = findViewById<ViewPager>(R.id.pager)
        pager.adapter = ViewPagerAdapter(ITEM_COUNT)
    }

    private fun initHeader() {
        val toolbar = findViewById<NavigationToolBarLayout>(R.id.navigation_toolbar_layout)
        toolbar.setAdapter(HeaderAdapter(ITEM_COUNT))
//        toolbar.setCurrentPosition(3)
    }

}
