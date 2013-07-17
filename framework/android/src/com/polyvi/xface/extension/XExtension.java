
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

package com.polyvi.xface.extension;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;

import com.polyvi.xface.plugin.api.XIWebContext;

/**
 * 为所有的js扩展功能提供的一个公共接口
 */
public abstract class XExtension {

    protected XExtensionContext mExtensionContext;

    protected XIWebContext mWebContext;

    /**
     * 扩展的初始化方法，所有的XExtension子类的构造函数都不能带参数，相关初始化操作在该方法中完成， 子类如果有自己的初始化操作，需要覆写该方法
     *
     * @param extensionContext
     *            扩展上下环境对象
     */
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        this.mExtensionContext = extensionContext;
        this.mWebContext = webContext;
    }

    /**
     * 工厂方法，用于获取当前扩展的功能实现对象，如果有该对象子类需要覆写该方法，并在init初始化方法中通过该方法 获取实现对象，并赋给对应的成员变量
     */
    protected Object getNativeWorker() {
        return null;
    }

    /**
     * 发送异步执行状态或结果给消息处理器（{@link XExtResultHandler}）
     *
     * @param result
     *            执行状态或结果
     */
    public abstract void sendAsyncResult(String result);

    /**
     * 判断指定的行为是否是异步
     *
     * @param action
     *            需要执行的行为
     * @return 异步 true， 同步 false
     */
    public abstract boolean isAsync(String action);

    /**
     * 执行js扩展
     *
     * @param action
     *            需要执行的扩展行为
     * @param args
     *            执行行为需要的参数
     * @param callbackCtx
     *            回调上下文环境
     */
    public abstract XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException;

    /**
     * 在Activity执行onDestroy（即退出xFace的时候）扩展需要做自己的回收的处理
     * 例如：之前注册了广播接收器的，那么在destroy中要进行反注册
     * */
    public void destroy() {
    }

    /**
     * 获取基础的context
     * @return
     */
    protected Context getContext() {
        return this.mExtensionContext.getSystemContext().getContext();
    }

    /**
     * 当页面切换时，通知每个 ext 的回调
     */
    public void onPageStarted() {
    }

    /**
     * 退出app时，通知每个 ext 回调
     */
    public void onAppClosed() {
    }

    /**
     * 当卸载app时，通知每个 ext 回调
     */
    public void onAppUninstalled() {
    }

    /**
     * 当app在后台时，通知每个ext回调
     * @param appId
     */
    public void onPause() {
    }

    /**
     * 当app恢复到前台时，通知每个ext回调
     * @param appId
     */
    public void onResume() {
    }
}
