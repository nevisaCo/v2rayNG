package com.v2ray.ang

import android.content.Context
import androidx.preference.PreferenceManager
import co.dev.ApplicationLoader
import com.tencent.mmkv.MMKV

class AngApplication : ApplicationLoader() {
    companion object {
        const val PREF_LAST_VERSION = "pref_last_version"
        lateinit var appContext: Context
    }

    var firstRun = false
        private set

    override fun onCreate() {
        MMKV.initialize(this)
        super.onCreate()

        appContext = applicationContext;

//        LeakCanary.install(this)

        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        firstRun = defaultSharedPreferences.getInt(PREF_LAST_VERSION, 0) != BuildConfig.VERSION_CODE
        if (firstRun)
            defaultSharedPreferences.edit().putInt(PREF_LAST_VERSION, BuildConfig.VERSION_CODE)
                .apply()

        //Logger.init().logLevel(if (BuildConfig.DEBUG) LogLevel.FULL else LogLevel.NONE)
    }
}
