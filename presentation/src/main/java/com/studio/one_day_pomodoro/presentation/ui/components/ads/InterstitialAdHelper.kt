package com.studio.one_day_pomodoro.presentation.ui.components.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdHelper {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadAd(context: Context, adUnitId: String = "ca-app-pub-3940256099942544/1033173712") {
        if (isLoading || interstitialAd != null) {
            android.util.Log.d("InterstitialAdHelper", "Ad is already loading or loaded")
            return
        }
        
        isLoading = true
        val appContext = context.applicationContext
        
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(appContext, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                android.util.Log.d("InterstitialAdHelper", "Ad loaded successfully")
                interstitialAd = ad
                isLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                android.util.Log.e("InterstitialAdHelper", "Ad failed to load: ${error.message} (code: ${error.code})")
                interstitialAd = null
                isLoading = false
            }
        })
    }

    fun showAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    android.util.Log.e("InterstitialAdHelper", "Ad failed to show: ${error.message}")
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            android.util.Log.d("InterstitialAdHelper", "Ad not ready")
            loadAd(activity)
            onAdDismissed()
        }
    }
}
