
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

/**
 * 该类定义一些常量，用于标识信息文件夹的类型（Android）
 * 相关参考： {{#crossLink "Messaging"}}{{/crossLink}}
 * @class MessageFolderTypes
 * @namespace xFace
 * @static
 * @platform Android
 * @since 3.0.0
 */
var MessageFolderTypes = function() {
};

/**
 * 草稿箱（Android）
 * @property DRAFTS
 * @type String
 * @static
 * @final
 * @platform Android
 * @since 3.0.0
 */
MessageFolderTypes.DRAFTS = "DRAFT";  //草稿箱
/**
 * 收件箱（Android）
 * @property INBOX
 * @type String
 * @static
 * @final
 * @platform Android
 * @since 3.0.0
 */
MessageFolderTypes.INBOX = "INBOX";   //收件箱
/**
 * 发件箱（Android）
 * @property OUTBOX
 * @type String
 * @static
 * @final
 * @platform Android
 * @since 3.0.0
 */
MessageFolderTypes.OUTBOX = "OUTBOX"; //发件箱
/**
 * 发出的信息（Android）
 * @property SENTBOX
 * @type String
 * @static
 * @final
 * @platform Android
 * @since 3.0.0
 */
MessageFolderTypes.SENTBOX = "SENT";  //发出的信息

module.exports = MessageFolderTypes;