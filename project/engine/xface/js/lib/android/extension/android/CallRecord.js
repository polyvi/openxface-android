
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
 * @module telephony
 */

 /**
 * 用来描述一个通话记录对象（Android）<br/>
 * @example
        var callRecord = new xFace.Telephony.CallRecord("*", "", "*", "", null, null);
 * @param {String} [callRecordAddress=null] 电话号码
 * @param {String} [callRecordId=null] 通话记录的id号
 * @param {String} [callRecordName=null] 联系人名字
 * @param {String} [callRecordType=null] 通话记录类型
 * @param {Number} [durationSeconds=null] 通话的时长(单位是秒)
 * @param {Date} [startTime=null] 通话开始时间
 * @class CallRecord
 * @namespace xFace.Telephony
 * @constructor
 * @platform Android
 * @since 3.0.0
 */
var CallRecord = function(callRecordAddress,callRecordId,callRecordName,callRecordType,durationSeconds,startTime){
    /**
     * 电话号码（Android）
     * @property callRecordAddress
     * @type String
     * @platform Android
     * @since 3.0.0
     */
    this.callRecordAddress = callRecordAddress || null;
    /**
     * 通话记录的id号（Android）
     * @property callRecordId
     * @type String
     * @platform Android
     * @since 3.0.0
     */
    this.callRecordId = callRecordId || null;
    /**
     * 联系人名字（Android）
     * @property callRecordName
     * @type String
     * @platform Android
     * @since 3.0.0
     */
    this.callRecordName = callRecordName || null;
    /**
     * 通话记录类型（Android）
     * @property callRecordType
     * @type String
     * @platform Android
     * @since 3.0.0
     */
    this.callRecordType = callRecordType || null;
    /**
     * 通话的时长(单位是秒)（Android）
     * @property durationSeconds
     * @type Number
     * @platform Android
     * @since 3.0.0
     */
    this.durationSeconds = durationSeconds || null;
    /**
     * 通话开始时间（Android）
     * @property startTime
     * @type Date
     * @platform Android
     * @since 3.0.0
     */
    this.startTime = startTime || null;
};
module.exports = CallRecord;
