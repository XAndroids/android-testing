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
package com.example.android.architecture.blueprints.todoapp.tasks

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.R.string
import com.example.android.architecture.blueprints.todoapp.ServiceLocator
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.DataBindingIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.deleteAllTasksBlocking
import com.example.android.architecture.blueprints.todoapp.util.monitorActivity
import com.example.android.architecture.blueprints.todoapp.util.saveTaskBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task模块的大型End-to-End测试
 * 该类主要测试TasksActivity中TasksFragment、AddEditTaskFragment、StatisticsFragment集成的Case
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TasksActivityTest {

    private lateinit var repository: TasksRepository

    //一个Idling Resource等待Data Binding有挂起的bingding
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
        //获取TaskRepository，清除Tasks
        repository = ServiceLocator.provideTasksRepository(getApplicationContext())
        repository.deleteAllTasksBlocking()
    }

    @After
    fun reset() {
        //重置TaskRepository
        ServiceLocator.resetRepository()
    }

    /**
     * Idling resources通知Espresso App是空闲或者忙碌。当Main Looper没有调度操作时（例如在不同的线程上执行时）
     * 就需要这样的操作
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * 注销你的Idling Resource，这样它就能被垃圾回收，并且不会导致任何内存泄露
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun createTask() {
        // start up Tasks screen
        // TODO

        // Click on the "+" button, add details, and save
        // TODO

        // Then verify task is displayed on screen
        // TODO
    }

    @Test
    fun editTask() {
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION"))

        //启动Tasks页面
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        //所有DataBinding数据绑定完毕后，才执行后面的指令
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //点击在列表中的任务，然后验证所有数据正确
        onView(withText("TITLE1")).perform(click())
        onView(withId(R.id.task_detail_title)).check(matches(withText("TITLE1")))
        onView(withId(R.id.task_detail_description)).check(matches(withText("DESCRIPTION")))
        onView(withId(R.id.task_detail_complete)).check(matches(not(isChecked())))

        //点击编辑按钮，编辑，然后保存
        onView(withId(R.id.fab_edit_task)).perform(click())
        onView(withId(R.id.add_task_title)).perform(replaceText("NEW TITLE"))
        onView(withId(R.id.add_task_description)).perform(replaceText("NEW DESCRIPTION"))
        onView(withId(R.id.fab_save_task)).perform(click())

        //验证任务在屏幕中的任务列表展示
        onView(withText("NEW TITLE")).check(matches(isDisplayed()))
        //验证前面任务不展示
        onView(withText("TITLE1")).check(doesNotExist())
    }

    @Test
    fun createOneTask_deleteTask() {
        //启动Tasks页面
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //添加活动的任务
        onView(withId(R.id.fab_add_task)).perform(click())
        onView(withId(R.id.add_task_title)).perform(typeText("TITLE1"), closeSoftKeyboard())
        onView(withId(R.id.add_task_description)).perform(typeText("DESCRIPTION"))
        onView(withId(R.id.fab_save_task)).perform(click())

        //打开它的详情页面
        onView(withText("TITLE1")).perform(click())
        //点击菜单中的删除任务
        onView(withId(R.id.menu_delete)).perform(click())

        //验证他已经被删除
        onView(withId(R.id.menu_filter)).perform(click())
        onView(withText(string.nav_all)).perform(click())
        onView(withText("TITLE1")).check(doesNotExist())
    }

    @Test
    fun createTwoTasks_deleteOneTask() {
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION"))
        repository.saveTaskBlocking(Task("TITLE2", "DESCRIPTION"))

        //启动Tasks页面
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //打开第二个任务的详情页面
        onView(withText("TITLE2")).perform(click())
        //点击菜单删除任务
        onView(withId(R.id.menu_delete)).perform(click())

        //验证仅仅一个任务被删除
        onView(withId(R.id.menu_filter)).perform(click())
        onView(withText(string.nav_all)).perform(click())
        onView(withText("TITLE1")).check(matches(isDisplayed()))
        onView(withText("TITLE2")).check(doesNotExist())
    }

    @Test
    fun markTaskAsCompleteOnDetailScreen_taskIsCompleteInList() {
        // Add 1 active task
        val taskTitle = "COMPLETED"
        repository.saveTaskBlocking(Task(taskTitle, "DESCRIPTION"))

        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the task on the list
        onView(withText(taskTitle)).perform(click())

        // Click on the checkbox in task details screen
        onView(withId(R.id.task_detail_complete)).perform(click())

        // Press back button to go back to the list
        pressBack()

        // Check that the task is marked as completed
        onView(allOf(withId(R.id.complete), hasSibling(withText(taskTitle))))
                .check(matches(isChecked()))
    }

    @Test
    fun markTaskAsActiveOnDetailScreen_taskIsActiveInList() {
        // Add 1 completed task
        val taskTitle = "ACTIVE"
        repository.saveTaskBlocking(Task(taskTitle, "DESCRIPTION", true))

        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the task on the list
        onView(withText(taskTitle)).perform(click())
        // Click on the checkbox in task details screen
        onView(withId(R.id.task_detail_complete)).perform(click())

        // Press back button to go back to the list
        pressBack()

        // Check that the task is marked as active
        onView(allOf(withId(R.id.complete), hasSibling(withText(taskTitle))))
                .check(matches(not(isChecked())))
    }

    @Test
    fun markTaskAsCompleteAndActiveOnDetailScreen_taskIsActiveInList() {
        // Add 1 active task
        val taskTitle = "ACT-COMP"
        repository.saveTaskBlocking(Task(taskTitle, "DESCRIPTION"))

        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the task on the list
        onView(withText(taskTitle)).perform(click())
        // Click on the checkbox in task details screen
        onView(withId(R.id.task_detail_complete)).perform(click())
        // Click again to restore it to original state
        onView(withId(R.id.task_detail_complete)).perform(click())

        // Press back button to go back to the list
        pressBack()

        // Check that the task is marked as active
        onView(allOf(withId(R.id.complete), hasSibling(withText(taskTitle))))
                .check(matches(not(isChecked())))
    }

    @Test
    fun markTaskAsActiveAndCompleteOnDetailScreen_taskIsCompleteInList() {
        // Add 1 completed task
        val taskTitle = "COMP-ACT"
        repository.saveTaskBlocking(Task(taskTitle, "DESCRIPTION", true))

        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the task on the list
        onView(withText(taskTitle)).perform(click())
        // Click on the checkbox in task details screen
        onView(withId(R.id.task_detail_complete)).perform(click())
        // Click again to restore it to original state
        onView(withId(R.id.task_detail_complete)).perform(click())

        // Press back button to go back to the list
        pressBack()

        // Check that the task is marked as active
        onView(allOf(withId(R.id.complete), hasSibling(withText(taskTitle))))
                .check(matches(isChecked()))
    }

    @Test
    fun createTask_solution() {
        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the "+" button, add details, and save
        onView(withId(R.id.fab_add_task)).perform(click())
        onView(withId(R.id.add_task_title)).perform(typeText("title"), closeSoftKeyboard())
        onView(withId(R.id.add_task_description)).perform(typeText("description"))
        onView(withId(R.id.fab_save_task)).perform(click())

        // Then verify task is displayed on screen
        onView(withText("title")).check(matches(isDisplayed()))
    }
}
