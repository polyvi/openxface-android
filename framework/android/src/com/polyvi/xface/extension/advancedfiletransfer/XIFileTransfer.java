
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

package com.polyvi.xface.extension.advancedfiletransfer;

import com.polyvi.xface.extension.XCallbackContext;

public interface XIFileTransfer {
    /**
     * 执行文件传输(上传和下载)
     * @param  callbackCtx  回调上下文环境
     */
    public void transfer(XCallbackContext callbackCtx);

    /**
     * 暂停文件传输
     */
    public void pause();
}
