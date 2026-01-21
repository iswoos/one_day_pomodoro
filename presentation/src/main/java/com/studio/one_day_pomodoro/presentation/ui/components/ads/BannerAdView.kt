package com.studio.one_day_pomodoro.presentation.ui.components.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import com.studio.one_day_pomodoro.presentation.util.findActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111" // AdMob Test Banner ID
) {
    AndroidView(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                val displayMetrics = context.resources.displayMetrics
                val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
                val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                setAdSize(adSize)
                
                this.adUnitId = adUnitId
                
                adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        android.util.Log.d("BannerAdView", "Banner ad loaded successfully")
                    }
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        android.util.Log.e("BannerAdView", "Banner ad failed to load: ${error.message} (code: ${error.code})")
                    }
                }
                
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}


