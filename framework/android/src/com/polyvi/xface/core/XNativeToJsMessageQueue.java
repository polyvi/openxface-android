
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;

import android.os.Message;
import android.util.Log;
import android.webkit.WebView;

import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.view.XAppWebView;

/**
 * 维护native发送给webview的js消息列表 实现native到js的桥接
 *
 */
public class XNativeToJsMessageQueue {
    private static final String LOG_TAG = XNativeToJsMessageQueue.class.getSimpleName();

    // 默认从本地执行js的桥接模式
    private static final int DEFAULT_BRIDGE_MODE = 1;

    // 设置该标志强制扩展的结果被编码成js格式的字符串而非自定义格式
    private static final boolean FORCE_ENCODE_USING_EVAL = false;

    // 由于可能导致安全问问题 默认关闭通过url实现的桥接模式
    public static final boolean ENABLE_LOCATION_CHANGE_EXEC_MODE = false;

    // 标志 当执行exec期间，是否允许从native发送js message
    public static final boolean DISABLE_EXEC_CHAINING = false;

    // js执行时 默认最大的负载长度
    private static int MAX_PAYLOAD_SIZE = 50 * 1024 * 10240;

    // 桥接模式索引
    private int mActiveListenerIndex;

    // 当js消息进入队列的时候，如果该标志位true 则不触发js的执行
    private boolean mPaused;

    // js消息列表
    private final LinkedList<JsMessage> mJsQueue = new LinkedList<JsMessage>();

    // native到js 桥接模式列表
    private final BridgeMode[] mRegisteredListeners;

    private final XISystemContext mSysContext;
    private final XAppWebView mWebView;

    public XNativeToJsMessageQueue(XAppWebView webView, XISystemContext context) {
        this.mSysContext = context;
        this.mWebView = webView;
        mRegisteredListeners = new BridgeMode[4];
        mRegisteredListeners[0] = null; // 轮询模式 本地不需要任何逻辑 js端通过timer实现
        mRegisteredListeners[1] = new LoadUrlBridgeMode();
        mRegisteredListeners[2] = new OnlineEventsBridgeMode();
        mRegisteredListeners[3] = new PrivateApiBridgeMode();
        reset();
    }

    /**
     * 改变桥接模式
     *
     * @param value
     *            桥接模式索引
     */
    public void setBridgeMode(int value) {
        if (value < 0 || value >= mRegisteredListeners.length) {
            XLog.d(LOG_TAG, "Invalid NativeToJsBridgeMode: " + value);
        } else {
            if (value != mActiveListenerIndex) {
                XLog.d(LOG_TAG, "Set native->JS mode to " + value);
                synchronized (this) {
                    mActiveListenerIndex = value;
                    BridgeMode activeListener = mRegisteredListeners[value];
                    if (!mPaused && !mJsQueue.isEmpty() && activeListener != null) {
                        activeListener.onNativeToJsMessageAvailable();
                    }
                }
            }
        }
    }

    /**
     * 清空队列 恢复默认的桥接模式
     */
    public void reset() {
        synchronized (this) {
            mJsQueue.clear();
            setBridgeMode(DEFAULT_BRIDGE_MODE);
        }
    }

    private int calculatePackedMessageLength(JsMessage message) {
        int messageLen = message.calculateEncodedLength();
        String messageLenStr = String.valueOf(messageLen);
        return messageLenStr.length() + messageLen + 1;
    }

    private void packMessage(JsMessage message, StringBuilder sb) {
        int len = message.calculateEncodedLength();
        sb.append(len).append(' ');
        message.encodeAsMessage(sb);
    }

