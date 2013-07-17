
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
 * @module message
 */
var argscheck = require('xFace/argscheck'),
    exec = require('xFace/exec'),
    Message = require('xFace/extension/Message');

var Messaging = {};

/**
 * 获取指定信息文件夹的信息数量，不支持读取SIM卡内信息（Android）<br/>
 * @example
        xFace.Messaging.getQuantities (xFace.MessageTypes.SMSMessage, xFace.MessageFolderTypes.INBOX, successCallback, errorCallback);
        function successCallback(num){
            alert("收件箱中有"+num+"条短信");}
        function errorCallback(){alert("failed");}
 * @method getQuantities
 * @for Messaging
 * @param {String} messageType 信息类型，取值范围见{{#crossLink "xFace.MessageTypes"}}{{/crossLink}}
 * @param {String} folderType  文件夹类型，取值范围见{{#crossLink "xFace.MessageFolderTypes"}}{{/crossLink}}
 * @param {Function} [successCallback] 成功回调函数
 * @param {Number} successCallback.num   获取到的信息数量
 * @param {Function} [errorCallback]   失败回调函数
 * @platform Android
 * @since 3.0.0
 */
Messaging.getQuantities = function(messageType, folderType, successCallback, errorCallback) {
    argscheck.checkArgs('ssfF', 'Messaging.getQuantities', arguments);
    exec(
        function(result) {
            successCallback(result);
        },
        errorCallback, null, "Messaging", "getQuantities", [messageType, folderType]);
};

/**
 * 获取指定文件夹类型中的指定索引位置的信息，不支持读取SIM卡内信息（Android）<br/>
 * @example
        xFace.Messaging.getMessage (xFace.MessageTypes.SMSMessage, xFace.MessageFolderTypes.INBOX, 0, successCallback, errorCallback);
        function successCallback(message) { alert(message.body);}
        function errorCallback(){alert("failed");}
 * @method getMessage
 * @for Messaging
 * @param {String} messageType 信息类型，取值范围见{{#crossLink "xFace.MessageTypes"}}{{/crossLink}}
 * @param {String} folderType  文件夹类型，取值范围见{{#crossLink "xFace.MessageFolderTypes"}}{{/crossLink}}
 * @param {Number} index           要获取的短信索引
 * @param {Function} [successCallback] 成功回调函数
 * @param {Message} successCallback.message 获取到的信息对象，参见 {{#crossLink "xFace.Message"}}{{/crossLink}}
 * @param {Function} [errorCallback]   失败回调函数
 * @platform Android
 * @since 3.0.0
 */
Messaging.getMessage = function(messageType, folderType, index, successCallback, errorCallback) {
    argscheck.checkArgs('ssnfF', 'Messaging.getMessage', arguments);
     var win = typeof successCallback !== 'function' ? null : function(result) {
        var message = new Message(result.messageId, result.subject, result.body, result.destinationAddresses,
                            result.messageType, new Date(result.date), result.isRead);
        successCallback(message);
    };
    exec(win, errorCallback, null, "Messaging", "getMessage", [messageType, folderType, index]);
};

/**
 * 获取某个文件夹中的所有信息，不支持读取SIM卡内信息（Android）<br/>
 * @example
        xFace.Messaging.getAllMessages (xFace.MessageTypes.SMSMessage, xFace.MessageFolderTypes.INBOX, successCallback, errorCallback);
        function successCallback(messages){  alert(messages.length);}
        function errorCallback(){alert("failed");}
 * @method getAllMessages
 * @for Messaging
 * @param {String} messageType 信息类型，取值范围见{{#crossLink "xFace.MessageTypes"}}{{/crossLink}}
 * @param {String} folderType  文件夹类型，取值范围见{{#crossLink "xFace.MessageFolderTypes"}}{{/crossLink}}
 * @param {Function} [successCallback] 成功回调函数
 * @param {Array} successCallback.messages 获取到的所有信息对象，该数组对象中的每个元素为一个{{#crossLink "xFace.Message"}}{{/crossLink}}类型对象
 * @param {Function} [errorCallback]   失败回调函数
 * @platform Android
 * @since 3.0.0
 */
