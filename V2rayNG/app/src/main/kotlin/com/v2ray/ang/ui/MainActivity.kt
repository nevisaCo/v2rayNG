package com.v2ray.ang.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.marginStart
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import co.dev.Config
import co.dev.GlobalStorage
import co.dev.models.Protocol
import co.dev.models.ProtocolUsage
import co.dev.models.Social
import co.dev.models.Sponsor
import co.nevisa.commonlib.admob.AdmobBaseClass.*
import co.nevisa.commonlib.admob.AdmobController
import co.nevisa.commonlib.admob.models.CountItem
import co.nevisa.commonlib.admob.models.NativeObject
import co.nevisa.commonlib.firebase.models.Dialog
import co.nevisa.commonlib.firebase.update.UpdateApp
import co.nevisa.commonlib.update.GoogleUpdateHelper
import co.nevisa.commonlib.utils.Cryptography
import co.nevisa.commonlib.volley.VolleyCallback
import co.nevisa.commonlib.volley.VolleyHelper
import co.nevisa.commonlib.volley.cache.CacheItem
import com.github.florent37.viewanimator.ViewAnimator
import com.github.florent37.viewanimator.ViewAnimator.INFINITE
import com.github.florent37.viewanimator.ViewAnimator.animate
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.tbruyelle.rxpermissions.RxPermissions
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityMainBinding
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.helper.SimpleItemTouchHelperCallback
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.util.*
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.*
import me.drakeet.support.toast.ToastCompat
import org.json.JSONObject
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding

    private val adapter by lazy { MainRecyclerAdapter(this) }
    private val mainStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_MAIN,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val settingsStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_SETTING,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                startV2Ray()
            }
        }
    private var mItemTouchHelper: ItemTouchHelper? = null
    val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        title = getString(R.string.app_name)
        if (Config.DEBUG_VERSION) {
            title = getString(R.string.app_name) + " beta"
        }
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener {

            connectClick(it)//customized:

            if (mainViewModel.isRunning.value == true || binding.fabProgressCircle.visibility == View.VISIBLE) {
                hideCircle()
                Utils.stopVService(this)
            } else if ((settingsStorage?.decodeString(AppConfig.PREF_MODE) ?: "VPN") == "VPN") {

                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startV2Ray()
                } else {
                    requestVpnPermission.launch(intent)
                }
            } else {
                startV2Ray()
            }
        }
        binding.tvTestState.setOnClickListener {
            if (mainViewModel.isRunning.value == true) {
                setTestState(getString(R.string.connection_test_testing))
                mainViewModel.testCurrentServerRealPing()
            } else {
                binding.tvTestState.text = getString(R.string.connection_test_fail)
            }
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val callback = SimpleItemTouchHelperCallback(adapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(binding.recyclerView)


        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
        "v${BuildConfig.VERSION_NAME} (${SpeedtestUtil.getLibVersion()})".also {
            binding.version.text = it
        }

        setupViewModel()
        copyAssets()
        migrateLegacy()

        customOnCreate()


    }


    private fun setupViewModel() {

        mainViewModel.updateListAction.observe(this) { position ->
            adapter.updateRows(position)
            Log.i(TAG, "setupViewModel: position:$position")
        }
        mainViewModel.updateTestResultAction.observe(this) { setTestState(it) }

        mainViewModel.isRunning.observe(this) { isRunning ->
            adapter.isRunning = isRunning
            if (isRunning) {
                binding.fab.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSelected))
            } else {
                binding.fab.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorUnselected))
            }
            hideCircle()

            updateConnectAnimation(isRunning)//customized:
        }
        mainViewModel.startListenBroadcast()
    }


    private fun copyAssets() {
        val extFolder = Utils.userAssetPath(this)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geo = arrayOf("geosite.dat", "geoip.dat")
                assets.list("")
                    ?.filter { geo.contains(it) }
                    ?.filter { !File(extFolder, it).exists() }
                    ?.forEach {
                        val target = File(extFolder, it)
                        assets.open(it).use { input ->
                            FileOutputStream(target).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.i(
                            ANG_PACKAGE,
                            "Copied from apk assets folder to ${target.absolutePath}"
                        )
                    }
            } catch (e: Exception) {
                Log.e(ANG_PACKAGE, "asset copy failed", e)
            }
        }
    }

    private fun migrateLegacy() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.migrateLegacyConfig(this@MainActivity)
            if (result != null) {
                launch(Dispatchers.Main) {
                    if (result) {
                        toast(getString(R.string.migration_success))
                        mainViewModel.reloadServerList()
                    } else {
                        toast(getString(R.string.migration_fail))
                    }
                }
            }
        }
    }

    fun startV2Ray() {
        if (mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER).isNullOrEmpty()) {
            return
        }
        showCircle()
