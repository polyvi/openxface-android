
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
 * @module notification
 */
var argscheck = require('xFace/argscheck'),
    exec = require('xFace/exec');
var notificationObj = {};

 /**
 * 调用系统接口使设备弹出spinner对话框.（Android）<br/>
 * @example
        var title="测试标题！";
        var message="测试消息！";
        navigator.notification.activityStart(title,message);
 * @method activityStart
 * @param {String} [title]   对话框的标题
 * @param {String} [message] 对话框显示的消息
 * @for Notification
 * @platform Android
 * @since 3.0.0
 */
notificationObj.activityStart = function(title,message) {
    argscheck.checkArgs('SS', 'Notification.activityStart', arguments);
    exec(null, null, null, "Notification", "activityStart", [title,message]);
};

 /**
 * 调用系统接口使设备关闭spinner对话框.（Android）<br/>
 * @example
        var title="测试标题！";
        var message="测试消息！";
        navigator.notification.activityStart(title,message);
        setTimeout(testStopSpinner,5000);
        function testStopSpinner() {
            navigator.notification.activityStop();
        }
 * @method activityStop
 * @for Notification
 * @platform Android
 * @since 3.0.0
 */
notificationObj.activityStop = function() {
    exec(null, null, null, "Notification", "activityStop", []);
};

 /**
 * 调用系统接口使设备弹出progress对话框.（Android）<br/>
 * @example
        var title="测试标题！";
        var message="测试消息！";
        navigator.notification.progressStart(title,message);
 * @method progressStart
 * @param {String} [title]   对话框的标题
 * @param {String} [message] 对话框显示的消息
 * @for Notification
 * @platform Android
 * @since 3.0.0
 */
notificationObj.progressStart = function(title,message) {
    argscheck.checkArgs('SS', 'Notification.progressStart', arguments);
    exec(null, null, null, "Notification", "progressStart", [title,message]);
};

 /**
 * 调用系统接口设置progress对话框的值.（Android）<br/>
 * @example
        var title="测试标题！";
        var message="测试消息！";
        navigator.notification.progressStart(title,message);
        navigator.notification.progressValue(20);
        setTimeout(testSetProgressValue,3000);
        function testSetProgressValue() {
            navigator.notification.progressValue(100);
        }
 * @method progressValue
 * @param {Number} value 进度条的值（0-100）
 * @for Notification
 * @platform Android
 * @since 3.0.0
 */
notificationObj.progressValue = function(value) {
    argscheck.checkArgs('n', 'Notification.progressValue', arguments);
    exec(null, null, null, "Notification", "progressValue", [value]);
};

 /**
 * 调用系统接口使设备关闭progress对话框.（Android）<br/>
 * @example
        var title="测试标题！";
        var message="测试消息！";
        navigator.notification.progressStart(title,message);
        navigator.notification.progressValue(20);
        setTimeout(testStopProgress,6000);
        function testStopProgress() {
            navigator.notification.progressStop();
        }
 * @method progressStop
 * @for Notification
 * @platform Android
 * @since 3.0.0
 */
notificationObj.progressStop = function() {
    exec(null, null, null, "Notification", "progressStop", []);
};

module.exports = notificationObj;