
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
var argscheck = require('xFace/argscheck'),
    exec = require('xFace/exec'),
    CallRecord = require('xFace/extension/android/CallRecord');

var telephonyObj = {};
/**
 * 删除指定通话记录类型的所有通话记录 (Android)
 *@example
       //删除已拨电话通话记录
       xFace.Telephony.deleteAllCallRecords(xFace.Telephony.CallRecordTypes.OUTGOING,
       deleteAllCallRecordsSuccess,deleteAllCallRecordsError);
       function deleteAllCallRecordsSuccess(){
           alert("success!");
       }
       function deleteAllCallRecordsError(){
           alert("fail!");
       }
 * @method deleteAllCallRecords
 * @for Telephony
 * @param {String} callRecordType 通话记录类型,具体用法请参考相关链接{{#crossLink "xFace.Telephony.CallRecordTypes"}}{{/crossLink}}
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @platform Android
 */
telephonyObj.deleteAllCallRecords = function(callRecordType,successCallback,errorCallback){
    argscheck.checkArgs('sFF', 'Telephony.deleteAllCallRecords', arguments);
    exec(successCallback, errorCallback, null, "Telephony", "deleteAllCallRecords", [callRecordType]);
};

/**
 * 删除指定通话记录类型和指定id的通话记录 (Android)
 * @example
        //删除拨出电话id为0的通话记录
        xFace.Telephony.deleteCallRecord(xFace.Telephony.CallRecordTypes.OUTGOING,
        "0",deleteCallRecordSuccess,deleteCallRecordError);
        function deleteCallRecordSuccess(){
            alert("delete " + xFace.Telephony.CallRecordTypes.OUTGOING + " 0 is success!");
        }
        function deleteCallRecordError(){
            alert("delete " + xFace.Telephony.CallRecordTypes.OUTGOING + " 0 is fail!");
        }
 * @method deleteCallRecord
 * @param {String} callRecordType 通话记录类型,具体用法请参考相关链接{{#crossLink "xFace.Telephony.CallRecordTypes"}}{{/crossLink}}
 * @param {String} id 通话记录的id号(不能为非数字的字符串),具体用法请参考相关链接{{#crossLink "xFace.Telephony.CallRecord"}}{{/crossLink}}的callRecordId属性
 * @param {Function} [successCallback] 成功回调函数
 * @param {Function} [errorCallback] 失败回调函数
 * @platform Android
 */
telephonyObj.deleteCallRecord = function(callRecordType,id,successCallback,errorCallback){
    argscheck.checkArgs('ssFF', 'Telephony.deleteCallRecord', arguments);
    exec(successCallback, errorCallback, null, "Telephony", "deleteCallRecord", [callRecordType,id]);
};

/**
 * 获取指定通话记录类型和id的通话记录 (Android)
 * @example
        //获取已拨打电话里的第一条通话记录
        xFace.Telephony.getCallRecord(xFace.Telephony.CallRecordTypes.OUTGOING,
        "1",getCallRcdSuccess,getCallRcdError);
        function getCallRcdSuccess(callRecord){
            if(null == callRecord.callRecordId){
                alert("指定id的该通话记录不存在！");
                return;
            }
            alert(callRecord.callRecordAddress);
            alert(callRecord.callRecordId);
            alert(callRecord.callRecordName);
            alert(callRecord.callRecordType);
            if(typeof callRecord.durationSeconds == undefined){
            alert(0);
            }else{
                alert(callRecord.durationSeconds);
            }
        }
        function getCallRcdError(){
            alert(xFace.Telephony.CallRecordTypes.OUTGOING + " 1 " + "  getCallRecord Error");
        }
 * @method getCallRecord
 * @param {String} callRecordType 通话记录类型,具体用法请参考相关链接{{#crossLink "xFace.Telephony.CallRecordTypes"}}{{/crossLink}}
 * @param {String} index 通话记录的索引(不能为非数字的字符串)
 * @param {Function} [successCallback] 成功回调函数
 * @param {CallRecord} successCallback.callRecord 指定通话记录类型和id的通话记录。具体用法请参考相关链接{{#crossLink "xFace.Telephony.CallRecord"}}{{/crossLink}}
 * @param {Function} [errorCallback] 失败回调函数
 * @platform Android
 */
telephonyObj.getCallRecord = function(callRecordType,index,successCallback,errorCallback){
    argscheck.checkArgs('ssfF', 'Telephony.getCallRecord', arguments);
    var success = function(result){
        var callRecord = new CallRecord(result.callRecordAddress,result.callRecordId,result.callRecordName,
                                        result.callRecordType,result.durationSeconds,new Date(result.startTime));
        successCallback(callRecord);
    };
    exec(success, errorCallback, null, "Telephony", "getCallRecord", [callRecordType,index]);
};

