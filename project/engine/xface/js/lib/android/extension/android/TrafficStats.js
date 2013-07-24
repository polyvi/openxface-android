
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

﻿
 /**
 * TrafficStats模块提供流量统计功能
 * @module trafficStats
 * @main trafficStats
 */

 /**
  *TrafficStats模块提供流量统计功能 (Android) <br/>
  *直接使用xFace.TrafficStats对象来获取流量的相关信息，获取的流量是从应用启动到调用接口其间的网络流量，
  * @class TrafficStats
  * @platform Android
  * @since 3.0.0
  */
    var exec = require('xFace/exec'),
        argscheck = require('xFace/argscheck'),
        TrafficStats = function() {};

/**
 * 获取2G和3G网络的网络流量
  @example
        xFace.getMobileTraffic( function(trafficData)
        {
            alert(trafficData);
        },null);
 * @method getMobileTraffic
 * @param {Function} successCallback 成功回调函数
 * @param {String} successCallback.trafficData 网络流量,单位为KB
 * @param {Function} [errorCallback]   失败回调函数
 * @platform Android
 * @since 3.0.0
 */
TrafficStats.prototype.getMobileTraffic = function(successCallback, errorCallback){
    argscheck.checkArgs('fF','TrafficStats.getMobileTraffic', arguments);
    exec(successCallback, errorCallback, null, "TrafficStats", "getMobileTraffic", []);
};

/**
 * 获取Wifi网络流量
  @example
        xFace.getWifiTraffic(
        function(trafficData)
        {
            alert(trafficData);
        },null);
 * @method getWifiTraffic
 * @param {Function} successCallback 成功回调函数
 * @param {String} successCallback.trafficData 网络流量,单位为KB
 * @param {Function} [errorCallback]   失败回调函数
 * @platform Android
 * @since 3.0.0
 */
TrafficStats.prototype.getWifiTraffic = function(successCallback, errorCallback){
    argscheck.checkArgs('fF','TrafficStats.getWifiTraffic', arguments);
    exec(successCallback, errorCallback, null, "TrafficStats", "getWifiTraffic", []);
};

module.exports = new TrafficStats();