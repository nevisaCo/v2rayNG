package co.dev

import co.dev.models.Protocol
import com.tencent.mmkv.MMKV
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.util.MmkvManager
import java.util.ArrayList


object Config {
    var PROTOCOLS_LIST: ArrayList<Protocol> = ArrayList<Protocol>() ;

    val URL: String = GlobalStorage.apiUrl()
    val TAG: String = "v2rayvpn"
    val DEBUG_VERSION: Boolean = BuildConfig.DEBUG_VERSION
    val API: String = URL + "/api/"
    val PROTOCOLS: String = API + "protocols"
    val PROTOCOLS_USAGE: String = API + "protocols"
}

