
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
 * @module event
 */
require('xFace/localStorage');
var privateModule = require('xFace/extension/privateModule');

module.exports = {
  id: "android",
  initialize:function() {
    var channel = require("xFace/channel"),
        xFace = require('xFace'),
        exec = require('xFace/exec');

    channel.onDestroy.subscribe(function() {
      xFace.shuttingDown = true;
    });
    // Inject a listener for the backbutton on the document.
    var backButtonChannel = xFace.addDocumentEventHandler('backbutton');
    backButtonChannel.onHasSubscribersChange = function() {
        // If we just attached the first handler or detached the last handler,
        // let native know we need to override the back button.
        if (this.numHandlers === 1) {
            privateModule.execCommand("_xFace_override_backbutton:", [true]);
        }else if (this.numHandlers === 0) {
            privateModule.execCommand("_xFace_override_backbutton:", [false]);
        }
    };


    // Inject a listener for the volumedownbutton on the document.
    var volumeButtonDownChannel = xFace.addDocumentEventHandler('volumedownbutton');
    volumeButtonDownChannel.onHasSubscribersChange = function() {
        // If we just attached the first handler or detached the last handler,
        // let native know we need to override the volumedownbutton.
        if (this.numHandlers === 1) {
            privateModule.execCommand("_xFace_override_volumebutton_down:", [true]);
        }else if (this.numHandlers === 0) {
            privateModule.execCommand("_xFace_override_volumebutton_down:", [false]);
        }
    };


    // Inject a listener for the volumeupbutton on the document.
    var volumeButtonUpChannel = xFace.addDocumentEventHandler('volumeupbutton');
    volumeButtonUpChannel.onHasSubscribersChange = function() {
        // If we just attached the first handler or detached the last handler,
        // let native know we need to override the volumeupbutton.
        if (this.numHandlers === 1) {
            privateModule.execCommand("_xFace_override_volumebutton_up:", [true]);
        }else if (this.numHandlers === 0) {
            privateModule.execCommand("_xFace_override_volumebutton_up:", [false]);
        }
    };


    // Add hardware MENU and SEARCH button handlers
    /**
     * 当菜单键被按下时，会触发该事件（Android）<br/>
     * @example
            function onMenuKeyDown() {
                alert("菜单键事件被触发");
            }
            document.addEventListener("menubutton", onMenuKeyDown, false);
     * @event menubutton
     * @for BaseEvent
     * @platform Android
     * @since 3.0.0
     */
    xFace.addDocumentEventHandler('menubutton');
    /**
     * 当搜索键被按下时，会触发该事件（Android）<br/>
     * @example
            function onSearchKeyDown() {
                alert("搜索键事件被触发");
            }
            document.addEventListener("searchbutton", onSearchKeyDown, false);
     * @event searchbutton
     * @for BaseEvent
     * @platform Android
     * @since 3.0.0
     */
    xFace.addDocumentEventHandler('searchbutton');

    /**
     * 当短信到来时，会触发该事件（Android）<br/>
     * @example
            function onMessageReceived(msgs) {
                alert("短信总数：" + msgs.length);
            }
            document.addEventListener("messagereceived", onMessageReceived, false);
     * @event messagereceived
     * @for BaseEvent
     * @param {xFace.Message[]} msgs 接收到的短信的数组
     * @platform Android
     * @since 3.0.0
     */
    channel.onMsgReceived = xFace.addDocumentEventHandler('messagereceived');
    /**
     * 当电话呼入时，会触发该事件（Android）<br/>
     * @example
            function onCallReceived(callStatus) {
                //callStatus是字符串需要转换成整形   
                var callStatus = parseInt(CallStatus);
                switch(callStatus){
                case 0:
                    alert("无状态(挂断)");
                    break;
                case 1:
                    alert("电话呼入，响铃中");
                    break;
                case 2:
                    alert("接听电话。。。。");
                    break;
           }

            document.addEventListener("callreceived", onCallReceived, false);
     * @event callreceived
     * @for BaseEvent
     * @param {String} CallStatus 接入状态信息.<br/>
     *            0：无状态 （挂断）<br/>
     *            1：电话拨入，响铃中<br/>
     *            2：接听中
     * @platform Android
     * @since 3.0.0
     */
    channel.onCallReceived = xFace.addDocumentEventHandler('callreceived');
	
	/**
     * 当商圈退出的时候，会触发该事件（Android）<br/>
     * @example
            function onCircleMessageReceived(status) {  
                var status = parseInt(status);
                switch(status){
                case 0:
                    alert("退出");
                    break;
                case 1:
                    alert("返回首页");
                    break;
                case 2:
                    alert("注册长时间无操作的监听");
                    break;
           }

            document.addEventListener("circlemessagereceived", onCircleMessageReceived, false);
     * @event circlemessagereceived
     * @for BaseEvent
     * @param {String} status 状态信息.<br/>
     *            0：退出 <br/>
     *            1：回首页<br/>
     *            2：注册长时间无操作的监听
     * @platform Android
     * @since 3.0.0
     */
	channel.onCircleMessageReceived = xFace.addDocumentEventHandler('circlemessagereceived');

    var storage = require('xFace/extension/android/storage');

    // First patch WebSQL if necessary
    if (typeof window.openDatabase == 'undefined') {
          // Not defined, create an openDatabase function for all to use!
          window.openDatabase = storage.openDatabase;
    } else {
          // Defined, but some Android devices will throw a SECURITY_ERR -
          // so we wrap the whole thing in a try-catch and shim in our own
          // if the device has Android bug 16175.
          var originalOpenDatabase = window.openDatabase;
          window.openDatabase = function(name, version, desc, size) {
              var db = null;
              try {
                  db = originalOpenDatabase(name, version, desc, size);
              }
              catch (ex) {
                    if (ex.code === 18) {
                        db = null;
                    } else {
                        throw ex;
                    }
              }

              if (db === null) {
                    return storage.openDatabase(name, version, desc, size);
              }
              else {
                    return db;
              }
          };
    }
    // Let native code know we are all done on the JS side.
    // Native code will then un-hide the WebView.
    channel.join(function() {
        privateModule.execCommand("_xFace_js_init_done:", []);
    }, [channel.onxFaceReady]);
  },
  objects: {
    xFace: {
        children: {
            MessageFolderTypes: {
                path: 'xFace/extension/android/MessageFolderTypes'
            },
            Telephony: {
                children: {
                    CallRecordTypes: {
                        path: 'xFace/extension/android/CallRecordTypes'
                    },
                    CallRecord: {
                        path: 'xFace/extension/android/CallRecord'
                    }
                }
            },
            TrafficStats:{
                path: 'xFace/extension/android/TrafficStats'
            },
            StatusBarNotification:{          
                path: 'xFace/extension/android/StatusBarNotification'
            },
            SoftKeyBoard:{
                path:'xFace/extension/android/SoftKeyBoard'
            }
        }
    },
    open:{ // exists natively on Android WebView, override
        path:'xFace/extension/InAppBrowser'
    },
    /*device: {
        path: 'xFace/extension/android/device'
    },*/
    File: { // exists natively on Android WebView, override
        path: "xFace/extension/File"
    },
    FileReader: { // exists natively on Android WebView, override
         path: "xFace/extension/FileReader"
    },
    FileError: { //exists natively on Android WebView on Android 4.x
        path: "xFace/extension/FileError"
    },
    MediaError: { // exists natively on Android WebView on Android 4.x
        path: 'xFace/extension/MediaError'
    },
    console: {
        path: 'xFace/extension/console'
    },
    SysComponent: {
        path: 'xFace/extension/android/SysComponent'
    },
    Video: {
    // 此部分实现是为了使Android 4.0以上能实现视频播放
    // 因为现在webkit对HTML5的video标签支持不佳
    // 播放视频时只有声音无图像
    // 现在只能使用native能力来实现播放
    // 待webkit完善后，此接口可删除
        path: 'xFace/extension/android/Video'
    }
  },
  merges:{
    xFace: {
        children: {
            Messaging: {
                path: 'xFace/extension/android/Messaging'
            },
            AdvancedFileTransfer: {
                path: 'xFace/extension/android/AdvancedFileTransfer'
            },
            Telephony: {
                path: 'xFace/extension/android/Telephony'
            }
        }
    },
    navigator: {
        children: {
            notification: {
                path: 'xFace/extension/android/Notification'
            },
            app: {
                path: 'xFace/extension/android/app'
            }
        }
    }
  }
};
