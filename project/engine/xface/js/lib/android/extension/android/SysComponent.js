
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
 * @module app
 */

/**
 * 该类定义一些常量，用于标识系统中的特定程序（Android）<br/>
 * 该类的用法参考{{#crossLink "App/startSystemComponent"}}{{/crossLink}}
 * @class SysComponent
 * @platform Android
 * @since 3.0.0
 */
function SysComponent() {
}

/**
 * 用于标识VPN设置界面（Android）
 * @property VPN
 * @type Number
 * @static
 * @final
 * @platform Android
 * @since 3.0.0
 */
SysComponent.VPN = 0;

/**
 * 用于标识网络设置界面（Android）
 * @property WIRELESS
 * @type Number
 * @static
 * @final
 * @platform Android
 * @since 3.0.0
 */
SysComponent.WIRELESS = 1;

/**
 * 用于标识GPS设置界面（Android）
 * @property GPS
 * @type Number
 * @static
 * @final
 * @platform Android
 * @since 3.0.0
 */
SysComponent.GPS = 2;

module.exports = SysComponent;