
/*
 Copyright 2012-2013, Polyvi Inc. (http://polyvi.github.io/openxface)
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

package com.polyvi.xface.extension.xmlhttprequest;

/**
 * 描述ajax请求异常信息
 * 
 */
public class XAjaxException extends Exception {

    private static final long serialVersionUID = 1L;

    private ErrorCode mErrorCode;

    /**
     * ajax执行对应的错误码
     * 
     */
    public enum ErrorCode {
        INVALID_STATE_ERR, INVALID_HEADER_VALUE, METHOD_NOT_SUPPORT, HTTP_REQUEST_ERROR
    };

    /**
     * ajax执行对应的错误信息
     */
    public static String[] ErrorMessages = new String[] { "Invalid Status",
            "Invalid header value", "Not Spport Method", "Network ERROR" };

    XAjaxException() {
        super();
    }

    XAjaxException(XAjaxException.ErrorCode errorCode) {
        mErrorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return ErrorMessages[mErrorCode.ordinal()];
    }

    /**
     * 获得错误码
     * 
     * @return
     */
    public int getErrorCode() {
        return mErrorCode.ordinal();
    }
}
