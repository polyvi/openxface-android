
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
import com.polyvi.xface.util.XLog;

/**
 * 负责向控制台打印log调试信息
 */
public class XConsoleExt extends XExtension {

    private static final String COMMAND_LOG = "log";

    private static final String JS_LOG_TAG = "xface-js";

    private static final String LOG_LEVEL = "logLevel";
    private static final String LOG_LEVEL_ERROR = "ERROR";
    private static final String LOG_LEVEL_WARN = "WARN";
    private static final String LOG_LEVEL_INFO = "INFO";

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
        if (COMMAND_LOG.equals(action)) {
            log(args.getString(0), args.getJSONObject(1).getString(LOG_LEVEL));
        }
        return new XExtensionResult(Status.NO_RESULT);
    }

    /**
     * 向控制台输出log信息
     *
     * @param message
     *            log信息
     * @param logLevel
     *            log级别，可能的值（"ERROR", "WARN", "INFO"）
     */
    private void log(String message, String logLevel) {
        if (LOG_LEVEL_ERROR.equals(logLevel)) {
            XLog.e(JS_LOG_TAG, message);
        } else if (LOG_LEVEL_WARN.equals(logLevel)) {
            XLog.w(JS_LOG_TAG, message);
        } else if (LOG_LEVEL_INFO.equals(logLevel)) {
            XLog.i(JS_LOG_TAG, message);
        }
    }
}
