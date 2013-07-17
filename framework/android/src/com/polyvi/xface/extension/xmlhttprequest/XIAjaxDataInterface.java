
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

import org.apache.http.Header;

/**
 *  获取ajax相关数据的接口
 *
 */
public interface XIAjaxDataInterface {
    /**
     * 获取ajax请求状态
     * 
     * @return
     */
    public int getReadyState();

    /**
     * 获得http状态
     * 
     * @return
     */
    public int getStatus();

    /**
     * 获得ajax的响应内容
     * 
     * @return
     */
    public String getResponseText();
    
    /**
     * 获得http所有的头部
     * @return
     */
    public Header[] getAllResponseHeader();
}
