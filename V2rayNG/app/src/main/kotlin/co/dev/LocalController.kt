package co.dev

import android.content.Context
import com.v2ray.ang.AngApplication
import com.v2ray.ang.R

fun getString(id: Int): String {

    return getString(id, AngApplication.appContext)
}

fun getString(id: Int, context: Context): String {
    var s = context.getString(id);
    return s.replace("v2rayNG", context.getString(R.string.app_name))
}

class LocalController {

}
