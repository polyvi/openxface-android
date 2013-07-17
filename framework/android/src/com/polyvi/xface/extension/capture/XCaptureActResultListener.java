
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.polyvi.xface.extension.XActivityResultListener;
import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.plugin.api.XIWebContext;

public class XCaptureActResultListener implements XActivityResultListener {

    private XCaptureExt mCaptureExt;
    private XIWebContext mWebContext = null;
    private XCallbackContext mCallbackCtx = null;
    private String mMediaStoreAbsPath = "";   // 保存 capture 结果的文件绝对路径
    private Long mLimit;                      // 用于记录允许最大的 capture 的次数，由外部 JS 指定
    private int mDuration;                    // 用于限制单次 capture video 时的最大时长
    private MediaType mMediaType;

    public XCaptureActResultListener(
            XCaptureExt ext, XIWebContext webContext, XCallbackContext callbackCtx,
            String fileAbsPath, Long limit, int duration, MediaType mediaType) {
        mCaptureExt = ext;
        mWebContext = webContext;
        mCallbackCtx = callbackCtx;
        mMediaStoreAbsPath = fileAbsPath;
        mLimit = limit;
        mDuration = duration;
        mMediaType = mediaType;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (mMediaType ==  MediaType.IMAGE) {
                mCaptureExt.saveCaptureImgResult(mWebContext, mCallbackCtx, mMediaStoreAbsPath, mLimit);
            } else if (mMediaType == MediaType.AUDIO) {
                Uri data = intent.getData();
                mCaptureExt.saveCaptureAudioResult(mWebContext, mCallbackCtx, data, mLimit);
            } else if (mMediaType == MediaType.VIDEO) {
                Uri data = intent.getData();
                mCaptureExt.saveCaptureVideoResult(mWebContext, mCallbackCtx, data, mLimit, mDuration);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            mCaptureExt.activityResultCanceled(mWebContext, mCallbackCtx);
        } else {
            mCaptureExt.activityResultError(mWebContext, mCallbackCtx);
        }
    }
}
