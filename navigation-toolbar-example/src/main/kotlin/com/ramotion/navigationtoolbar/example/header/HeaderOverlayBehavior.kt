package com.ramotion.navigationtoolbar.example.header

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

class HeaderOverlayBehavior(context: Context, attrs: AttributeSet) :
        CoordinatorLayout.Behavior<FrameLayout>(context, attrs) {

    override fun layoutDependsOn(parent: CoordinatorLayout, child: FrameLayout, dependency: View): Boolean {
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: FrameLayout, dependency: View): Boolean {
        update(dependency, child)
        return false
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: FrameLayout, layoutDirection: Int): Boolean {
        val dependencies = parent.getDependencies(child)
        for (i in 0 until dependencies.size) {
            val dependency = dependencies[i]
            if (dependency is AppBarLayout) {
                update(dependency, child)
                break
            }
        }
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    private fun update(dependency: View, child: FrameLayout) {
        child.y = (dependency.bottom - child.height).toFloat()
    }
}