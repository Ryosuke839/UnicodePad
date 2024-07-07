package com.google.android.material.bottomsheet

import android.annotation.SuppressLint
import android.view.View
import androidx.viewpager.widget.ViewPager
import jp.ddo.hotmist.unicodepad.R

class ViewPagerBottomSheetBehavior<V : View> : BottomSheetBehavior<V>() {
    @SuppressLint("VisibleForTests")
    override fun findScrollingChild(view: View?): View? {
        if (view is ViewPager) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child.getTag(R.id.TAB_ID) == view.currentItem) {
                    return findScrollingChild(child)
                }
            }
        }
        return super.findScrollingChild(view)
    }
}