package com.sum.demo.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.sum.framework.log.LogUtil

/**
 * Lifecycle 的三种实现方式
 */

// 第一种：
// 1.自定义LifecycleObserver观察者，用注解声明每个方法观察宿主的状态
class MyLifecycleObserver : LifecycleObserver { // 这里，只注册了两个函数： onStart()-onStop()
    @OnLifecycleEvent(Lifecycle.Event.ON_START) // 这些标签，看一下应用能够狠快就懂，以前用过不少标签
    fun onStart(owner: LifecycleOwner) {
        LogUtil.e("MyLifecycleObserver:onStart")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop(owner: LifecycleOwner) {
        LogUtil.e("MyLifecycleObserver:onStop")
    }
}

// 第二种
class MyFullLifeObserver : DefaultLifecycleObserver { // 这个，覆盖Activity 生命周期方法中的 6 种方法，为什么不也写下 onRestart() 呢？

    override fun onCreate(owner: LifecycleOwner) {
        LogUtil.e("MyFullLifeObserver:onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        LogUtil.e("MyFullLifeObserver:onStart")
    }

    override fun onResume(owner: LifecycleOwner) {}

    override fun onPause(owner: LifecycleOwner) {}

    override fun onStop(owner: LifecycleOwner) {}

    override fun onDestroy(owner: LifecycleOwner) {}
}

// 第三种
interface LifecycleEventObserver : LifecycleObserver { // 这种方法，仅只这一个函数吗？
    // 宿主生命周期变化的事件都会通知到这个方法：
    fun onStateChanged(owner: LifecycleOwner?, event: Lifecycle.Event?)
}

// 继承自LifecycleEventObserver，复写onStateChanged方法
internal class MyLifecycleEventObserver : LifecycleEventObserver {
    override fun onStateChanged(owner: LifecycleOwner?, event: Lifecycle.Event?) {
        //需要自行判断Event是onStart还是onStop
        LogUtil.e("MyLifecycleEventObserver:Event:${event?.name}")
    }
}
