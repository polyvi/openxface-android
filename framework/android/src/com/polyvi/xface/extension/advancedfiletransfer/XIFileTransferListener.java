
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

public interface XIFileTransferListener {
    /**
     * 文件传输成功回调
     */
    public void onSuccess();

    /**
     * 文件输出失败回调
     * @param errorCode 失败错误码
     */
    public void onError(int errorCode);

    /**
     * 更新传输进度
     * @param completeSize 已输出的大小
     * @param totalSize    要传输的总大小(下载时没有用到该参数，可以直接传0)
     */
    public void onProgressUpdated(int completeSize, long totalSize);
}