/**
 * 按照带匹配的通话记录查找指定范围内的通话记录 (Android)
 * @example
        //联系人使用"*"通配，电话号码也使用"*"通配，其余字段不考虑，查找的是取结果的第2至4条记录
        var compairedCallRecord = new xFace.Telephony.CallRecord("*","","*","",null,null);
        xFace.Telephony.findCallRecords(compairedCallRecord,1,3,success,fail);
        function success(result){
            alert("找到了" + result.length +" 条通话记录");
        }
        function fail() {
            alert("failed");
        }
 * @method findCallRecords
 * @param {CallRecord} comparisonCallRecord 查找带匹配属性的通话记录,具体用法请参考相关链接{{#crossLink "xFace.Telephony.CallRecord"}}{{/crossLink}}
 * @param {Number} startIndex 开始位置索引(不能为负数)
 * @param {Number} endIndex 结束位置索引(不能为负数)
 * @param {Function} [successCallback] 成功回调函数
 * @param {Array} successCallback.callRecords 该数组对象
 * 中的每个元素为一个{{#crossLink "xFace.Telephony.CallRecord"}}{{/crossLink}}类型对象。
 * @param {Function} [errorCallback] 失败回调函数
 * @platform Android
 */
telephonyObj.findCallRecords = function(comparisonCallRecord,startIndex,endIndex,successCallback,errorCallback){
    argscheck.checkArgs('onnfF', 'Telephony.findCallRecords', arguments);
    if(startIndex < 0 && endIndex < 0){
        throw "ivalid_parameter";
    }
    var comparison = {callRecordAddress:"*",callRecordId:"",callRecordName:"*",callRecordType:"",durationSeconds:-1,startTime:-1};
    if(null !== comparisonCallRecord){
        comparison.callRecordAddress = comparisonCallRecord.callRecordAddress === null ? "" : comparisonCallRecord.callRecordAddress;
        comparison.callRecordId = comparisonCallRecord.callRecordId === null ? "" : comparisonCallRecord.callRecordId;
        comparison.callRecordName = comparisonCallRecord.callRecordName === null ? "" : comparisonCallRecord.callRecordName;
        comparison.callRecordType = comparisonCallRecord.callRecordType === null ? "" : comparisonCallRecord.callRecordType;
        comparison.durationSeconds = comparisonCallRecord.durationSeconds === null ? -1 : comparisonCallRecord.durationSeconds;//如果该项留空则将该项值设为-1,java层检查是否为-1，-1表示留空忽略该项
        comparison.startTime = comparisonCallRecord.startTime === null ? -1 : (comparisonCallRecord.startTime.getTime());//如果该项留空则将该项值设为-1,java层检查是否为-1，-1表示留空忽略该项
    }
    var success = function(result){
        var len = result.length;
        var callRecordArr = [];
        for(var i = 0 ; i < len ; i++){
            var callRecord = new CallRecord(result[i].callRecordAddress,result[i].callRecordId,result[i].callRecordName,
                                        result[i].callRecordType,result[i].durationSeconds,new Date(result[i].startTime));
            callRecordArr.push(callRecord);
        }
        successCallback(callRecordArr);
    };
    exec(success, errorCallback, null, "Telephony", "findCallRecords", [comparison,startIndex,endIndex]);
};

/**
 * 获取指定通话记录类型的通话记录总数 (Android)
 * @example
        xFace.Telephony.getCallRecordCount(xFace.Telephony.CallRecordTypes.OUTGOING,
        getCallRecordCountSuccess,getCallRecordCountError);
        function getCallRecordCountSuccess(count){
            alert(xFace.Telephony.CallRecordTypes.OUTGOING + " count is : " + count);
        }
        function getCallRecordCountError(){
            alert("get " + xFace.Telephony.CallRecordTypes.OUTGOING + " count fail");
        }
 * @method getCallRecordCount
 * @for Telephony
 * @param {String} callRecordType 通话记录类型,具体用法请参考相关链接{{#crossLink "xFace.Telephony.CallRecordTypes"}}{{/crossLink}}
 * @param {Function} successCallback 成功回调函数
 * @param {Number} successCallback.count 通话记录总条数
 * @param {Function} [errorCallback] 失败回调函数
 * @platform Android
 */
telephonyObj.getCallRecordCount = function(callRecordType,successCallback,errorCallback){
    argscheck.checkArgs('sfF', 'Telephony.findCallRecords', arguments);
    var success = function(result){
        successCallback(result);
    };
    exec(success, errorCallback, null, "Telephony", "getCallRecordCount", [callRecordType]);
};

module.exports = telephonyObj;
