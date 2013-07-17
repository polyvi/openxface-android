
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
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.polyvi.xface.app.XApplication;
import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;

public class XFileDownloader implements XIFileTransferListener, XIFileTransfer {

    private static final String CLASS_NAME = XFileDownloader.class.getSimpleName();
    private static final int TIME_OUT_MILLISECOND = 5000;

    /** 定义三种下载的状态：初始化状态，正在下载状态，暂停状态 */
    private static final int INIT = 1;
    private static final int DOWNLOADING = 2;
    private static final int PAUSE = 3;
    private int mState = INIT;

    private static final int CONNECTION_ERR = 3;

    /**定义下载文件的划分倍数和单位文件大小*/
    private static final int DIVIDE_SIZE_TWO   = 2;
    private static final int DIVIDE_SIZE_TEN    = 10;
    private static final int DIVIDE_SIZE_TWENTY = 20;
    private static final int SIZE_KB = 1024;

    /**标示文件不存在*/
    private static final int FILE_NOT_EXIST = 0;

    /**标示temp文件后缀*/
    private static final String TEMP_FILE_SUFFIX = ".temp";

    /**网络断了的时候进行重新连接次数*/
    private static final int RETRY = 3;

    /**定义下载重连时间为1秒*/
    private static final int RETRY_INTERVAL = 1000;

    /**定义下载缓冲区大小*/
    private int mBufferSize;

    /** 下载器网络标识 */
    private String mUrl;

    /** 存储下载文件的本地路径 */
    private String mLocalFilePath;

    /** native端js回调的上下文环境 */
    private XCallbackContext mCallbackCtx;

    /** 当前的应用 */
    private XApplication mApp;

    /** 已下载的具体信息 */
    private XFileDownloadInfo mDownloadInfo;

    /** 操作配置文件的对象 */
    private XFileTransferRecorder mFileTransferRecorder;

    /** 下载管理器 */
    private XFileTransferManager mFileTransferManager;

    private Context mContext;

    private CookieSyncManager mCookieSyncManager;

    public XFileDownloader(Context context,String url, String localFilePath, XExtensionContext extensionContext,
            XApplication app, XFileTransferRecorder recorder, XFileTransferManager manager) {
        init(context,url, localFilePath, extensionContext, app, recorder, manager);
    }

    /** 初始化方法 */
    private void init(Context context,String url, String localFilePath,XExtensionContext extensionContext,
            XApplication app, XFileTransferRecorder recorder, XFileTransferManager manager) {
        mUrl = url;
        mLocalFilePath = localFilePath;
        mApp = app;
        mFileTransferManager = manager;
        mFileTransferRecorder = recorder;
        mContext = context;
        mCookieSyncManager = CookieSyncManager.createInstance(mContext);
    }

