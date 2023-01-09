package co.dev

import android.util.Log
import co.dev.flurry.FlurryHelper
import co.nevisa.commonlib.ApplicationLoader
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R

open class ApplicationLoader : ApplicationLoader() {
    override fun onCreate() {
        super.onCreate()
        Log.i(Config.TAG, "onCreate: ")

        super.init(R.drawable.ic_stat_name, BuildConfig.DEBUG_VERSION, BuildConfig.FLURRY_APP_ID)

        FlurryHelper().init(this)

    }
}