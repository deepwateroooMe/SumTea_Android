package com.sum.video

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_NEVER
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.sum.common.constant.KEY_VIDEO_PLAY_LIST
import com.sum.common.constant.VIDEO_ACTIVITY_PLAYER
import com.sum.framework.base.BaseDataBindActivity
import com.sum.framework.ext.gone
import com.sum.framework.ext.onClick
import com.sum.framework.ext.visible
import com.sum.framework.log.LogUtil
import com.sum.framework.toast.TipsToast
import com.sum.framework.utils.StatusBarSettingHelper
import com.sum.framework.utils.ViewUtils
import com.sum.framework.utils.dpToPx
import com.sum.framework.utils.getStringFromResource
import com.sum.room.entity.VideoInfo
import com.sum.video.adapter.VideoAdapter
import com.sum.video.manager.PagerLayoutManager
import com.sum.video.databinding.ActivityVideoPlayBinding
import com.sum.video.listener.OnViewPagerListener
import com.sum.video.view.RotateNoteView


/**
 * @author mingyan.su
 * @date   2023/4/2 13:28
 * @dese   视频播放页
 */
@Route(path = VIDEO_ACTIVITY_PLAYER)
class VideoPlayActivity : BaseDataBindActivity<ActivityVideoPlayBinding>() { //  它的绑定类：《》是的，会是自动生成的，但要标记以便昨天的【自动视图绑定实现】

    // 播放位置
    private var mPlayingPosition = 0

    // 当前播放URL
    private var mPlayUrl: String? = null

    // 自动播放：【缓存够足以开始播放时，就自动播放；然后边播放边缓存】
    private val mStartAutoPlay = true

    // 缓存对象
    private lateinit var mCache: Cache

    // ExoPlayer
    private var mExoPlayer: ExoPlayer? = null

    // 播放器
    private var mPlayView: StyledPlayerView? = null

    // 媒体资源加载工厂类
    private lateinit var mMediaSource: MediaSource.Factory

    // 动画view
    private var mRotateNoteView: RotateNoteView? = null

    private lateinit var mAdapter: VideoAdapter

    // 需要添加JvmField注解
    @Autowired(name = KEY_VIDEO_PLAY_LIST)
    @JvmField
    var mData: ArrayList<VideoInfo>? = null

    override fun initView(savedInstanceState: Bundle?) {
        ARouter.getInstance().inject(this) // ARouter: 【TODO】：这个，也要弄明白 
        StatusBarSettingHelper.setStatusBarTranslucent(this) // 透明状态条
        StatusBarSettingHelper.statusBarLightMode(this@VideoPlayActivity, true) // 什么【日 day 模式】
        initPlayerView()
        initRecyclerView()
        mBinding.ivBack.onClick { finish() }
        mBinding.tvRetry.onClick {
            // 重试
            mExoPlayer?.prepare()
        }
        ViewUtils.setClipViewCornerRadius(mBinding.tvRetry, dpToPx(4))
    }

