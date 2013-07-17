
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XConstant;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XUtils;

enum MediaType {
    VIDEO,
    AUDIO,
    IMAGE
}

public class XCaptureExt extends XExtension {

    private static final String CLASS_NAME = XCaptureExt.class.getSimpleName();

    /**  Capture 提供给js用户的接口名字*/
    private static final String COMMAND_GET_FORMATDATA = "getFormatData";
    private static final String COMMAND_CAPTURE_IMAGE = "captureImage";
    private static final String COMMAND_CAPTURE_AUDIO = "captureAudio";
    private static final String COMMAND_CAPTURE_VIDEO = "captureVideo";
    private static final String COMMAND_CAPTURE_SCREEN = "captureScreen";

    private static final String VIDEO_3GPP = "video/3gpp";
    private static final String AUDIO_3GPP = "audio/3gpp";

    private static final String VIDEO_TYPE = "video/";
    private static final String AUDIO_TYPE = "audio/";
    private static final String IMAGE_TYPE = "image/";

    private static final String PROP_HEIGHT = "height";
    private static final String PROP_WIDTH = "width";
    private static final String PROP_BITRATE = "bitrate";
    private static final String PROP_DURATION = "duration";
    private static final String PROP_CODECS = "codecs";
    private static final String PROP_NAME = "name";
    private static final String PROP_FULLPATH = "fullPath";
    private static final String PROP_TYPE = "type";
    private static final String PROP_LASTMODIFIEDDATE = "lastModifiedDate";
    private static final String PROP_SIZE = "size";
    private static final String PROP_CODE = "code";
    private static final String PROP_MESSAGE = "message";
    private static final String PROP_LIMIT = "limit";

    public static final String SYS_INTENT_NAME_CAPTURE_IMG = android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
    public static final String SYS_INTENT_NAME_CAPTURE_AUDIO = android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION;
    public static final String SYS_INTENT_NAME_CAPTURE_VIDEO = android.provider.MediaStore.ACTION_VIDEO_CAPTURE;
    private static final String SYS_PROP_DURATION = android.provider.MediaStore.EXTRA_DURATION_LIMIT;

    private enum errorCode {
        CAPTURE_INTERNAL_ERR,
        CAPTURE_APPLICATION_BUSY,
        CAPTURE_INVALID_ARGUMENT,
        CAPTURE_NO_MEDIA_FILES
    }

    private HashMap<XCallbackContext, ArrayList<JSONObject>> mResultMap;     // 用于存储每个 XJsCallback对象对应的capture结果

