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

package com.polyvi.xface.extension;

import java.io.File;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.polyvi.xface.ams.XAppInstallListener;
import com.polyvi.xface.ams.XAppList;
import com.polyvi.xface.ams.XAppStartListenerImp;
import com.polyvi.xface.app.XIApplication;
import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XConstant;

public class XAmsExt extends XExtension {

    /** AMS 提供给js用户的接口名字 */
    private static final String COMMAND_LIST_INSTALLED_APPLICATIONS = "listInstalledApplications";
    private static final String COMMAND_START_APPLICATION = "startApplication";
    private static final String COMMAND_UNINSTALL_APPLICATION = "uninstallApplication";
    private static final String COMMAND_INSTALL_APPLICATION = "installApplication";
    private static final String COMMAND_UPDATE_APPLICATION = "updateApplication";
    private static final String COMMAND_LIST_PRESET_APPLICATIONS = "listPresetAppPackages";
    private static final String COMMAND_GET_START_APP_INFO = "getStartAppInfo";

    /** 定义一些tag常量 */
    private static final String TAG_APP_ID = "appid";
    private static final String TAG_NAME = "name";
    private static final String TAG_BACKGROUND_COLOR = "icon_background_color";
    private static final String TAG_ICON = "icon";
    private static final String TAG_VERSION = "version";
    private static final String TAG_TYPE = "type";
    private static final String TAG_HEIGHT = "height";
    private static final String TAG_WIDTH = "width";

    /** 应用管理器 */
    private XAms mAms;

    /**
     * Ams扩展的初始化<br>
     * 这里选择了重载init方法，而不是覆写init方法的原因，主要是在Ams扩展比较特殊，它不是通过配置文件加载的，
     * 而是直接由引擎在XRuntime中默认加载进来的，为了不在XExtensionContext中暴露Ams相关对象，所以
     * 选择覆写init方法，并添加了XAms对象作为参数
     *
     * @param extensionContext
     *            扩展上下文环境
     * @param ams
     *            负责应用的安装/卸载/更新
     */
    public void init(XExtensionContext extensionContext, XAms ams,
            XIWebContext webContext) {
        super.init(extensionContext, webContext);
        mAms = ams;
    }

    /**
     * 安装app
     *
     * @param webContext
     *            调用该接口的应用对象
     * @param packagePath
     *            安装包路径
     * @param callbackCtx
     *            callback上下文环境
     */
    private void installApplication(XIWebContext webContext,
            String packagePath, XCallbackContext callbackCtx) {
        XAppInstallListener listener = new XAppInstallListener(callbackCtx);
        mAms.installApp(webContext, packagePath, listener);
    }

    /**
     * 更新app
     *
     * @param webContext
     *            调用该接口的应用对象
     * @param packagePath
     *            更新包路径
     * @param callbackCtx
     *            js回调的上下文环境
     */
    private void updateApplication(XIWebContext webContext, String packagePath,
            XCallbackContext callbackCtx) {
        XAppInstallListener listener = new XAppInstallListener(callbackCtx);
        mAms.updateApp(webContext, packagePath, listener);
    }

    /**
     * 卸载application
     *
     * @param webContext
     *            调用该接口的应用对象
     * @param appId
     *            需要卸载的应用id
     * @param callbackCtx
     *            js回调上下文环境
     */
    private void uninstallApplication(XIWebContext webContext, String appId,
            XCallbackContext callbackCtx) {
        XAppInstallListener listener = new XAppInstallListener(callbackCtx);
        mAms.uninstallApp(appId, listener);
    }

    /**
     * 启动一个应用程序
     *
     * @param appId
     *            启动应用的id
     */
    private void startApplication(XIWebContext webContext, String appId,
            String params, XCallbackContext callbackCtx) {
        final String fAppId = appId;
        final XCallbackContext cbContext = callbackCtx;
        final String fParams = params;
        mExtensionContext.getSystemContext().runOnUiThread(new Runnable() {
            public void run() {
                // 由于会操作到UI，确保在UI线程中执行
                XAppStartListenerImp startResult = new XAppStartListenerImp(
                        cbContext);
                mAms.startApp(fAppId, fParams, startResult);
            }
        });
    }

