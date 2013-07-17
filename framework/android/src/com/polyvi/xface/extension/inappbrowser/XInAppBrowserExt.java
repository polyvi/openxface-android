
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

package com.polyvi.xface.extension.inappbrowser;

import java.util.HashMap;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XConstant;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.view.XAppWebView;

/**
 * 本扩展用来实现对WebKit浏览器的封装
 */
public class XInAppBrowserExt extends XExtension implements XIBrowserListener {
    private static final String INAPPBROWSER_EXTENSION_TYPE = "InAppBrowser";
    private static final String NULL = "null";
    private static final String COMMAND_OPEN = "open";
    private static final String COMMAND_CLOSE = "close";
    private static final String COMMAND_INJECT_SCRIPT_CODE = "injectScriptCode";
    private static final String COMMAND_INJECT_SCRIPT_FILE = "injectScriptFile";
    private static final String COMMAND_INJECT_STYLE_CODE  = "injectStyleCode";
    private static final String COMMAND_INJECT_STYLE_FILE  = "injectStyleFile";

    private static final String BROWSER_TAG ="browser";

    protected static final String CLASS_NAME = XInAppBrowserExt.class
            .getSimpleName();
    /** _self表示在 在当前xface页面打开 */
    private static final String SELF = "_self";
    /** _system表示在系统浏览器打开 */
    private static final String SYSTEM = "_system";
    /** 表示是否显示地址栏 */
    private static final String LOCATION = "location";

    private static final String URL_NAME = "url";

    /** 开始加载标识 */
    private static final String LOAD_START_EVENT = "loadstart";
    /** 加载停止标识 */
    private static final String LOAD_STOP_EVENT = "loadstop";
    /** 退出事件标识 */
    private static final String EXIT_EVENT = "exit";
    /** 时间类型 */
    private static final String TYPE_NAME = "type";

    /** 是否显示地址栏 */
    private boolean mIsShowLocationBar = true;
    private XCallbackContext mCallbackCtx;

