
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

public class FileTransferProgress {

    private boolean lengthAvailable = false;  // 标示是否知道文件总大小
    private long loaded = 0;                  // 已经传输的字节
    private long total = 0;                   // 文件总字节数

    public boolean isLengthAvailable() {
        return lengthAvailable;
    }

    public void setLengthComputable(boolean available ) {
        this.lengthAvailable = available;
    }

    public long getLoaded() {
        return loaded;
    }

    public void setLoaded(long bytes) {
        this.loaded = bytes;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long bytes) {
        this.total = bytes;
    }

    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject(
                "{loaded:" + loaded +
                ",total:" + total +
                ",lengthAvailable:" + (lengthAvailable ? "true" : "false") + "}");
    }
}
