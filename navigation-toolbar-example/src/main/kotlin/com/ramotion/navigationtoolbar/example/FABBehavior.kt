package com.ramotion.navigationtoolbar.example

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FABBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {
    private val hideBorder = context.resources.displayMetrics.heightPixels / 2

    override fun layoutDependsOn(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        updateFABVisibility(dependency, child)
        return false
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: FloatingActionButton, layoutDirection: Int): Boolean {
        val dependencies = parent.getDependencies(child)
        for (i in 0 until dependencies.size) {
            val dependency = dependencies[i]
            if (dependency is AppBarLayout) {
                updateFABVisibility(dependency, child)
                break
            }
        }
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    private fun updateFABVisibility(dependency: View, child: FloatingActionButton) {
        val show = dependency.bottom <= hideBorder
        if (show) child.show() else child.hide()
    }
}