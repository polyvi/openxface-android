
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
import org.json.JSONObject;

import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XLog;


public class XCallbackContext {
    private static final String CLASS_NAME = XCallbackContext.class.getName();
    private boolean mFinished;
    private String mCallbackId;
    private XIWebContext mWebContext;

    public XCallbackContext(XIWebContext webContext, String callbackId) {
        mWebContext = webContext;
        mCallbackId = callbackId;
    }

    public boolean isFinished() {
        return mFinished;
    }

    public String getCallbackId() {
        return mCallbackId;
    }

    /**
     * 发送执行扩展的结果
     * @param extResult
     */
    public void sendExtensionResult(XExtensionResult extResult) {
        synchronized (this) {
            if (mFinished) {
                XLog.w(CLASS_NAME, "Send a callbackID: " + mCallbackId
                        + "\nResult was: " + extResult.getMessage());
                return;
            } else {
                mFinished = !extResult.getKeepCallback();
            }
            mWebContext.sendExtensionResult(extResult, mCallbackId);
        }
    }

    /**
     * 发送执行扩展的结果
     * @param message
     */
    public void sendExtensionResult(String message) {
        mWebContext.sendJavascript(message);
    }

    /**
     * 发送没有附加内容的成功回调结果
     */
    public void success() {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.OK));
    }

    /**
     * 发送JSON数据对象信息的成功回调结果
     * @param message
     */
    public void success(JSONObject message) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.OK, message));
    }

    /**
     * 发送String数据对象信息的成功回调结果
     * @param message
     */
    public void success(String message) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.OK, message));
    }

    /**
     * 发送JSONArray数据对象信息的成功回调结果
     * @param message
     */
    public void success(JSONArray message) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.OK, message));
    }

    /**
     * 发送long类型数据信息的成功回调结果
     * @param message
     */
    public void success(long value) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.OK, value));
    }

    /**
     * 发送boolean类型数据信息的成功回调结果
     * @param message
     */
    public void success(boolean value) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.OK, value));
    }

    /**
     * 发送没有附加内容的失败回调结果
     */
    public void error() {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.ERROR));
    }

    /**
     * 发送String数据对象信息的失败回调结果
     * @param message
     */
    public void error(String message) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.ERROR, message));
    }

    /**
     * 发送int数据对象信息的失败回调结果
     * @param message
     */
    public void error(int message) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.ERROR, message));
    }

    /**
     * 发送JSONObject数据对象信息的失败回调结果
     * @param message
     */
    public void error(JSONObject message) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.ERROR, message));
    }

    /**
     * 发送JSONArray数据对象信息的失败回调结果
     * @param message
     */
    public void error(JSONArray message) {
        sendExtensionResult(new XExtensionResult(XExtensionResult.Status.ERROR, message));
    }
}
