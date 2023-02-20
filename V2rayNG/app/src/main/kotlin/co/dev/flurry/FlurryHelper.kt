package co.dev.flurry

import android.util.Log
import co.dev.ApplicationLoader
import co.dev.Config
import co.dev.GlobalStorage
import co.nevisa.commonlib.flurry.FlurryHelper
import co.nevisa.commonlib.flurry.IFlurryCallback
import com.flurry.android.FlurryConfig
import com.tencent.mmkv.MMKV
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.MmkvManager

class FlurryHelper : IFlurryCallback {

    val TAG = Config.TAG + "fh"
    fun init(context: ApplicationLoader) {
        Log.i(TAG, "FlurryHelper > init: ")
        val flurryHelper = FlurryHelper();
        flurryHelper.initialize(this)
    }

    override fun onFetched(isCache: Boolean, flurryConfig: FlurryConfig) {
        if (isCache) {
            Log.e(TAG, "onFetched: cached!")
            return
        }
        Log.i(TAG, "onFetched: " + flurryConfig.getString("edit_version", "0"))

        GlobalStorage.apiUrl(flurryConfig.getString("api_url", ""))

        GlobalStorage.flurryId(flurryConfig.getString("flurry_id", ""))

        GlobalStorage.privacyUrl( flurryConfig.getString("privacy_url", ""))

        GlobalStorage.socialMedia(flurryConfig.getString("social_media", ""))

        GlobalStorage.sponsor(flurryConfig.getString("sponsor", ""))

        GlobalStorage.serverCacheTime(flurryConfig.getInt("server_cache_time", 0))

        val servers = flurryConfig.getString("servers", "")
        servers.replace(" ", "\n").split('\n').forEach {
            AngConfigManager.importBatchConfig(
                it,
                "",
                true
            )
        }
    }
}