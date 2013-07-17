
/*
 This file was modified from or inspired by Apache Cordova.

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied. See the License for the
 specific language governing permissions and limitations
 under the License.
*/

package com.polyvi.xface.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.polyvi.xface.XSecurityPolicy;
import com.polyvi.xface.XStartParams;
import com.polyvi.xface.extension.XActivityResultListener;
import com.polyvi.xface.view.XAppWebView;

public interface XISystemContext {

    /**
     * 启动一个activity,当此activity退出时会调用onActivityResult
     *
     * @param listener
     *            用于确认在哪一个扩展中调用此方法
     * @param requestCode
     *            传人参数
     * @param intent
     *            启动Activity的intent
     */
    public void startActivityForResult(XActivityResultListener listener,
            Intent intent, int requestCode);

    /**
     * 在系统Activity上注册一个listner，用于监听通过
     * {@link Activity#startActivityForResult(Intent, int)}
     * 启动新Activity后的执行结果，不需要注销<br/>
     * 此方法与{@link Activity#startActivityForResult(Intent, int)}配对使用
     *
     * @param requestCode
     *            与{@link Activity#startActivityForResult(Intent, int)}
     *            中传入的requestCode一致
     * @param listener
     *            结果回调监听器
     */
    public void registerActivityResultListener(int requestCode,
            XActivityResultListener listener);

    /**
     * 开始显示系统SplashScreen图片
     */
    public void startBootSplash();

    /**
     * 停止显示系统SplashScreen图片
     */
    public void stopBootSplash();

    /**
     * 开始显示应用SplashScreen图片
     */
    public void startAppSplash(String imagePath);

    /**
     * 停止显示应用SplashScreen图片
     */
    public void stopAppSplash();

    /**
     * 在UI线程中运行指定的方法
     */
    public void runOnUiThread(Runnable runnable);

    /**
     * 是否正在显示splash
     *
     * @return
     */
    public boolean isBootSplashShowing();

    /**
     * 获取context
     *
     * @return
     */
    public Context getContext();

    /**
     * 启动加载应用对话框
     */
    public void waitingDialogForAppStart();

    /**
     * 关闭加载应用对话框
     */
    public void waitingDialogForAppStartFinished();

    /**
     * 弹出Toast提示框
     */
    public void toast(String message);

    /**
     * app退出
     */
    public void finish();

    /**
     * 获取启动的参数
     *
     * @return 启动参数
     */
    public XStartParams getStartParams();

    /**
     * 获得activity对象
     */
    public Activity getActivity();

    /**
     * 添加一个子视图到Activity的content view，如果view是可见的，则view会被显示在屏幕上
     *
     * @param view
     *            子视图
     */
    public void addView(XAppWebView view);

    /**
     * splash是否显示
     *
     * @return
     */
    public boolean isSplashShowing();

    /**
     * 获取安全策略
     *
     * @return 安全策略
     */
    public XSecurityPolicy getSecurityPolily();
}
