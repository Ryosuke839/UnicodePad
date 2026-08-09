package jp.ddo.hotmist.unicodepad

import android.util.DisplayMetrics
import android.view.View
import android.widget.LinearLayout

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

import android.app.Activity
import android.content.SharedPreferences

internal class AdCompatImpl : AdCompat {
    override val showAdSettings = true
    override fun renderAdToContainer(activity: Activity, pref: SharedPreferences): Int {
        val adContainer = activity.findViewById<LinearLayout>(R.id.adContainer)
        return if (adContainer != null) {
            if (!pref.getBoolean("no-ad", false)) {
                if (adContainer.childCount == 0) {
                    try {
                        MobileAds.initialize(activity) { }
                        AdView(activity).let {
                            val outMetrics = DisplayMetrics()
                            @Suppress("DEPRECATION")
                            activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                            it.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, (outMetrics.widthPixels / outMetrics.density).toInt()))
                            it.adUnitId = "ca-app-pub-8779692709020298/6882844952"
                            (activity.findViewById<View>(R.id.adContainer) as LinearLayout).addView(it)
                            val adRequest = AdRequest.Builder().build()
                            it.loadAd(adRequest)
                            it.adSize?.height
                        }
                    } catch (e: NullPointerException) {
                        null
                    }
                } else {
                    adContainer.getChildAt(0).let {
                        (it as? AdView)?.adSize?.height
                    }
                }
            } else {
                if (adContainer.childCount > 0) {
                    adContainer.removeAllViews()
                }
                null
            }
        } else {
            null
        } ?: 0
    }
}