//        toast(R.string.toast_services_start)
        V2RayServiceManager.startV2Ray(this)
//        hideCircle()
    }

    fun restartV2Ray() {
        if (mainViewModel.isRunning.value == true) {
            Utils.stopVService(this)
        }
        Observable.timer(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                startV2Ray()
            }
    }

    public override fun onResume() {
        super.onResume()
        mainViewModel.reloadServerList()

        GoogleUpdateHelper.getInstance().resume()

        if (!hasSponsor) {
            showNativeAsSponsor()
        }

        resumeChronometer()
    }

    public override fun onPause() {
        super.onPause()
        pauseChronometer()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        initCustomOptionMenu(menu)

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        AdmobController.getInstance().showInterstitial("action_menu")
        return when (item.itemId) {
            R.id.import_qrcode -> {
                importQRcode(true)
                true
            }
            R.id.import_clipboard -> {
                importClipboard()
                true
            }
            R.id.import_manually_vmess -> {
                importManually(EConfigType.VMESS.value)
                true
            }
            R.id.import_manually_vless -> {
                importManually(EConfigType.VLESS.value)
                true
            }
            R.id.import_manually_ss -> {
                importManually(EConfigType.SHADOWSOCKS.value)
                true
            }
            R.id.import_manually_socks -> {
                importManually(EConfigType.SOCKS.value)
                true
            }
            R.id.import_manually_trojan -> {
                importManually(EConfigType.TROJAN.value)
                true
            }
            R.id.import_config_custom_clipboard -> {
                importConfigCustomClipboard()
                true
            }
            R.id.import_config_custom_local -> {
                importConfigCustomLocal()
                true
            }
            R.id.import_config_custom_url -> {
                importConfigCustomUrlClipboard()
                true
            }
            R.id.import_config_custom_url_scan -> {
                importQRcode(false)
                true
            }

            //        R.id.sub_setting -> {
            //            startActivity<SubSettingActivity>()
            //            true
            //        }

            R.id.sub_update -> {
                importConfigViaSub()
                true
            }

            R.id.export_all -> {
                if (AngConfigManager.shareNonCustomConfigsToClipboard(
                        this,
                        mainViewModel.serverList
                    ) == 0
                ) {
                    toast(R.string.toast_success)
                } else {
                    toast(R.string.toast_failure)
                }
                true
            }

            R.id.ping_all -> {
                mainViewModel.testAllTcping()
                true
            }

            R.id.real_ping_all -> {
                mainViewModel.testAllRealPing()
                true
            }

            R.id.service_restart -> {
                restartV2Ray()
                true
            }

            R.id.del_all_config -> {
                AlertDialog.Builder(this, R.style.AlertDialogCustom)
                    .setIcon(R.drawable.outline_delete_24)
                    .setTitle(R.string.del_config_comfirm)
                    .setMessage(getString(R.string.delete_all_server_message))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        MmkvManager.removeAllServer()
                        mainViewModel.reloadServerList()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
                true
            }

            R.id.del_invalid_config -> {
                AlertDialog.Builder(this, R.style.AlertDialogCustom)
                    .setIcon(R.drawable.outline_delete_24)
                    .setTitle(R.string.del_config_comfirm)
                    .setMessage(getString(R.string.delete_all_invalid_server_message))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        MmkvManager.removeInvalidServer()
                        mainViewModel.reloadServerList()
                    }.setNegativeButton(getString(R.string.cancel), null)
                    .show()
                true
            }

            R.id.sort_by_test_results -> {
                MmkvManager.sortByTestResults()
                mainViewModel.reloadServerList()
                true
            }

            R.id.filter_config -> {
                mainViewModel.filterConfig(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun importManually(createConfigType: Int) {
        startActivity(
            Intent()
                .putExtra("createConfigType", createConfigType)
                .putExtra("subscriptionId", mainViewModel.subscriptionId)
                .setClass(this, ServerActivity::class.java)
        )
    }

    /**
     * import config from qrcode
     */
    fun importQRcode(forConfig: Boolean): Boolean {
//        try {
//            startActivityForResult(Intent("com.google.zxing.client.android.SCAN")
//                    .addCategory(Intent.CATEGORY_DEFAULT)
//                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), requestCode)
//        } catch (e: Exception) {
        RxPermissions(this)
            .request(Manifest.permission.CAMERA)
            .subscribe {
                if (it)
                    if (forConfig)
                        scanQRCodeForConfig.launch(Intent(this, ScannerActivity::class.java))
                    else
                        scanQRCodeForUrlToCustomConfig.launch(
                            Intent(
                                this,
                                ScannerActivity::class.java
                            )
                        )
                else
                    toast(R.string.toast_permission_denied)
            }
//        }
        return true
    }

    private val scanQRCodeForConfig =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                importBatchConfig(it.data?.getStringExtra("SCAN_RESULT"))
            }
        }

    private val scanQRCodeForUrlToCustomConfig =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                importConfigCustomUrl(it.data?.getStringExtra("SCAN_RESULT"))
            }
        }

    /**
     * import config from clipboard
     */
    private fun importClipboard()
            : Boolean {
        try {
            val clipboard = Utils.getClipboard(this)
            importBatchConfig(clipboard)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun importBatchConfig(server: String?, subid: String = "") {
        importBatchConfig(server, subid, null)
    }

    private fun importBatchConfig(
        server: String?,
        subid: String = "",
        customConfig: Protocol?
    ) {
        val subid2 = if (subid.isEmpty()) {
            mainViewModel.subscriptionId
        } else {
            subid
        }
        val append = subid.isEmpty()

        var count = AngConfigManager.importBatchConfig(server, subid2, append, customConfig)
        if (count <= 0) {
            count = AngConfigManager.importBatchConfig(
                Utils.decode(server!!),
                subid2,
                append,
                customConfig
            )
        }
        if (count > 0) {
            toast(R.string.toast_success)
            mainViewModel.reloadServerList()
        } else {
            toast(R.string.toast_failure)
        }
    }

    fun importConfigCustomClipboard()
            : Boolean {
        try {
            val configText = Utils.getClipboard(this)
            if (TextUtils.isEmpty(configText)) {
                toast(R.string.toast_none_data_clipboard)
                return false
            }
            importCustomizeConfig(configText)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from local config file
     */
    fun importConfigCustomLocal(): Boolean {
        try {
            showFileChooser()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importConfigCustomUrlClipboard()
            : Boolean {
        try {
            val url = Utils.getClipboard(this)
            if (TextUtils.isEmpty(url)) {
                toast(R.string.toast_none_data_clipboard)
                return false
            }
            return importConfigCustomUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from url
     */
    fun importConfigCustomUrl(url: String?): Boolean {
        try {
            if (!Utils.isValidUrl(url)) {
                toast(R.string.toast_invalid_url)
                return false
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val configText = try {
                    Utils.getUrlContentWithCustomUserAgent(url)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
                launch(Dispatchers.Main) {
                    importCustomizeConfig(configText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * import config from sub
     */
    fun importConfigViaSub()
            : Boolean {
        try {
            toast(R.string.title_sub_update)
            MmkvManager.decodeSubscriptions().forEach {
                if (TextUtils.isEmpty(it.first)
                    || TextUtils.isEmpty(it.second.remarks)
                    || TextUtils.isEmpty(it.second.url)
                ) {
                    return@forEach
                }
                if (!it.second.enabled) {
                    return@forEach
                }
                val url = Utils.idnToASCII(it.second.url)
                if (!Utils.isValidUrl(url)) {
                    return@forEach
                }
                Log.d(ANG_PACKAGE, url)
                lifecycleScope.launch(Dispatchers.IO) {
                    val configText = try {
                        Utils.getUrlContentWithCustomUserAgent(url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            toast("\"" + it.second.remarks + "\" " + getString(R.string.toast_failure))
                        }
                        return@launch
                    }
                    launch(Dispatchers.Main) {
                        importBatchConfig(configText, it.first)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * show file chooser
     */
    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            chooseFileForCustomConfig.launch(
                Intent.createChooser(
                    intent,
                    getString(R.string.title_file_chooser)
                )
            )
        } catch (ex: ActivityNotFoundException) {
            toast(R.string.toast_require_file_manager)
        }
    }

    private val chooseFileForCustomConfig =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri = it.data?.data
            if (it.resultCode == RESULT_OK && uri != null) {
                readContentFromUri(uri)
            }
        }

    /**
     * read content from uri
     */
    private fun readContentFromUri(uri: Uri) {
        RxPermissions(this)
            .request(Manifest.permission.READ_EXTERNAL_STORAGE)
            .subscribe {
                if (it) {
                    try {
                        contentResolver.openInputStream(uri).use { input ->
                            importCustomizeConfig(input?.bufferedReader()?.readText())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else
                    toast(R.string.toast_permission_denied)
            }
    }

    /**
     * import customize config
     */
    fun importCustomizeConfig(server: String?) {
        try {
            if (server == null || TextUtils.isEmpty(server)) {
                toast(R.string.toast_none_data)
                return
            }
            mainViewModel.appendCustomConfigServer(server)
            mainViewModel.reloadServerList()
            toast(R.string.toast_success)
            //adapter.notifyItemInserted(mainViewModel.serverList.lastIndex)
        } catch (e: Exception) {
            ToastCompat.makeText(
                this,
                "${getString(R.string.toast_malformed_josn)} ${e.cause?.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
            return
        }
    }

    fun setTestState(content: String?) {
        binding.tvTestState.text = content
    }

//    val mConnection = object : ServiceConnection {
//        override fun onServiceDisconnected(name: ComponentName?) {
//        }
//
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            sendMsg(AppConfig.MSG_REGISTER_CLIENT, "")
//        }
//    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    fun showCircle() {
        binding.fabProgressCircle.visibility = View.VISIBLE
        /* animWorld?.cancel()
         animWorld = animate(binding.imgWorld)
             .rotation(-360*10F)
             .interpolator(LinearInterpolator())
             .repeatCount(-1)
             .duration(500*10)
             .start()*/
    }

    private fun hideCircle() {
        try {
            job?.cancel()
            Observable.timer(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    try {
                        binding.fabProgressCircle.visibility = View.INVISIBLE
                    } catch (e: Exception) {
                        Log.w(ANG_PACKAGE, e)
                    }
                }
        } catch (e: Exception) {
            Log.d(ANG_PACKAGE, e.toString())
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            //R.id.server_profile -> activityClass = MainActivity::class.java
            R.id.sub_setting -> {
                startActivity(Intent(this, SubSettingActivity::class.java))
            }
            R.id.settings -> {
                startActivity(
                    Intent(this, SettingsActivity::class.java)
                        .putExtra("isRunning", mainViewModel.isRunning.value == true)
                )
            }
            R.id.user_asset_setting -> {
                startActivity(Intent(this, UserAssetActivity::class.java))
            }
            R.id.feedback -> {
                Utils.openUri(this, AppConfig.v2rayNGIssues)
            }

            R.id.apps -> {
                startActivity(Intent(this, PerAppProxyActivity::class.java))
            }

            R.id.promotion -> {
                doShare(
                    shareUrl
                        ?: ("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
                )
            }
            R.id.logcat -> {
                startActivity(Intent(this, LogcatActivity::class.java))
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        AdmobController.getInstance().showInterstitial("drawer_menu")
        return true
    }

    //region customized:
    val TAG = Config.TAG + "ma"

    @SuppressLint("HardwareIds")
    private fun customOnCreate() {
        startConnectAnimation()

        startWorldAnimation()

        getData()

        initAdmob()

        initAppUpdate()


        GlobalStorage.deviceId(
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        )

        initSponsor()

        initSocialMedia()

        initCoins()

        initChronometer()

        binding.tvTestState.tag = binding.tvTestState.currentTextColor
        mainViewModel.updateConnection.observe(this) {
            Log.i(TAG, "customOnCreate: $it")
            when (it) {
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    startChronometer()
                    increaseServerUsage()
                    binding.tvTestState.text = getString(R.string.connection_connected)
                    binding.tvTestState.setTextColor(Color.GREEN)

                    AdmobController.getInstance().showInterstitial("connect")
                    AdmobController.getInstance().showReward("connect", null, iRewardCallback)
                }
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    decreaseServerUsage()
                    stopChronometer()
                    binding.tvTestState.text = getString(R.string.connection_stopped)
                    binding.tvTestState.setTextColor(Color.YELLOW)

                    AdmobController.getInstance().showInterstitial("disconnect")
                    AdmobController.getInstance().showReward("disconnect", null, iRewardCallback)
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    binding.tvTestState.text = getString(R.string.connection_start_fail)
                    binding.tvTestState.setTextColor(Color.RED)
                }
                AppConfig.MSG_MEASURE_DELAY_SUCCESS -> {
                    binding.tvTestState.text = getString(R.string.ping_completed)
                    binding.tvTestState.setTextColor(Color.GREEN)
                }
                else -> {
                    binding.tvTestState.text = getString(R.string.connection_not_connected)
                    binding.tvTestState.setTextColor(binding.tvTestState.tag as Int)
                }
            }
        }


    }


    lateinit var chronometer: Chronometer
    private fun initChronometer() {
        chronometer = Chronometer(this)
        chronometer.onChronometerTickListener = Chronometer.OnChronometerTickListener {
            binding.txtTime.text = it.text
        }
        chronometer.isClickable = false
        chronometer.isFocusable = false
        chronometer.alpha = 0F
        binding.root.addView(chronometer, 0)

    }

    private fun initCoins() {
/*        if (!Config.FULL_VERSION) {
            return
        }*/
        binding.txtCoins.setOnClickListener {
            menuRewardItem.callOnClick()
        }
        updateCoins(0)

    }

    private fun startChronometer() {
        chronometer.start()
        chronometer.base = SystemClock.elapsedRealtime()
        GlobalStorage.connectedTime(chronometer.base)

    }

    @SuppressLint("SetTextI18n")
    private fun stopChronometer() {
        GlobalStorage.connectedTime(0)
        binding.txtTime.text = "00:00:00"
        chronometer.stop()
    }

    private fun pauseChronometer() {
        chronometer.stop()
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun resumeChronometer() {
        binding.txtTime.text = "00:00:00"
        val t0 = GlobalStorage.connectedTime()
        if (t0 == 0L) {
            return
        }
        chronometer.start()
        chronometer.base = t0
    }

    var shareUrl: String? = null
    private fun initSocialMedia() {
        try {
            val json = GlobalStorage.socialMedia()
            if (json.isEmpty()) {
                binding.socialPlace.visibility = View.GONE
                return
            }
            if (BuildConfig.DEBUG_VERSION) {
                Log.i(TAG, "initSocialMedia: $json")
            }
            val social: Social = Gson().fromJson(json, Social::class.java)

            binding.socialMediaInsta.visibility = getSocialVisibility(social.instagram)
            binding.socialMediaInsta.setOnClickListener { openExternalUrl(social.instagram) }

            binding.socialMediaTelegram.visibility = getSocialVisibility(social.telegram)
            binding.socialMediaTelegram.setOnClickListener { openExternalUrl(social.telegram) }

            binding.socialMediaFaceBook.visibility = getSocialVisibility(social.facebook)
            binding.socialMediaFaceBook.setOnClickListener { openExternalUrl(social.facebook) }

            binding.socialMediaTwiter.visibility = getSocialVisibility(social.twiter)
            binding.socialMediaTwiter.setOnClickListener { openExternalUrl(social.twiter) }

            binding.socialMediaYoutube.visibility = getSocialVisibility(social.youtube)
            binding.socialMediaYoutube.setOnClickListener { openExternalUrl(social.youtube) }

            binding.socialMediaTiktok.visibility = getSocialVisibility(social.tiktok)
            binding.socialMediaTiktok.setOnClickListener { openExternalUrl(social.tiktok) }

            binding.socialMediaShare.visibility = getSocialVisibility(social.share)
            binding.socialMediaShare.setOnClickListener { doShare(social.share) }
            shareUrl = social.share
        } catch (e: Exception) {
            Log.e(TAG, "initSocialMedia: ", e)
        }

    }

    private fun doShare(url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }
        ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setChooserTitle(R.string.app_name)
            .setText(url)
            .startChooser();
    }

    private fun getSocialVisibility(telegram: String?): Int {
        if (telegram.isNullOrEmpty()) {
            return View.GONE
        }
        return View.VISIBLE
    }


    var hasSponsor: Boolean = true
    private fun initSponsor() {
        try {
            binding.includeSponsor.sponsorLayout.visibility = View.INVISIBLE
            val json = GlobalStorage.sponsor()
            if (json.isNullOrEmpty()) {
                hasSponsor = false
                return


            }
            if (BuildConfig.DEBUG_VERSION) {
                Log.i(TAG, "initSponsor: $json")
            }
            val sponsor: Sponsor = Gson().fromJson(json, Sponsor::class.java)
            if (sponsor.link.isNullOrEmpty() && sponsor.title.isNullOrEmpty()) {
                Log.i(TAG, "initSponsor: sponsor link/title is empty")
                hasSponsor = false
                return
            }
            hasSponsor = true


            binding.includeSponsor.sponsorLayout.visibility = View.VISIBLE;
            binding.includeSponsor.sponsorLayout.alpha = 0F
            val anim = animate(binding.includeSponsor.sponsorLayout)
                .interpolator(LinearInterpolator())
                .alpha(0F, 1F)
                .duration(2000)


            if (sponsor.image!!.isNotEmpty()) {
                Picasso.get()
                    .load(sponsor.image)
                    .error(R.drawable.ic_launcher_background)
                    .into(binding.includeSponsor.image, object : Callback {
                        override fun onSuccess() {
                            anim.start()
                        }

                        override fun onError(e: java.lang.Exception?) {
                            anim.start()
                        }
                    })
            } else {
                anim.start()
            }

            binding.includeSponsor.txtTitle.text = sponsor.title ?: "No title"
            binding.includeSponsor.txtDescription.text =
                sponsor.description ?: "Click on the button to see more..."
            binding.includeSponsor.txtLabel.text = sponsor.label ?: "Sponsor"
            binding.includeSponsor.btnSee.text = sponsor.btnText ?: "Open"
            if (sponsor.link!!.isNotEmpty()) {
                binding.includeSponsor.btnSee.setOnClickListener {
                    openExternalUrl(sponsor.link)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "initSponsor: ", e)
        }

    }

    private fun showNativeAsSponsor() {
        AdmobController.getInstance().getNativeItem("sponsor") {
            Log.i(TAG, "showNativeAsSponsor: ${it.javaClass}")
            if (it is NativeObject) {
                val ad = it.nativeAd ?: return@getNativeItem

                val nav = NativeAdView(this)
                nav.setNativeAd(ad)
                nav.isSelected = true
                nav.callToActionView = binding.includeSponsor.btnSee
                binding.includeSponsor.sponsorLayout.addView(nav)

                binding.includeSponsor.txtTitle.text = ad.headline
                binding.includeSponsor.txtDescription.text = ad.body
                if (ad.images.isNotEmpty()) {
                    val img: NativeAd.Image = ad.images[0]
                    if (img.drawable != null) {
                        binding.includeSponsor.image.setImageDrawable(img.drawable)
                    }
                }
                binding.includeSponsor.btnSee.text = "Open"
                binding.includeSponsor.txtLabel.text = "Ad"

                binding.includeSponsor.sponsorLayout.visibility = View.VISIBLE;
                binding.includeSponsor.sponsorLayout.alpha = 0F
                val anim = animate(binding.includeSponsor.sponsorLayout)
                    .interpolator(LinearInterpolator())
                    .alpha(0F, 1F)
                    .duration(2000)
                anim.start()
            }
        }
    }

    private fun openExternalUrl(url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }
        var s = url;

        if (!url.startsWith("http")) {
            s = "http://$url"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s))
        startActivity(intent)
    }

    private fun initAppUpdate() {
        GoogleUpdateHelper.getInstance().init(this) {
            val snackbar = Snackbar.make(
                binding.drawerLayout,
                "An update has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("RESTART") { GoogleUpdateHelper.getInstance().completeUpdate() }
            snackbar.show()
        }

        checkPushUpdate()
    }

    private fun checkPushUpdate() {
        val updateApp = UpdateApp()
/*        val update = Update()
        update.version = 2
        update.isForce = true

        updateApp.setDebugData(update)*/
        updateApp.checkUpdate({
            val dialog = Dialog()
            dialog.btnCancelText = "Cancel"
            dialog.btnOkText = "Update"
            dialog.title = "Update App"
            dialog.content =
                "A new update is available. Update your app to take advantage of the latest changes."
            if (!it.text.isNullOrEmpty()) {
                dialog.content = it.text
            }
//            dialog.imageUrl = Config.URL + "/content/images/update_dialog.png"


            val alertDialog =
                android.app.AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
            alertDialog.setIcon(R.drawable.baseline_system_update_24)
            alertDialog.setTitle(dialog.title)
            alertDialog.setMessage(dialog.content)
            alertDialog.setPositiveButton(dialog.btnOkText) { dialogInterface, i ->
                val appPackageName =
                    packageName // getPackageName() from Context or Activity object
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$appPackageName")
                        )
                    )
                } catch (anfe: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                        )
                    )
                }
                dialogInterface.dismiss()
            }
            if (!it.isForce) {
                alertDialog.setNegativeButton(dialog.btnCancelText, null)
            }
            alertDialog.setCancelable(!it.isForce)
            alertDialog.create().show()

        }, BuildConfig.VERSION_CODE)
    }

    private fun updateConnectAnimation(isRunning: Boolean) {
        Log.i(TAG, "updateConnectAnimation: $isRunning")
        if (isRunning) {
            binding.imgAnimateCircle.clearAnimation()
            binding.imgAnimateCircle.visibility = View.INVISIBLE
            binding.imgWorld.setImageResource(R.drawable.btn_tap_connected)
/*            increaseServerUsage()

            Observable.timer(0, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                }*/
        } else {
//            decreaseServerUsage()
            binding.imgWorld.setImageResource(R.drawable.btn_tap_connecting)
            binding.imgAnimateCircle.visibility = View.VISIBLE
            startConnectAnimation()
        }
        job?.cancel()
    }

    var animWorldConnected: ViewAnimator? = null
    private fun startWorldAnimation() {
        if (!Config.FULL_VERSION){
            return
        }

        animWorldConnected?.cancel()
        animWorldConnected = animate(binding.imgWorld)
            .rotation(360 * 2F)
            .interpolator(LinearInterpolator())
            .repeatCount(-1)
            .duration(90 * 1000)
            .start()

    }

    private fun startConnectAnimation() {
//        animWorldConnected?.cancel()
        animate(binding.imgAnimateCircle)
            .repeatCount(INFINITE)
            .duration(1500)
            .scale(1F, 1.3F)
            .alpha(0F, .3F, 1F, 0.1F)
            .start()
    }


    private fun getData() {
        mainViewModel.reloadServerList()
        if (Config.PROTOCOLS_LIST.size > 0) {
            initData(Config.PROTOCOLS_LIST)
            Config.PROTOCOLS_LIST.clear()
            return
        }

        val cache = CacheItem("servers", Calendar.MINUTE, GlobalStorage.serverCacheTime())
        VolleyHelper.getInstance()
            .apply(
                VolleyHelper.GET,
                Config.PROTOCOLS, null,
                object : VolleyCallback<ArrayList<Protocol>> {
                    override fun onSuccess(p0: ArrayList<Protocol>) {
                        Log.i(TAG, "onSuccess: " + p0.size)
                        initData(p0)
                    }

                    override fun onFailure(p0: VolleyCallback.ErrorType?, p1: JSONObject?) {
                        Log.e(TAG, "onFailure: ")
                    }
                }, TAG, cache
            )
    }

    private fun initData(p: ArrayList<Protocol>) {
        Log.i(TAG, "initData: ")
        try {
            MmkvManager.updateServer(mainViewModel.serversCache.filter { c -> c.config.customConfig != null })
            p.onEach {
                val s = Cryptography.get(it.value, "key_${it.id}")
                it.value = ""
                importBatchConfig(s, "", it)
//                Log.i(TAG, "onSuccess:hash: $s")
            }
            mainViewModel.reloadServerList()
        } catch (e: Exception) {
            Log.e(TAG, "initData: ", e)
        }
    }

    private fun initAdmob() {
        val callback = object : IInitializeCallback {
            override fun before() {
                if (Config.DEBUG_VERSION) {
                    val countItems: ArrayList<CountItem> = ArrayList()
                    countItems.add(CountItem(10, "open_app"))
                    countItems.add(CountItem(10, "connect"))
                    countItems.add(CountItem(10, "disconnect"))
                    countItems.add(CountItem(10, "action_menu"))
                    countItems.add(CountItem(10, "drawer_menu"))
                    AdmobController.getInstance().setInterstitialTargets(countItems)
                    Log.i(
                        TAG,
                        "LaunchActivity > before: Interstitial count Items json:" + Gson().toJson(
                            countItems
                        )
                    )
                    countItems.clear()
                    countItems.add(CountItem(1, "servers", 5))
                    countItems.add(CountItem(1, "sponsor"))
                    countItems.add(CountItem(1, "drawer_footer"))
                    Log.i(
                        TAG,
                        "LaunchActivity > before: Native count Items json:" + Gson().toJson(
                            countItems
                        )
                    )
                    AdmobController.getInstance().setNativeTargets(countItems)

                    countItems.clear()
                    countItems.add(CountItem(19, "connect"))
                    countItems.add(CountItem(19, "disconnect"))
                    countItems.add(CountItem(1, "reward"))
                    countItems.add(CountItem(15, "open_app"))

                    Log.i(
                        TAG,
                        "LaunchActivity > before: reward count Items json:" + Gson().toJson(
                            countItems
                        )
                    )
                    AdmobController.getInstance().setRewardTargets(countItems)

                }
            }

            override fun onComplete() {
                Log.i(TAG, "onInterstitialServed: ")
                AdmobController.getInstance().showInterstitial("open_app")
                AdmobController.getInstance()
                    .showReward("open_app", null, iRewardCallback)

                AdmobController.getInstance().getNativeItem("drawer_footer") {
                    if (it == null) {
                        Log.i(TAG, "onComplete: null")
                        return@getNativeItem
                    }
                    Log.i(TAG, "onComplete: 0")
                    if (it is NativeObject) {
                        Log.i(TAG, "onComplete: 1")
                        binding.llDrawerFooter.addView(it.nativeAddCell, 0)

                    }
                }
            }
        }

        val servedCallback = IServedCallback { ad ->
            if (ad == null) {
                return@IServedCallback
            }

            if (ad is InterstitialAd) {
                AdmobController.getInstance().showInterstitial("open_app")
            }
            if (ad is NativeAd) {
                adapter.updateRows(-1)
            }

            if (ad is RewardedAd) {
                updateRewardIcon()
                AdmobController.getInstance()
                    .showReward("open_app", null, iRewardCallback)
            }
        }

        AdmobController.getInstance().init(this, callback, servedCallback)


    }

    lateinit var menuRewardItem: ImageView

    private fun updateRewardIcon() {
        animate(menuRewardItem)
            .repeatCount(INFINITE)
            .duration(1500)
            .scale(1F, 1.1F, 1.2F, 1.3F, 1.1F, 1F, 1F, 1F)
            .onStop {
                menuRewardItem.tag = false
                menuRewardItem.rotation = 0F
                menuRewardItem.setImageResource(R.drawable.baseline_diamond)
            }
            .start()
    }

    private fun initCustomOptionMenu(menu: Menu) {
        menuRewardItem = menu.findItem(R.id.menuSeeReward).actionView as ImageView
        menuRewardItem.setImageResource(R.drawable.baseline_diamond)

//        updateRewardIcon()

        menuRewardItem.tag = false
        menuRewardItem.setOnClickListener {
            if (menuRewardItem.tag == true) {
                return@setOnClickListener
            }
            menuRewardItem.tag = true
            menuRewardItem.setImageResource(R.drawable.baseline_change_circle)
            val anim = animate(menuRewardItem)
                .repeatCount(INFINITE)
                .rotation(360F)
                .duration(1000)
                .onStop {
                    menuRewardItem.tag = false
                    menuRewardItem.rotation = 0F
                    menuRewardItem.setImageResource(R.drawable.baseline_diamond)

                }
                .start()

            AdmobController.getInstance().showReward(
                "reward",
                null,
                object : IRewardCallback {
                    override fun onUserEarnedReward(p0: RewardItem) {
                        toast("rewarded coin:" + p0.amount)
                        anim.cancel()
                        iRewardCallback.onUserEarnedReward(p0)

                    }

                    override fun onFail(p0: Any?) {
                        anim.cancel()
                        iRewardCallback.onFail(p0)
                    }


                })
        }
    }

    private val iRewardCallback = object : IRewardCallback {
        override fun onUserEarnedReward(p0: RewardItem) {
            updateCoins(p0.amount);
        }

        override fun onFail(p0: Any?) {

        }
    }

    @SuppressLint("SetTextI18n")
    fun updateCoins(amount: Int) {
        var coins = GlobalStorage.coins()
        if (amount != 0) {
            coins += amount;
            GlobalStorage.coins(coins)
            animate(binding.txtCoins)
                .scale(1F, 1.2F, 1.4f, 1.2F, 1F, .9F, .8F, .9F, 1F)
                .duration(2000)
                .startDelay(3000)
                .start()
        }
        binding.txtCoins.text = "$coins"

    }

    var job: Job? = null
    private fun connectClick(it: View?) {
        //connection time out in 10 seccond
        job = GlobalScope.launch {
            repeat(1) {
                delay(1000 * 20)
                runOnUiThread() {
                    Utils.stopVService(baseContext)
                    hideCircle()
                    binding.tvTestState.text = getString(R.string.connection_timeout)
                    binding.tvTestState.setTextColor(Color.RED)
                }
                this.cancel()
                job = null
            }
        }
        setTestState(getString(R.string.connecting))

    }

    private fun decreaseServerUsage() {
        Log.i(TAG, "decreaseServerUsage: ")
        val pu = ProtocolUsage()
        pu.ServerId = GlobalStorage.oldServerId()
        pu.ProtocolId = GlobalStorage.oldProtocolId()
        pu.OldServerId = 0
        pu.OldProtocolId = 0
        toggleServerUsage(VolleyHelper.POST, pu)
    }

    private fun increaseServerUsage() {
        Log.i(TAG, "increaseServerUsage: ")
        val guid = mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER) ?: ""
        val pu = ProtocolUsage()
        val server = MmkvManager.decodeServerConfig(guid)

        if (server?.customConfig == null) {
            Log.e(TAG, "increaseServerUsage: custom config is null")
            return
        }

        pu.ServerId = server.customConfig!!.serverId
        pu.ProtocolId = server.customConfig!!.id
        pu.OldServerId = GlobalStorage.oldServerId()
        pu.OldProtocolId = GlobalStorage.oldProtocolId()

        if (pu.ServerId == pu.OldServerId && pu.ProtocolId == pu.OldProtocolId) {
            Log.i(TAG, "increaseServerUsage: the usage already set!")
            return
        }
        toggleServerUsage(VolleyHelper.PUT, pu)

    }

    private fun toggleServerUsage(method: Int, pu: ProtocolUsage) {
        val url = Config.PROTOCOLS_USAGE

        if (pu.ServerId == 0 || pu.ProtocolId == 0) {
            Log.i(TAG, "toggleServerUsage: server or protocol id is zero")
            return
        }
        try {
            pu.DeviceId = GlobalStorage.deviceId()
        } catch (e: Exception) {
            Log.e(TAG, "toggleServerUsage : ", e)
        }

        VolleyHelper.getInstance()
            .apply(method, url, pu.getJson(), object : VolleyCallback<String> {
                override fun onSuccess(p0: String?) {
                    if (method == VolleyHelper.PUT) {
                        GlobalStorage.oldServerId(pu.ServerId)
                        GlobalStorage.oldProtocolId(pu.ProtocolId)
                    } else {
                        GlobalStorage.oldServerId(0)
                        GlobalStorage.oldProtocolId(0)
                    }
                    Log.i(TAG, "toggleServerUsage > onSuccess: " + p0)
                }

                override fun onFailure(p0: VolleyCallback.ErrorType?, p1: JSONObject?) {
                    Log.e(TAG, "toggleServerUsage > onFailure: ")
                }
            })

    }
//endregion
}
