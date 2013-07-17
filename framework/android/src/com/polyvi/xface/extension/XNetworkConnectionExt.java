
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XLog;

public class XNetworkConnectionExt extends XExtension {

    private final String CLASS_NAME = XNetworkConnectionExt.class.getSimpleName();

    /**  NetworkConnection 提供给js用户的接口名字*/
    private static final String COMMAND_GET_CONNECTIONINFO = "getConnectionInfo";

    public static int NOT_REACHABLE = 0;
    public static int REACHABLE_VIA_CARRIER_DATA_NETWORK = 1;
    public static int REACHABLE_VIA_WIFI_NETWORK = 2;

    public static final String WIFI = "wifi";
    public static final String WIMAX = "wimax";
    // mobile
    public static final String MOBILE = "mobile";
    // 2G network types
    public static final String GSM = "gsm";
    public static final String GPRS = "gprs";
    public static final String EDGE = "edge";
    // 3G network types
    public static final String CDMA = "cdma";
    public static final String UMTS = "umts";
    public static final String HSPA = "hspa";
    public static final String HSUPA = "hsupa";
    public static final String HSDPA = "hsdpa";
    public static final String ONEXRTT = "1xrtt";
    public static final String EHRPD = "ehrpd";
    // 4G network types
    public static final String LTE = "lte";
    public static final String UMB = "umb";
    public static final String HSPA_PLUS = "hspa+";
    // return types
    public static final String TYPE_UNKNOWN = "unknown";
    public static final String TYPE_ETHERNET = "ethernet";
    public static final String TYPE_WIFI = "wifi";
    public static final String TYPE_2G = "2g";
    public static final String TYPE_3G = "3g";
    public static final String TYPE_4G = "4g";
    public static final String TYPE_NONE = "none";

    private ConnectivityManager mSockMan;
    private BroadcastReceiver mReceiver;
    private XCallbackContext mCallbackCtx;


    public XNetworkConnectionExt() {
        this.mReceiver = null;
    }

    @Override
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        super.init(extensionContext, webContext);
        initConnectionContext();
    }

    @Override
    public void sendAsyncResult(String result) {
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
        mCallbackCtx = callbackCtx;

        if (action.equals(COMMAND_GET_CONNECTIONINFO)) {
            NetworkInfo info = mSockMan.getActiveNetworkInfo();
            XExtensionResult pluginResult = new XExtensionResult(XExtensionResult.Status.OK, this.getConnectionInfo(info));
            pluginResult.setKeepCallback(true);
            return pluginResult;
        }

        return new XExtensionResult(status, result);
    }

    /**
     * 初始化 plugin 的 NetworkConnection 环境.
     * 在此方法中与 Android 系统服务建立联系，并注册BroadcastReceiver，接收 Android 系统的 broadcast
     *
     */
    public void initConnectionContext() {
        Context context = getContext();
        this.mSockMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 需要监听网络连接变化事件，以便及时更新connection
        IntentFilter intentFilter = new IntentFilter() ;
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        if (this.mReceiver == null) {
            this.mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateConnectionInfo((NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO));
                }
            };
            context.registerReceiver(this.mReceiver, intentFilter);
        }
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * 当connection改变时，更新JS端信息.
     *
     * @param info   当前活跃的网络信息
     * @return
     */
    private void updateConnectionInfo(NetworkInfo info) {
        // 发送更新信息到JS："xFace.network.connection"
        sendUpdate(this.getConnectionInfo(info));
    }

    /**
     * 获得最新的网络连接信息.
     *
     * @param info   当前活跃的网络信息
     * @return       表示网络信息的一个 JSONObject
     */
    private String getConnectionInfo(NetworkInfo info) {
        String type = TYPE_NONE;
        if (null != info) {
            // 如果没有连接到任何网络，则设置type为none
            if (!info.isConnected()) {
                type = TYPE_NONE;
            }
            else {
                type = getType(info);
            }
        }
        return type;
    }

    /**
     * 生成一个新的 plugin result，并将其传回JS.
     *
     * @param type  网络连接类型，设置到xFace.network.connection.type
     */
    private void sendUpdate(String type) {
        if(null != mCallbackCtx) {
            XExtensionResult result = new XExtensionResult(XExtensionResult.Status.OK, type);
            result.setKeepCallback(true);
            mCallbackCtx.sendExtensionResult(result);
        }
        // TODO: 发送消息给所有的plugin
        // 例如 XExtensionManager.postMessage("networkconnection", type);
        // 每个 plugin 使用 onMessage() 来响应这个消息.
    }

    /**
     * 确定并获得网络连接的类型.
     *
     * @param info 当前活跃的网络信息
     * @return     当前正在使用的移动网络类型
     */
    private String getType(NetworkInfo info) {
        if (null != info) {
            String type = info.getTypeName();

            if (type.toLowerCase().equals(WIFI)) {
                return TYPE_WIFI;
            }
            else if (type.toLowerCase().equals(MOBILE)) {
                type = info.getSubtypeName();
                if (type.toLowerCase().equals(GSM) ||
                        type.toLowerCase().equals(GPRS) ||
                        type.toLowerCase().equals(EDGE)) {
                    return TYPE_2G;
                }
                else if (type.toLowerCase().startsWith(CDMA) ||
                        type.toLowerCase().equals(UMTS)  ||
                        type.toLowerCase().equals(ONEXRTT) ||
                        type.toLowerCase().equals(EHRPD) ||
                        type.toLowerCase().equals(HSUPA) ||
                        type.toLowerCase().equals(HSDPA) ||
                        type.toLowerCase().equals(HSPA)) {
                    return TYPE_3G;
                }
                else if (type.toLowerCase().equals(LTE) ||
                        type.toLowerCase().equals(UMB) ||
                        type.toLowerCase().equals(HSPA_PLUS)) {
                    return TYPE_4G;
                }
            }
        }
        else {
            return TYPE_NONE;
        }
        return TYPE_UNKNOWN;
    }

    @Override
    public void destroy() {
        if(null != mReceiver){
            try {
                getContext().unregisterReceiver(mReceiver);
                mReceiver = null;
            } catch (Exception e) {
                XLog.e(CLASS_NAME,
                        "Error unregistering networkconnection receiver: "
                                + e.getMessage(), e);
            }
        }
    }
}