    /**
     * 初始化下载信息(如果是第一次下载，执行创建本地文件，获取文件的总大小以及在配置文件中添加该条记录，
     * 如果不是第一次下载，则从配置文件中取出已经下载了的信息，完成断点续传)
     */
    private void initDownloadInfo() {
            int totalSize = 0;
            if (isFirst(mUrl)) {
                HttpURLConnection connection= null;
                try {
                    URL url = new URL(mUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(TIME_OUT_MILLISECOND);
                    connection.setRequestMethod("GET");
                    System.getProperties().setProperty("http.nonProxyHosts",url.getHost());
                    //设置cookie数据
                    setCookieProperty(connection, mUrl);
                    if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
                        totalSize = connection.getContentLength();
                        if (-1 != totalSize) {
                            mDownloadInfo = new XFileDownloadInfo(totalSize, 0,
                                    mUrl);
                            // 保存mDownloadInfo中的数据到配置文件
                            mFileTransferRecorder.saveDownloadInfo(mDownloadInfo);
                        } else {
                            XLog.e(CLASS_NAME, "cannot get totalSize");
                        }
                        // 如果第一次下的时候存在temp文件则删除之
                        File file = new File(mLocalFilePath + TEMP_FILE_SUFFIX);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                } catch (IOException e) {
                    XLog.e(CLASS_NAME, e.getMessage());
                }
                finally{
                       if( null != connection) {
                           connection.disconnect();
                       }
                }
            } else {
                // 得到配置文件中已有的url的下载器的具体信息
                mDownloadInfo = mFileTransferRecorder.getDownloadInfo(mUrl);
                totalSize = mDownloadInfo.getTotalSize();
                mDownloadInfo.setCompleteSize(getCompleteSize(mLocalFilePath + TEMP_FILE_SUFFIX));
            }
            mBufferSize = getSingleTransferLength(totalSize);
    }

    /**
     * 判断是否是第一次 下载
     */
    private boolean isFirst(String url) {
        return !mFileTransferRecorder.hasDownloadInfo(url);
    }

    @Override
    public void transfer(XCallbackContext callbackCtx) {
        initDownloadInfo();
        if (mState == DOWNLOADING) {
            return;
        }
        mCallbackCtx = callbackCtx;
        if (null == mDownloadInfo) {
            onError(CONNECTION_ERR);
        } else {
            setState(DOWNLOADING);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpURLConnection connection = null;
                    RandomAccessFile randomAccessFile = null;
                    InputStream is = null;
                    int retry = RETRY;
                    //TODO:以后重连次数可能从配置文件中读取
                    do {
                        int completeSize = mDownloadInfo.getCompleteSize();
                        try {
                            URL url = new URL(mUrl);
                            connection = (HttpURLConnection) url
                                    .openConnection();
                            connection.setConnectTimeout(TIME_OUT_MILLISECOND);
                            connection.setRequestMethod("GET");
                            // 设置范围，格式为Range：bytes x-;
                            connection.setRequestProperty("Range", "bytes="
                                    + completeSize + "-");
                            //设置cookie
                            setCookieProperty(connection,mUrl);
                            // 文件未下载成功先加个.temp标示
                            randomAccessFile = new RandomAccessFile(
                                    mLocalFilePath + TEMP_FILE_SUFFIX, "rwd");
                            randomAccessFile.seek(completeSize);
                            // 将要下载的文件写到保存在保存路径下的文件中
                            is = connection.getInputStream();
                            byte[] buffer = new byte[mBufferSize];
                            int length = -1;
                            while ((length = is.read(buffer)) != -1) {
                                try {
                                    randomAccessFile.write(buffer, 0, length);
                                } catch (Exception e) {
                                    retry = -1;
                                    break;
                                }
                                completeSize += length;
                                onProgressUpdated(completeSize, 0);
                                if (PAUSE == mState) {
                                    break;
                                }
                            }
                            if (mDownloadInfo.isDownloadCompleted()) {
                                // 文件下载成功后去掉.temp标示
                                renameFile(mLocalFilePath + TEMP_FILE_SUFFIX,
                                        mLocalFilePath);
                                onSuccess();
                                break;
                            }
                        } catch (IOException e) {
                            if (retry <= 0) {
                                onError(CONNECTION_ERR);
                                XLog.e(CLASS_NAME, e.getMessage());
                            }
                            // 网络异常,睡1秒超时再连接
                            try {
                                Thread.sleep(RETRY_INTERVAL);
                            } catch (InterruptedException ex) {
                                XLog.e(CLASS_NAME,"sleep be interrupted",ex);
                            }
                        } finally {
                            try {
                                if (null != is) {
                                    is.close();
                                }
                                if (null != randomAccessFile) {
                                    // new URL可能报异常，这种情况下randomAccessFile为null
                                    randomAccessFile.close();
                                }
                                if (null != connection) {
                                    // new URL可能报异常，这种情况下connection为null
                                    connection.disconnect();
                                }
                            } catch (IOException e) {
                                XLog.e(CLASS_NAME, e.getMessage());
                            }
                        }
                    }while ((DOWNLOADING == mState) && (0 < retry--));
                }
            }).start();
        }
    }

    private synchronized void setState(int state) {
        mState = state;
    }

    @Override
    public void pause() {
        setState(PAUSE);
    }

    @Override
    public void onSuccess() {
        mFileTransferRecorder.deleteDownloadInfo(mUrl);
        mFileTransferManager.removeFileTranferTask(mApp.getAppId(), mUrl);
        setState(INIT);
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj = XFileUtils.getEntry(mApp.getWorkSpace(), new File(mLocalFilePath));
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage());
        }
        mCallbackCtx.success(jsonObj);
    }

    @Override
    public void onError(int errorCode) {
        setState(INIT);
        String fullPath = null;
        String workspace = mApp.getWorkSpace();
        if(mLocalFilePath.equals(workspace)) {
            fullPath = File.separator;
        }
        else {
            fullPath = mLocalFilePath.substring(workspace.length());
        }
        JSONObject error = new JSONObject();
        try {
            error.put("code", errorCode);
            error.put("source", mUrl);
            error.put("target", fullPath);
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage());
        }

        mCallbackCtx.error(error);
    }

    @Override
    public void onProgressUpdated(int completeSize, long totalSize) {
        mDownloadInfo.setCompleteSize(completeSize);
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("loaded", completeSize);
            jsonObj.put("total", mDownloadInfo.getTotalSize());
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage());
        }
        XExtensionResult result = new XExtensionResult(XExtensionResult.Status.PROGRESS_CHANGING, jsonObj);
        result.setKeepCallback(true);
        mCallbackCtx.sendExtensionResult(result);
    }

    /**
     * 把下载分成几次更新会使进度条更新更平滑。<br/>
     * 获取每次要上传文件块的大小 。<br/>
     * 如果文件大小不超过1k，则分成2次更新。<br/>
     * 如果文件大小在1k-1M之间，则分成10次更新。<br/>
     * 如果文件大小在1M-10M之间，则分成20次更新。<br/>
     * 如果文件大小超过10M，则每次下载2M。<br/>
     * */
    private int getSingleTransferLength(int totalSize) {
        // 文件总大小
        int totalLength = totalSize;
        //如果文件小于100字节则直接一次更新进度条
        if (totalLength < SIZE_KB / DIVIDE_SIZE_TEN) {
            return SIZE_KB / DIVIDE_SIZE_TEN;
        } else if (totalLength < SIZE_KB) {
            return totalLength / DIVIDE_SIZE_TWO;
        } else if (totalLength < SIZE_KB * SIZE_KB) {
            return totalLength / DIVIDE_SIZE_TEN;
        } else if (totalLength < DIVIDE_SIZE_TEN * SIZE_KB * SIZE_KB) {
            return totalLength / DIVIDE_SIZE_TWENTY;
        } else {
            return DIVIDE_SIZE_TWO * SIZE_KB * SIZE_KB;
        }
    }

    /**
     * 文件重命名
     *
     * @param oldName:文件旧名字
     * @param newName:文件新名字
     */
    private void renameFile(String oldName,String newName) {
        File file = new File(oldName);
        file.renameTo(new File(newName));
    }

    /**
     * 获取指定文件大小，如果文件不存在则返回-1
     *
     * @param fileName:文件名字
     */
    private int getCompleteSize(String fileName) {
        File file = new File(fileName);
        if(file.exists()){
            return (int)file.length();
        }
        return FILE_NOT_EXIST;
    }

    /**
     * 设置connection的Cookie
     * @param connection   Http连接
     * @param domain       cookie对应的域
     */
    private void setCookieProperty(HttpURLConnection connection, String domain) {
        //Add cookie support
        mCookieSyncManager.startSync();
        String cookie = CookieManager.getInstance().getCookie(domain);
        if(cookie != null)
        {
          connection.setRequestProperty("cookie", cookie);
        }
    }
}
