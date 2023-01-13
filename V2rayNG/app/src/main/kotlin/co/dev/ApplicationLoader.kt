package co.dev

import android.util.Log
import co.dev.flurry.FlurryHelper
import co.nevisa.commonlib.ApplicationLoader
import co.nevisa.commonlib.volley.ILoginNeedCallback
import co.nevisa.commonlib.volley.VolleyHelper
import com.v2ray.ang.BuildConfig.*
import com.v2ray.ang.R
import java.util.Objects

open class ApplicationLoader : ApplicationLoader() {
    override fun onCreate() {
        super.onCreate()
        Log.i(Config.TAG, "onCreate: ")

        super.init(
            R.drawable.ic_stat_name,
            DEBUG_VERSION,
            FLURRY_APP_ID,
            APPLICATION_ID
        )

        FlurryHelper().init(this)

        val callback = ILoginNeedCallback {

        }

        VolleyHelper.getInstance().init("status", "data", "", false, callback);
    }


}