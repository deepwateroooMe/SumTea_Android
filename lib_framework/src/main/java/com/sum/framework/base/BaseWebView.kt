package com.sum.framework.base

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.WebSettings
import com.sum.framework.helper.SumAppHelper
import com.tencent.smtt.sdk.WebView

/**
 * 腾讯X5WebView: 就是用这个第三方库呀，是库，是来自第三方的。。
 */ // 这些注释标签，还不懂
open class BaseWebView @JvmOverloads constructor(
    context: Context, attr: AttributeSet? = null
) : WebView(context, attr) {

    /**
     * 滑动距离监听
     */
    private var mOnWebScrollListener: OnWebScrollListener? = null

    init {
        initSetting()

        /**
         * WebView在安卓5.0之前默认允许其加载混合网络协议内容
         * 在安卓5.0之后，默认不允许加载http与https混合内容，
         * 需要设置webView允许其加载混合网络协议内容
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    @SuppressLint("SetJavaScriptEnabled") // 所以，下面会引用第三方库里的类，什么的、微信。。 com.tencent.smtt.....
    private fun initSetting() {
        overScrollMode = View.OVER_SCROLL_ALWAYS // 就是，可以翻屏，如手指触屏、翻屏页面内容 
        settings.apply {
            defaultTextEncodingName = "utf-8"
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            //适应手机屏幕
            useWideViewPort = true
            loadWithOverviewMode = true
            layoutAlgorithm = com.tencent.smtt.sdk.WebSettings.LayoutAlgorithm.NARROW_COLUMNS
            //无限放大
            setSupportZoom(false) // 这个，感觉是，否绝放大，不能放大缩小！！细节不用管。。。
            builtInZoomControls = false

            setAppCacheEnabled(true)
            setGeolocationEnabled(true)
            setAppCacheMaxSize(Long.MAX_VALUE) // 应用，最大缓存内存， for WebView
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
//            mixedContentMode = com.tencent.smtt.sdk.WebSettings.LOAD_NORMAL

            if (SumAppHelper.isDebug()) {
                setWebContentsDebuggingEnabled(true)
            }
            // 不使用缓存
            settings.mixedContentMode = WebSettings.LOAD_NO_CACHE
            // 添加useragent
            //settings.setUserAgentString(settings.getUserAgentString() + " tuoduni-android-" + AppUtil.getVersionName(mContext.getApplicationContext()));
        }
        x5WebViewExtension?.apply {
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            setScrollBarFadingEnabled(false)
        }
    }

    override fun onScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY)
        mOnWebScrollListener?.onWebScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY)
    }

    /**
     * 设置滑动监听
     */
    fun setOnWebScrollListener(onWebScrollListener: OnWebScrollListener) {
        this.mOnWebScrollListener = onWebScrollListener
    }

    /**
     * 滑动距离监听
     */
    interface OnWebScrollListener {
        fun onWebScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int)
    }
}