Messaging.getAllMessages =function(messageType, folderType, successCallback, errorCallback) {
    argscheck.checkArgs('ssfF', 'Messaging.getAllMessages', arguments);
    var win = typeof successCallback !== 'function' ? null : function(result) {
        var retVal = [];
        for (var i = 0; i < result.length; i++) {
            var message = new Message(result[i].messageId, result[i].subject, result[i].body, result[i].destinationAddresses,
                                result[i].messageType, new Date(result[i].date), result[i].isRead);
            retVal.push(message);
        }
        successCallback(retVal);
    };
    exec(win, errorCallback, null, "Messaging", "getAllMessages", [messageType, folderType]);
};

/**
 * 在指定文件夹中查找匹配的信息，不支持读取SIM卡内信息（Android）<br/>
 * @example
        Messaging.findMessages (message, xFace.MessageFolderTypes.INBOX, 0, 3, success, errorCallback);
        function success(messages) {alert(messages.length);}
        function errorCallback(){alert("failed");}
 * @method findMessages
 * @for Messaging
 * @param {Message} comparisonMsg   要查找的信息，参见{{#crossLink "xFace.Message"}}{{/crossLink}}
 * @param {String} folderType  文件夹类型，取值范围见{{#crossLink "xFace.MessageFolderTypes"}}{{/crossLink}}
 * @param {Number} startIndex  起始索引
 * @param {Number} endIndex  结束索引
 * @param {Function} [successCallback] 成功回调函数
 * @param {Array} successCallback.messages 查找到的所有信息对象，该数组对象中的每个元素为一个{{#crossLink "xFace.Message"}}{{/crossLink}}类型对象
 * @param {Function} [errorCallback]   失败回调函数
 * @platform Android
 * @since 3.0.0
 */
Messaging.findMessages = function(comparisonMsg, folderType, startIndex, endIndex, successCallback, errorCallback) {
    argscheck.checkArgs('osnnfF', 'Messaging.findMessages', arguments);
    var comparison = {messageId:"", subject:"", destinationAddresses:"", body:"", isRead:-1};
    if(null !== comparisonMsg){
        comparison.messageId = comparisonMsg.messageId || "";
        comparison.subject = comparisonMsg.subject || "";
        comparison.destinationAddresses = comparisonMsg.destinationAddresses || "";
        comparison.body = comparisonMsg.body || "";
        if(null === comparisonMsg.isRead) {
            comparison.isRead = -1;
        }
        else if(comparisonMsg.isRead) {
            comparison.isRead = 1;
        }
        else {
            comparison.isRead = 0;
        }
    }
    var win = typeof successCallback !== 'function' ? null : function(result) {
        var retVal = [];
        for(var i = 0 ; i < result.length ; i++){
            var message = new Message(result[i].messageId, result[i].subject, result[i].body, result[i].destinationAddresses, result[i].messageType,
                                        new Date(result[i].date), result[i].isRead);
            retVal.push(message);
        }
        successCallback(retVal);
    };
    exec(win, errorCallback, null, "Messaging", "findMessages", [comparison, folderType, startIndex, endIndex]);
};

function msgRcvHandler(msgJsonStr) {
    eval("var msgJsonObjArray = " + msgJsonStr);
    var msgs = [];
    for(var i = 0; i < msgJsonObjArray.length; i++){
        var result = msgJsonObjArray[i];
        var message = new Message(result.messageId, result.subject, result.body, result.destinationAddresses,result.messageType, new Date(result.date), result.isRead);
        message.originatingAddress = result.originatingAddress;
        msgs.push(message);
    }
    handler.call(document,msgs);
}

/**
 * 注册接收短信的监听器
 */
Messaging.addEventListener = function(evt, handler) {
    var e = evt.toLowerCase();
    // 接收短信的事件
    if("messagereceived" == e){
        window.addEventListener(e, msgRcvHandler);
    }
};

module.exports = Messaging;