    public XCaptureExt() {
        mResultMap = new HashMap<XCallbackContext, ArrayList<JSONObject>>();
    }

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        mResultMap.put(callbackCtx, list);
        JSONObject options = args.optJSONObject(0);
        Long limit = (long) 0;
        int duration = 0;
        if (options != null) {
            limit = options.optLong(PROP_LIMIT, 1);
            duration = options.optInt(PROP_DURATION, 0);
        }
        if (action.equals(COMMAND_GET_FORMATDATA)) {
            try {
                JSONObject obj = getFormatData(args.getString(0), mWebContext.getWorkSpace(), args.getString(1));
                return new XExtensionResult(XExtensionResult.Status.OK, obj);
            } catch (JSONException e) {
                return new XExtensionResult(XExtensionResult.Status.ERROR);
            }
        } else if (action.equals(COMMAND_CAPTURE_IMAGE)) {
            captureImage(mWebContext, callbackCtx, limit);
        } else if (action.equals(COMMAND_CAPTURE_AUDIO)) {
            captureAudio(mWebContext, callbackCtx, limit);
        } else if (action.equals(COMMAND_CAPTURE_VIDEO)) {
            captureVideo(mWebContext, callbackCtx, limit, duration);
        } else if (action.equals(COMMAND_CAPTURE_SCREEN)) {
            XCaptureScreenOptions screenOptions = new XCaptureScreenOptions(args.optJSONObject(0));
            captureScreen(mWebContext, callbackCtx, screenOptions);
            XExtensionResult rx = new XExtensionResult(XExtensionResult.Status.NO_RESULT);
            rx.setKeepCallback(false);
            return rx;
        }
        XExtensionResult r = new XExtensionResult(XExtensionResult.Status.NO_RESULT);
        r.setKeepCallback(true);
        return r;
    }

    /**
     * 依据 mime type 提供多媒体文件数据.
     *
     * @param filePath
     *                  文件的绝对路径
     * @param appWorkspace
     *                 app的workspace
     * @param mimeType
     *                 media 类型
     * @return 一个 MediaFileData 对象
     */
    private JSONObject getFormatData(String filePath, String appWorkspace, String mimeType) {
        JSONObject obj = new JSONObject();
        String path = (new XPathResolver(filePath, appWorkspace)).resolve();
        String type = mimeType;
        try {
            // 设置默认值
            obj.put(PROP_HEIGHT, 0);
            obj.put(PROP_WIDTH, 0);
            obj.put(PROP_BITRATE, 0);
            obj.put(PROP_DURATION, 0);
            obj.put(PROP_CODECS, "");

            if (type == null || type.equals("")) {
                type = XFileUtils.getMimeType(path);
            }

            if (type.startsWith(IMAGE_TYPE)) {
                obj = getImageData(path, obj);
            } else if (type.startsWith(AUDIO_TYPE)) {
                obj = getAudioVideoData(path, obj, false);
            } else if (type.startsWith(VIDEO_TYPE)) {
                obj = getAudioVideoData(path, obj, true);
            }
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, "Error: setting media file data object");
        }
        return obj;
    }

    /**
     * 获得 Image 指定的属性.
     *
     * @param fileAbsPath 文件的绝对路径
     * @param obj 多媒体文件的数据
     * @return 一个表示多媒体文件数据的 JSONObject
     * @throws JSONException
     */
    private JSONObject getImageData(String fileAbsPath, JSONObject obj)
            throws JSONException {
        Bitmap bitmap = BitmapFactory.decodeFile(fileAbsPath);
        if (bitmap != null) {
            obj.put(PROP_HEIGHT, bitmap.getHeight());
            obj.put(PROP_WIDTH, bitmap.getWidth());
            bitmap.recycle();
        }
        return obj;
    }

    /**
     * 获得 audio 或者 video 指定的属性.
     *
     * @param fileAbsPath 文件的绝对路径
     * @param obj 多媒体文件数据
     * @param isVideo 是否是 video
     * @return 一个表示多媒体文件数据的 JSONObject
     * @throws JSONException
     */
    private JSONObject getAudioVideoData(String fileAbsPath, JSONObject obj, boolean isVideo)
            throws JSONException {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(fileAbsPath);
            player.prepare();
            obj.put(PROP_DURATION, player.getDuration() / XConstant.MILLISECONDS_PER_SECOND);
            if (isVideo) {
                obj.put(PROP_HEIGHT, player.getVideoHeight());
                obj.put(PROP_WIDTH, player.getVideoWidth());
            }
        } catch (IOException e) {
            XLog.e(CLASS_NAME, "Error: loading video file");
        }
        player.release();
        return obj;
    }

    /**
     * 为捕获图片建立一个 intent.
     *
     * @param app 执行扩展的app
     * @param jsCallback callback上下文环境
     * @param limit  capture的最大次数
     */
    public void captureImage(XIWebContext webContext, XCallbackContext callbackCtx, Long limit) {
        Intent intent = new Intent(SYS_INTENT_NAME_CAPTURE_IMG);
        String outputFile = createCaptureFile(webContext.getWorkSpace());
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(outputFile)));

        mExtensionContext.getSystemContext().startActivityForResult(
                        new XCaptureActResultListener(this, webContext, callbackCtx,
                                outputFile, limit, 0, MediaType.IMAGE),
                        intent, XUtils.genActivityRequestCode());
    }

    /**
     * 创建 img file.
     * @return
     */
    private String createCaptureFile(String workSpace) {
        long time = Calendar.getInstance().getTimeInMillis();
        File dir = new File(workSpace, "img");
        if(!dir.exists()) {
            dir.mkdir();
        }
        XFileUtils.setPermission(XFileUtils.ALL_PERMISSION, dir.getAbsolutePath());
        String picName = String.valueOf(time) + ".jpg";
        String path = new File(dir.getAbsolutePath(), picName).getAbsolutePath();
        return path;
    }

    /**
     * 为捕获音频建立一个 intent.
     *
     * @param app 执行扩展的app
     * @param jsCallback XJsCallback对象
     * @param limit  capture的最大次数
     */
    public void captureAudio(XIWebContext webContext, XCallbackContext callbackCtx, Long limit) {
        Intent intent = new Intent(SYS_INTENT_NAME_CAPTURE_AUDIO);
        mExtensionContext.getSystemContext().startActivityForResult(
                        new XCaptureActResultListener(this, webContext, callbackCtx, "", limit, 0, MediaType.AUDIO),
                        intent, XUtils.genActivityRequestCode());
    }

    /**
     * 为捕获视频建立一个 intent.
     *
     * @param app 执行扩展的app
     * @param callbackCtx js回调的上下文环境
     * @param limit  capture的最大次数
     * @param duration  capture的最大秒数
     */
    public void captureVideo(XIWebContext webContext, XCallbackContext callbackCtx, Long limit, int duration) {
        Intent intent = new Intent(SYS_INTENT_NAME_CAPTURE_VIDEO);
        intent.putExtra(SYS_PROP_DURATION, duration);
        mExtensionContext.getSystemContext().startActivityForResult(
                    new XCaptureActResultListener(this, webContext, callbackCtx, "", limit, duration, MediaType.VIDEO),
                    intent, XUtils.genActivityRequestCode());
    }

    /**
     * 为屏幕截图
     *
     * @param app 执行扩展的app
     * @param callbackCtx js回调的上下文环境
     */
    private void captureScreen(XIWebContext webContext,
            XCallbackContext callbackCtx, XCaptureScreenOptions options) {
        Activity activity = mExtensionContext.getSystemContext().getActivity();
        View view = (View) mWebContext.getApplication().getView();
        String workspace = mWebContext.getWorkSpace();
        new XCaptureScreenImpl(activity, callbackCtx,workspace, options, view).startCaptureScreen();
    }

    /**
     * 保存capture img结果.
     *
     * @param webContext  执行扩展的 app
     * @param callbackCtx 回调的上下文环境
     * @param absPath  capture文件的绝对路径
     * @param limit  capture的最大次数
     * @return 已保存的capture的个数
     */
    public void saveCaptureImgResult(XIWebContext webContext, XCallbackContext callbackCtx, String absPath, Long limit) {
        ArrayList<JSONObject> list = mResultMap.get(callbackCtx);
        list.add(createImgFile(absPath));
        mResultMap.put(callbackCtx, list);

        if (list.size() < limit) {
            captureImage(webContext, callbackCtx, limit);
        } else {
            sendbackResult(webContext, callbackCtx);
        }
        return;
    }

    /**
     * 保存capture audio结果.
     *
     * @param app  执行扩展的 app
     * @param callbackCtx js回调的上下文环境
     * @param mediaFileUri
     * @param limit  capture的最大次数
     */
    public void saveCaptureAudioResult(
            XIWebContext webContext, XCallbackContext callbackCtx, Uri mediaFileUri, Long limit) {
        ArrayList<JSONObject> list = mResultMap.get(callbackCtx);
        list.add(createAudioVideoFile(mediaFileUri));
        mResultMap.put(callbackCtx, list);

        if (list.size() < limit) {
            captureAudio(webContext, callbackCtx, limit);
        } else {
            sendbackResult(webContext, callbackCtx);
        }
        return;
    }

    /**
     * 保存capture video结果.
     *
     * @param webContext  执行扩展的 app
     * @param jsCallback  js回调的上下文环境
     * @param mediaFileUri
     * @param limit  capture的最大次数
     * @param duration  每次 capture 的最大长度
     */
    public void saveCaptureVideoResult(
            XIWebContext webContext, XCallbackContext callbackCtx, Uri mediaFileUri, Long limit, int duration) {
        ArrayList<JSONObject> list = mResultMap.get(callbackCtx);
        list.add(createAudioVideoFile(mediaFileUri));
        mResultMap.put(callbackCtx, list);

        if (list.size() < limit) {
            captureVideo(webContext, callbackCtx, limit, duration);
        } else {
            sendbackResult(webContext, callbackCtx);
        }
        return;
    }

    /**
     * 当一个 Activity 被用户取消时调用.
     *
     * @param webContext  执行扩展的app
     * @param jsCallback
     */
    public void activityResultCanceled(XIWebContext webContext, XCallbackContext callbackCtx) {
        ArrayList<JSONObject> list = mResultMap.get(callbackCtx);
        if (list.size() > 0) {
            sendbackResult(webContext, callbackCtx);    // 如果存在部分结果，仍然回传给用户
        } else {
            // 用户取消了操作
            sendbackError(webContext, callbackCtx, errorCode.CAPTURE_NO_MEDIA_FILES, "Canceled.");
        }
    }

    /**
     * 当一个 Activity 出现异常时调用.
     *
     * @param app  执行扩展的app
     * @param callbackCtx
     */
    public void activityResultError(XIWebContext webContext, XCallbackContext callbackCtx) {
        ArrayList<JSONObject> list = mResultMap.get(callbackCtx);
        if (list.size() > 0) {
            sendbackResult(webContext, callbackCtx);    // 如果存在部分结果，仍然回传给用户
        } else {
            sendbackError(webContext, callbackCtx, errorCode.CAPTURE_NO_MEDIA_FILES, "Did not complete!");
        }
    }

    /**
     * 创建一个 IMG 文件.
     *
     * @param path          image 文件的绝对路径
     * @return JSONObject   表示 image 文件的 JSONObject
     */
    private JSONObject createImgFile(String path){
        File fp = new File(path);
        return computeFileProp(fp);
    }

    /**
     * 创建一个 Audio 或者 Video 文件.
     *
     * @param mediaFileUri  media 文件的 Uri
     * @return JSONObject   表示 image 文件的 JSONObject
     */
    private JSONObject createAudioVideoFile(Uri mediaFileUri){
        XPathResolver pathResolver = new XPathResolver(mediaFileUri.toString(),
                null, getContext());
        String mediaStorePath = pathResolver.resolve();
        File fp = new File(mediaStorePath);
        JSONObject obj = computeFileProp(fp);

        try {
            // 对以".3gp" 或 ".3gpp" 格式的文件进行重新获取 type
            // 因为getMimeType函数无法获取到正确的类型
            if (fp.getAbsoluteFile().toString().endsWith(".3gp")
                    || fp.getAbsoluteFile().toString().endsWith(".3gpp")) {
                if (mediaFileUri.toString().contains("/audio/")) {
                    obj.put(PROP_TYPE, AUDIO_3GPP);
                } else {
                    obj.put(PROP_TYPE, VIDEO_3GPP);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * 计算文件相关属性.
     *
     * @param file  需要获取属性的文件对象
     * @return  已放入文件属性的 JSONObject
     */
    private JSONObject computeFileProp(File file) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(PROP_NAME, file.getName());
            obj.put(PROP_FULLPATH, XConstant.FILE_SCHEME + file.getAbsolutePath());
            obj.put(PROP_TYPE, XFileUtils.getMimeType(file.getAbsolutePath()));
            obj.put(PROP_LASTMODIFIEDDATE, file.lastModified());
            obj.put(PROP_SIZE, file.length());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * 将 capture 的结果回传给 JS.
     *
     * @param app 执行扩展的app
     * @param jsCallback XJsCallback对象
     */
    public void sendbackResult(XIWebContext webContext, XCallbackContext callbackCtx) {
        ArrayList<JSONObject> list = mResultMap.get(callbackCtx);
        Iterator<JSONObject> itor = list.iterator();
        JSONArray resultArray = new JSONArray();
        while (itor.hasNext()) {
            resultArray.put(itor.next());
        }

        callbackCtx.success(resultArray);
    }

    /**
     * 将 capture 过程中发生的错误回传给 JS.
     *
     * @param webContext 执行扩展的app
     * @param callbackCtx
     * @param code 错误码
     * @param message  错误信息
     */
    public void sendbackError(XIWebContext webContext, XCallbackContext callbackCtx, errorCode code, String message) {
        JSONObject errorObj = createErrorObject(code, message);
        callbackCtx.error(errorObj);
    }

    /**
     * 创建用于保存报错信息 JSONObject.
     *
     * @param code      错误码
     * @param message   错误信息
     * @return
     */
    private JSONObject createErrorObject(errorCode code, String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(PROP_CODE, code);
            obj.put(PROP_MESSAGE, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
