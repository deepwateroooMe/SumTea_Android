@file:JvmName("TipToast")

package com.sum.framework.toast

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sum.framework.R
import com.sum.framework.databinding.WidgetTipsToastBinding

object TipsToast { // 先把这个简单的帮助类，看懂

    private var toast: Toast? = null

    private lateinit var mContext: Application
    private val mToastHandler = Looper.myLooper()?.let { Handler(it) } // 所有UI 视图相关的更新，只能主线程操作。需要发给主线程的 MessageQueue 里给它去处理

    private val mBinding by lazy { // 这个是，【自动视图绑定、底层库的一个应用，去把底层设计实现原理弄明白】
        WidgetTipsToastBinding.inflate(LayoutInflater.from(mContext), null, false) // <<<<<<<<<<<<<<<<<<<< 【TODO】：一会儿就看
    }

    fun init(context: Application) { // 【多进程、跨进程模块】：ApplicationContext, 当前进程的？
        mContext = context // 会用这个 Application 的上下文，来拿当前进程的？资源，字符串资源什么的
    }

    fun showTips(@StringRes stringId: Int) {
        val msg = mContext.getString(stringId) // 仅只：当前进程吗？
        showTipsImpl(
            msg,
            Toast.LENGTH_SHORT
        )
    }

    fun showTips(msg: String?) {
        showTipsImpl(
            msg,
            Toast.LENGTH_SHORT
        )
    }

    fun showTips(msg: String?, duration: Int) {
        showTipsImpl(msg, duration)
    }

    fun showTips(@StringRes stringId: Int, duration: Int) {
        val msg = mContext.getString(stringId)
        showTipsImpl(msg, duration)
    }

    fun showSuccessTips(msg: String?) {
        showTipsImpl(
            msg,
            Toast.LENGTH_SHORT,
            R.mipmap.widget_toast_success
        )
    }

    fun showSuccessTips(@StringRes stringId: Int) {
        val msg = mContext.getString(stringId)
        showTipsImpl(
            msg,
            Toast.LENGTH_SHORT,
            R.mipmap.widget_toast_success
        )
    }

    fun showWarningTips(msg: String?) {
        showTipsImpl(
            msg,
            Toast.LENGTH_SHORT,
            R.mipmap.widget_toast_warning
        )
    }

    fun showWarningTips(@StringRes stringId: Int) {
        val msg = mContext.getString(stringId)
        showTipsImpl(
            msg,
            Toast.LENGTH_SHORT,
            R.mipmap.widget_toast_warning
        )
    }

    private fun showTipsImpl( // 实现原理：给主线程的MessegeQueue 发一个【显示 Toast】的消息任务：要它——主线程，显示 toast 的内容、时长、显示在哪里等。功能简单
        msg: String?,
        duration: Int,
        @DrawableRes drawableId: Int = 0,
    ) {
        if (msg.isNullOrEmpty()) 
            return
        toast?.let { // 这里没看懂：这个变量，是在哪里申明，与初始化的？
            cancel()
            toast = null
        }
        mToastHandler?.postDelayed({
            try {
                toast = Toast(mContext)
                toast?.view = mBinding.root
                mBinding.tipToastTxt.text = msg // 这个文本框名，是在【视图 .xml】中写死文本框的名字的
                mBinding.tipToastTxt.setCompoundDrawablesWithIntrinsicBounds( // 大致看下：TextView 控件里，设置Drawable显示在text的左、上、右、下位置
                    drawableId,
                    0,
                    0,
                    0
                )
                toast?.setGravity(Gravity.CENTER, 0, 0)
                toast?.duration = duration
                toast?.show()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("show tips error", "$e")
            }
        }, 50) // 稍微延迟了一小会儿：目的是，
    }

    fun cancel() {
        toast?.cancel()
        mToastHandler?.removeCallbacksAndMessages(null)
    }
}