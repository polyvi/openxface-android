
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
 * 该扩展提供状态提示信息的功能(Android)
 * @module StatusBarNotification
 * @main StatusBarNotification
 */

/**
  * 该类定义了状态提示的产生和清除（Android）<br/>
  * 该类不能通过new来创建相应的对象，只能通过xFace.StatusBarNotification对象来直接使用该类中定义的方法
  * @class StatusBarNotification
  * @static
  * @platform Android
  * @since 3.1.0
  */
var argscheck = require('xFace/argscheck'),
    exec = require('xFace/exec');
var StatusBarNotification = function(){};

/**
 * 产生状态提示消息.（Android）<br/>
 * @example
        xFace.StatusBarNotification.notify('Tag1', 'my title', 16, 'my message', notifySuccess, notifyFail);
        function notifySuccess() {
            document.getElementById('result').innerHTML = "Create success";}
        function notifyFail() {document.getElementById('result').innerHTML = "Create fail";}
 * @method notify
 * @for StatusBarNotification
 * @param {String} tag	状态提示消息的标签，用于标示一个标签，产生、清除一个标签使用相同的tag值
 * @param {String} title	状态提示消息的名字
 * @param {Number} flag	状态提示消息的标志位,该值是按位或,该位取值情况：<br/>
   flag=1(0x00000001) 若设置该位，当提示信息产生时，LED会打开；<br/>
   flag=2(0x00000002) 若提示消息所指向的任务是持续的，例如一个电话，就应设置该位；若提示消息所指向的任务只发生在某一时刻，例如一未接个电话，就不应设置该位；<br/>
   flag=4(0x00000004) 若设置该位，提示音会一直进行，直至该状态提示消息被取消或通知窗口被打开；<br/>
   flag=8(0x00000008) 若设置该位，每次产生提示消息时有声音和/或振动，即使在它没有被取消之前；<br/>
   flag=16(0x00000010) 若设置该位，当提示消息被点击时，提示消息会被取消；<br/>
   flag=32(0x00000020) 若设置该位，用户点击清除所有按钮时，提示信息不会被取消；<br/>
   flag=64(0x00000040) 若提示消息代表一个正在运行的服务，该位应该被设置。<br/> 
   详情请查看<a href="http://developer.android.com/reference/android/app/Notification.html#FLAG_AUTO_CANCEL">官方文档</a>   
 * @param {String} [body]	状态提示消息的内容
 * @param {Function} [successCallback]  成功的回调函数
 * @param {Function} [errorCallback]     失败的回调函数 
 * @platform Android
 * @since 3.1.0
 */

StatusBarNotification.prototype.notify = function(tag, title, flag, body, successCallback, errorCallback){
	argscheck.checkArgs('ssnSFF', "xFace.StatusBarNotification.notify", arguments);		
	this.body = body||'';	
	var newArguments = [];	
	newArguments = [tag, title, this.body, flag];
	exec(successCallback, errorCallback, null, "StatusBarNotification", "notify", newArguments);
};

/**
 * 清除状态提示消息.（Android）<br/>
 * @example
        xFace.StatusBarNotification.clear('Tag1', clearSuccess, clearFail);
        function clearSuccess() {
            document.getElementById('result').innerHTML = "Clear success";}
        function clearFail() {document.getElementById('result').innerHTML = "Clear fail";}
 * @method clear
 * @for StatusBarNotification
 * @param {String} tag	状态提示消息的标签，用于标示一个标签，产生、清除一个标签使用相同的tag值
 * @param {Function} [successCallback]   成功的回调函数
 * @param {Function} [errorCallback]     失败的回调函数
 * @platform Android
 * @since 3.1.0
 */

StatusBarNotification.prototype.clear=function(tag, successCallback, errorCallback){
	argscheck.checkArgs('sFF',"xFace.StatusBarNotification.clear", arguments);
	var newArguments = [];
	newArguments = [tag];
	exec(successCallback, errorCallback, null, "StatusBarNotification", "clear", newArguments);	
};

/**
 * 清除所有显示的状态提示消息.（Android）<br/>
 * @example
        xFace.StatusBarNotification.clearAll(clearSuccess, clearFail);
        function clearAllSuccess() {
            document.getElementById('result').innerHTML = "Clear all success";}
        function clearAllFail() {document.getElementById('result').innerHTML = "Clear all fail";}
 * @method clearAll
 * @for StatusBarNotification
 * @param {Function} [successCallback]   成功的回调函数
 * @param {Function} [errorCallback]     失败的回调函数
 * @platform Android
 * @since 3.1.0
 */

StatusBarNotification.prototype.clearAll=function(successCallback, errorCallback){
  argscheck.checkArgs('FF',"xFace.StatusBarNotification.clearAll", arguments);
  exec(successCallback, errorCallback, null, "StatusBarNotification", "clearAll", []); 
};

module.exports = new StatusBarNotification();