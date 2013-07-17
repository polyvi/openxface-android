
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

/**
 * 用于存储扩展相关信息的数据结构
 */
public class XExtensionEntry {
    /** 扩展名称*/
    private String mExtName = null;
    /** 扩展的类名*/
    private String mExtClassName = null;

    public XExtensionEntry(String extName, String extClassName) {
        this.mExtName = extName;
        this.mExtClassName = extClassName;
    }

    /**
     * 获取扩展的类名
     * @return
     */
    public String getExtClassName() {
        return this.mExtClassName;
    }
}
