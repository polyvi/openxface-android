
/*
 Copyright 2012-2013, Polyvi Inc. (http://polyvi.github.io/openxface)
 This program is distributed under the terms of the GNU General Public License.

 This file is part of xFace.

 xFace is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 xFace is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with xFace.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.polyvi.xface.core;

import android.content.Context;
import android.util.Pair;

import com.polyvi.xface.XStartParams;
import com.polyvi.xface.app.XAppInfo;
import com.polyvi.xface.app.XApplication;
import com.polyvi.xface.app.XApplicationCreator;
import com.polyvi.xface.app.XIApplication;
import com.polyvi.xface.event.XEvent;
import com.polyvi.xface.event.XEventType;
import com.polyvi.xface.event.XIKeyEventListener;
import com.polyvi.xface.event.XISystemEventReceiver;
import com.polyvi.xface.event.XSystemEventCenter;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.extension.XExtensionManager;
import com.polyvi.xface.ssl.XSSLManager;
import com.polyvi.xface.view.XAppWebView;

/**
 * app的运行时环境，默认情况下只有一个实例，负责主要业务组件的生命周期管理及调度 包括Application Controller, Command
 * Dispatcher, WebServer及AMS
 *
 * @see {@link XApplicationCreator}
 * @see {@link XExtensionManager}
 * @see {@link XISystemContext}
 */
public class XRuntime implements XISystemEventReceiver {

    /** App生成器 */
    private XApplicationCreator mCreator;

    /** 扩展执行上下文 */
    private XExtensionContext mExtensionContext;

    private XISystemContext mSystemContext;

    /**
     * 被启动的app
     */
    private XApplication mStartApp;

    public XRuntime() {
        super();
    }

    /**
     * runtime的初始化
     */
    public void init(XISystemContext systemContext) {
        mSystemContext = systemContext;
        initExtensionContext();
        registerSystemEventReceiver();
        getAppFactory();
        XSSLManager.createInstance(systemContext.getContext());
    }

    /**
     * 初始化startapp
     *
     * @param appInfo
     *            startapp的信息
     * @return
     */
    public boolean initStartApp(XAppInfo appInfo) {
        if (null == appInfo) {
            mSystemContext
                    .toast("config.xml: app_package id not match to app id in app.xml.");
            return false;
        }
        XIApplication app = mCreator.create(appInfo);
        mStartApp = XApplicationCreator.toWebApp(app);
        return true;
    }

    public void runStartApp(XStartParams startParams) {
        if (null != mStartApp) {
            mStartApp.start(startParams);
        }
    }

    /**
     * 注册事件接收器
     */
    private void registerSystemEventReceiver() {
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.CLOSE_APP);
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.CLOSE_ENGINE);
        XSystemEventCenter.getInstance().registerReceiver(this,
                XEventType.XAPP_MESSAGE);
    }

    /**
     * 创建app工厂
     */
    public XApplicationCreator getAppFactory() {
        if (null != mCreator) {
            return mCreator;
        }
        mCreator = new XApplicationCreator(mSystemContext, mExtensionContext);
        return mCreator;
    }

    /** 创建扩展上下文环境对象 */
    private XExtensionContext createExtensionContext(
            XISystemContext systemContext) {
        XExtensionContext extensionContext = new XExtensionContext();
        extensionContext.setSystemContext(systemContext);
        return extensionContext;
    }

    /**
     * 初始化扩展管理模块
     *
     * @param ctx
     *            Activity上下文
     */
    private void initExtensionContext() {
        if (null == mExtensionContext) {
            mExtensionContext = createExtensionContext(mSystemContext);
        }
    }

    /**
     * 获得扩展执行上下文
     *
     * @return
     */
    public XExtensionContext getExtensionContext() {
        return mExtensionContext;
    }

    /**
     * 设置启动app
     *
     * @param app
     */
    public void setStartApp(XApplication app) {
        mStartApp = app;
    }

    /**
     * 获取Key事件监听器
     *
     * @return
     */
    public XIKeyEventListener getKeyListener() {
        return mStartApp.getEventHandler();
    }

    /**
     * 获取startapp
     *
     * @return
     */
    public XApplication getStartApp() {
        return mStartApp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceived(Context context, XEvent evt) {
        if (evt.getType() == XEventType.CLOSE_APP) {
            int viewId = (Integer) evt.getData();
            mStartApp.getEventHandler().onCloseApplication(viewId);
        } else if (evt.getType() == XEventType.CLOSE_ENGINE) {
            mSystemContext.getActivity().finish();
        } else if (evt.getType() == XEventType.XAPP_MESSAGE) {
            Pair<XAppWebView, String> data = (Pair<XAppWebView, String>) evt
                    .getData();
            mStartApp.getEventHandler().onXAppMessageReceived(data.first,
                    data.second);
        }
    }
}
