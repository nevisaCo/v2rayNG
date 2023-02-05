package co.dev

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import co.dev.flurry.FlurryHelper
import co.nevisa.commonlib.ApplicationLoader
import co.nevisa.commonlib.BuildVars
import co.nevisa.commonlib.config.AdConfig
import co.nevisa.commonlib.config.BaseConfig
import co.nevisa.commonlib.config.VolleyConfig
import co.nevisa.commonlib.volley.VolleyHelper
import com.google.firebase.messaging.FirebaseMessaging
import com.v2ray.ang.BuildConfig.*
import com.v2ray.ang.R
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

open class ApplicationLoader : ApplicationLoader() {

    init {
        instance = this
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: co.dev.ApplicationLoader? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

    }

    @SuppressLint("HardwareIds")
    override fun onCreate() {
        super.onCreate()
        var s = "33BE2250B43518CCDA7DE426D04EE231";
        if (DEBUG_VERSION) {
            val androidId: String =
                Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
            s = md5(androidId).uppercase(Locale.getDefault())
            Log.i(Config.TAG, "onCreate: s:$s")
        }

        val adConfig = AdConfig();
        adConfig.isPreServe = false
        adConfig.isTestDevice = DEBUG_VERSION;
        adConfig.testDeviceIds = s
        adConfig.retryOnFail = 2;
        adConfig.isLoadSingleNative = false;
        adConfig.setNativeRefreshTime(15)

        val baseConfig = BaseConfig()
        baseConfig.applicationId = APPLICATION_ID
        baseConfig.isDebugMode = DEBUG_VERSION
        baseConfig.tag = Config.TAG + "lib"
        baseConfig.icLauncher = R.drawable.ic_stat_name
        baseConfig.volleyDataCacheStatus(true)

        val fi = GlobalStorage.flurryId()

        super.init(
            baseConfig,
            adConfig,
            fi
        )

        FlurryHelper().init(this)


        //volley config
        val volleyConfig = VolleyConfig();
        volleyConfig.apiAccessToken = ""
        volleyConfig.statusKey = "status"
        volleyConfig.dataKey = "data"
        volleyConfig.isApiSign = true

        volleyConfig.setLoginNeedCallback {

        }

        VolleyHelper.setVolleyConfig(volleyConfig);

        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all")
            if (BuildVars.DEBUG_VERSION) FirebaseMessaging.getInstance().subscribeToTopic("debug")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest: MessageDigest = MessageDigest
                .getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest: ByteArray = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) {
                var h = Integer.toHexString(0xFF and messageDigest[i].toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            Log.e(Config.TAG, "md5: ", e)
        }
        return ""
    }

}