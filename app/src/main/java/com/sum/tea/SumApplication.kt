package com.sum.tea

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.multidex.MultiDex
import com.sum.framework.manager.AppFrontBack
import com.sum.framework.manager.AppFrontBackListener
import com.sum.framework.log.LogUtil
import com.sum.framework.manager.ActivityManager
import com.sum.framework.toast.TipsToast
import com.sum.stater.dispatcher.TaskDispatcher
import com.sum.tea.task.InitMmkvTask
import com.sum.tea.task.InitAppManagerTask
import com.sum.tea.task.InitRefreshLayoutTask
import com.sum.tea.task.InitArouterTask
import com.sum.tea.task.InitSumHelperTask

/**
 * @author mingyan.su
 * @date   2023/2/9 23:19
 * @desc   应用类
 */
class SumApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base) // MultiDex: 当多模块的时候，必须的？【TODO】： 
    }
 // 【多进程跨进程应用】的总框架：不分进程启动的？没看明白。几个必要的初始化
    override fun onCreate() {
        super.onCreate()

        //注册APP前后台切换监听：没明白，注册这个的功用，有哪些便利？
        appFrontBackRegister()
        // App启动立即注册监听：同样去找，注册后的便利功能 
        registerActivityLifecycle()
        TipsToast.init(this)

        //1.启动器：TaskDispatcher初始化。【现在还没弄明白，这个功能模块的任务管理？】【TODO】：这个类是重点，回头要细看一下
        TaskDispatcher.init(this)
        //2.创建dispatcher实例
        val dispatcher: TaskDispatcher = TaskDispatcher.createInstance()
        //3.添加任务并且启动任务
        dispatcher.addTask(InitSumHelperTask(this))
            .addTask(InitMmkvTask())
            .addTask(InitAppManagerTask())
            .addTask(InitRefreshLayoutTask())
            .addTask(InitArouterTask())
            .start()
        //4.等待，需要等待的方法执行完才可以往下执行
        dispatcher.await()
    }

    /**
     * 注册APP前后台切换监听
     */
    private fun appFrontBackRegister() { // 极浅表实现：打印个日志说，我在前台、后台？！【注册了两个监听回调而已】
        AppFrontBack.register(this, object : AppFrontBackListener {
                                        override fun onBack(activity: Activity?) {
                                            LogUtil.d("onBack")
                                        }

                                        override fun onFront(activity: Activity?) {
                                            LogUtil.d("onFront")
                                        }
                                    })
    }

    /**
     * 注册Activity生命周期监听
     */
    private fun registerActivityLifecycle() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                                               override fun onActivityPaused(activity: Activity) {
                                               }

                                               override fun onActivityStarted(activity: Activity) {
                                               }

                                               override fun onActivityDestroyed(activity: Activity) {
                                                   ActivityManager.pop(activity)
                                               }

                                               override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {
                                               }

                                               override fun onActivityStopped(activity: Activity) {
                                               }

                                               override fun onActivityCreated(activity: Activity, p1: Bundle?) {
                                                   ActivityManager.push(activity)
                                               }

                                               override fun onActivityResumed(activity: Activity) {
                                               }
                                           })
    }
}