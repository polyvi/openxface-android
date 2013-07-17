
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

import com.polyvi.xface.core.XISystemContext;

import android.os.Handler;

/**
 * 保存扩展上下文环境相关数据 每一个扩展都会持有该对象，以获取扩展执行所需要的环境数据
 */
public final class XExtensionContext {
    /** js代码执行器 */
    private Handler mJsEvaluator;

    /** android程序对应的XISystemContext对象 */
    private XISystemContext mSystemContext;

    /** 设置js代码执行器 */
    public void setJsEvaluator(Handler jsEvaluator) {
        this.mJsEvaluator = jsEvaluator;
    }

    /** 获取js代码执行器 */
    public Handler getJsEvaluator() {
        return mJsEvaluator;
    }

    /** 设置android程序Context对象 */
    public void setSystemContext(XISystemContext systemContext) {
        this.mSystemContext = systemContext;
    }

    /** 获取android程序Context对象 */
    public XISystemContext getSystemContext() {
        return mSystemContext;
    }
}