    public XInAppBrowserExt()
    {
    }

    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        Status status = Status.NO_RESULT;
        String result = "";
        try {
            if (action.equals(COMMAND_OPEN)) {
                String url = args.getString(0);
                String target = args.optString(1);
                // 目标窗口参数为：null、空串 、null字符串时在xface当前页面打开
                if (target == null || target.equals("") || target.equals(NULL)) {
                    target = SELF;
                }
                HashMap<String, Boolean> features = parseFeature(args
                        .optString(2));
                url = updateUrl(url, mWebContext.getApplication().getBaseUrl());
                if(!checkUrl(url))
                {
                    XLog.e(CLASS_NAME,"Malformed url = " + url);
                    return new XExtensionResult(Status.MALFORMED_URL_EXCEPTION);
                }
                if(!isUrlAllowedLoad(url))
                {
                    mExtensionContext.getSystemContext().toast("Network is not available for load:" + url);
                    return new XExtensionResult(Status.ERROR);
                }
                // SELF：当前xface页面打开
                if (SELF.equals(target)) {
                    // 在当前页面加载Url
                        loadUrlInAppView(mWebContext.getApplication().getView(), url);
                }
                // SYSTEM：系统浏览器打开
                else if (SYSTEM.equals(target)) {
                    loadUrlInSysBrowser(mWebContext.getApplication().getView(), url);
                    result = "";
                }
                // BLANK - or anything else
                else {
                    result = loadUrlInAppBrowser(mWebContext, url, features, callbackCtx);
                }
            } else if (action.equals(COMMAND_CLOSE)) {
                getBrowerOfApp(mWebContext).closeDialog();
            } else if (action.equals(COMMAND_INJECT_SCRIPT_CODE)) {
                injectScriptCode(args.getString(0), args.optBoolean(1),callbackCtx);
            } else if (action.equals(COMMAND_INJECT_SCRIPT_FILE)) {
                injectScriptFile(args.getString(0), args.optBoolean(1),callbackCtx);
            } else if (action.equals(COMMAND_INJECT_STYLE_CODE)) {
                injectStyleCode(args.getString(0), args.optBoolean(1),callbackCtx);
            } else if (action.equals(COMMAND_INJECT_STYLE_FILE)) {
                injectStyleFile(args.getString(0), args.optBoolean(1),callbackCtx);
            } else {
                status = Status.INVALID_ACTION;
            }
            return new XExtensionResult(status, result);
        } catch (JSONException e) {
            return new XExtensionResult(Status.JSON_EXCEPTION);
        }
    }

    private boolean checkUrl(String url)
    {
        //当为file头时检查文件是否存在
        if(url.startsWith(XConstant.FILE_SCHEME))
        {
            return XFileUtils.fileExists(getContext(), url);
        }

        //不为file头时不做检查
        return true;
    }

    /**
     * 在需要加载htpp页面时，判断网络连接是否可用
     * @param url 要加载的页面的url
     * @return 网络可用返回true，否则false
     */
    private boolean isUrlAllowedLoad(String url)
    {
        //当为http头时检测网络连接是否可用
        if(url.startsWith(XConstant.HTTP_SCHEME)|| url.startsWith(XConstant.HTTPS_SCHEME))
        {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) getContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager
                    .getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }else{
                return false;
            }
        }
        return true;
    }


    private XInAppBrowser getBrowerOfApp(XIWebContext webContext)
    {
        return (XInAppBrowser)webContext.getApplication().getData(BROWSER_TAG);
    }

    /**
     * 在AppView中加载Url
     *
     * @param appView
     * @param url
     */
    private void loadUrlInAppView(XAppWebView appView, String url) {
        appView.loadUrl(url, false, false, getContext());
    }

    /**
     * 在系统浏览器中中加载Url
     *
     * @param appView
     * @param url
     */
    private void loadUrlInSysBrowser(XAppWebView appView, String url) {
        appView.loadUrl(url, true, false, getContext());
    }

    public String getName() {
        return INAPPBROWSER_EXTENSION_TYPE;
    }

    /**
     * 解析特性列表
     *
     * @param optString
     *            可选特性字符串
     * @return
     */
    private HashMap<String, Boolean> parseFeature(String optString) {
        if (optString.equals(NULL)) {
            return null;
        } else {
            HashMap<String, Boolean> map = new HashMap<String, Boolean>();
            StringTokenizer features = new StringTokenizer(optString, ",");
            StringTokenizer option;
            while (features.hasMoreElements()) {
                option = new StringTokenizer(features.nextToken(), "=");
                if (option.hasMoreElements()) {
                    String key = option.nextToken();
                    Boolean value = option.nextToken().equals("no") ? Boolean.FALSE
                            : Boolean.TRUE;
                    map.put(key, value);
                }
            }
            return map;
        }
    }

    /**
     * 将相对路径Url转化为绝对路径
     *
     * @param url
     * @return
     */
    private String updateUrl(String url, String baseUrl) {
        Uri newUrl = Uri.parse(url);
        if (newUrl.isRelative()) {
            url = baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1) + url;
        }
        return url;
    }

    /**
     * 在新的浏览器中显示指定的URL
     *
     * @param webContext
     *            触发加载页面操作的View
     * @param url
     *            要加载的URL
     * @param features
     *            特性
     * @param isCustomed
     *            是不是定制InAppBrowser
     * @return
     */
    private String loadUrlInAppBrowser(final XIWebContext webContext,
            final String url, HashMap<String, Boolean> features, XCallbackContext callbackCtx) {
        mCallbackCtx = callbackCtx;
        // 默认显示地址栏
        mIsShowLocationBar = true;
        if (features != null) {
            mIsShowLocationBar = features.get(LOCATION).booleanValue();
        }
        // 在新的线程中创建InAppBrowserView
        Runnable runnable = new Runnable() {
            public void run() {
                XIBrowserBuilder browserBuilder = createBrowserBuilder(url, webContext);
                XInAppBrowser browser = browserBuilder.buildBrowser();
                webContext.getApplication().setData("browser", browser);
                browser.show();
            }
        };
        mExtensionContext.getSystemContext().runOnUiThread(runnable);
        return "";
    }

    private XIBrowserBuilder createBrowserBuilder(String url, XIWebContext webContext) {
        return new XInAppBrowser(getContext(), this, url, webContext,
                mIsShowLocationBar);
    }

    @Override
    public void onLoadFinished(String url, XIWebContext webContext) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(TYPE_NAME, LOAD_STOP_EVENT);
            obj.put(URL_NAME, url);
            sendUpdate(obj, true, webContext);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoadStart(String url, XIWebContext webContext) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(TYPE_NAME, LOAD_START_EVENT);
            obj.put(URL_NAME, url);
            sendUpdate(obj, true, webContext);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDismiss(XIWebContext webContext) {
        try {
            JSONObject obj = new JSONObject();
            obj.put(TYPE_NAME, EXIT_EVENT);
            sendUpdate(obj, false, webContext);
            mCallbackCtx = null;
            webContext.getApplication().removeData(BROWSER_TAG);
        } catch (JSONException e) {
            Log.d(CLASS_NAME, e.getMessage());
        }
    }

    @Override
    public void onPageStarted() {
        mCallbackCtx = null;
    }

    /**
     * 执行js回调
     *
     * @param obj
     *            json对象
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback, XIWebContext webContext) {
        XExtensionResult result = new XExtensionResult(Status.OK, obj);
        result.setKeepCallback(keepCallback);
        if (null != mCallbackCtx) {
            mCallbackCtx.sendExtensionResult(result);
        }
    }

    public void sendAsyncResult(String result) {
    }

    /**
     * 判断指定的行为是否是异步
     *
     * @param action
     *            需要执行的行为
     * @return 异步 true， 同步 false
     */
    public boolean isAsync(String action) {
        return true;
    }

    /**
     * 注入JS代码
     * @param source:要注入的源字符串
     * @param isCallbackExist：成功回调函数是否存在
     * @param callbackCtx：回调上下文环境
     */
    private void injectScriptCode(String source, boolean isCallbackExist, XCallbackContext callbackCtx) {
        String jsWrapper = null;
        if (isCallbackExist) {
            jsWrapper = String.format("prompt(JSON.stringify([eval(%%s)]), 'xface-iab://%s')", callbackCtx.getCallbackId());
        }
        injectSource(source, jsWrapper, callbackCtx);
    }

    /**
     * 注入JS文件
     * @param source:要注入的源字符串
     * @param isCallbackExist：成功回调函数是否存在
     * @param callbackCtx：回调上下文环境
     */
    private void injectScriptFile(String source, boolean isCallbackExist, XCallbackContext callbackCtx) {
        String jsWrapper;
        if (isCallbackExist) {
            jsWrapper = String.format("(function(d) { var c = d.createElement('script'); c.src = %%s; c.onload = function() { prompt('', 'xface-iab://%s'); }; d.body.appendChild(c); })(document)", callbackCtx.getCallbackId());
        } else {
            jsWrapper = "(function(d) { var c = d.createElement('script'); c.src = %s; d.body.appendChild(c); })(document)";
        }
        injectSource(source, jsWrapper, callbackCtx);
    }

    /**
     * 注入CSS代码
     * @param source:要注入的源字符串
     * @param isCallbackExist：成功回调函数是否存在
     * @param callbackCtx：回调上下文环境
     */
    private void injectStyleCode(String source, boolean isCallbackExist, XCallbackContext callbackCtx) {
        String jsWrapper;
        if (isCallbackExist) {
            jsWrapper = String.format("(function(d) { var c = d.createElement('style'); c.innerHTML = %%s; d.body.appendChild(c); prompt('', 'xface-iab://%s');})(document)", callbackCtx.getCallbackId());
        } else {
            jsWrapper = "(function(d) { var c = d.createElement('style'); c.innerHTML = %s; d.body.appendChild(c); })(document)";
        }
        injectSource(source, jsWrapper, callbackCtx);
    }

    /**
     * 注入CSS文件
     * @param source:要注入的源字符串
     * @param isCallbackExist：成功回调函数是否存在
     * @param callbackCtx：回调上下文环境
     */
    private void injectStyleFile(String source, boolean isCallbackExist, XCallbackContext callbackCtx) {
        String jsWrapper;
        if (isCallbackExist) {
            jsWrapper = String.format("(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %%s; d.head.appendChild(c); prompt('', 'xface-iab://%s');})(document)",callbackCtx.getCallbackId());
        } else {
            jsWrapper = "(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %s; d.head.appendChild(c); })(document)";
        }
        injectSource(source, jsWrapper, callbackCtx);
    }

    /**
     * 注入script或者css内容
     * @param source: 要注入的源字符串
     * @param jsWrapper: 用来封装源串的字符串
     * @param callbackCtx 回调上下文环境
     */
    private void injectSource(String source, String jsWrapper, XCallbackContext callbackCtx) {
        XInAppBrowser InAppBrowser = getBrowerOfApp(mWebContext);
        InAppBrowser.setInjectCallbackContext(callbackCtx);
        InAppBrowser.injectDeferredObject(source, jsWrapper);
    }
}
