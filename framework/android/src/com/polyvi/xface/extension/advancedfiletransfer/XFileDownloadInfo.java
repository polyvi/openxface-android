
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

/** 该类用于记录下载的具体信息（包括下载的地址，下载文件的总大小以及下载完成了的大小,
 *  这些数据将记录到配置文件中用于断点续传）*/
public class XFileDownloadInfo {

    /** 要下载的文件总大小 */
    private int mTotalSize;

    /** 已下载的大小 */
    private int mCompleteSize;

    /** 下载地址 */
    private String mUrl;

    public XFileDownloadInfo(int totalSize, int completeSize, String url) {
        super();
        mTotalSize = totalSize;
        mCompleteSize = completeSize;
        mUrl = url;
    }

    public int getCompleteSize() {
        return mCompleteSize;
    }

    public void setCompleteSize(int completeSize) {
        mCompleteSize = completeSize;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public int getTotalSize() {
        return mTotalSize;
    }

    public void setTotalSize(int totalSize) {
        mTotalSize = totalSize;
    }

    public boolean isDownloadCompleted() {
		return mTotalSize == mCompleteSize;
	}

    @Override
    public String toString() {
        return "DownloadInfo [mTotalSize=" + mTotalSize + ", mCompeleteSize="
                + mCompleteSize + ", mUrl=" + mUrl + "]";
    }
}
