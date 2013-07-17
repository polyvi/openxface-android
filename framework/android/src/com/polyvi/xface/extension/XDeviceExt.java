
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XDeviceInfo;
import com.polyvi.xface.util.XLog;

public class XDeviceExt extends XExtension {
    private JSONObject mDeviceInfo = null;

    @Override
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        super.init(extensionContext, webContext);
        mDeviceInfo = new JSONObject();
        HashMap<String, Object> deviceInfo = new XDeviceInfo().getDeviceInfo(getContext());
        Iterator<Entry<String, Object>> it = deviceInfo.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();
            try {
                mDeviceInfo.put((String) entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                XLog.e(XDeviceExt.class.getSimpleName(), "JSONException:", e);
            }
        }
    }

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
        XExtensionResult.Status status = XExtensionResult.Status.OK;
        String result = "";
        if (action.equals("getDeviceInfo")) {
            if(null == mDeviceInfo){
                return new XExtensionResult(XExtensionResult.Status.JSON_EXCEPTION);
            }
            return new XExtensionResult(status, mDeviceInfo);
        }
        return new XExtensionResult(status, result);
    }

}
