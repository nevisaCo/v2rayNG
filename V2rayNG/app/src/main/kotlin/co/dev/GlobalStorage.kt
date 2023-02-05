package co.dev

import co.nevisa.commonlib.utils.Cryptography
import com.tencent.mmkv.MMKV
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.util.MmkvManager

object GlobalStorage {
    private val globalStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.KEY_GLOBAL,
            MMKV.MULTI_PROCESS_MODE
        )
    }

    fun apiUrl(url: String) {
        globalStorage?.encode("apiUrl", url)
    }

    fun apiUrl(): String {
        var s = globalStorage?.decodeString("apiUrl", "") ?: ""
        if (s.isEmpty()) {
            s = BuildConfig.API_URL
        }
        return get(s)
    }

    private fun get(s: String): String {
        return try {
            Cryptography.get(s, BuildConfig.APPLICATION_ID.replace(".debug", ""))
        } catch (e: Exception) {
            s
        }
    }

    fun flurryId(id: String) {
        globalStorage?.encode("flurryId", id)
    }

    fun flurryId(): String {
        var s = globalStorage?.decodeString("flurryId", "") ?: ""
        if (s.isEmpty()) {
            s = BuildConfig.FLURRY_APP_ID
        }
        return get(s)
    }

    fun privacyUrl(id: String) {
        globalStorage?.encode("privacyUrl", id)
    }

    fun privacyUrl(): String {
        var s = globalStorage?.decodeString("privacyUrl", "") ?: ""
        if (s.isEmpty()) {
            s = BuildConfig.PRIVACY_URL
        }
        return s
    }

    fun socialMedia(id: String) {
        globalStorage?.encode("socialMedia", id)
    }

    fun socialMedia(): String {
        return globalStorage?.decodeString("socialMedia", "") ?: ""
    }

    fun sponsor(id: String) {
        globalStorage?.encode("sponsor", id)
        if (id.isEmpty()) {
            globalStorage.remove("sponsor")
        }
    }

    fun sponsor(): String {
        return globalStorage?.decodeString("sponsor", "") ?: ""
    }

    fun coins(id: Int) {
        globalStorage?.encode("coins", id)
    }

    fun coins(): Int {
        return globalStorage?.decodeInt("coins", 0) ?: 0
    }

    fun boughtServers(id: String) {
        globalStorage?.encode("boughtServers", id)
    }

    fun boughtServers(): String {
        return globalStorage?.decodeString("boughtServers", "") ?: ""
    }

    fun deviceId(id: String) {
        globalStorage?.encode("deviceId", id)
    }

    fun deviceId(): String {
        return globalStorage?.decodeString("deviceId", "") ?: ""
    }

    fun shownPrivacyScreen(status: Boolean) {
        globalStorage?.encode("shownPrivacyScreen", status)
    }

    fun shownPrivacyScreen(): Boolean {
        return globalStorage?.decodeBool("shownPrivacyScreen", false) ?: false
    }

    fun connectedTime(id: Long) {
        globalStorage?.encode("connectedTime", id)
    }

    fun connectedTime(): Long {
        return globalStorage?.decodeLong("connectedTime", 0L) ?: 0L
    }

    fun oldServerId(id: Int) {
        globalStorage?.encode("oldServerId", id)

    }

    fun oldServerId(): Int {
        return globalStorage?.decodeInt("oldServerId", 0) ?: 0
    }

    fun oldProtocolId(id: Int) {
        globalStorage?.encode("oldProtocolId", id)
    }

    fun oldProtocolId(): Int {
        return globalStorage?.decodeInt("oldProtocolId", 0) ?: 0
    }

    fun serverCacheTime(id: Int) {
        globalStorage?.encode("serverCacheTime", id)
    }

    fun serverCacheTime(): Int {
        return globalStorage?.decodeInt("serverCacheTime", 0) ?: 0
    }

}