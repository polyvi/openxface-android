
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
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XNotification;

public class XNotificationExt extends XExtension {
    private static final String COMMAND_ALERT = "alert";
    private static final String COMMAND_CONFIRM = "confirm";
    private static final String COMMAND_BEEP = "beep";
    private static final String COMMAND_VIBRATE = "vibrate";
    private static final String COMMAND_ACTIVITY_START = "activityStart";
    private static final String COMMAND_ACTIVITY_STOP  = "activityStop";
    private static final String COMMAND_PROGRESS_START = "progressStart";
    private static final String COMMAND_PROGRESS_VALUE = "progressValue";
    private static final String COMMAND_PROGRESS_STOP  = "progressStop";

    /**定义confirm的按键类型*/
    private static final int BUTTON_POSITIVE = 1;
    private static final int BUTTON_NEUTRAL = 2;
    private static final int BUTTON_NEGATIVE = 3;
    private static final int BUTTON_UNKOWN = -1;


    private  XNotification mXNotification = null;
    private XCallbackContext mCallbackCtx;

    @Override
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        super.init(extensionContext, webContext);
        mXNotification = new XNotification(mExtensionContext.getSystemContext());
    }

    @Override
    public void sendAsyncResult(String result) {

    }

    @Override
    public boolean isAsync(String action) {
        if (action.equals(COMMAND_BEEP) || action.equals(COMMAND_VIBRATE)) {
            return true;
        }

        return false;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        XExtensionResult.Status status = XExtensionResult.Status.OK;
        String result = "";
        mCallbackCtx = callbackCtx;
        try {
            if (action.equals(COMMAND_ALERT)) {
                alert(args.getString(0), args.getString(1), args.getString(2));
                XExtensionResult r = new XExtensionResult(XExtensionResult.Status.NO_RESULT);
                r.setKeepCallback(true);
                return r;
            }
            else if (action.equals(COMMAND_CONFIRM)) {
                confirm(args.getString(0), args.getString(1), args.getString(2));
                XExtensionResult r = new XExtensionResult(XExtensionResult.Status.NO_RESULT);
                r.setKeepCallback(true);
                return r;
            } else if (action.equals(COMMAND_BEEP)) {
                mXNotification.beep(args.getLong(0));
            } else if (action.equals(COMMAND_VIBRATE)) {
                mXNotification.vibrate(args.getLong(0));
            } else if (action.equals(COMMAND_ACTIVITY_START)) {
                mXNotification.activityStart(args.getString(0), args.getString(1));
            } else if (action.equals(COMMAND_ACTIVITY_STOP)) {
                mXNotification.activityStop();
            } else if (action.equals(COMMAND_PROGRESS_START)) {
                mXNotification.progressStart(args.getString(0), args.getString(1));
            } else if (action.equals(COMMAND_PROGRESS_VALUE)) {
                mXNotification.progressValue(args.getInt(0));
            } else if (action.equals(COMMAND_PROGRESS_STOP)) {
                mXNotification.progressStop();
            } else {
                status = XExtensionResult.Status.INVALID_ACTION;
                result = "Unsupported Operation: " + action;
            }
            return new XExtensionResult(status, result);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new XExtensionResult(XExtensionResult.Status.ERROR);
        }
    }

    /**
     * 显示alert提示框.
     *
     * @param message       提示信息
     * @param title         提示框标题
     * @param buttonLabel   按钮显示信息
     * @param jsCallback    XJsCallback对象
     */
    private synchronized void alert(final String message,
            final String title, final String buttonLabel)  {
         AlertDialog.OnClickListener actionListener = new AlertDialog.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 dialog.dismiss();
                 mCallbackCtx.success();
             }
         };
         mXNotification.alert(message,title,buttonLabel,actionListener);
    }

    /**
     * 显示confirm提示框
     *
     * @param message       提示信息
     * @param title         提示框标题
     * @param buttonLabels  按钮显示信息(以','分割, 1-3个按钮)
     * @param jsCallback    XJsCallback对象
     */
    private synchronized void confirm(final String message, final String title, final String buttonLabels)  {
        AlertDialog.OnClickListener actionListener = new AlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                XExtensionResult r = new XExtensionResult(XExtensionResult.Status.NO_RESULT, BUTTON_UNKOWN);
                if(AlertDialog.BUTTON_POSITIVE == which){
                    r = new XExtensionResult(XExtensionResult.Status.OK, BUTTON_POSITIVE);
                } else if(AlertDialog.BUTTON_NEUTRAL == which){
                    r = new XExtensionResult(XExtensionResult.Status.OK, BUTTON_NEUTRAL);
                } else if(AlertDialog.BUTTON_NEGATIVE == which){
                    r = new XExtensionResult(XExtensionResult.Status.OK, BUTTON_NEGATIVE);
                }
                mCallbackCtx.sendExtensionResult(r);
            }
        };
        mXNotification.confirm(message, title, buttonLabels, actionListener, actionListener, actionListener);
    }
}
