
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

package com.polyvi.xface.event;

import android.content.Context;
import android.view.KeyEvent;
import android.webkit.WebView;

import com.polyvi.xface.app.XApplication;
import com.polyvi.xface.core.XRuntime;
import com.polyvi.xface.extension.XExtensionManager;
import com.polyvi.xface.util.XConstant;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.view.XAppWebView;

public class XAppEventHandler implements XISystemEventReceiver,
        XIWebAppEventListener, XIKeyEventListener {

    protected XApplication mOwnerApp;

    /** 扩展管理器 */
    private XExtensionManager mExtensionManager;

    /** javascript执行器 */

    public XAppEventHandler(XApplication app, XExtensionManager em) {
        mOwnerApp = app;
        mExtensionManager = em;
    }

    /**
     * 注册系统事件接收器
     */
    public void registerSystemEventReceiver() {
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.PAUSE);
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.RESUME);
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.DESTROY);
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.MSG_RECEIVED);
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.CALL_RECEIVED);
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.PUSH_MSG_RECEIVED);
    }

    /**
     * 反注册系统事件接收器
     */
    public void unRegisterSystemEventReceiver() {
        XSystemEventCenter.getInstance().unregisterReceiver(this);
    }

    @Override
    public void onReceived(Context context, XEvent evt) {
        if (evt.getType() == XEventType.PAUSE) {
            handlePause();
        } else if (evt.getType() == XEventType.RESUME) {
            handleResume();
        } else if (evt.getType() == XEventType.DESTROY) {
            handleDestroy();
        } else if (evt.getType() == XEventType.MSG_RECEIVED) {
            String msgs = (String) evt.getData();
            handleMsgEvent(msgs);
        } else if (evt.getType() == XEventType.CALL_RECEIVED) {
            int callStatus = (Integer) evt.getData();
            handleCallReceived(callStatus);
        }
    }

    /**
     * 处理resume事件
     */
    private void handleResume() {
        String jsScript = "try{xFace.require('xFace/channel').onResume.fire();}catch(e){console.log('exception firing resume event from native');}";
        XAppWebView appView = (XAppWebView) mOwnerApp.getView();
        if (null != appView && appView.isJsInitFinished()) {
            mOwnerApp.sendJavascript(jsScript);
        }
        if (null != mExtensionManager) {
            mExtensionManager.onResume();
        }
    }

    /**
     * 处理pause事件
     */
    private void handlePause() {
        String jsScript = "try{xFace.require('xFace/channel').onPause.fire();}catch(e){console.log('exception firing pause event from native');}";
        XAppWebView appView = (XAppWebView) mOwnerApp.getView();
        if (null != appView && appView.isJsInitFinished()) {
            mOwnerApp.sendJavascript(jsScript);
        }
        if (null != mExtensionManager) {
            mExtensionManager.onPause();
        }
    }

    /**
     * 处理destroy事件
     */
    public void handleDestroy() {
    }

    /**
     * 处理短信事件
     *
     * @param msgs
     */
    public void handleMsgEvent(String msgs) {
        String jsScript = "try{ xFace.require('xFace/channel').onMsgReceived.fire('"
                + msgs + "');}catch(e){console.log('msg rcv : ' + e);}";
        XAppWebView appView = (XAppWebView) mOwnerApp.getView();
        if (null != appView && appView.isJsInitFinished()) {
            mOwnerApp.sendJavascript(jsScript);
        }
    }

    /**
     * 处理来电事件
     *
     * @param callStatus
     */
    public void handleCallReceived(int callStatus) {
        String jsScript = "try{ xFace.require('xFace/channel').onCallReceived.fire('"
                + callStatus + "');}catch(e){console.log('call rcv : ' + e);}";
        XAppWebView appView = (XAppWebView) mOwnerApp.getView();
        if (null != appView && appView.isJsInitFinished()) {
            mOwnerApp.sendJavascript(jsScript);
        }
    }

    /**
     * 处理通知事件
     *
     * @param message
     */
    public void handleNotificationReceived(String message) {
        String jsScript = "try{ xFace.require('xFace/extension/PushNotification').fire('"
                + message + "');}catch(e){console.log('call rcv : ' + e);}";
        XAppWebView appView = (XAppWebView) mOwnerApp.getView();
        if (null != appView && appView.isJsInitFinished()) {
            mOwnerApp.sendJavascript(jsScript);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent evt) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 消除自定义视图(e.g. 播放视频视图)
            if (mOwnerApp.getView().hideCustomView()) {
                return false;
            } else {
                if (mOwnerApp.isOverrideBackbutton()) {
                    String jsScript = "xFace.fireDocumentEvent('backbutton');";
                    mOwnerApp.sendJavascript(jsScript);
                    return false;
                } else {
                    WebView webView = (XAppWebView) mOwnerApp.getView();
                    if (webView.canGoBack()) {
                        webView.goBack();
                        return false;
                    }
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            String jsScript = "xFace.fireDocumentEvent('menubutton');";
            mOwnerApp.sendJavascript(jsScript);
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            String jsScript = "xFace.fireDocumentEvent('searchbutton');";
            mOwnerApp.sendJavascript(jsScript);
            return false;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        // volume down和up这两个键在keyUp当中处理无法避免对音量的控制作用
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mOwnerApp.isOverrideVolumeButtonDown()) {
                String jsScript = "xFace.fireDocumentEvent('volumedownbutton');";
                mOwnerApp.sendJavascript(jsScript);
                return false;
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (mOwnerApp.isOverrideVolumeButtonUp()) {
                String jsScript = "xFace.fireDocumentEvent('volumeupbutton');";
                mOwnerApp.sendJavascript(jsScript);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onPageLoadingFinished(XAppWebView view) {
        String params = (String) mOwnerApp
                .getData(XConstant.TAG_APP_START_PARAMS);
        if (params != null) {
            mOwnerApp.removeData(XConstant.TAG_APP_START_PARAMS);
        }
        String script = "try{ xFace.require('xFace/channel').onNativeReady.fire('"
                + params + "');}catch(e){_nativeReady = true;}";
        XLog.d(XRuntime.class.getSimpleName(), script);
        // 注意 由于需要在启动xapp的时候 传入参数 这段js只能在本地执行 故不能统一纳入XNativeToJsMessageQueue结构
        view.loadUrl("javascript:" + script);
        script = "try{xFace.require('xFace/privateModule').initPrivateData(['"
                + mOwnerApp.getAppId()
                + "',"
                + false
                + "]);}catch(e){console.log('exception set app id from native');}";
        mOwnerApp.sendJavascript(script);
    }

    @Override
    public void onJsInitFinished(XAppWebView view) {
        XAppWebView appView = (XAppWebView) view;
        appView.setJsInitFinished(true);
        mOwnerApp.tryShowView();
    }

    @Override
    public void onOverrideBackbutton(XAppWebView view,
            boolean overrideBackbutton) {
        setOverrideBackbutton(view.getViewId(), overrideBackbutton);
    }

    @Override
    public void onOverrideVolumeButtonDown(XAppWebView view,
            boolean overrideVolumeButtonDown) {
        setOverrideVolumeButtonDown(view.getViewId(), overrideVolumeButtonDown);
    }

    @Override
    public void onOverrideVolumeButtonUp(XAppWebView view,
            boolean overrideVolumeButtonUp) {
        setOverrideVolumeButtonUp(view.getViewId(), overrideVolumeButtonUp);
    }

    @Override
    public void onCloseApplication(int viewId) {
        XEvent evt = XEvent.createEvent(XEventType.CLOSE_ENGINE);
        XSystemEventCenter.getInstance().sendEventSync(evt);
    }

    @Override
    public void onPageStarted(XAppWebView view) {
        this.mOwnerApp.resetJsMessageQueue();
        // 在页面加载时，将该页面的返回键，控件音量的键重写状态置为false
        int viewId = view.getViewId();
        setOverrideBackbutton(viewId, false);
        setOverrideVolumeButtonDown(viewId, false);
        setOverrideVolumeButtonUp(viewId, false);
        // app的页面发生切换，所以该app一定存在不为空
        // 当页面切换时，通知每个 ext，例如，有需要在 ext 中销毁创建的本地对象的，就可以在此实现
        mExtensionManager.onPageStarted();
    }

    @Override
    public void onXAppMessageReceived(XAppWebView view, String msgData) {

    }

    private void setOverrideBackbutton(int viewId, boolean isOverrideBackbutton) {
        mOwnerApp.setOverrideBackbutton(isOverrideBackbutton);
    }

    private void setOverrideVolumeButtonDown(int viewId,
            boolean isOverrideVolumeButtonDown) {
        mOwnerApp.setOverrideVolumeButtonDown(isOverrideVolumeButtonDown);
    }

    private void setOverrideVolumeButtonUp(int viewId,
            boolean isOverrideVolumeButtonUp) {
        mOwnerApp.setOverrideVolumeButtonUp(isOverrideVolumeButtonUp);
    }
}
