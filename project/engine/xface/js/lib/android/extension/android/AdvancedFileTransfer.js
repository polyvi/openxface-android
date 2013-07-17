
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
* @module fileTransfer
*/

var argscheck = require('xFace/argscheck'),
    exec = require('xFace/exec'),
    ProgressEvent = require('xFace/extension/ProgressEvent');

module.exports = {

/**
 * 上传一个文件到指定的路径(Android)<br/>
 * 下载过程中会通过onprogress属性更新文件传输进度
 * @example
        var uploadUrl = "http://polyvi.net:8091/mi/UploadServer";
        var fileTransfer = new xFace.AdvancedFileTransfer("test_upload2.rar",uploadUrl,true);
        fileTransfer.upload(successCallback, errorCallback);
        fileTransfer.onprogress = function(evt){
            var progress  = evt.loaded / evt.total;
        };
        function success(entry) {
            alert(entry.isDirectory);
            alert(entry.isFile);
            alert(entry.name);
            alert(entry.fullPath);
        }
        function fail(error) {
            alert(error.code);
            alert(error.source);
            alert(error.target);
        }
 * @method upload
 * @param {Function} [successCallback] 成功回调函数
 * @param {FileEntry} successCallback.fileEntry 成功回调返回下载得到的文件的{{#crossLink "FileEntry"}}{{/crossLink}}对象
 * @param {Function} [errorCallback] 失败回调函数
 * @param {Object} errorCallback.errorInfo 失败回调返回的参数
 * @param {Number} errorCallback.errorInfo.code 错误码（在<a href="FileTransferError.html">FileTransferError</a>中定义）
 * @param {String} errorCallback.errorInfo.source 上传源地址
 * @param {String} errorCallback.errorInfo.target 上传目标地址
 * @for xFace.AdvancedFileTransfer
 * @platform Android
 * @since 3.0.0
 */
upload : function(successCallback, errorCallback) {
    argscheck.checkArgs('FF', 'AdvancedFileTransfer.upload', arguments);
    var me = this;
    var s = function(result) {
        if (typeof me.onprogress === "function") {
                me.onprogress(new ProgressEvent("progress", {loaded:result.loaded, total:result.total}));
            }
    };

    exec(successCallback, errorCallback, s, 'AdvancedFileTransfer', 'upload', [this.source, this.target]);
}

};