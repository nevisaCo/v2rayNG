package com.v2ray.ang.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import co.dev.BuyController
import co.dev.Config
import co.nevisa.commonlib.admob.AdmobController
import co.nevisa.commonlib.admob.cells.NativeAddCell
import com.google.android.gms.ads.nativead.NativeAd
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ItemQrcodeBinding
import com.v2ray.ang.databinding.ItemRecyclerFooterBinding
import com.v2ray.ang.databinding.ItemRecyclerMainBinding
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.ServerConfig
import com.v2ray.ang.dto.ServersCache
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.extension.toast
import com.v2ray.ang.helper.ItemTouchHelperAdapter
import com.v2ray.ang.helper.ItemTouchHelperViewHolder
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.util.*
import java.util.concurrent.TimeUnit

class MainRecyclerAdapter(activity: MainActivity) :
    RecyclerView.Adapter<MainRecyclerAdapter.BaseViewHolder>(), ItemTouchHelperAdapter {
    private val TAG: String = Config.TAG + "mra"
    private var mActivity: MainActivity = activity

    companion object {
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_FOOTER = 2
        private const val VIEW_TYPE_NATIVE = 3
    }


    private val mainStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_MAIN,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val subStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SUB, MMKV.MULTI_PROCESS_MODE) }
    private val settingsStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_SETTING,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val share_method: Array<out String> by lazy {
        mActivity.resources.getStringArray(R.array.share_method)
    }
    var isRunning = false

    override fun getItemCount() = items.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        try {
            if (holder is MainViewHolder) {
                val sc: ServersCache = items[position] as ServersCache
                val guid = sc.guid
                val config = sc.config
    //            //filter
    //            if (mActivity.mainViewModel.subscriptionId.isNotEmpty()
    //                && mActivity.mainViewModel.subscriptionId != config.subscriptionId
    //            ) {
    //                holder.itemMainBinding.cardView.visibility = View.GONE
    //            } else {
    //                holder.itemMainBinding.cardView.visibility = View.VISIBLE
    //            }

                val outbound = config.getProxyOutbound()
                val aff = MmkvManager.decodeServerAffiliationInfo(guid)
    //            U+1F1EE U+1F1F3
                holder.itemMainBinding.tvName.text =
                    getCountryFlag("U+1F1EE U+1F1F3") + config.remarks
    //            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                holder.itemMainBinding.txtPingResult.text = aff?.getTestDelayString() ?: ""
                if ((aff?.testDelayMillis ?: 0L) < 0L) {
                    holder.itemMainBinding.txtPingResult.setTextColor(
                        ContextCompat.getColor(
                            mActivity,
                            R.color.colorPingRed
                        )
                    )
                } else {
                    holder.itemMainBinding.txtPingResult.setTextColor(
                        ContextCompat.getColor(
                            mActivity,
                            R.color.colorPing
                        )
                    )
                }

                if (guid == mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)) {
                    holder.itemMainBinding.layoutIndicator.setBackgroundResource(R.color.colorSelected)
                } else {
                    holder.itemMainBinding.layoutIndicator.setBackgroundResource(R.color.colorUnselected)
                }
                holder.itemMainBinding.tvSubscription.text = ""
                val json = subStorage?.decodeString(config.subscriptionId)
                if (!json.isNullOrBlank()) {
                    val sub = Gson().fromJson(json, SubscriptionItem::class.java)
                    holder.itemMainBinding.tvSubscription.text = sub.remarks
                }

                var shareOptions = share_method.asList()
                when (config.configType) {
                    EConfigType.CUSTOM -> {
                        holder.itemMainBinding.tvType.text =
                            mActivity.getString(R.string.server_customize_config)
                        shareOptions = shareOptions.takeLast(1)
                    }
                    EConfigType.VLESS -> {
                        holder.itemMainBinding.tvType.text = config.configType.name
                    }
                    else -> {
                        holder.itemMainBinding.tvType.text = config.configType.name.lowercase()
                    }
                }
                "${outbound?.getServerAddress()} : ${outbound?.getServerPort()}".also {
                    holder.itemMainBinding.tvStatistics.text = it
                }

                holder.itemMainBinding.imgShare.setOnClickListener {
                    AlertDialog.Builder(mActivity).setItems(shareOptions.toTypedArray()) { _, i ->
                        try {
                            when (i) {
                                0 -> {
                                    if (config.configType == EConfigType.CUSTOM) {
                                        shareFullContent(guid)
                                    } else {
                                        val ivBinding =
                                            ItemQrcodeBinding.inflate(LayoutInflater.from(mActivity))
                                        ivBinding.ivQcode.setImageBitmap(
                                            AngConfigManager.share2QRCode(
                                                guid
                                            )
                                        )
                                        AlertDialog.Builder(mActivity).setView(ivBinding.root).show()
                                    }
                                }
                                1 -> {
                                    if (AngConfigManager.share2Clipboard(mActivity, guid) == 0) {
                                        mActivity.toast(R.string.toast_success)
                                    } else {
                                        mActivity.toast(R.string.toast_failure)
                                    }
                                }
                                2 -> shareFullContent(guid)
                                else -> mActivity.toast("else")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.show()
                }

                holder.itemMainBinding.imgEdit.setOnClickListener {
                    val intent = Intent().putExtra("guid", guid)
                        .putExtra("isRunning", isRunning)
                    if (config.configType == EConfigType.CUSTOM) {
                        mActivity.startActivity(
                            intent.setClass(
                                mActivity,
                                ServerCustomConfigActivity::class.java
                            )
                        )
                    } else {
                        mActivity.startActivity(intent.setClass(mActivity, ServerActivity::class.java))
                    }
                }

                holder.itemMainBinding.imgDelete.setOnClickListener {
                    if (guid != mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)) {
                        if (settingsStorage?.decodeBool(AppConfig.PREF_CONFIRM_REMOVE) == true) {
                            AlertDialog.Builder(mActivity).setMessage(R.string.del_config_comfirm)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    removeServer(guid, position)
                                }
                                .show()
                        } else {
                            removeServer(guid, position)
                        }
                    }
                }

                holder.itemMainBinding.itemBg.setOnClickListener {
                    val selected = mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)
                    if (guid != selected) {
                        val result = BuyController.buy(config) {
                            if (it == 1) {
                                BuyController.showDialog(mActivity)
                            } else if (it == 2) {
                                mActivity.updateCoins(0)
                            }
                        }

                        if (!result) {
                            return@setOnClickListener
                        }

                        mainStorage?.encode(MmkvManager.KEY_SELECTED_SERVER, guid)
                        if (!TextUtils.isEmpty(selected)) {
                            updateRows(getPosition(selected!!))
                        }
                        updateRows(getPosition(guid))

                        if (isRunning) {
                            mActivity.showCircle()
                            Utils.stopVService(mActivity)
                            Observable.timer(500, TimeUnit.MILLISECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    V2RayServiceManager.startV2Ray(mActivity)
                                }
                        }
                    }
                }

                updateRowUi(holder, config)

            } else if (holder is FooterViewHolder) {
                //if (activity?.defaultDPreference?.getPrefBoolean(AppConfig.PREF_INAPP_BUY_IS_PREMIUM, false)) {
                if (true) {
                    holder.itemFooterBinding.layoutEdit.visibility = View.INVISIBLE
                } else {
                    holder.itemFooterBinding.layoutEdit.setOnClickListener {
                        Utils.openUri(
                            mActivity,
                            "${Utils.decode(AppConfig.promotionUrl)}?t=${System.currentTimeMillis()}"
                        )
                    }
                }
            } else {
                val nativeAdView = holder.itemView as NativeAddCell
                nativeAdView.setBackgroundResource(R.drawable.server_item_bg)
                val ad: NativeAd = items[position] as NativeAd
                nativeAdView.setStyle(
                    android.R.attr.selectableItemBackground,
                    0,
                    0,
                    10,
                    0,
                    0,
                    null
                )
                nativeAdView.setAdd(ad)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onBindViewHolder>>>>>>>>>>>>: ",e )
        }
    }

    private fun getCountryFlag(flag: String): String {
        if (flag.isEmpty()) {
            return ""
        }
        val a = try {
            val l = flag
                .replace(" ", ",")
                .replace("\n", ",")
                .split(',');
            var result = ""
            l.forEach() {
                val item = it.replace("U+", "0x").toInt()
                result += String(Character.toChars(item))
            }
            result
        } catch (e: Exception) {
           ""
        }
        Log.i(TAG, "getCountryFlag: $a")
        return a
    }

    private fun shareFullContent(guid: String) {
        if (AngConfigManager.shareFullContent2Clipboard(mActivity, guid) == 0) {
            mActivity.toast(R.string.toast_success)
        } else {
            mActivity.toast(R.string.toast_failure)
        }
    }

    private fun removeServer(guid: String, position: Int) {
        mActivity.mainViewModel.removeServer(guid)
        updateRows(position, true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM ->
                MainViewHolder(
                    ItemRecyclerMainBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            VIEW_TYPE_FOOTER -> {
                FooterViewHolder(
                    ItemRecyclerFooterBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> NativeViewHolder(NativeAddCell(mActivity, true, true))

        }
    }

    override fun getItemViewType(position: Int): Int {
        val i = if (items[position] is String) {
            VIEW_TYPE_FOOTER
        } else if (items[position] is NativeAd) {
            VIEW_TYPE_NATIVE
        } else {
            VIEW_TYPE_ITEM
        }
        return i;
    }

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY)
        }

        fun onItemClear() {
            itemView.setBackgroundColor(0)
        }
    }

    class MainViewHolder(val itemMainBinding: ItemRecyclerMainBinding) :
        BaseViewHolder(itemMainBinding.root), ItemTouchHelperViewHolder

    class FooterViewHolder(val itemFooterBinding: ItemRecyclerFooterBinding) :
        BaseViewHolder(itemFooterBinding.root), ItemTouchHelperViewHolder

    class NativeViewHolder(native: NativeAddCell) :
        BaseViewHolder(native), ItemTouchHelperViewHolder

    override fun onItemDismiss(position: Int) {
        val sc: ServersCache = items.getOrNull(position) as ServersCache
        val guid = sc.guid
        if (guid != mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)) {
//            mActivity.alert(R.string.del_config_comfirm) {
//                positiveButton(android.R.string.ok) {
            mActivity.mainViewModel.removeServer(guid)
            updateRows(position, true)
//                show()
//            }
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (items[fromPosition] is ServersCache && items[toPosition] is ServersCache) {

            val ssc: ServersCache = items.getOrNull(fromPosition) as ServersCache;
            val sPosition: Int = mActivity.mainViewModel.getPosition(ssc.guid)

            val dsc: ServersCache = items.getOrNull(toPosition) as ServersCache;
            val dPosition: Int = mActivity.mainViewModel.getPosition(dsc.guid)

            Collections.swap(items, fromPosition, toPosition)
            mActivity.mainViewModel.swapServer(sPosition, dPosition)

            notifyItemMoved(fromPosition, toPosition)
            // position is changed, since position is used by click callbacks, need to update range
            if (toPosition > fromPosition)
                notifyItemRangeChanged(fromPosition, toPosition - fromPosition + 1)
            else
                notifyItemRangeChanged(toPosition, fromPosition - toPosition + 1)
            return true
        }
        return false
    }

    override fun onItemMoveCompleted() {
        // do nothing
    }


    //customized:
    private var items: MutableList<Any> = ArrayList()

    init {
        updateRows(0)
    }

    internal fun updateRows(position: Int) {
        updateRows(position, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    internal fun updateRows(position: Int, delete: Boolean) {
        items.clear()
        items.addAll(mActivity.mainViewModel.serversCache)

        AdmobController.getInstance().applyNativeOnCollection("servers", items as ArrayList<Any>?) {
            updateRows(-1, false)
        }

        items.add("footer")
        if (position >= 0) {
            if (delete) {
                notifyItemRemoved(position)
            } else {
                notifyItemChanged(position)
            }
        } else {
            notifyDataSetChanged()
        }
        notifyDataSetChanged()
    }

    private fun getPosition(guid: String): Int {
        items.forEachIndexed { index, it ->
            if (it is ServersCache) {
                if (it.guid == guid)
                    return index
            }
        }
        return -1
    }


    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun updateRowUi(holder: MainViewHolder, config: ServerConfig) {
        val cc = config.customConfig
        val visibility = if (cc == null) View.VISIBLE else View.INVISIBLE
        holder.itemMainBinding.tvStatistics.visibility = visibility
        holder.itemMainBinding.imgShare.visibility = visibility
        holder.itemMainBinding.imgEdit.visibility = visibility
        holder.itemMainBinding.txtUsage.visibility = View.INVISIBLE

        holder.itemMainBinding.txtPingResult.visibility =
            if (holder.itemMainBinding.txtPingResult.text.isNullOrEmpty()) {
                View.INVISIBLE
            } else View.VISIBLE

        holder.itemMainBinding.tvSubscription.visibility =
            if (holder.itemMainBinding.tvSubscription.text.isNullOrEmpty()) {
                View.INVISIBLE
            } else View.VISIBLE

        holder.itemMainBinding.txtPrice.visibility = View.INVISIBLE
        holder.itemMainBinding.txtUsage.visibility = View.INVISIBLE

        if (cc == null) {
            return
        }
        holder.itemMainBinding.txtUsage.visibility = View.VISIBLE
        holder.itemMainBinding.txtPrice.visibility = View.VISIBLE

        if (cc.price > 0) {
            holder.itemMainBinding.txtPrice.text = "${cc.price} Diamonds"
            holder.itemMainBinding.txtPrice.setTextColor(
                ContextCompat.getColor(
                    mActivity,
                    R.color.teal_200
                )
            )
        } else {
            holder.itemMainBinding.txtPrice.text = "Free"
            holder.itemMainBinding.txtPrice.setTextColor(
                ContextCompat.getColor(
                    mActivity,
                    R.color.colorPing
                )
            )
        }

        holder.itemMainBinding.txtUsage.text = "${cc.usage}"

        holder.itemMainBinding.imgEdit.setOnClickListener(null)
        holder.itemMainBinding.imgShare.setOnClickListener(null)


    }
}
