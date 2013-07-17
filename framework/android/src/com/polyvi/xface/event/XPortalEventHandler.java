
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

import android.view.KeyEvent;

import com.polyvi.xface.ams.XAppManagement;
import com.polyvi.xface.app.XApplication;
import com.polyvi.xface.app.XApplicationCreator;
import com.polyvi.xface.extension.XExtensionManager;
import com.polyvi.xface.view.XAppWebView;

/**
 * portal应用的事件处理器
 */
public class XPortalEventHandler extends XAppEventHandler {

    private XAppManagement mAppManagement;

    public XPortalEventHandler(XApplication app,  XExtensionManager em,
           XAppManagement ams) {
        super(app, em);
        mAppManagement = ams;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent evt) {
        XApplication currApp = XApplicationCreator.toWebApp(mAppManagement
                .getCurrActiveApp());
        if (null != currApp) {
            return currApp.getEventHandler().onKeyUp(keyCode, evt);
        }
        return super.onKeyUp(keyCode, evt);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        XApplication currApp = XApplicationCreator.toWebApp(mAppManagement
                .getCurrActiveApp());
        if (null != currApp) {
            return currApp.getEventHandler().onKeyDown(keyCode, evt);
        }
        return super.onKeyDown(keyCode, evt);
    }

    @Override
    public void onCloseApplication(int viewId) {
        if(mOwnerApp.getViewId() != viewId) {
            mAppManagement.closeApp(viewId);
            return;
        }
        super.onCloseApplication(viewId);
    }

    @Override
    public void onXAppMessageReceived(XAppWebView view, String msgData) {
        mAppManagement.handleAppMessage(mOwnerApp, view, msgData);
    }

    @Override
    public void handleDestroy() {
        mAppManagement.closeAllApp();
    }

}
