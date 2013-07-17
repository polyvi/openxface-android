
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
 * @module media
 */
var argscheck = require('xFace/argscheck'),
    exec = require('xFace/exec');

/**
 * Video对象提供视频播放能力，支持的视频格式仅限于系统播放器支持的格式 (Android) </br>
 * 请注意：</br>
 * iOS:请使用<a href="http://www.html5rocks.com/en/tutorials/video/basics/">html5 video element</a>，支持本地和网络视频</br>
 * Android 2.x: 可以使用html5 video element，也可以使用xFace Video对象</br>
 * Android 4.x: 请使用xFace Video对象，因为html5 video支持不完善
 @example
     function playLocalVideoMp4() {
         var path = "testvideo.mp4";        // path relative to workspace
         var videomp4 = new Video(path, onSuccess, onError);
         videomp4.play();
     }

     function onSuccess(message) {
         console.log(“successfully playing video”);
         console.log(message);
     }

     function onError(error) {
         console.log(error);
     }
 * @class Video
 * @constructor
 * @param {String} src 源文件路径，可以是本地文件（相对于xapp workspace)，也可以是网络url
 * @param {Function} [successCallback]      成功回调函数
 * @param {String} successCallback.message  附加描述信息，例如播放正常结束的信息
 * @param {Function} [errorCallback]        失败回调函数
 * @param {String} errorCallback.error      错误信息
 * @platform Android
 * @since 3.0.0
 */
var Video = function(src, successCallback, errorCallback) {
    argscheck.checkArgs('sFF', 'Video.Video', arguments);
    this.src = src;
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
};

/**
 * 播放视频文件（Android）
 @example
      video.play();
 * @method play
 * @platform Android
 * @since 3.0.0
 */
Video.prototype.play = function() {
    exec(this.successCallback, this.errorCallback, null, "Video", "play", [this.src]);
};

module.exports = Video;
