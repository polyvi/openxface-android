
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

package com.polyvi.xface.view;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage.QuotaUpdater;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.polyvi.xface.app.XWhiteList;
import com.polyvi.xface.event.XEvent;
import com.polyvi.xface.event.XEventType;
import com.polyvi.xface.event.XIWebAppEventListener;
import com.polyvi.xface.event.XSystemEventCenter;
import com.polyvi.xface.util.XLog;

/**
 * 主要负责实现webview提供的回调函数
 *
 */
public class XWebChromeClient extends WebChromeClient implements
        OnCompletionListener, OnErrorListener {

    private static final String TOKEN_EXECUTE_EXTENSION = "_xFace_jsscript:";
    /** < 执行本地扩展标志串，由javascript传进来 */
    private static final String TOKEN_JS_INIT_DONE = "_xFace_js_init_done:";
    /** < javascript初始化完毕的标志串， 由javascript传进来 */
    private static final String TOKEN_OVERRIDE_BACKBUTTON = "_xFace_override_backbutton:";
    private static final String TOKEN_OVERRIDE_VOLUME_BUTTON_DOWN = "_xFace_override_volumebutton_down:";
    private static final String TOKEN_OVERRIDE_VOLUME_BUTTON_UP = "_xFace_override_volumebutton_up:";
    private static final String TOKEN_CLOSE_APPLICATION = "xFace_close_application:";
    /** < 关闭当前的App */
    private static final String TOKEN_SEND_MESSAGE = "xFace_app_send_message:";
    /** < 发送消息 */
    private static final String TOKEN_JS_BRIDGE_SEPARATOR = "_xFace_native_bridge_separator_";
    private static final String TOKEN_JS_GAP_BRIDGE_MODE = "gap_bridge_mode:";
    private static final String TOKEN_JS_GAP_POLL = "gap_poll:";

    /** <js调用native扩展时用于分隔串的命令部分和参数部分 */
    private static final String ALERT_TITLE = "alert";
    /** < alert框的标题 */
    private static final String CONFIRM_TITLE = "confirm";
    /** < confirm框的标题 */

    private static final String CLASS_NAME = XWebChromeClient.class
            .getSimpleName();

    /**
     * js通过bridge调用native功能的命令类型定义
     */
    private enum JsCommandType {
        COMMAND_EXECUTE_EXTENSION, /** < 执行扩展 */
        COMMAND_JS_INIT_DONE, /** < 告诉本地代码，js已经初始化完毕 */
        COMMAND_OVERRIDE_BACKBUTTON, /** < 告知本地代码appview需要重写backbutton */
        COMMAND_DEFAULT, /** < 请求显示prompt/alert/confirm对话框 */
        COMMAND_CLOSE_APPLICATION, /** < 关闭当前的App的指令码 */
        COMMAND_SEND_MESSAGE, /** < 表示app发送消息的指令码 */
        COMMAND_OVERRIDE_VOLUMEBUTTONDOWN, /**
         * < 告知本地代码appview需要重写volume button
         * down
         */
        COMMAND_OVERRIDE_VOLUMEBUTTONUP,
        /** < 告知本地代码appview需要重写volume button up */
        COMMAND_GAP_BRIDGE_MODE, COMMAND_GAP_POLL
    };

    private Context mCtx;
    /** < activity上下文 */

    private XAppWebView mAppWebView;
    private VideoView mCustomVideoView = null;
    private CustomViewCallback mCustomViewCallback = null;

    /**
     * 在回调函数中设置离线应用最大缓存限制
     */
    @Override
    public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
            QuotaUpdater quotaUpdater) {
        quotaUpdater.updateQuota(requiredStorage * 2);
    }

    public XWebChromeClient(Context ctx, XAppWebView appWebView) {
        mCtx = ctx;
        mAppWebView = appWebView;
    }

    /**
     * 弹出js的alert对话框，以及实现部分js异步扩展接口的调用
     */
    @Override
    public boolean onJsAlert(WebView view, String url, String message,
            final JsResult result) {
        // TODO check 安全性 alert是否来自于发起该url的page
        JsCommandType cmdType = resolvePromptCommand(message);
        switch (cmdType) {
        case COMMAND_EXECUTE_EXTENSION: {
            int separatorIndex = message.indexOf(TOKEN_JS_BRIDGE_SEPARATOR);
            if (separatorIndex != -1) {
                // FIXME: 以后调用extension同步接口时，需要将执行结果返回给js端
                try {
                    // 解析命令参数
                    final JSONArray array = new JSONArray(message.substring(
                            TOKEN_EXECUTE_EXTENSION.length(), separatorIndex));
                    String extName = array.getString(0);
                    final String action = array.getString(1);
                    final String callbackId = array.getString(2);
                    mAppWebView
                            .getOwnerApp()
                            .getJSNativeBridge()
                            .exec(extName,
                                    action,
                                    callbackId,
                                    message.substring(separatorIndex
                                            + TOKEN_JS_BRIDGE_SEPARATOR
                                                    .length()));
                    result.confirm();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
            break;
        case COMMAND_DEFAULT:
            showAlert(message, result);
            break;
        default:
            break;
        }
        return true;
    }

    /**
     * 显示alert对话框
     */
    private void showAlert(String message, final JsResult result) {
        // FIXME： 为了实现模态框皮肤的可配置，考虑将所有模态框的实现独立出来，统一管理。
        AlertDialog.Builder dlg = new AlertDialog.Builder(mCtx);
        dlg.setMessage(message);
        dlg.setTitle(ALERT_TITLE);
        // Don't let alerts break the back button
        dlg.setCancelable(true);
        dlg.setPositiveButton(android.R.string.ok,
                new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
        dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                result.confirm();
            }
        });
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            // DO NOTHING
            public boolean onKey(DialogInterface dialog, int keyCode,
                    KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    result.confirm();
                    return false;
                } else
                    return true;
            }
        });
        dlg.create();
        dlg.show();
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message,
            final JsResult result) {
        // FIXME： 为了实现模态框皮肤的可配置，考虑将所有模态框的实现独立出来，统一管理。
        AlertDialog.Builder dlg = new AlertDialog.Builder(mCtx);
        dlg.setMessage(message);
        dlg.setTitle(CONFIRM_TITLE);
        dlg.setCancelable(true);
        dlg.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
        dlg.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        result.cancel();
                    }
                });
        dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                result.cancel();
            }
        });
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode,
                    KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    result.cancel();
                    return false;
                } else
                    return true;
            }
        });
        dlg.create();
        dlg.show();
        return true;
    }

    /**
     * 执行在UI线程
     */
    @Override
    public boolean onJsPrompt(WebView view, String url, String message,
            String defaultValue, JsPromptResult result) {
        /**安全检查确保请求合法*/
        boolean reqOk = false;
        XWhiteList whiteList = mAppWebView.getOwnerApp().getAppInfo().getWhiteList();
        if (url.startsWith("file://") || (null != whiteList && whiteList.isUrlWhiteListed(url))) {
            reqOk = true;
        }
        // TODO check 安全性 prompt是否来自于发起该url的page
        JsCommandType command = resolvePromptCommand(defaultValue);
        XIWebAppEventListener evtListener = mAppWebView.getAppEventListener();
        if (null == evtListener) {
            XLog.e(CLASS_NAME, "onJsPrompt: XIWebAppEventListener is null!");
            command = JsCommandType.COMMAND_DEFAULT;
        }
        switch (command) {
        case COMMAND_EXECUTE_EXTENSION: {
            if(reqOk) {
                try {
                    final JSONArray array = new JSONArray(
                            defaultValue.substring(TOKEN_EXECUTE_EXTENSION.length()));
                    String extName = array.getString(0);
                    final String action = array.getString(1);
                    final String callbackId = array.getString(2);
                    String r = mAppWebView.getOwnerApp().getJSNativeBridge()
                            .exec(extName, action, callbackId, message);
                    result.confirm(r == null ? "" : r);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
            break;
        case COMMAND_JS_INIT_DONE: {
            onJsInitFinished(view);
            result.confirm("OK");
        }
            break;
        case COMMAND_OVERRIDE_BACKBUTTON: {
            XAppWebView appView = ((XAppWebView) view);
            try {
                JSONArray args = new JSONArray(message);
                evtListener.onOverrideBackbutton(appView, args.getBoolean(0));
            } catch (JSONException e) {
                XLog.d(CLASS_NAME, e.getMessage());
                evtListener.onOverrideBackbutton(appView, false);
            }
            result.confirm("OK");
            break;
        }
        case COMMAND_OVERRIDE_VOLUMEBUTTONDOWN: {
            XAppWebView appView = ((XAppWebView) view);
            try {
                JSONArray args = new JSONArray(message);
                evtListener.onOverrideVolumeButtonDown(appView,
                        args.getBoolean(0));
            } catch (JSONException e) {
                XLog.d(CLASS_NAME, e.getMessage());
                evtListener.onOverrideVolumeButtonDown(appView, false);
            }
            result.confirm("OK");
            break;
        }
        case COMMAND_OVERRIDE_VOLUMEBUTTONUP: {
            XAppWebView appView = ((XAppWebView) view);
            try {
                JSONArray args = new JSONArray(message);
                evtListener.onOverrideVolumeButtonUp(appView,
                        args.getBoolean(0));
            } catch (JSONException e) {
                XLog.d(CLASS_NAME, e.getMessage());
                evtListener.onOverrideVolumeButtonUp(appView, false);
            }
            result.confirm("OK");
            break;
        }
        case COMMAND_CLOSE_APPLICATION: {
            XAppWebView appView = ((XAppWebView) view);
            int viewId = appView.getViewId();
            XEvent evt = XEvent.createEvent(XEventType.CLOSE_APP, viewId);
            XSystemEventCenter.getInstance().sendEventSync(evt);
            result.confirm("OK");
        }
            break;
        case COMMAND_SEND_MESSAGE: {
            try {
                JSONArray args = new JSONArray(message);
                XAppWebView appView = ((XAppWebView) view);
                Pair<XAppWebView, String> appMessage = new Pair<XAppWebView, String>(
                        appView, args.getString(0));
                XEvent evt = XEvent.createEvent(XEventType.XAPP_MESSAGE, appMessage);
                XSystemEventCenter.getInstance().sendEventSync(evt);
            } catch (JSONException e) {
                XLog.d(CLASS_NAME, e.getMessage());
            }
            result.confirm("OK");
        }
            break;
        case COMMAND_GAP_BRIDGE_MODE:
            if(reqOk) {
                int value = Integer.parseInt(message);
                mAppWebView.getOwnerApp().getJSNativeBridge()
                        .setNativeToJsBridgeMode(value);
                result.confirm("");
            }
            break;
        case COMMAND_GAP_POLL:
            if(reqOk) {
                String r = mAppWebView.getOwnerApp().getJSNativeBridge()
                        .retrieveJsMessages();
                result.confirm(r == null ? "" : r);
            }
            break;
        case COMMAND_DEFAULT:
            showPrompt(message, defaultValue, result);
            break;
        default:
            break;
        }
        return true;
    }

    /**
     * 解析prompt命令
     *
     * @param promptStr
     *            js传进来的命令串
     * @return
     */
    private JsCommandType resolvePromptCommand(String promptStr) {
        int jsTokenLen = TOKEN_EXECUTE_EXTENSION.length();
        if (null != promptStr
                && promptStr.length() > jsTokenLen - 1
                && promptStr.substring(0, jsTokenLen).equals(
                        TOKEN_EXECUTE_EXTENSION)) {
            return JsCommandType.COMMAND_EXECUTE_EXTENSION;
        } else if (null != promptStr && promptStr.equals(TOKEN_JS_INIT_DONE)) {
            return JsCommandType.COMMAND_JS_INIT_DONE;
        } else if (null != promptStr
                && promptStr.equals(TOKEN_OVERRIDE_BACKBUTTON)) {
            return JsCommandType.COMMAND_OVERRIDE_BACKBUTTON;
        } else if (null != promptStr
                && promptStr.equals(TOKEN_CLOSE_APPLICATION)) {
            return JsCommandType.COMMAND_CLOSE_APPLICATION;
        } else if (null != promptStr && promptStr.equals(TOKEN_SEND_MESSAGE)) {
            return JsCommandType.COMMAND_SEND_MESSAGE;
        } else if (null != promptStr
                && promptStr.equals(TOKEN_OVERRIDE_VOLUME_BUTTON_DOWN)) {
            return JsCommandType.COMMAND_OVERRIDE_VOLUMEBUTTONDOWN;
        } else if (null != promptStr
                && promptStr.equals(TOKEN_OVERRIDE_VOLUME_BUTTON_UP)) {
            return JsCommandType.COMMAND_OVERRIDE_VOLUMEBUTTONUP;
        } else if (null != promptStr
                && promptStr.equals(TOKEN_JS_GAP_BRIDGE_MODE)) {
            return JsCommandType.COMMAND_GAP_BRIDGE_MODE;
        } else if (null != promptStr && promptStr.equals(TOKEN_JS_GAP_POLL)) {
            return JsCommandType.COMMAND_GAP_POLL;
        } else {
            return JsCommandType.COMMAND_DEFAULT;
        }
    }

    /**
     * 显示prompt模态对话框
     *
     * @param message
     *            需要显示的消息
     * @param defaultValue
     *            默认值
     * @param res
     *            prompt返回结果
     */
    private void showPrompt(String message, String defaultValue,
            final JsPromptResult res) {
        // FIXME： 为了实现模态框皮肤的可配置，考虑将所有模态框的实现独立出来，统一管理。
        AlertDialog.Builder dlg = new AlertDialog.Builder(mCtx);
        dlg.setMessage(message);
        final EditText input = new EditText(mCtx);
        if (defaultValue != null) {
            input.setText(defaultValue);
        }
        dlg.setView(input);
        dlg.setCancelable(false);
        dlg.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String usertext = input.getText().toString();
                        res.confirm(usertext);
                    }
                });
        dlg.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        res.cancel();
                    }
                });
        dlg.create();
        dlg.show();
    }

    /**
     * js初始化完毕，取消启动画面，将webview显示出来
     *
     * @param view
     *            被启动的webview
     */
    private void onJsInitFinished(WebView view) {
        // TODO:取消启动画面
        XIWebAppEventListener evtListener = mAppWebView.getAppEventListener();
        if (null == evtListener) {
            XLog.e(CLASS_NAME,
                    "onJsInitFinished: XIWebAppEventListener is null!");
            return;
        }
        XAppWebView appView = ((XAppWebView) view);
        evtListener.onJsInitFinished(appView);
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin,
            Callback callback) {
        super.onGeolocationPermissionsShowPrompt(origin, callback);
        callback.invoke(origin, true, false);
    }

    /**
     * js错误信息输出回调函数
     *
     * @param message
     *            错误信息
     * @param lineNumber
     *            错误出现的行号
     * @param sourceID
     *            错误出现的js文件的绝对路径
     */
    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        XLog.e(sourceID, "Line %d : %s", lineNumber, message);
        super.onConsoleMessage(message, lineNumber, sourceID);
    }

    @Override
    // 此回调是为了支持 Android 2.x 利用 HTML5 video 标签播放视频而添加
    // 触发此回调的时机是在当 JS 端有如下语句时：
    /**
     * < video id="video" height="240" width="360" >
     *   < source src="...file path..." >
     * < /video >
     *
     * var video = document.getElementById('video');
     * video.play();
     */
    public void onShowCustomView(View view, CustomViewCallback callback) {
        // CustomViewCallback 是在 WebChromeClient 中定义的，此回调由外部传入
        // TODO: 如果用户在播放过程中，点击了返回键，应该进行 onCompletion 相同操作：释放资源并隐藏播放 video 的 view
        // 这些操作应该放在 XRuntime 中由 handleKeyUp 函数来处理。
        super.onShowCustomView(view, callback);
        if (view instanceof FrameLayout) {
            FrameLayout frame = (FrameLayout) view;
            this.mCustomViewCallback = callback;
            if (frame.getFocusedChild() instanceof VideoView) {
                mCustomVideoView = (VideoView) frame.getFocusedChild();
                frame.setVisibility(View.VISIBLE);

                FrameLayout.LayoutParams layoutParameters = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.FILL_PARENT,
                        FrameLayout.LayoutParams.FILL_PARENT);

                ((Activity) mCtx).addContentView(frame, layoutParameters);

                mCustomVideoView.setOnCompletionListener(this);
                mCustomVideoView.setOnErrorListener(this);
                mCustomVideoView.start();
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        XLog.e(CLASS_NAME, "Error : " + what + ", " + extra);

        // 释放资源并隐藏播放 video 的 view
        mCustomVideoView.stopPlayback();
        mCustomViewCallback.onCustomViewHidden();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // 释放资源并隐藏播放 video 的 view
        mCustomVideoView.stopPlayback();
        mCustomViewCallback.onCustomViewHidden();
    }

}