    /**
     * 从队列返回 需要执行的js消息，为了提高效率，在不超过最大负载的情况下 尽可能多的返回js消息
     *
     * @return
     */
    public String popAndEncode() {
        synchronized (this) {
            if (mJsQueue.isEmpty()) {
                return null;
            }
            int totalPayloadLen = 0;
            int numMessagesToSend = 0;
            for (JsMessage message : mJsQueue) {
                int messageSize = calculatePackedMessageLength(message);
                if (numMessagesToSend > 0
                        && totalPayloadLen + messageSize > MAX_PAYLOAD_SIZE
                        && MAX_PAYLOAD_SIZE > 0) {
                    break;
                }
                totalPayloadLen += messageSize;
                numMessagesToSend += 1;
            }

            StringBuilder sb = new StringBuilder(totalPayloadLen);
            for (int i = 0; i < numMessagesToSend; ++i) {
                JsMessage message = mJsQueue.removeFirst();
                packMessage(message, sb);
            }

            if (!mJsQueue.isEmpty()) {
                // 表示队列有还有没有处理的消息 js层会用到这个标志
                sb.append('*');
            }
            String ret = sb.toString();
            return ret;
        }
    }

    /**
     * 功能和popAndEncode类似，但是返回的js消息可以直接被执行 没有被用户编码
     *
     * @return
     */
    private String popAndEncodeAsJs() {
        synchronized (this) {
            int length = mJsQueue.size();
            if (length == 0) {
                return null;
            }
            int totalPayloadLen = 0;
            int numMessagesToSend = 0;
            for (JsMessage message : mJsQueue) {
                int messageSize = message.calculateEncodedLength() + 50; // overestimate.
                if (numMessagesToSend > 0
                        && totalPayloadLen + messageSize > MAX_PAYLOAD_SIZE
                        && MAX_PAYLOAD_SIZE > 0) {
                    break;
                }
                totalPayloadLen += messageSize;
                numMessagesToSend += 1;
            }
            boolean willSendAllMessages = numMessagesToSend == mJsQueue.size();
            StringBuilder sb = new StringBuilder(totalPayloadLen
                    + (willSendAllMessages ? 0 : 100));
            for (int i = 0; i < numMessagesToSend; ++i) {
                JsMessage message = mJsQueue.removeFirst();
                if (willSendAllMessages && (i + 1 == numMessagesToSend)) {
                    message.encodeAsJsMessage(sb);
                } else {
                    sb.append("try{");
                    message.encodeAsJsMessage(sb);
                    sb.append("}finally{");
                }
            }
            if (!willSendAllMessages) {
                sb.append("window.setTimeout(function(){xFace.require('xFace/exec').pollOnce();},0);");
            }
            for (int i = willSendAllMessages ? 1 : 0; i < numMessagesToSend; ++i) {
                sb.append('}');
            }
            String ret = sb.toString();
            return ret;
        }
    }

    /**
     * 增加js语句到js消息列表
     */
    public void addJavaScript(String statement) {
        enqueueMessage(new JsMessage(statement));
    }

    /**
     * 增加回调结果到js消息列表
     */
    public void addPluginResult(XExtensionResult result, String callbackId) {
        if (callbackId == null) {
            XLog.e(LOG_TAG, "Got plugin result with no callbackId",
                    new Throwable());
            return;
        }

        boolean noResult = result.getStatus() == XExtensionResult.Status.NO_RESULT
                .ordinal();
        boolean keepCallback = result.getKeepCallback();
        if (noResult && keepCallback) {
            return;
        }
        JsMessage message = new JsMessage(result, callbackId);
        if (FORCE_ENCODE_USING_EVAL) {
            StringBuilder sb = new StringBuilder(
                    message.calculateEncodedLength() + 50);
            message.encodeAsJsMessage(sb);
            message = new JsMessage(sb.toString());
        }

        enqueueMessage(message);
    }

    private void enqueueMessage(JsMessage message) {
        synchronized (this) {
            mJsQueue.add(message);
            if (!mPaused && mRegisteredListeners[mActiveListenerIndex] != null) {
                mRegisteredListeners[mActiveListenerIndex]
                        .onNativeToJsMessageAvailable();
            }
        }
    }

