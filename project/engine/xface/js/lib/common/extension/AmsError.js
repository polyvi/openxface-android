
/*
 Copyright 2012-2013, Polyvi Inc. (http://www.xface3.com)
 This program is distributed under the terms of the GNU General Public License.

 This file is part of xFace.

 xFace is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 xFace is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with xFace.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @module ams
 */

/**
 * 该类定义了AMS的错误码，相关用法参考{{#crossLink "AMS"}}{{/crossLink}} （Android, iOS, WP8）<br/>
 * @class AmsError
 * @static
 * @platform Android,iOS,WP8
 * @since 3.0.0
 */
function AmsError(error) {
 /**
  * 应用操作的错误码，用于表示具体的应用操作的错误(Android, iOS, WP8)<br/>
  * 其取值范围参考{{#crossLink "AmsError"}}{{/crossLink}}中定义的常量
  * @example
        function errorCallback(amsError) {
            if( amsError.code == AmsError.NO_SRC_PACKAGE) {
                print("Package does not exist");
            }
        }
  * @property code
  * @type Number
  * @platform Android, iOS, WP8
  * @since 3.0.0
  */
  this.code = error || null;
}

// ams error codes

/**
 * 应用安装包不存在
 * @property NO_SRC_PACKAGE
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.0.0
 */
AmsError.NO_SRC_PACKAGE = 1;

/**
 * 应用已经存在
 * @property APP_ALREADY_EXISTED
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.0.0
 */
AmsError.APP_ALREADY_EXISTED =  2;

/**
 * IO异常错误
 * @property IO_ERROR
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.0.0
 */

AmsError.IO_ERROR = 3;
/**
 * 用于标识没有找到待操作的目标应用
 * @property NO_TARGET_APP
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.0.0
 */

AmsError.NO_TARGET_APP = 4;
/**
 * 应用包中的配置文件不存在
 * @property NO_APP_CONFIG_FILE
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.0.0
 */
AmsError.NO_APP_CONFIG_FILE = 5;


/**
 * 启动的应用不存在
 * @property APP_NOT_FOUND
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.1.0
 */
AmsError.APP_NOT_FOUND = 7;

/**
 * 应用已经启动
 * @property APP_ALREADY_RUNNING
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.1.0
 */
AmsError.APP_ALREADY_RUNNING = 8;

/**
 * 应用入口错误
 * @property APP_ENTRY_ERR
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.1.0
 */
AmsError.APP_ENTRY_ERR = 9;

/**
 * 启动native应用错误
 * @property START_NATIVE_APP_ERR
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.1.0
 */
AmsError.START_NATIVE_APP_ERR = 10;

/**
 * 未知错误
 * @property UNKNOWN
 * @type Number
 * @final
 * @platform Android,iOS,WP8
 * @since 3.0.0
 */
AmsError.UNKNOWN = 11;

module.exports = AmsError;
