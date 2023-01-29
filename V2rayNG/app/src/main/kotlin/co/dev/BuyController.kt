package co.dev

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.tencent.mmkv.MMKV
import com.v2ray.ang.R
import com.v2ray.ang.dto.ServerConfig
import com.v2ray.ang.ui.MainActivity
import com.v2ray.ang.util.MmkvManager

object BuyController {

    fun interface IBuyCallback {
        fun onResult(id: Int)
    }

    fun buy(config: ServerConfig, iBuyCallback: IBuyCallback?): Boolean {
        val price = config.customConfig?.price ?: 0
        val serverId = config.customConfig?.serverId ?: 0

        if (price > 0 && serverId > 0) {

            val servers = GlobalStorage.boughtServers()
            if (servers.contains("-$serverId-")) {
                iBuyCallback?.onResult(0)
                return true
            }

            var coins = GlobalStorage.coins()
            if (coins < price) {
                iBuyCallback?.onResult(1)
                return false
            }
            coins -= price
            GlobalStorage.coins(coins)
            GlobalStorage.boughtServers("-${config.customConfig!!.serverId}-")
        }
        iBuyCallback?.onResult(2)
        return true
    }

    fun showDialog(activity: Activity) {
        val builder =
            AlertDialog.Builder(activity, R.style.AlertDialogCustom)
        builder.setIcon(R.drawable.ic_diamond)
        builder.setCancelable(false)
        builder.setTitle(R.string.diamond_not_title)
        builder.setMessage(R.string.coin_error)

        builder.setPositiveButton("See Now") { dialog, _ ->
            if (activity is MainActivity) {
                activity.menuRewardItem.callOnClick()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(android.R.string.no) { dialog, _ ->
            dialog.dismiss()

        }

        builder.show()


    }
}