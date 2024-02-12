package com.sum.demo.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sum.demo.R
import com.sum.demo.databinding.FragmentMainBinding

/**
 * @author mingyan.su
 * @date   2023/5/12 16:46
 * @desc   HomeNavFragment
 */
class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController() // 宿主？碎片，中的缺省方法；其它非宿主碎片中调用，会抛异常

        // 下面的三类：为什么是分 Activity-Fragment-Dialog 类型来跳转的，而不是实例名，要找应用中对应页面，看一下
        binding.tvNavActivity.setOnClickListener {
            //跳转Activity（目的地）
            navController.navigate(R.id.nav_activity)
        }
        binding.tvNavFragment.setOnClickListener {
            //跳转Fragment
            navController.navigate(R.id.nav_fragment)
        }
        binding.tvNavDialog.setOnClickListener {
            ////跳转Dialog
            navController.navigate(R.id.nav_dialog)
        }
    }
}