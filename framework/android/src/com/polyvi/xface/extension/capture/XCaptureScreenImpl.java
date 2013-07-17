
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

package com.polyvi.xface.extension.capture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.view.View;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.util.XBase64;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XStringUtils;

public class XCaptureScreenImpl {
    private static final  String CLASS_NAME = XCaptureScreenImpl.class.getSimpleName();
    private static final  String DATE_FORMAT_TYPE = "yyyyMMdd_HHmmss";
    private static final  String TAG_DEFAULT_IMAGE_TYPE = ".jpeg";
    private static final  String MIME_PNG_TYPE = "image/png";
    private static final  String MIME_JPEG_TYPE = "image/jpeg";
    private static final int HIGHQUALITY = 100;
    /**返回的json结果*/
    private static final String TAG_CODE = "code";
    private static final String TAG_RESULT = "result";
    /**标示屏幕截图结果码*/
    public static enum ResultCode {
        SUCCESS,
        ARGUMENT_ERROR,
        IO_ERROR,
    };
    private Activity mActivity;
    private XCallbackContext mCallbackContext;
    private String mWorkspace;
    private XCaptureScreenOptions mOptions;
    private View mView;

    public XCaptureScreenImpl(Activity activity,
            XCallbackContext callbackContext,
            String workspace,
            XCaptureScreenOptions options,
            View view) {
        mActivity = activity;
        mCallbackContext = callbackContext;
        mWorkspace = workspace;
        mOptions = options;
        mView = view;
    }