    public void setPaused(boolean value) {
        if (mPaused && value) {
            XLog.e(LOG_TAG, "nested call to setPaused detected.",
                    new Throwable());
        }
        mPaused = value;
        if (!value) {
            synchronized (this) {
                if (!mJsQueue.isEmpty()
                        && mRegisteredListeners[mActiveListenerIndex] != null) {
                    mRegisteredListeners[mActiveListenerIndex]
                            .onNativeToJsMessageAvailable();
                }
            }
        }
    }

    public boolean getPaused() {
        return mPaused;
    }

    private interface BridgeMode {
        void onNativeToJsMessageAvailable();
    }

    /** 通过webview的loadurl执行js消息 */
    private class LoadUrlBridgeMode implements BridgeMode {
        final Runnable runnable = new Runnable() {
            public void run() {
                String js = popAndEncodeAsJs();
                if (js != null) {
                    mWebView.loadUrl("javascript:" + js);
                }
            }
        };

        public void onNativeToJsMessageAvailable() {
            mSysContext.runOnUiThread(runnable);
        }
    }

    /** 通过online/line事件 通知js 获取本地js消息 */
    private class OnlineEventsBridgeMode implements BridgeMode {
        boolean online = true;
        final Runnable runnable = new Runnable() {
            public void run() {
                if (!mJsQueue.isEmpty()) {
                    online = !online;
                    mWebView.setNetworkAvailable(online);
                }
            }
        };

        OnlineEventsBridgeMode() {
            mWebView.setNetworkAvailable(true);
        }

        public void onNativeToJsMessageAvailable() {
            mSysContext.runOnUiThread(runnable);
        }
    }

    /**
     * 通过反射执行js消息
     */
    private class PrivateApiBridgeMode implements BridgeMode {
        private static final int EXECUTE_JS = 194;

        Method sendMessageMethod;
        Object webViewCore;
        boolean initFailed;

        @SuppressWarnings("rawtypes")
        private void initReflection() {
            Object webViewObject = mWebView;
            Class webViewClass = WebView.class;
            try {
                Field f = webViewClass.getDeclaredField("mProvider");
                f.setAccessible(true);
                webViewObject = f.get(mWebView);
                webViewClass = webViewObject.getClass();
            } catch (Throwable e) {
                return;
            }

            try {
                Field f = webViewClass.getDeclaredField("mWebViewCore");
                f.setAccessible(true);
                webViewCore = f.get(webViewObject);

                if (webViewCore != null) {
                    sendMessageMethod = webViewCore.getClass()
                            .getDeclaredMethod("sendMessage", Message.class);
                    sendMessageMethod.setAccessible(true);
                }
            } catch (Throwable e) {
                initFailed = true;
                Log.e(LOG_TAG,
                        "PrivateApiBridgeMode failed to find the expected APIs.",
                        e);
            }
        }

        public void onNativeToJsMessageAvailable() {
            if (sendMessageMethod == null && !initFailed) {
                initReflection();
            }
            if (sendMessageMethod != null) {
                String js = popAndEncodeAsJs();
                Message execJsMessage = Message.obtain(null, EXECUTE_JS, js);
                try {
                    sendMessageMethod.invoke(webViewCore, execJsMessage);
                } catch (Throwable e) {
                    Log.e(LOG_TAG, "Reflection message bridge failed.", e);
                }
            }
        }
    }

    private static class JsMessage {
        final String mJsPayloadOrCallbackId;
        final XExtensionResult mPluginResult;

        JsMessage(String js) {
            if (js == null) {
                throw new NullPointerException();
            }
            mJsPayloadOrCallbackId = js;
            mPluginResult = null;
        }

        JsMessage(XExtensionResult pluginResult, String callbackId) {
            if (callbackId == null || pluginResult == null) {
                throw new NullPointerException();
            }
            mJsPayloadOrCallbackId = callbackId;
            this.mPluginResult = pluginResult;
        }

