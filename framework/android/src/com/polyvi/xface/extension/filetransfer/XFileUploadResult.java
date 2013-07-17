
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

package com.polyvi.xface.extension.filetransfer;

import org.json.JSONException;
import org.json.JSONObject;

public class XFileUploadResult {

    /** 上传文件时向服务器所发送的字节数 */
    private long mBytesSent = 0;

    /** 服务器端返回的HTTP响应代码 */
    private int mResponseCode = -1;

    /** 服务器端返回的HTTP响应*/
    private String mResponse = null;

    public long getBytesSent() {
        return mBytesSent;
    }

    public void setBytesSent(long bytes) {
        this.mBytesSent = bytes;
    }

    public int getResponseCode() {
        return mResponseCode;
    }

    public void setResponseCode(int responseCode) {
        this.mResponseCode = responseCode;
    }

    public String getResponse() {
        return mResponse;
    }

    public void setResponse(String response) {
        this.mResponse = response;
    }

    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject(
                "{bytesSent:" + mBytesSent +
                ",responseCode:" + mResponseCode +
                ",response:" + JSONObject.quote(mResponse) + "}");
    }
}