    // 下面：想找RecyclerView, 最快的方法，是安装进亲爱的表哥的活宝妹的手机，直接运行，下午再读源码，会读得更高效一点儿。
    // 读起来，感觉像是用 RecyclerView 里每 item 是ViewPager ？而 ViewPager 里插视频? 确认一下
 // 这里没有看懂：是把 RecyclerView 的适配器，直接暴力更改为视频播放控制适配器，用来控制视频播放？【TODO】：
 // 运行程序：应用最下几个Tab; 每个Tab 里，有不同 layout 的 RecyclerView. 应该也是ViewPager 里嵌套 RecyclerView. RecyclerView 里每项是短视频
    private fun initRecyclerView() { // 这里奇怪：视频播放，是用 RecyclerView 的吗？以前没有这个概念。找这个 RecyclerView 是哪个控件、模块在用
        mAdapter = VideoAdapter()
         // 下面这行弄懂：一行源码，轻舟已过万重山。。。略去细节【底层原理】无数。。。
        val manager = PagerLayoutManager(this@VideoPlayActivity, LinearLayoutManager.VERTICAL, false)
        manager.setOnViewPagerListener(onScrollPagerListener)
        mBinding.recyclerView.apply {
            layoutManager = manager
            adapter = mAdapter
        }
        mAdapter.setData(mData)
        mAdapter.onItemClickListener = { view: View, position: Int ->
            if (mExoPlayer?.isPlaying == true) { // 正在放：就暂停
                mExoPlayer?.playWhenReady = false
                mBinding.ivVideoPause.visible()
            } else { // 没在放：等待缓存数据足够、就绪后，就自动播放
                mExoPlayer?.playWhenReady = true
                mBinding.ivVideoPause.gone()
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (mExoPlayer?.isPlaying == false) {
            mExoPlayer?.playWhenReady = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (mExoPlayer?.isPlaying == true) {
            mExoPlayer?.playWhenReady = false
        }
    }

    /**
     * 创建exoplayer播放器实例
     * 视屏画面渲染工厂类，语音选择器，缓存控制器
     */
    private fun initPlayerView(): Boolean {
        // 创建exoplayer播放器实例
        mPlayView = initStylePlayView()

        // 创建 MediaSource 媒体资源 加载的工厂类
        // 因为由它创建的 MediaSource 能够实现边缓冲边播放的效果, 【边缓冲边播放：实现极少等待尽早播放、边播放边缓存】
        // 如果需要播放hls,m3u8 则需要创建DashMediaSource.Factory()
        mMediaSource = ProgressiveMediaSource.Factory(buildCacheDataSource())

        mExoPlayer = initExoPlayer()
        // 缓冲完成自动播放：【应该是缓存了、足够启动开始播放的量大小】
        mExoPlayer?.playWhenReady = mStartAutoPlay
        // 将显示控件绑定ExoPlayer
        mPlayView?.player = mExoPlayer

        //       val haveStartPosition = startItemIndex != C.INDEX_UNSET
        //       if (haveStartPosition) {
        // 初始化播放位置
        //       mExoPlayer?.seekTo(startItemIndex, startPosition)
        //       }
        // 设置ExoPlayer需要播放的多媒体item
        //        mExoPlayer?.setMediaItems(mediaItems  /* resetPosition= !haveStartPosition*/)
        // 资源准备，如果设置 setPlayWhenReady(true) 则资源准备好就立马播放。
        mExoPlayer?.prepare()
        return true
    }

    /**
     * 初始化ExoPlayer
     */
    private fun initExoPlayer(): ExoPlayer { // 【基类】：直接来自 google 第三方库
        // val playerBuilder = ExoPlayer.Builder(this)
        val playerBuilder = ExoPlayer.Builder(this).setMediaSourceFactory(mMediaSource) // 初始化为：边播放边缓存
        // 视频每一帧的画面如何渲染,实现默认的实现类：【TODO】：就是库里的基础，不懂，不明白库 ExoPlayer 里，每帧渲染的原理 
        val renderersFactory: RenderersFactory = DefaultRenderersFactory(this)
        playerBuilder.setRenderersFactory(renderersFactory)
        // 视频的音视频轨道如何加载,使用默认的轨道选择器
        playerBuilder.setTrackSelector(DefaultTrackSelector(this))
        // 视频缓存控制逻辑,使用默认的即可
        playerBuilder.setLoadControl(DefaultLoadControl())

        return playerBuilder.build()
    }

    /**
     * 创建exoplayer播放器实例
     */
    private fun initStylePlayView(): StyledPlayerView { // 继承、扩展自、系统基类
        return StyledPlayerView(this).apply {
            controllerShowTimeoutMs = 10000 // 卡 10 秒：算超时？
            setKeepContentOnPlayerReset(false) // 一个功能：
            setShowBuffering(SHOW_BUFFERING_NEVER)// 不展示缓冲view
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            useController = false // 是否使用默认控制器，如需要可参考PlayerControlView 应该就是实现了个【个性化】了的播放控制器，找下：有哪些好用与不同
            //            keepScreenOn = true
        }
    }

    /**
     * CacheDataSourceFactory: 看明白：如何自己实现【边播放边缓存的】的！！！
     * 创建能够 边播放边缓存的 本地资源加载和http网络数据写入的工厂类
     * Cache cache：缓存写入策略和缓存写入位置的对象
     * DataSource.Factory upstreamFactory：http视频资源如何加载的工厂对象
     * DataSource.Factory cacheReadDataSourceFactory：本地缓存数据如何读取的工厂对象
     * DataSink.Factory cacheWriteDataSinkFactory：http网络数据如何写入本地缓存的工厂对象
     * CacheDataSource.Flags int flags：加载本地缓存数据进行播放时的策略,如果遇到该文件正在被写入数据,或读取缓存数据发生错误时的策略
     * CacheDataSource.EventListener eventListener  缓存数据读取的回调
     */
    private fun buildCacheDataSource(): DataSource.Factory {
        // 创建http视频资源如何加载的工厂对象
        val upstreamFactory = DefaultHttpDataSource.Factory()
        //        val cacheFile = getExternalFilesDir( /* type= */null) ?: filesDir
        //        val downloadContentDirectory: File = File(cacheFile, "video")
        LogUtil.e("cacheDir${this.cacheDir}", tag = TAG)

        // 创建缓存，指定缓存位置，和缓存策略,为最近最少使用原则,最大为200m
        mCache = SimpleCache(
            application.cacheDir, // 给应用配置过的【默认视频资源缓存目录】位置
            LeastRecentlyUsedCacheEvictor(1024 * 1024 * 200), // 像个LRU 缓存之类的、缓存擦除策略
            StandaloneDatabaseProvider(this)
        )

        // 把缓存对象cache和负责缓存数据读取、写入的工厂类CacheDataSinkFactory 相关联
        // 它是【缓存工厂】桥：负责执行缓存工作；从 http 提供视频资源，并缓存入【缓存对象 cache】里
        val cacheDataSinkFactory = CacheDataSink.Factory().setCache(mCache).setFragmentSize(Long.MAX_VALUE)
        return CacheDataSource.Factory()
            .setCache(mCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory()) // 视频源头：自http 网络视频、下载下来的文件？
            .setCacheWriteDataSinkFactory(cacheDataSinkFactory) // 缓存写工厂：
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR) // 如果缓存出错，不管！可能会造成哪些问题呢？假如过程中缓存中断，缓存的视频放完，会卡住！！
    }

    /**
     * 滑动监听
     */ // 下面是：一个控件的、几个常用回调监听配置
    private val onScrollPagerListener = object : OnViewPagerListener {
        override fun onInitComplete(view: View?) {
            startPlay(0, view)
        }
        override fun onPageRelease(isNext: Boolean, position: Int, view: View?) {
            LogUtil.i("onPageRelease===$isNext | $position", tag = TAG)
            // 还应该暂停掉列表上正在播放的那个
            // TODO
            val rotateNoteView = view?.findViewById<RotateNoteView>(R.id.rotate_note_view)
            rotateNoteView?.stopAnim()
            //            mExoPlayer?.removeListener(playerBackListener)
        }
        override fun onPageSelected(position: Int, isBottom: Boolean, view: View?) {
            LogUtil.i("onPageSelected===$position | $isBottom | ${mAdapter.itemCount}", tag = TAG)
            if (position < 0 || position >= mAdapter.itemCount) return
            if (position == mPlayingPosition) return
            mPlayingPosition = position
            startPlay(position, view)
        }
    }

    /**
     * 播放
     * @param position
     * @param view
     */
    private fun startPlay(position: Int, view: View?) {
        // 播放器视图
        if (view == null) return
        val item = mAdapter.getItem(position)
        if (item == null || item.playUrl?.isEmpty() == true) return
        showErrorView(false)
        // // 如果父容器不等于this,则把playView添加进去
        // parent如果不为空则是被添加到别的容器中了，需要移除
        (mPlayView?.parent as? FrameLayout)?.removeAllViews()
        val frameLayout = view.findViewById<FrameLayout>(R.id.fl_container)
        mRotateNoteView = view.findViewById<RotateNoteView>(R.id.rotate_note_view)
        mRotateNoteView?.initAnimator()
        mRotateNoteView?.startAnim()
        // 添加播放器
        frameLayout.addView(mPlayView)

        // 判断当前播放的和即将播放的是否同一个媒体资源
        // 如果是同一个视频资源,则不需要从重新创建mediaSource
        if (mPlayUrl == item.playUrl) {

        } else {
            showLoading()
            LogUtil.i("position:$position, playUrl:${item.playUrl}", tag = TAG)
            val mediaSource: MediaSource =
                mMediaSource.createMediaSource(MediaItem.fromUri(item.playUrl ?: ""))
            // 设置ExoPlayer需要播放的多媒体item
            mExoPlayer?.setMediaSource(mediaSource)
            mExoPlayer?.prepare()

            //        // 构建媒体播放的一个Item， 一个item就是一个播放的多媒体文件
            //        val mediaItem = MediaItem.fromUri("https:// vdn1.vzuu.com/HD/c8af2fd6-438d-11eb-991f-da1190f1515e.mp4")
            //        // 设置ExoPlayer需要播放的多媒体item
            //        mExoPlayer?.setMediaItem(mediaItem)

            mExoPlayer?.repeatMode = Player.REPEAT_MODE_ONE // 无限循环
            mPlayUrl = item.playUrl
            // 但需要onPlayerStateChanged 否则不会触发onPlayerStateChanged()
            mExoPlayer?.addListener(playerBackListener)
        }

        mExoPlayer?.playWhenReady = true// 资源缓冲好后立马播放
    }

    /**
     * 播放监听回调
     */
    val playerBackListener = object : Player.Listener {
        /**
         * 播放状态改变
         */
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            LogUtil.i("onPlayerStateChanged playWhenReady == $playWhenReady | playbackState:$playbackState", tag = TAG)
            // 判断当前视屏是否已经准备好
            val mIsPlaying = mExoPlayer?.bufferedPosition != 0L && playWhenReady
            when (playbackState) {
                Player.STATE_IDLE -> {// 播放错误
                    mBinding.ivVideoPause.gone()
                    dismissLoading()
                }
                Player.STATE_BUFFERING -> {// 缓冲中
                    // mBuffer.setVisibility(View.VISIBLE)
                    showLoading()
                }
                Player.STATE_READY -> {// 准备完毕，开始播放
                    dismissLoading()
                    // 播放中
                    mBinding.ivVideoPause.visibility = if (mIsPlaying) View.GONE else View.VISIBLE
                }
                Player.STATE_ENDED -> {// 播放完成，无限循环模式则不会回调这里
                    // 播放完成
                }
            }
        }

        /**
         * 是否正在播放改变
         */
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying) {
                mRotateNoteView?.startAnim()
                //                mBinding.ivVideoPause.gone()
            } else {
                mRotateNoteView?.pauseAnim()
                //                mBinding.ivVideoPause.visible()
            }
            LogUtil.i("onIsPlayingChanged isPlaying == $isPlaying ", tag = TAG)
        }

        /**
         * 播放错误
         */
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            showErrorView(true)
            LogUtil.e("onPlayerError:error == $error ", tag = TAG)
            TipsToast.showTips(getStringFromResource(R.string.video_play_error))
        }
    }

    /**
     * 展示错误view
     */
    fun showErrorView(show: Boolean) {
        if (show) {
            mBinding.ivVideoPause.gone()
            mBinding.tvPlayError.visible()
            mBinding.tvRetry.visible()
            dismissLoading()
        } else {
            mBinding.tvPlayError.gone()
            mBinding.tvRetry.gone()
        }
    }

    /**
     * 释放视频播放资源
     */
    private fun releasePlayer() {
        // 清理ExoPlayer资源
        mExoPlayer?.apply {
            playWhenReady = false
            stop()
            release()
            removeListener(playerBackListener)
        }
        mExoPlayer = null
        // 是否播放器资源
        mPlayView?.adViewGroup?.removeAllViews()
        mPlayView?.player = null
        mPlayView = null
        // 清除动画
        mRotateNoteView?.stopAnim()
        mRotateNoteView = null
        // 清理缓存
        mCache.release()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

}