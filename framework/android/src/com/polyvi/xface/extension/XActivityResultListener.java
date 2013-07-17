
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

import android.content.Intent;

public interface XActivityResultListener {

    /**
     * 由startActivityForResult启动的activity退出时调用
     * @param requestCode       startActivityForResult 提供的值
     * @param resultCode        由启动的activity中 setResult确定.
     * @param intent            从新的Activity传过来的intent
     */
    void onActivityResult(int requestCode, int resultCode, Intent intent);
}
