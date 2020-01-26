/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.util


import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingResource
import java.util.UUID

/**
 * 一个espresso idling resource实现，它报告所有data binding布局的空闲装填。data binding使用一种机制来发送
 * Espresso没有被跟踪的消息。
 *
 * 因为这个应用程序仅仅使用fragments，这个resource仅仅检测fragment和它们的孩子，而不是整个视图树结构。
 */
class DataBindingIdlingResource : IdlingResource {
    // 注册callbak列表
    private val idlingCallbacks = mutableListOf<IdlingResource.ResourceCallback>()
    //给它一个唯一的id类解决espresso的bug，在这个bug中不能注册/注销同一个名字的idling resource
    private val id = UUID.randomUUID().toString()

    //保存是否调用isIdle，结构是否为false。我们跟踪它来避免调用onTransitionToIdle回调，如果Espresso从未
    //想过我们是空闲的
    private var wasNotIdle = false

    lateinit var activity: FragmentActivity

    override fun getName() = "DataBinding $id"

    override fun isIdleNow(): Boolean {
        //所有的DataBinding是否需要更新数据
        val idle = !getBindings().any { it.hasPendingBindings() }

        @Suppress("LiftReturnOrAssignment")
        if (idle) {
            if (wasNotIdle) {
                //通知观察者避免espresso race 探测器
                idlingCallbacks.forEach { it.onTransitionToIdle() }
            }
            wasNotIdle = false
        } else {
            //如果不是，则16ms后继续监测下一帧，知道DataBinding数据更新完毕
            wasNotIdle = true
            activity.findViewById<View>(android.R.id.content).postDelayed({
                isIdleNow
            }, 16)
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        idlingCallbacks.add(callback)
    }

    /**
     * 在所有当前有效的Fragment中查找所有binding类
     */
    private fun getBindings(): List<ViewDataBinding> {
        val fragments = (activity as? FragmentActivity)
                ?.supportFragmentManager
                ?.fragments

        val bindings =
                fragments?.mapNotNull {
                    it.view?.getBinding()
                } ?: emptyList()
        val childrenBindings = fragments?.flatMap { it.childFragmentManager.fragments }
                ?.mapNotNull { it.view?.getBinding() } ?: emptyList()

        return bindings + childrenBindings
    }
}

private fun View.getBinding(): ViewDataBinding? = DataBindingUtil.getBinding(this)

/**
 * 使用ActivityScenario设置ActivityScenario，用于DataBindingIdlingResource
 */
fun DataBindingIdlingResource.monitorActivity(activityScenario: ActivityScenario<out FragmentActivity>) {
    activityScenario.onActivity {
        this.activity = it
    }
}

/**
 * 使用FragmentScenarios设置fragment，用于DataBindingIdlingResource
 */
fun DataBindingIdlingResource.monitorFragment(fragmentScenario: FragmentScenario<out Fragment>) {
    fragmentScenario.onFragment {
        this.activity = it.requireActivity()
    }
}
