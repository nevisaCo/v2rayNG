package co.dev

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import co.dev.models.Protocol
import co.nevisa.commonlib.utils.Cryptography
import co.nevisa.commonlib.volley.VolleyCallback
import co.nevisa.commonlib.volley.VolleyHelper
import co.nevisa.commonlib.volley.cache.CacheItem
import com.github.florent37.viewanimator.AnimationBuilder
import com.github.florent37.viewanimator.ViewAnimator
import com.github.florent37.viewanimator.ViewAnimator.animate
import com.tencent.mmkv.MMKV
import com.v2ray.ang.R
import com.v2ray.ang.ui.BaseActivity
import com.v2ray.ang.ui.MainActivity
import com.v2ray.ang.util.MmkvManager
import org.json.JSONObject
import java.util.*

class WelcomeActivity : BaseActivity() {


    private val TAG: String = Config.TAG + "wa"
    var anim: ViewAnimator? = null
    private var btnAccept: Button? = null;
    private var progressBar: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val seenPrivacy = GlobalStorage.shownPrivacyScreen()

        if (seenPrivacy) {
            startActivity()
            setContentView(R.layout.activity_welcom)
            progressBar = findViewById(R.id.progressBar);
            progressBar?.visibility = View.VISIBLE


            val imageButton = findViewById<ImageView>(R.id.imgWorld)
            if (Config.FULL_VERSION) {
                imageButton.visibility = View.VISIBLE
                progressBar?.visibility = View.GONE
            }
            anim = animate(imageButton)
                .rotation(360 * 5F)
                .duration(1000 * 20)
                .interpolator(LinearInterpolator())
                .onStop {
                    runOnUiThread {
                        startActivity()
                    }
                }
                .start()
        } else {
            setContentView(R.layout.activity_privacy)

            btnAccept = findViewById(R.id.btnAccept)
            progressBar = findViewById(R.id.progressBar);

            btnAccept?.setOnClickListener {
                GlobalStorage.shownPrivacyScreen(true)
                it.isEnabled = false
                startActivity()
            }
            findViewById<TextView>(R.id.txtMore)
                .setOnClickListener {
                    var url = GlobalStorage.privacyUrl()
                    Log.i(TAG, "onCreate: $url")

                    if (url.isEmpty()) {
                        return@setOnClickListener
                    }

                    if (!url.startsWith("http")) {
                        url = "http://$url"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

                    startActivity(intent)
                }

        }
        getData()

    }

    var i: Int = 0;
    private fun startActivity() {
        i++
        if (i >= 2) {
            val intent = Intent(this, MainActivity::class.java)
            anim?.cancel()
            startActivity(intent)
        }

    }

    private fun getData() {
        progressBar?.visibility = View.VISIBLE
        val cache = CacheItem("servers", Calendar.MINUTE, GlobalStorage.serverCacheTime())
        VolleyHelper.getInstance()
            .apply(
                VolleyHelper.GET,
                Config.PROTOCOLS, null,
                object : VolleyCallback<ArrayList<Protocol>> {
                    override fun onSuccess(p0: ArrayList<Protocol>) {
                        Log.i(TAG, "onSuccess: ")
                        Config.PROTOCOLS_LIST = p0
                        progressBar?.visibility = View.GONE
                        btnAccept?.isEnabled = true
                        startActivity()
                    }

                    override fun onFailure(p0: VolleyCallback.ErrorType?, p1: JSONObject?) {
                        Log.e(TAG, "onFailure: ")
                        startActivity()
                        progressBar?.visibility = View.GONE
                        btnAccept?.isEnabled = true
                    }
                }, TAG, cache
            )
    }
}