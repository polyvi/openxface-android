
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.plugin.api.XIWebContext;


public class XFileTransferManager {
    private static final String COMMAND_DOWNLOAD = "download";

    /**
     * 为每个app创建一个Map<String, XIFileTransfer>,第一个key代表appId, Map<String,
     * XIFileTransfer>表示为每个source创建一个XIFileTransfer,
     * 这里的key在下载时表示服务器地址，上传是表示要上传的文件地址
     */
    private Map<String, Map<String, XIFileTransfer>> mHashMapFileTransfers = new HashMap<String, Map<String, XIFileTransfer>>();

    /** 为每个app创建一个XFileTransferRecorder，因为给个应用有自己单独的配置文件，这里的key代表appId */
    private Map<String, XFileTransferRecorder> mFileTransferRecorders = new HashMap<String, XFileTransferRecorder>();

    private Context mContext;
    public XFileTransferManager(Context context) {
        mContext = context;
    }

    public XFileTransferManager() {
    }

    /**
     * 当文件传输完成后移除XIFileTransfer
     * @param source  下载时表示服务器地址，上传是表示要上传的文件地址
     */
    public void removeFileTranferTask(String appId, String source) {
        Map<String, XIFileTransfer> fileTransfers = mHashMapFileTransfers.get(appId);
        if (fileTransfers != null) {
            fileTransfers.remove(source);
        }
    }

    /**
     * 当有文件传输任务发起时，增加一个传输任务
     * @param source            下载时表示服务器地址，上传是表示要上传的文件地址
     * @param target            下载时表示存储下载文件的本地地址，上传是表示要上传的服务器地址
     * @param extensionContext  XExtensionContext对象
     * @param callbackCtx       回调上下文环境
     * @param webContext        当前应用
     * @param type              传输的类型(上传或下载两种)
     */
    public void addFileTranferTask(String source, String target, XExtensionContext extensionContext,
            XCallbackContext callbackCtx, XIWebContext webContext, String type) {

        Map<String, XIFileTransfer> fileTransfers = mHashMapFileTransfers.get(webContext.getApplication().getAppId());
        if(fileTransfers == null) {
            fileTransfers = new HashMap<String, XIFileTransfer>();
        }
        XIFileTransfer fileTransfer = getFileTransfer(fileTransfers, source, target, extensionContext, webContext, type);

        if(!mHashMapFileTransfers.containsValue(fileTransfers)) {
            mHashMapFileTransfers.put(webContext.getApplication().getAppId(), fileTransfers);
        }
        fileTransfer.transfer(callbackCtx);
    }

    /**
     * 获取XIFileTransfer对象，如果Map<String, XIFileTransfer>中有就直接获取，没有就创建
     * @param fileTransfers     存储XIFileTransfer对象的Map
     * @param source            下载时表示服务器地址，上传是表示要上传的文件地址
     * @param target            下载时表示存储下载文件的本地地址，上传是表示要上传的服务器地址
     * @param extensionContext  XExtensionContext对象
     * @param webContext        当前应用
     * @param type              传输的类型(上传或下载两种)
     */
    private XIFileTransfer getFileTransfer(Map<String, XIFileTransfer> fileTransfers, String source, String target,
            XExtensionContext extensionContext, XIWebContext webContext, String type) {
        XFileTransferRecorder fileTransferRecorder = mFileTransferRecorders.get(webContext.getApplication().getAppId());
        if (fileTransferRecorder == null) {
            fileTransferRecorder = new XFileTransferRecorder(webContext);
            mFileTransferRecorders.put(webContext.getApplication().getAppId(), fileTransferRecorder);
        }

        XIFileTransfer fileTransfer = fileTransfers.get(source);
        if (fileTransfer == null) {
            if (type.equals(COMMAND_DOWNLOAD)) {
                fileTransfer = new XFileDownloader(mContext,source, target,
                        extensionContext, webContext.getApplication(), fileTransferRecorder, this);
            } else {
                fileTransfer = new XFileUploader(source, target,
                        extensionContext, webContext, fileTransferRecorder, this);
            }
            fileTransfers.put(source, fileTransfer);
        }
        return fileTransfer;
    }

    /**
     * 暂停指定的文件传输任务
     * @param source    下载时表示服务器地址，上传是表示要上传的文件地址
     */
    public void pause(String appId, String source) {
        Map<String, XIFileTransfer> fileTransfers = mHashMapFileTransfers.get(appId);
        if (fileTransfers != null) {
            XIFileTransfer fileTransfer = fileTransfers.get(source);
            if (fileTransfer != null) {
                fileTransfer.pause();
            }
        }
    }

    /**
     * 停止某个app中的所有文件传输任务
     */
    public void stopAllByApp(String appId) {
        Map<String, XIFileTransfer> fileTransfers = mHashMapFileTransfers.get(appId);
        if (fileTransfers != null) {
            for (XIFileTransfer fileTransfer : fileTransfers.values()) {
                fileTransfer.pause();
            }
        }
    }

    /**
     * 停止所有app中的所有任务
     */
    public void stopAll() {
        for (Map<String, XIFileTransfer> fileTransfers : mHashMapFileTransfers.values()) {
            for (XIFileTransfer fileTransfer : fileTransfers.values()) {
                fileTransfer.pause();
            }
        }
    }

    /**
     * 取消指定的文件传输任务
     * @param appId     当前app的id
     * @param source    下载时表示服务器地址，上传是表示要上传的文件地址
     * @param target    下载时表示存储下载文件的本地地址，上传是表示要上传的服务器地址
     */
    public void cancel(String appId, String source, String target, String type) {
        pause(appId, source);
        removeFileTranferTask(appId, source);
        XFileTransferRecorder recorder = mFileTransferRecorders.get(appId);
        if (type.equals(COMMAND_DOWNLOAD)) {
            if (null != recorder) {
                recorder.deleteDownloadInfo(source);
            }
            File file = new File(target);
            if (file.exists()) {
                file.delete();
            }
        } else {
            if (null != recorder) {
                recorder.deleteUploadInfo(source);
            }
        }

    }

    public Map<String, Map<String, XIFileTransfer>> getHashMapFileTransfer() {
        return mHashMapFileTransfers;
    }

    public Map<String, XFileTransferRecorder> getFileTransferRecorders() {
        return mFileTransferRecorders;
    }
}
