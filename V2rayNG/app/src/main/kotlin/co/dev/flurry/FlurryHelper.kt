package co.dev.flurry

import android.util.Log
import co.dev.ApplicationLoader
import co.dev.Config
import co.nevisa.commonlib.flurry.FlurryHelper
import co.nevisa.commonlib.flurry.IFlurryCallback
import com.flurry.android.FlurryConfig
import com.google.gson.Gson
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.util.AngConfigManager

class FlurryHelper : IFlurryCallback {

    val TAG = Config.TAG + "fh"

    fun init(context: ApplicationLoader) {
        Log.i(TAG, "FlurryHelper > init: ")
        val flurryHelper = FlurryHelper();

        flurryHelper.setPreServe(false)
        flurryHelper.setRetryOnFail(1)

        flurryHelper.initialize(this)
    }

    override fun onFetched(isCache: Boolean, flurryConfig: FlurryConfig) {
        if (isCache && !BuildConfig.DEBUG_VERSION) {
            Log.e(TAG, "onFetched: cached!")
            return
        }
        Log.i(TAG, "onFetched: " + flurryConfig.getString("edit_version", "0"))
        val servers = flurryConfig.getString("servers", "")
        servers.replace(" ","\n").split('\n').forEach {
            AngConfigManager.importBatchConfig(
                it,
                "",
                true
            )
        }


    }
}