    /**
     * 开始截图操作
     */
    public void startCaptureScreen() {
        if(null == mView){
            mCallbackContext.error(getResult(ResultCode.ARGUMENT_ERROR, "argument is invalid"));;
        }
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mView.setDrawingCacheEnabled(true);
                mView.buildDrawingCache();
                Bitmap bitmap = mView.getDrawingCache();
                onCaptured(bitmap);
            }
        });
    }

    private void onCaptured(Bitmap originBitmap) {
        try {
            /**获取截取到的图片*/
            Bitmap bitmap = getBitmap(originBitmap);
            if (null == bitmap) {
                mCallbackContext.error(getResult(ResultCode.ARGUMENT_ERROR, "argument is invalid"));
                return;
            }
            /**得到图片的绝对地址和MIMEType*/
            String path = getResolvePath(mOptions.getDestionationFile(), mWorkspace);
            if(XStringUtils.isEmptyString(path)) {
                mCallbackContext.error(getResult(ResultCode.IO_ERROR, "resolve path error"));
                return;
            }
            CompressFormat type = getMimeType(path);
            if(null == type) {
                mCallbackContext.error(getResult(ResultCode.ARGUMENT_ERROR, "argument is invalid"));
                return;
            }
            /** 得到图片数据 */
            int destinationType = mOptions.getDestinationType();
            if(destinationType == XCaptureScreenOptions.getTagFileUri()) {
                getFileUri(bitmap, path, type);
            } else {
                getDataUrl(bitmap, type);
            }
        } finally {
            mView.destroyDrawingCache();
        }
    }

    /**
     * 获取要截取的类型
     * @param path:文件的绝对路径
     */
    private CompressFormat getMimeType(String path) {
        String mimeType = XFileUtils.getMimeType(path);
        CompressFormat type = Bitmap.CompressFormat.JPEG;
        if(mimeType.equalsIgnoreCase(MIME_PNG_TYPE)) {
            type = Bitmap.CompressFormat.PNG;
        } else if(mimeType.equalsIgnoreCase(MIME_JPEG_TYPE)) {
            type = Bitmap.CompressFormat.JPEG;
        } else {
            return null;
        }
        return type;
    }

    /**
     * 获取截取到的图片
     * @param originBitmap 原始图片
     * @return
     */
    private Bitmap getBitmap(Bitmap originBitmap) {
        if(null == originBitmap) {
            return null;
        }
        /** 如果用户没有输入x和y坐标及高宽，则默认全屏 */
        if (mOptions.iSDefault()) {
            return originBitmap;
        }
        mOptions = getValidOptions(originBitmap);
        if (null == mOptions) {
            return null;
        }
        return Bitmap.createBitmap(originBitmap, mOptions.getX(),
                mOptions.getY(), mOptions.getWidth(), mOptions.getHeight());
    }


    /**
     * 得到合法的x和y坐标width，height
     */
    private XCaptureScreenOptions getValidOptions(Bitmap bitmap) {
        int captureRectX = mOptions.getX();
        int captureRectY = mOptions.getY();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        int width = mOptions.getWidth();
        int height = mOptions.getHeight();
        Rect captureRect = new Rect(0, 0, 0, 0);
        /**如果用户只是输入x y坐标，则默认认为以x，y为起点的截图区域*/
        if(mOptions.iSOnlyXAndYInput()) {
            captureRect = new Rect(captureRectX, captureRectY, bitmapWidth + captureRectX, bitmapHeight + captureRectY);
        } else {
            captureRect = new Rect(captureRectX, captureRectY, width + captureRectX, height + captureRectY);
        }
        Rect bitmapRect = new Rect(0, 0, bitmapWidth, bitmapHeight);
        if (captureRect.intersect(bitmapRect)) {
            mOptions.setX(captureRect.left);
            mOptions.setY(captureRect.top);
            mOptions.setWidth(captureRect.right - captureRect.left);
            mOptions.setHeight(captureRect.bottom - captureRect.top);
            /**如果宽高为负数 则参数不合法*/
            if(mOptions.getWidth() <= 0 || mOptions.getHeight() <= 0) {
                return null;
            }
            return mOptions;
        } else {
            return null;
        }
    }

    /**
     * 得到截屏的结果,以文件url地址返回
     */
    private void getFileUri(Bitmap bitmap, String savePath, CompressFormat mimeType){
        /** 将截屏数据输出到文件中 */
        mkDir(savePath);
        File pic = new File(savePath);
        String absPath = pic.getAbsolutePath();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(pic);
            bitmap.compress(mimeType, HIGHQUALITY, fos);
            fos.flush();
            fos.close();
            mCallbackContext.success(getResult(ResultCode.SUCCESS, pic.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            String msg = "File Not Found! Path is " + absPath;
            XLog.e(CLASS_NAME, msg);
            mCallbackContext.error(getResult(ResultCode.IO_ERROR, msg));
        } catch (IOException e) {
            String msg = e.getMessage();
            XLog.e(CLASS_NAME, msg);
            mCallbackContext.error(getResult(ResultCode.IO_ERROR, msg));
        }
    }

    /**
     * 得到截屏的结果,以图片数据的base64形式返回
     */
    private void getDataUrl(Bitmap bitmap, CompressFormat mimeType) {
        ByteArrayOutputStream fos = null;
        try {
            fos = new ByteArrayOutputStream();
            bitmap.compress(mimeType, HIGHQUALITY, fos);
            byte[] result = fos.toByteArray();
            if (null != fos) {
                fos.close();
            }
            mCallbackContext.success(getResult(ResultCode.SUCCESS, new String(XBase64.encode(result, XBase64.NO_WRAP))));
        } catch (IOException e) {
            String msg = "get data Exception:" + e.getMessage();
            XLog.e(CLASS_NAME, msg);
            mCallbackContext.error(getResult(ResultCode.IO_ERROR, msg));
        }
    }

    /**
     * 解析给定的路径
     *
     * @param savePath
     *            : 要保存的路径
     * @param workSpace
     *            : 应用的工作路径
     * @return 解析后得到的路径
     */
    private String getResolvePath(String savePath, String workSpace) {
        savePath = XStringUtils.isEmptyString(savePath.replace(" ", "")) ? getFileName()
                : savePath;
        String resolveSavePath = new XPathResolver(savePath, workSpace)
                .resolve();
        if (null == resolveSavePath) {
            return null;
        }
        /** 如果sdcard不存在但用户又想保存在sdcard上的话，返回null*/
        String sdcardPath = XFileUtils.getSdcardPath();
        if(null == sdcardPath && !XStringUtils.isEmptyString(savePath) && savePath.startsWith("file://sdcard")) {
            return null;
        }
        /**如果path是目录，则赋予默认文件名*/
        return isDirectory(resolveSavePath) ? new File(resolveSavePath, getFileName())
                .getAbsolutePath() : resolveSavePath;
    }

    /**
     * 根据截屏的时间来命名文件名.
     *
     * @param savePath
     *            传入的地址
     * @return 默认文件的名字
     */
    private String getFileName() {
        return new SimpleDateFormat(DATE_FORMAT_TYPE).format(Calendar
                .getInstance().getTime()) + TAG_DEFAULT_IMAGE_TYPE;
    }

    /**
     * 创建目录和文件
     *
     * @param savePath 要创建的目录
     * @return
     */
    private boolean mkDir(String savePath) {
        File dirFile = new File(savePath).getParentFile();
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return dirFile.mkdirs();
        }
        return true;
    }

    /**
     * 构造json结果
     */
    private JSONObject getResult(ResultCode code, String result) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(TAG_CODE, code.ordinal());
            obj.put(TAG_RESULT, result);
            return obj;
        }catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage());
        }
        return null;
    }

    /**
     * 如果url是否是文件目录
     * url输入格式形如：
     * file://sdcard/test/sdcard.jpg, 返回true
     * /test/workspace.jpg, 返回true
     * file://sdcard/test/, 返回false
     * file://sdcard/test, 返回false
     * file://sdcard/test., 返回false
     */
    private boolean isDirectory(String url) {
        /**截取"/"后的字段*/
        String fileName = new File(url).getName();
        if(XStringUtils.isEmptyString(fileName)) {
            return true;
        }
        int indexOfPoint = fileName.lastIndexOf(".");
        /**如果最后一个点找不到或者在url最后都是目录*/
        if(indexOfPoint < 0 || indexOfPoint + 1 == fileName.length()) {
            return true;
        }
        return false;
    }
}
