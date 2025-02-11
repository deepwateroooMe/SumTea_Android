package com.sum.framework.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * @author mingyan.su
 * @date   2023/3/9 08:11
 * @desc   基本ViewHolder
 */
open class BaseViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView)
 // 带自动【视图绑定】的
open class BaseBindViewHolder<B : ViewBinding>(val binding: B) : BaseViewHolder(binding.root)