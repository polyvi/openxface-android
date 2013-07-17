
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XPathResolver;

public class XAdvancedFileTransferExt extends XExtension {
    private static final String CLASS_NAME = XAdvancedFileTransferExt.class
            .getSimpleName();

    private static final String ILLEGAL_ARGUMENT_EXCEPTION_NOT_IN_ROOT_DIR = "filePath is not in root directory";
    private static final String ILLEGAL_ARGUMENT_EXCEPTION_NAME_CONTAINS_COLON = "This file has a : in its name";

    private static final String COMMAND_DOWNLOAD = "download";
    private static final String COMMAND_UPLOAD = "upload";
    private static final String COMMAND_PAUSE = "pause";
    private static final String COMMAND_CANCEL = "cancel";

    private static final int FILE_NOT_FOUND_ERR = 1;
    private static final int INVALID_URL_ERR = 2;
    private static final int CONNECTION_ERR = 3;

    private static XFileTransferManager mFileTransferManager;

    public XAdvancedFileTransferExt() {
    }

    @Override
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        super.init(extensionContext, webContext);
        if (null == mFileTransferManager) {
            mFileTransferManager = new XFileTransferManager(getContext());
        }
    }

    @Override
    public void sendAsyncResult(String result) {

    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }

    @Override
    public void destroy() {
        // 退出引擎时停止所有的任务
        mFileTransferManager.stopAll();
    }

    @Override
    public void onPageStarted() {
        // 页面切换时暂停该app中的所有任务
        mFileTransferManager.stopAllByApp(mWebContext.getApplication().getAppId());
    }

    @Override
    public void onAppClosed() {
        // 退出app时暂停该app中的所有任务
        mFileTransferManager.stopAllByApp(mWebContext.getApplication().getAppId());
    }

    @Override
    public void onAppUninstalled() {
        // 卸载app时移除FileTransferManager中该app对应的Map<String, XIFileTransfer>
        Map<String, Map<String, XIFileTransfer>> hashFileTransfers = mFileTransferManager
                .getHashMapFileTransfer();
        Map<String, XIFileTransfer> fileTransfers = hashFileTransfers
                .get(mWebContext.getApplication().getAppId());
        if (null != fileTransfers) {
            hashFileTransfers.remove(mWebContext.getApplication().getAppId());
        }

        // 卸载app时移除FileTransferManager中该app对应的XFileTransferRecorder
        Map<String, XFileTransferRecorder> recorders = mFileTransferManager
                .getFileTransferRecorders();
        recorders.remove(mWebContext.getApplication().getAppId());
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        String source = null;
        String target = null;
        String appId = mWebContext.getApplication().getAppId();
        try {
            if (action.equals(COMMAND_DOWNLOAD)) {
                source = args.getString(0);
                target = args.getString(1);
                download(mWebContext, source, target, callbackCtx);
                XLog.d(CLASS_NAME, "*** About to return a result from download");
                return new XExtensionResult(XExtensionResult.Status.NO_RESULT);
            } else if (action.equals(COMMAND_UPLOAD)) {
                source = args.getString(0);
                target = args.getString(1);
                upload(mWebContext, source, target, callbackCtx);
                XLog.d(CLASS_NAME, "*** About to return a result from upload");
                return new XExtensionResult(XExtensionResult.Status.NO_RESULT);
            } else if (action.equals(COMMAND_PAUSE)) {
                source = args.getString(0);
                mFileTransferManager.pause(appId, source);
                return new XExtensionResult(XExtensionResult.Status.OK);
            } else if (action.equals(COMMAND_CANCEL)) {
                source = args.getString(0);
                target = args.getString(1);
                boolean isUpload = args.getBoolean(2);
                if (isUpload) {
                    mFileTransferManager.cancel(appId, source, null,
                            COMMAND_UPLOAD);
                } else {
                    target = new File(mWebContext.getWorkSpace(), target)
                            .getAbsolutePath();
                    mFileTransferManager.cancel(mWebContext.getApplication()
                            .getAppId(), source, target, COMMAND_DOWNLOAD);
                }
                return new XExtensionResult(XExtensionResult.Status.OK);
            }

        } catch (FileNotFoundException e) {
            XLog.e(CLASS_NAME, e.getMessage());
            JSONObject error = createFileTransferError(FILE_NOT_FOUND_ERR,
                    source, target);
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (IllegalArgumentException e) {
            XLog.e(CLASS_NAME, e.getMessage());
            JSONObject error = createFileTransferError(INVALID_URL_ERR, source,
                    target);
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (IOException e) {
            XLog.e(CLASS_NAME, e.getMessage());
            JSONObject error = createFileTransferError(CONNECTION_ERR, source,
                    target);
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage(), e);
            return new XExtensionResult(XExtensionResult.Status.JSON_EXCEPTION);
        }
        return new XExtensionResult(XExtensionResult.Status.INVALID_ACTION);
    }

    /**
     * 发起一个下载请求
     *
     * @param app
     *            当前应用
     * @param url
     *            服务器的URL
     * @param filePath
     *            设备上的路径
     * @param callbackCtx
     *            回调上下文环境
     */
    public void download(XIWebContext webContext, String url, String filePath,
            XCallbackContext callbackCtx) throws FileNotFoundException, IOException {
        // 目前下载目的地址只支持http协议
        if (!url.startsWith("http://")) {
            throw new IllegalArgumentException();
        }
        if (filePath.contains(":")) {
            throw new FileNotFoundException(
                    ILLEGAL_ARGUMENT_EXCEPTION_NAME_CONTAINS_COLON);
        }
        File file = new File(webContext.getWorkSpace(), filePath);

        if (!XFileUtils.isFileAncestorOf(webContext.getWorkSpace(),
                file.getCanonicalPath())) {
            throw new FileNotFoundException(
                    ILLEGAL_ARGUMENT_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        file.getParentFile().mkdirs();

        mFileTransferManager.addFileTranferTask(url, file.getCanonicalPath(),
                mExtensionContext, callbackCtx, webContext, COMMAND_DOWNLOAD);
    }

    public void upload(XIWebContext webContext, String filePath, String server,
            XCallbackContext callbackCtx) throws FileNotFoundException,
            IllegalArgumentException {

        // 目前上传目的地址只支持http协议
        if (!server.startsWith("http://")) {
            throw new IllegalArgumentException();
        }
        // 在此处检测文件是否存在，防止由于文件不存在运行很多不该执行的代码
        XPathResolver pathResolver = new XPathResolver(filePath,
                webContext.getWorkSpace());
        String absoluteFilePath = pathResolver.resolve();
        if (null != absoluteFilePath) {
            File uploadFile = new File(absoluteFilePath);
            if (uploadFile.exists()) {
                mFileTransferManager.addFileTranferTask(filePath, server,
                        mExtensionContext, callbackCtx, webContext,
                        COMMAND_UPLOAD);
            } else {
                throw new FileNotFoundException();
            }
        } else {
            throw new FileNotFoundException();
        }
    }

    /**
     * 创建FileTransferError对象
     *
     * @param errorCode
     *            错误码
     * @return JSONObject 包含错误的JSON对象
     */
    private JSONObject createFileTransferError(int errorCode, String source,
            String target) {
        JSONObject error = null;
        try {
            error = new JSONObject();
            error.put("code", errorCode);
            error.put("source", source);
            error.put("target", target);
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage(), e);
        }
        return error;
    }
}
