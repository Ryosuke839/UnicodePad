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
    override fun renderAdToContainer(activity: Activity, pref: SharedPreferences) {
        val adContainer = activity.findViewById<LinearLayout>(R.id.adContainer)
        if (adContainer != null) {
            if (!pref.getBoolean("no-ad", false)) {
                if (adContainer.childCount == 0) {
                    try {
                        MobileAds.initialize(activity) { }
                        AdView(activity).also {
                            val outMetrics = DisplayMetrics()
                            @Suppress("DEPRECATION")
                            activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                            it.adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, (outMetrics.widthPixels / outMetrics.density).toInt())
                            it.adUnitId = "ca-app-pub-8779692709020298/6882844952"
                            (activity.findViewById<View>(R.id.adContainer) as LinearLayout).addView(it)
                            val adRequest = AdRequest.Builder().build()
                            it.loadAd(adRequest)
                        }
                    } catch (e: NullPointerException) {}
                }
            } else {
                if (adContainer.childCount > 0) {
                    adContainer.removeAllViews()
                }
            }
        }
    }
}
