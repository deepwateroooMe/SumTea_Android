package com.sum.demo.livedata

import androidx.lifecycle.*
import java.util.concurrent.ConcurrentHashMap
/**
 * 跨页面事件总线：这个类，关于避开【粘性事件】的，看得似懂非懂，改天再看一下
 * object单例，实现共享
 */
// 通过while分发事件，LiveData是默认粘性事件处理的，我们有时候不需要粘性事件，怎么办？下面的LiveDataBus提供了解决方案。
object LiveDataBus { // 这个类的功能作用，没弄清楚：说是 LiveData 一定是发送【粘性事件】；但是我们需求【不粘性】事件，就不得不自己实现一个 LiveDataBus. 可是还没理解透彻
    private var mEventMap = ConcurrentHashMap<String, StickyLiveData<*>>() // 多线程安全 HashMap; 粘性事件：补一下。。先把狗屁贴子看完。。

    /**
     * 消息总线，是需要名称的：
     */
    fun <T> with(eventName: String): StickyLiveData<T> { // 【泛型封装】：任意自定义类型，有名字，都可以入字典管理
        //基于事件名称 订阅、分发消息，为什么要返回StickyLiveData对象，因为每一种对象对应的消息体都是不一样的，【感觉这里没能说透彻，解释得不透彻！】
        //由于一个LiveData只能发送一种数据类型，所以不同的Event事件，需要使用不同的LiveData实例去分发

        var liveData = mEventMap[eventName]
        if (liveData == null) {
            liveData = StickyLiveData<T>(eventName) // 字典里没有，就添加【键值对】进字典里
            mEventMap[eventName] = liveData
        }
        return liveData as StickyLiveData<T>
    }
 // 【定义——粘性实时数据泛型类？】
    class StickyLiveData<T>(private val mEventName: String) : LiveData<T>() {
        var mStickyData: T? = null
        var mVersion = 0

        /**
         * 调用mVersion++
         * 在我们注册一个Observer的时候，我们需要把它包装一下，目的是为了让Observer的version和LiveData的version对齐
         * 但是LiveData的version字段拿不到，所以需要自己管理version,在对齐的时候使用这个就可以了【自己管理了 VERSION, 就是方便自己使用】
         *
         * @param value
         */
        override fun setValue(value: T) {
            mVersion++
            super.setValue(value)
        }

        override fun postValue(value: T) {
            mVersion++
            super.postValue(value)
        }

        /**
         * 发送粘性事件
         * 只能在主线程发送数据：【其实，它是说，设置——粘性事件，必须是在主线程中】
         *
         * @param stickyData
         */
        fun setStickData(stickyData: T) {
            mStickyData = stickyData
            setValue(stickyData)
        }

        /**
         * 发送粘性事件，不受线程限制：就是子线程【异步线程】都可以【更改？发布粘性事件】。哪里一定会有个【异步线程】同步到【主线程】的步骤？
         *
         * @param stickyData
         */
        fun postStickData(stickyData: T) {
            mStickyData = stickyData
            postValue(stickyData) // 这个函数内部逻辑里：有确保同步到主线程的机制，就是：【下面2 行自己添加的】
                // // 发送到主线程执行
                // ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable); // 所以一定会同步到主线程中去的
        }

        override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
            observeSticky(owner, observer, false)
        }

        /**
         * 暴露方法，是否关心之前发送的数据,再往宿主上面添加一个addObserver监听生命周期事件，如果是DESTORY则
         * 主动移除LiveData
         *
         * @param owner
         * @param observer
         * @param sticky   是否为粘性事件，sticky=true,如果之前存在已经发送数据，那么者Observer就会收到之前的粘性事件消息
         */
        fun observeSticky(owner: LifecycleOwner, observer: Observer<in T>, sticky: Boolean) { // 感觉这个，与网络上普通网文，稍微修改了一下
            // 这里是，自己模拟、自定义、实现了一个粘性XXX, 有个自己管理的多线程安全字典。所以作必要的字典管理
            owner.lifecycle.addObserver(LifecycleEventObserver { source, event ->
                if (event == Lifecycle.Event.ON_DESTROY) { // 这里，如果是某个 Activity 实例的【销毁事件】，就从管理字典里，移除【键值对】
                    mEventMap?.remove(mEventName)
                }
            })
            super.observe(owner, StickyObserver(this, observer as Observer<T>, sticky)) // 调用系统方法 
        }
    }

    /**
     * 包装StickyObserver，有新的消息会回调onChanged方法，从这里判断是否要分发这条消息
     * 这只是完成StickyObserver的包装，用于控制事件的分发与否，但是事件的发送还是依靠LiveData来完成的
     * @param <T>
     */
    internal class StickyObserver<T>(
        val stickyLiveData: StickyLiveData<T>,
        val observer: Observer<in T>,
        val sticky: Boolean//是否开启粘性事件,为false则只能接受到注册之后发送的消息，如果需要接受粘性事件则传true
    ) : Observer<T> {
        //标记该Observer已经接收几次数据了，过滤老数据防止重复接收
        private var mLastVersion = 0
        override fun onChanged(t: T) {
            if (mLastVersion >= stickyLiveData.mVersion) { //如果相等则说明没有更新的数据要发送
                //但是如果当前Observer是关系粘性事件的，则分发给他
                if (sticky && stickyLiveData.mStickyData != null) {
                    observer.onChanged(stickyLiveData.mStickyData)
                }
                return
            }
            mLastVersion = stickyLiveData.mVersion
            observer.onChanged(t)
        }
    }

}