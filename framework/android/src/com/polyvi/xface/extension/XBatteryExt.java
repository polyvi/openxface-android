
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
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.polyvi.xface.util.XLog;

public class XBatteryExt extends XExtension {

    private static final String CLASS_NAME = XBatteryExt.class.getSimpleName();

    private static final String COMMAND_START = "start";/**< js语句的start命令*/

    private static final String COMMAND_STOP = "stop";/**< js语句的stop命令*/

    private BroadcastReceiver mBroadcastReceiver;/**< 广播接收器*/


    public XBatteryExt() {
        mBroadcastReceiver = null;
    }

    @Override
    public void sendAsyncResult(String result) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        XExtensionResult.Status status = XExtensionResult.Status.INVALID_ACTION;
        String result = "Unsupported Operation: " + action;
        if (COMMAND_START.equals(action)) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            final XCallbackContext cbCtx = callbackCtx;
            if (null == mBroadcastReceiver) {
                mBroadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        XLog.d(CLASS_NAME, "received broadcast of battery changing");
                        updateBatteryInfo(intent, cbCtx);
                    }
                };
                getContext().registerReceiver(
                        mBroadcastReceiver, intentFilter);
            }
            XExtensionResult extensionResult = new XExtensionResult(
                    XExtensionResult.Status.NO_RESULT);
            extensionResult.setKeepCallback(true);
            return extensionResult;
        } else if (COMMAND_STOP.equals(action)) {
            removeBatteryListener();
            sendUpdate(new JSONObject(), callbackCtx, false);
            return new XExtensionResult(XExtensionResult.Status.OK);
        }
        return new XExtensionResult(status, result);
    }

    /**
     * 更新电池信息
     * @param batteryIntent 接收电池电量改变的intent
     * @param callbackCtx 回调上下文环境
     * */
    private void updateBatteryInfo(Intent batteryIntent, XCallbackContext callbackCtx) {
        sendUpdate(getBatteryInfo(batteryIntent), callbackCtx, true);
    }

    /**
     * 删除电池监听器，反注册广播接收器
     * */
    private void removeBatteryListener() {
        if (null != mBroadcastReceiver) {
            try {
                getContext().unregisterReceiver(
                        mBroadcastReceiver);
                mBroadcastReceiver = null;
            } catch (Exception e) {
                XLog.e(CLASS_NAME,
                        "Error unregistering battery receiver: "
                                + e.getMessage(), e);
            }
        }
    }

    /**
     * 根据电池信息的改变新生成ExtensionResult，并且将该消息发送给js
     *
     * @param info 电池状态的json数据
     * @param keepCallback 是否保持回调
     * @param callbackCtx 回调上下文环境
     */
    private void sendUpdate(JSONObject info, XCallbackContext callbackCtx,
            boolean keepCallback) {
        XExtensionResult result = new XExtensionResult(
                XExtensionResult.Status.OK, info);
        result.setKeepCallback(keepCallback);
        callbackCtx.sendExtensionResult(result);
    }

    /**
     * 根据当前的电池状态信息构建json对象
     *
     * @param batteryIntent 可以获取当前电池状态信息的intent
     * @return 包含电池信息的json对象
     */
    private JSONObject getBatteryInfo(Intent batteryIntent) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("level", batteryIntent.getIntExtra(
                    android.os.BatteryManager.EXTRA_LEVEL, 0));
            obj.put("isPlugged", batteryIntent.getIntExtra(
                    android.os.BatteryManager.EXTRA_PLUGGED, -1) > 0 ? true
                    : false);
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage(), e);
        }
        return obj;
    }

    @Override
    public void destroy() {
        removeBatteryListener();
    }
}
