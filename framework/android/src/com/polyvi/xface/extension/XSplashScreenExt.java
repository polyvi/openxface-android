
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

import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XStringUtils;

public class XSplashScreenExt extends XExtension{
    private static final String ACTION_SHOW = "show";
    private static final String ACTION_HIDE = "hide";

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return false;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        if (action.equals(ACTION_SHOW)){
            String imagePath = args.optString(0, "");
            if(!XStringUtils.isEmptyString(imagePath)) {
                XPathResolver pathResolver = new XPathResolver(imagePath, mWebContext.getWorkSpace());
                imagePath = pathResolver.resolve();
            }
            show(imagePath);
        } else if(action.equals(ACTION_HIDE)){
            hide();
        } else{
            return new XExtensionResult(Status.INVALID_ACTION);
        }
        return new XExtensionResult(Status.OK);
    }

    /** 显示SplashScreen */
    private void show(final String imagePath){
        Runnable showRunnable = new Runnable() {
            public void run() {
                if(!XStringUtils.isEmptyString(imagePath)) {
                    mExtensionContext.getSystemContext().startAppSplash(imagePath);
                } else {
                    mExtensionContext.getSystemContext().startBootSplash();
                }
            };
        };
        mExtensionContext.getSystemContext().runOnUiThread(showRunnable);
    }

    /** 隐藏SplashScreen */
    private void hide(){
        Runnable hideRunnable = new Runnable() {
            public void run() {
                mExtensionContext.getSystemContext().stopBootSplash();
                mExtensionContext.getSystemContext().stopAppSplash();
            };
        };
        mExtensionContext.getSystemContext().runOnUiThread(hideRunnable);
    }
}
