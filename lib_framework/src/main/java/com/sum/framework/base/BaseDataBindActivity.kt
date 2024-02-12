package com.sum.framework.base

import android.view.LayoutInflater
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding
import com.sum.framework.ext.saveAs
import com.sum.framework.ext.saveAsUnChecked
import java.lang.reflect.ParameterizedType

/**
 * @date   2/17 11:27
 * @desc   dataBinding Activity基类
 */
abstract class BaseDataBindActivity<DB : ViewBinding> : BaseActivity() { // 【抽象、数据绑定基类】
    lateinit var mBinding: DB

    override fun setContentLayout() { // 自动设置【视图绑定】：利用【参数标记的类名】来，反射绑定对应视图
//      mBinding = DataBindingUtil.setContentView(this, getLayoutResId())
        val type = javaClass.genericSuperclass
        val vbClass: Class<DB> = type!!.saveAs<ParameterizedType>().actualTypeArguments[0].saveAs()
        val method = vbClass.getDeclaredMethod("inflate", LayoutInflater::class.java)
        mBinding = method.invoke(this, layoutInflater)!!.saveAsUnChecked()
        setContentView(mBinding.root)
    }

    override fun getLayoutResId(): Int = 0 // 这个，实体类里，还要再写、改装什么的吗？
}