
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

public class XEchoExt extends XExtension{
    private static final String ECHO = "echo";
    private static final String ECHOASYNC = "echoAsync";

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return ECHOASYNC.equals(action);
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        try {
            String message = args.getString(0);
            if (ECHO.equals(action) || ECHOASYNC.equals(action)) {
                return new XExtensionResult(XExtensionResult.Status.OK, message);
            }
            return new XExtensionResult(XExtensionResult.Status.INVALID_ACTION);
        } catch (JSONException e) {
            return new XExtensionResult(XExtensionResult.Status.JSON_EXCEPTION);
        }
    }
}
