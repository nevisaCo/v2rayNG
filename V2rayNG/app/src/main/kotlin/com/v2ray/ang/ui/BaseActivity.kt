package com.v2ray.ang.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.v2ray.ang.util.MyContextWrapper
import com.v2ray.ang.util.Utils

abstract class BaseActivity : AppCompatActivity() {
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressedDispatcher.onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkDarkMode()
    }

    private fun checkDarkMode() {
        val day = !Utils.getDarkModeStatus(this)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = day
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = day

        /* if (Utils.getDarkModeStatus(this)) {
             if (this.javaClass.simpleName == "MainActivity") {
                 setTheme(R.style.AppThemeDayNight_NoActionBar)
             } else {
                 setTheme(R.style.AppThemeDark)
             }
         } else {
             if (this.javaClass.simpleName == "MainActivity") {
                 setTheme(R.style.AppThemeDayNight_NoActionBar)
             } else {
                 setTheme(R.style.AppThemeDayNight)
             }
         }*/
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let {
            MyContextWrapper.wrap(newBase, Utils.getLocale())
        }
        super.attachBaseContext(context)
    }


}
