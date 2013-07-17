
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

/**
 * 执行扩展命令，无返回值(异步函数,本地代码将会调用xFace.callbackSuccess or xFace.callbackError or xFace.callbackStatusChange)
 * @param {Function} success   成功的回调函数
 * @param {Function} fail      失败的回调函数
 * @param {String} service     扩展的名字
 * @param {String} action      调用扩展的行为
 * @param {String[]} [args]    调用扩展的参数
 */
var xFace = require('xFace');

module.exports = function(success, fail, statusChanged,  service, action, args) {
  try {
    var callbackId = service + xFace.callbackId++;
    if (success || fail || statusChanged) {
        xFace.callbacks[callbackId] = {success:success, fail:fail, statusChanged:statusChanged};
    }

    alert("_xFace_jsscript:"+JSON.stringify([service, action, callbackId, true]) + "_xFace_native_bridge_separator_" + JSON.stringify(args));

  } catch (e2) {
    console.log("Error: "+e2);
  }

};