        int calculateEncodedLength() {
            if (mPluginResult == null) {
                return mJsPayloadOrCallbackId.length() + 1;
            }
            int statusLen = String.valueOf(mPluginResult.getStatus()).length();
            int ret = 2 + statusLen + 1 + mJsPayloadOrCallbackId.length() + 1;
            switch (mPluginResult.getMessageType()) {
            case XExtensionResult.MESSAGE_TYPE_BOOLEAN: // f or t
            case XExtensionResult.MESSAGE_TYPE_NULL: // N
                ret += 1;
                break;
            case XExtensionResult.MESSAGE_TYPE_NUMBER: // n
                ret += 1 + mPluginResult.getMessage().length();
                break;
            case XExtensionResult.MESSAGE_TYPE_STRING: // s
                ret += 1 + mPluginResult.getStrMessage().length();
                break;
            case XExtensionResult.MESSAGE_TYPE_BINARYSTRING:
                ret += 1 + mPluginResult.getMessage().length();
                break;
            case XExtensionResult.MESSAGE_TYPE_ARRAYBUFFER:
                ret += 1 + mPluginResult.getMessage().length();
                break;
            case XExtensionResult.MESSAGE_TYPE_JSON:
            default:
                ret += mPluginResult.getMessage().length();
            }
            return ret;
        }

        void encodeAsMessage(StringBuilder sb) {
            if (mPluginResult == null) {
                sb.append('J').append(mJsPayloadOrCallbackId);
                return;
            }
            int status = mPluginResult.getStatus();
            boolean noResult = status == XExtensionResult.Status.NO_RESULT
                    .ordinal();
            boolean resultOk = status == XExtensionResult.Status.OK.ordinal()
                    || status == XExtensionResult.Status.PROGRESS_CHANGING
                            .ordinal();
            boolean keepCallback = mPluginResult.getKeepCallback();

            sb.append((noResult || resultOk) ? 'S' : 'F')
                    .append(keepCallback ? '1' : '0').append(status)
                    .append(' ').append(mJsPayloadOrCallbackId).append(' ');
            switch (mPluginResult.getMessageType()) {
            case XExtensionResult.MESSAGE_TYPE_BOOLEAN:
                sb.append(mPluginResult.getMessage().charAt(0)); // t or f.
                break;
            case XExtensionResult.MESSAGE_TYPE_NULL: // N
                sb.append('N');
                break;
            case XExtensionResult.MESSAGE_TYPE_NUMBER: // n
                sb.append('n').append(mPluginResult.getMessage());
                break;
            case XExtensionResult.MESSAGE_TYPE_STRING: // s
                sb.append('s');
                sb.append(mPluginResult.getStrMessage());
                break;
            case XExtensionResult.MESSAGE_TYPE_BINARYSTRING: // S
                sb.append('S');
                sb.append(mPluginResult.getMessage());
                break;
            case XExtensionResult.MESSAGE_TYPE_ARRAYBUFFER: // A
                sb.append('A');
                sb.append(mPluginResult.getMessage());
                break;
            case XExtensionResult.MESSAGE_TYPE_JSON:
            default:
                sb.append(mPluginResult.getMessage()); // [ or {
            }
        }

        void encodeAsJsMessage(StringBuilder sb) {
            if (mPluginResult == null) {
                sb.append(mJsPayloadOrCallbackId);
            } else {
                int status = mPluginResult.getStatus();
                boolean success = (status == XExtensionResult.Status.PROGRESS_CHANGING
                        .ordinal() || status == XExtensionResult.Status.OK
                        .ordinal())
                        || (status == XExtensionResult.Status.NO_RESULT
                                .ordinal());
                sb.append("xFace.callbackFromNative('")
                        .append(mJsPayloadOrCallbackId).append("',")
                        .append(success).append(",").append(status)
                        .append(",[").append(mPluginResult.getMessage())
                        .append("],").append(mPluginResult.getKeepCallback())
                        .append(");");
            }
        }
    }
}
