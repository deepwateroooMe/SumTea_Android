package com.sum.main.ui.category.viewmodel

import androidx.lifecycle.MutableLiveData
import com.sum.common.model.CategoryItem
import com.sum.framework.toast.TipsToast
import com.sum.network.callback.IApiErrorCallback
import com.sum.network.manager.ApiManager
import com.sum.network.viewmodel.BaseViewModel

/**
 * @author mingyan.su
 * @date   2023/3/3 8:12
 * @desc   分类ViewModel
 */
class CategoryViewModel : BaseViewModel() {
     // 这里，不是把它们直接串起来了吗？异步网络调用的结果，直接写在这个实时可变数据里？
    val categoryItemLiveData = MutableLiveData<MutableList<CategoryItem>?>()

    /**
     * 获取分类信息
     * 不依赖repository,错误回调实现IApiErrorCallback
     */
    fun getCategoryData() {
        launchUIWithResult(responseBlock = {
                               ApiManager.api.getCategoryData() // <<<<<<<<<<<<<<<<<<<< 去找：这个方法的实现细节【TODO】：
                           }, errorCall = object : IApiErrorCallback {
                                  override fun onError(code: Int?, error: String?) {
                                      super.onError(code, error)
                                      TipsToast.showTips(error)
                                      categoryItemLiveData.value = null  // 跨进程？拿数据出错，把这个实时数据置空。再去看【跨进程？拿数据】？弄懂
                                  }
                              }) {
            categoryItemLiveData.value = it
        }
    }
}