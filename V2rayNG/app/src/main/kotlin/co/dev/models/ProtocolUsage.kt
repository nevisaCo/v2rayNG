package co.dev.models

import android.util.Log
import co.dev.Config
import com.google.gson.Gson
import org.json.JSONObject

class ProtocolUsage : java.io.Serializable {
//    @SerializedName("server_id")
    var ServerId: Int = 0

//    @SerializedName("protocol_id")
    var ProtocolId: Int = 0
    var OldServerId: Int = 0
    var OldProtocolId: Int = 0
    var DeviceId: String = ""

    fun getJson(): JSONObject {
        val json = Gson().toJson(this)
        Log.i(Config.TAG + "pu", "getJson: " + json)
        return JSONObject(json)
    }

}