
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

import com.polyvi.xface.core.XNativeToJsMessageQueue;
import com.polyvi.xface.extension.XExtensionManager;

import org.json.JSONException;

/**
 * js调用到本地代码的bridge
 *
 */
public class XJSNativeBridge {

    private XExtensionManager mExtManager;
    private XNativeToJsMessageQueue mJsMessageQueue;

    public XJSNativeBridge(XExtensionManager em, XNativeToJsMessageQueue queue) {
        mExtManager = em;
        mJsMessageQueue = queue;
    }

    /**
     * js接口会调用到这里
     *
     * @param service
     *            扩展的名字
     * @param action
     *            扩展对应的方法
     * @param callbackId
     *            回调id
     * @param arguments
     *            参数
     * @return
     * @throws JSONException
     */
    public String exec(String service, String action, String callbackId,
            String arguments) throws JSONException {
        if (arguments == null) {
            return "@Null arguments.";
        }
        mJsMessageQueue.setPaused(true);
        try {
            boolean wasSync = mExtManager.exec(service, action, callbackId, arguments);
            String ret = "";
            if (!XNativeToJsMessageQueue.DISABLE_EXEC_CHAINING || wasSync) {
                ret = mJsMessageQueue.popAndEncode();
            }
            return ret;
        } finally {
            mJsMessageQueue.setPaused(false);
        }
    }

    /**
     * 设置native执行js的桥接模式
     *
     * @param value
     */
    public void setNativeToJsBridgeMode(int value) {
        mJsMessageQueue.setBridgeMode(value);
    }

    /**
     * 从本地检索js消息
     *
     * @return
     */
    public String retrieveJsMessages() {
        return mJsMessageQueue.popAndEncode();
    }
}
