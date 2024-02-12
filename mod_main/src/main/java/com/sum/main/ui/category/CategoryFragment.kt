package com.sum.main.ui.category

import android.os.Bundle
import android.util.SparseArray
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.sum.framework.adapter.ViewPage2FragmentAdapter
import com.sum.framework.base.BaseMvvmFragment
import com.sum.framework.ext.gone
import com.sum.framework.ext.toJson
import com.sum.framework.ext.visible
import com.sum.main.R
import com.sum.main.databinding.FragmentCategoryBinding
import com.sum.main.ui.category.adapter.CategoryTabAdapter
import com.sum.main.ui.category.viewmodel.CategoryViewModel

/**
 * @author mingyan.su
 * @date   2023/3/3 8:10
 * @desc   分类
 */
class CategoryFragment : BaseMvvmFragment<FragmentCategoryBinding, CategoryViewModel>() {
    //当前选中的position
    private var mCurrentSelectPosition = 0
    private var fragments = SparseArray<Fragment>() // 创建一个实例：一堆碎片的、一个适配器的用例
    private lateinit var mTabAdapter: CategoryTabAdapter
    private var mViewPagerAdapter: ViewPage2FragmentAdapter? = null  // 一个使用这个适配器的用例

    override fun initView(view: View, savedInstanceState: Bundle?) {
        initTabRecyclerView()
        initViewPager2()
    }

    private fun initTabRecyclerView() {
        mTabAdapter = CategoryTabAdapter()
        mBinding?.tabRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mTabAdapter
        }
        mTabAdapter.onItemClickListener = { view: View, position: Int ->
            updateTabItem(position)
            mBinding?.viewPager2?.setCurrentItem(position, false)
        }
    }

    private fun initViewPager2() {
        activity?.let {
            mViewPagerAdapter = ViewPage2FragmentAdapter(childFragmentManager, lifecycle, fragments)
//            mViewPagerAdapter = ViewPage2FragmentAdapter(it, fragments)
        }
        mBinding?.viewPager2?.apply {
            offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
            orientation = ViewPager2.ORIENTATION_VERTICAL
            registerOnPageChangeCallback(viewPager2Callback)
            adapter = mViewPagerAdapter
        }
    }

    override fun initData() { // 配置初始化的数据：一堆碎片等
        showLoading() // 加载进展提示会话框
        mViewModel.getCategoryData() // 数据驱动、数据的准备过程：去找一下，走过程
        mViewModel.categoryItemLiveData.observe(this) { // 订阅者模式
            dismissLoading()
            it?.let {
                mBinding?.viewEmptyData?.gone()
                //默认第一条选中
                it.firstOrNull()?.isSelected = true
                mTabAdapter.setData(it) // Tab-layout ？反正是，一个又一个Tab, 每个Tab 里一个碎片，下面实例化各种【数据驱动】的碎片
                it.forEachIndexed { index, item ->
                    val fragment = CategorySecondFragment.newInstance(item.articles?.toJson(true) ?: "")
                    fragments.append(index, fragment) // 一堆碎片实例，数据驱动的
                }
                mViewPagerAdapter?.notifyItemRangeChanged(0, it.size) // 发布通知
            } ?: kotlin.run {
                mBinding?.viewEmptyData?.visible()
            }
        }
    }

    /**
     * 更新Tab状态
     * @param position 选择的item
     */
    private fun updateTabItem(position: Int) {
        mTabAdapter.setCurrentPosition(position)

        if (mCurrentSelectPosition != position) {
            //更新上一条item
            val selectedItem = mTabAdapter.getItem(mCurrentSelectPosition)
            selectedItem?.let { it.isSelected = false }
            //更新当前item
            val newItem = mTabAdapter.getItem(position)
            newItem?.let { it.isSelected = true }

            mCurrentSelectPosition = position
            mTabAdapter.notifyDataSetChanged()
            mBinding?.tabRecyclerView?.smoothScrollToPosition(position)
        }
    }

    /**
     * VIewPager2选择回调
     */
    private val viewPager2Callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateTabItem(position)
        }
    }

    override fun onDestroyView() {
        mBinding?.viewPager2?.unregisterOnPageChangeCallback(viewPager2Callback)
        super.onDestroyView()
    }

}