package jp.ddo.hotmist.unicodepad

import android.app.Activity
import android.content.SharedPreferences

internal interface AdCompat {
  fun renderAdToContainer(activity: Activity, pref: SharedPreferences)
  val showAdSettings: Boolean
}
