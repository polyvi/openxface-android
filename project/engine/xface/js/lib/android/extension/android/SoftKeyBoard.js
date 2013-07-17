
/*
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
 * 该模块提供软键盘操作的支持
 * @module SoftKeyBoard
 * @main SoftKeyBoard
 */

 /**
  * 该类提供软键盘的显示和隐藏功能（Android）
  * @class SoftKeyBoard
  * @platform Android
  * @since 3.1.0
  */
var exec=require('xFace/exec');
var argscheck = require('xFace/argscheck');

function SoftKeyBoard(){}

/**
 * 显示软键盘（Android）
 * @example
        xFace.SoftKeyBoard.show(successCallback,errorCallback);
 * @method show
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @platform Android
 * @since 3.1.0
 */
SoftKeyBoard.prototype.show=function(successCallback,errorCallback){
    argscheck.checkArgs('FF', 'SoftKeyBoard.show', arguments);
    exec(successCallback,errorCallback,null,"SoftKeyBoard","show",[]);
};

/**
 * 隐藏软键盘（Android）
 * @example
        xFace.SoftKeyBoard.hide(successCallback,errorCallback);
 * @method hide
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @platform Android
 * @since 3.1.0
 */
SoftKeyBoard.prototype.hide=function(successCallback,errorCallback){
    argscheck.checkArgs('FF', 'SoftKeyBoard.hide', arguments);
    exec(successCallback,errorCallback,null,"SoftKeyBoard","hide",[]);
};

/**
 * 提示软键盘当前状态，即是隐藏还是显示状态（Android）
 * @example
        xFace.SoftKeyBoard.isShowing(successCallback,errorCallback);
 * @method isShowing
 * @param {Function} successCallback 成功回调函数
 * @param {Boolean} successCallback.isShowing 成功回调返回软键盘是否显示，true表示显示，false表示没显示
 * @param {Function} [errorCallback] 失败回调函数
 * @platform Android
 * @since 3.1.0
 */

SoftKeyBoard.prototype.isShowing = function(successCallback,errorCallback) {
    argscheck.checkArgs('fF', 'SoftKeyBoard.isShowing', arguments);
    exec(successCallback,errorCallback,null,"SoftKeyBoard","isShowing",[]);
};

module.exports=new SoftKeyBoard();