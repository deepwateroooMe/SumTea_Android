package com.sum.network.interceptor

import com.sum.framework.log.LogUtil
import com.sum.network.constant.KEY_SAVE_USER_LOGIN
import com.sum.network.constant.KEY_SAVE_USER_REGISTER
import com.sum.network.constant.KEY_SET_COOKIE
import com.sum.network.manager.CookiesManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author mingyan.su
 * @date   2023/3/27 07:26
 * @desc   Cookies拦截器: 亲爱的表哥的活宝妹，搞不懂，这些拦截器，为什么要拦截？它说 CookiesManager 管理器，拦截了要记住用户名与密码；可以实现一段时间内，用户免再登录？
 */
class CookiesInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newBuilder = request.newBuilder()

        val response = chain.proceed(newBuilder.build())
        val url = request.url().toString()
        val host = request.url().host()

        // set-cookie maybe has multi, login to save cookie
        if ((url.contains(KEY_SAVE_USER_LOGIN) || url.contains(KEY_SAVE_USER_REGISTER))
                && response.headers(KEY_SET_COOKIE).isNotEmpty()
        ) {
            val cookies = response.headers(KEY_SET_COOKIE)
            val cookiesStr = CookiesManager.encodeCookie(cookies)
            CookiesManager.saveCookies(cookiesStr) // <<<<<<<<<<<<<<<<<<<< 管理器：记住了！有效时间内、免再登录？
            LogUtil.e("CookiesInterceptor:cookies:$cookies", tag = "smy")
        }
        return response
    }
}