    /**
     * 列出系统已经安装过的应用程序
     *
     * @return 通过json数组的形式返回
     */
    private JSONArray listInstalledApplication() {
        JSONArray result = new JSONArray();
        XAppList appList = mAms.getAppList();
        Iterator<XIApplication> appIterator = appList.iterator();
        while (appIterator.hasNext()) {
            JSONObject obj = translateAppInfoToJson(appIterator.next());
            result.put(obj);
        }
        return result;
    }

    private JSONObject translateAppInfoToJson(XIApplication app) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(TAG_APP_ID, app.getAppInfo().getAppId());
            obj.put(TAG_NAME, app.getAppInfo().getName());
            obj.put(TAG_BACKGROUND_COLOR, app.getAppInfo()
                    .getIconBackgroudColor());
            obj.put(TAG_ICON, app.getAppIconUrl());
            obj.put(TAG_VERSION, app.getAppInfo().getVersion());
            obj.put(TAG_TYPE, app.getAppInfo().getType());
            obj.put(TAG_WIDTH, app.getAppInfo().getWidth());
            obj.put(TAG_HEIGHT, app.getAppInfo().getHeight());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * 列出预置安装包，每一项是预制安装包的路径，该路径是在默认app的workspace下面的pre_set目录中
     * */
    private JSONArray listPresetAppPackages() {
        String[] presetApps = mAms.getPresetAppPackages(mWebContext
                .getApplication().getWorkSpace());
        JSONArray presetAppsJsonArray = new JSONArray();
        if (null != presetApps) {
            for (String presetAppName : presetApps) {
                presetAppsJsonArray.put(XConstant.PRE_SET_APP_PACKAGE_DIR_NAME
                        + File.separator + presetAppName);
            }
        }
        return presetAppsJsonArray;
    }

    @Override
    public void sendAsyncResult(String result) {

    }

    @Override
    public boolean isAsync(String action) {
        if (action.equals(COMMAND_INSTALL_APPLICATION)
                || action.equals(COMMAND_UNINSTALL_APPLICATION)
                || action.equals(COMMAND_UPDATE_APPLICATION)
                || action.equals(COMMAND_LIST_PRESET_APPLICATIONS)) {
            return true;
        }
        return false;
    }

    @Override
    public XExtensionResult exec(String action, JSONArray args,
            XCallbackContext callbackCtx) throws JSONException {
        XExtensionResult er = null;
        if (action.equals(COMMAND_INSTALL_APPLICATION)) {
            String packagePath = args.getString(0);
            installApplication(mWebContext, packagePath, callbackCtx);
            er = new XExtensionResult(Status.NO_RESULT);
        } else if (action.equals(COMMAND_UNINSTALL_APPLICATION)) {
            String appId = args.getString(0);
            uninstallApplication(mWebContext, appId, callbackCtx);
            er = new XExtensionResult(Status.NO_RESULT);
        } else if (action.equals(COMMAND_START_APPLICATION)) {
            String appId = args.getString(0);
            String params = getStartParams(args);
            startApplication(mWebContext, appId, params, callbackCtx);
            er = new XExtensionResult(Status.NO_RESULT);
            er.setKeepCallback(true);
        } else if (action.equals(COMMAND_LIST_INSTALLED_APPLICATIONS)) {
            JSONArray apps = listInstalledApplication();
            er = new XExtensionResult(Status.OK, apps);
        } else if (action.equals(COMMAND_UPDATE_APPLICATION)) {
            String packagePath = args.getString(0);
            updateApplication(mWebContext, packagePath, callbackCtx);
            er = new XExtensionResult(Status.NO_RESULT);
        } else if (COMMAND_LIST_PRESET_APPLICATIONS.equals(action)) {
            JSONArray presetApps = listPresetAppPackages();
            er = new XExtensionResult(Status.OK, presetApps);
        } else if (action.equals(COMMAND_GET_START_APP_INFO)) {
            JSONObject json = translateAppInfoToJson(mWebContext
                    .getApplication());
            er = new XExtensionResult(Status.OK, json);
        }
        return er;
    }

    private String getStartParams(JSONArray args) throws JSONException {
        String params = "";
        int argLen = args.length();
        if (argLen == 2) {
            params = args.getString(1);
        }
        return params;
    }

}
