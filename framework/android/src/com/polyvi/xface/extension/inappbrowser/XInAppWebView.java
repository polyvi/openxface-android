
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

import com.polyvi.xface.plugin.api.XIWebContext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

/**
 * 应用内部WebView，属性已被指定
 * @author Administrator
 *
 */
@SuppressLint("ViewConstructor")
public class XInAppWebView extends WebView {

    private XIBrowserListener mListener;
    public XInAppWebView(Context context, XIBrowserListener listener,
            String url, XIWebContext webContext) {
        super(context);
        mListener = listener;
        init(url,webContext);
    }

    /**
     * 初始化InAppWebView，对布局、行为、支持的特性进行初始化
     * @param url
     */
    private void init(String url,final XIWebContext webContext) {
        setLayoutParams(new LinearLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT));
        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient()
        {

            @Override
            public void onPageFinished(WebView view, String url) {
                mListener.onLoadFinished(url, webContext);
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mListener.onLoadStart(url, webContext);
                super.onPageStarted(view, url, favicon);
            }

        });
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(true);
        settings.setPluginsEnabled(true);
        settings.setDomStorageEnabled(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setUseWideViewPort(true);
        requestFocus();
        requestFocusFromTouch();

        loadUrl(url);
    }
}
