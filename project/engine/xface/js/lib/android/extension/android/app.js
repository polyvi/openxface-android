
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
 * @module app
 */
var argscheck = require('xFace/argscheck'),
    exec = require('xFace/exec');
var app = {};


app.exitApp = function(){
    //closeApplication
    exec(null, null, null, "App", "exitApp", []);
};

/**
 * 安装一个本地应用包（Android）
 * @example
        navigator.app.install("/test.apk",win, fail);
 * @method install
 * @param {String} path 要安装的本地应用的路径
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @for App
 * @platform Android
 * @since 3.0.0
 * */
app.install = function(path, successCallback, errorCallback){
    argscheck.checkArgs('sFF', 'App.install', arguments);
    exec(successCallback, errorCallback, null, "App", "install", [path]);
};

/**
 * 启动一个系统的特定程序（Android）
 * @example
        function win() {}
        function fail(msg) {
            console.log("Error message: " + msg);
        }
        var componentName = SysComponent.WIRELESS;//网络设置界面
        navigator.app.startSystemComponent(componentName, win, fail);
 * @method startSystemComponent
 * @param {Number} name 标识要启动的程序，在{{#crossLink "SysComponent"}}{{/crossLink}}中定义
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @param {String} errorCallback.msg 失败描述信息
 * @for App
 * @platform Android
 * @since 3.0.0
 * */
app.startSystemComponent = function(name, successCallback, errorCallback){
    argscheck.checkArgs('nFF', 'App.startSystemComponent', arguments);
    exec(successCallback, errorCallback, null, "App", "startSystemComponent", [name]);
};

/**
 * 设置wifi休眠策略（Android）
 * @example
        function win() {}
        function fail(msg) {
            console.log("Error message: " + msg);
        }
        var policy = "wifi_sleep_policy_never";
        navigator.app.setWifiSleepPolicy(policy,win,fail);
 * @method setWifiSleepPolicy
 * @param {String} wifiSleepPolicy wifi休眠策略名称，
            目前支持的策略："wifi\_sleep\_policy\_default"（默认策略，即要休眠）,
            "wifi\_sleep\_policy\_never\_while\_plugged"（充电的时候不休眠）,
            "wifi\_sleep\_policy\_never"（从不休眠）
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @param {String} errorCallback.msg 失败描述信息
 * @for App
 * @platform Android
 * @since 3.0.0
 * */
app.setWifiSleepPolicy = function(wifiSleepPolicy,successCallback, errorCallback){
    argscheck.checkArgs('sFF', 'App.setWifiSleepPolicy', arguments);
    exec(successCallback, errorCallback, null, "App", "setWifiSleepPolicy", [wifiSleepPolicy]);
};

/**
 * 在浏览器或者xFace引擎中加载一个url链接（Android）
 * @example
        var url = "http://www.baidu.com"
        var openexternal = true;
        var clearhistory = false;
        navigator.app.loadUrl(url,openexternal,clearhistory,win,fail);
 * @method loadUrl
 * @param {String} url 要加载的url
 * @param {Boolean} openexternal 是否通过系统浏览器方式打开，
 *                     true表示用系统浏览器打开，false表示用xFace引擎加载
 * @param {Boolean} clearhistory 是否清除xFace引擎页面历史，
 *                     true表示清除页面历史，false表示不清除页面历史
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @for App
 * @platform Android
 * @since 3.0.0
 * */
app.loadUrl = function(url,openexternal,clearhistory,successCallback, errorCallback){
    argscheck.checkArgs('sbbFF', 'App.loadUrl', arguments);
    exec(successCallback, errorCallback, null, "App", "loadUrl", [url,openexternal,clearhistory]);
};


app.backHistory = function(successCallback, errorCallback){
    argscheck.checkArgs('FF', 'App.backHistory', arguments);
    exec(successCallback, errorCallback, null, "App", "backHistory", []);
};

/**
 * 清除当前应用的页面浏览历史记录（Android）
 * @example
        navigator.app.clearHistory(win,fail);
 * @method clearHistory
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @for App
 * @platform Android
 * @since 3.0.0
 */
app.clearHistory = function(successCallback, errorCallback){
    argscheck.checkArgs('FF', 'App.clearHistory', arguments);
    exec(successCallback, errorCallback, null, "App", "clearHistory", []);
};

/**
 * 清除当前应用的页面缓存（Android）
 * @example
        navigator.app.clearCache(win, fail);
 * @method clearCache
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @for App
 * @platform Android
 * @since 3.0.0
 */
app.clearCache = function(successCallback, errorCallback){
    argscheck.checkArgs('FF', 'App.clearCache', arguments);
    exec(successCallback, errorCallback, null, "App", "clearCache", []);
};

/**
 * 设置是否支持点击view上的数字触发联系人添加操作（Android）
 * @example
        //不支持点击view上的数字触发联系人添加操作
        navigator.app.telLinkEnable(false, win, fail);
 * @method telLinkEnable
 * @param {Boolean}  [isTelLinkEnable]   是否支持点击view上的数字触发联系人添加操作,true:支持，false：不支持。
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback]   失败回调函数
 * @for App
 * @platform Android
 * @since 3.0.0
 */
app.telLinkEnable = function(isTelLinkEnable, successCallback, errorCallback){
    argscheck.checkArgs('bFF', 'App.telLinkEnable', arguments);
    exec(successCallback, errorCallback, null, "App", "telLinkEnable", [isTelLinkEnable]);
};


/**
 * 验证指定的应用是否被安装（Android）
 * @example
        var packageName = "test";
        navigator.app.isNativeAppInstalled(packageName, successCallback, errorCallback);
        function successCallback(result) {
            if(result == true) {
                alert(packageName+"is installed!");
            } else {
                alert(packageName+"is not installed!");
            }
        }
        function errorCallback() {
            alert("error!");
        }
 * @method isNativeAppInstalled
 * @param {String} packageName 应用ID
 * @param {Function} [successCallback] 成功回调函数
 * @param {Boolean} successCallback.result 表示应用是否被安装的结果<br />true表示该指定的应用已经安装，false表示该指定的应用未安装
 * @param {Function} [errorCallback] 失败回调函数
 * @for App
 * @platform Android
 * @since 3.0.0
 */
app.isNativeAppInstalled = function(packageName, successCallback, errorCallback){
    argscheck.checkArgs('sFF', 'App.isNativeAppInstalled', arguments);
    exec(successCallback, errorCallback, null, "App", "isNativeAppInstalled", [packageName]);
};

module.exports = app;