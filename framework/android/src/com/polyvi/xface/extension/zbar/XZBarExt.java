
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

package com.polyvi.xface.extension.zbar;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;

import com.polyvi.xface.extension.XActivityResultListener;
import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.util.XUtils;

/**
 *dependent-libs: armeabi/libiconv.so,armeabi/libzbarjni.so,zbar.jar
 */
public class XZBarExt extends XExtension implements XActivityResultListener{

    private static final String COMMAND_START = "start";
    private static final int ZBAR_REQUEST_CODE = XUtils.genActivityRequestCode();        //ZBar startActivityForResult的标志位
    private static boolean mLock = false;        //不能同时调用多次这个扩展，需要锁。

    /*
     * TODO:在XExtension中抽象一个函数出来 用于根据API level判断是否可以执行
     * 该扩展的action isSupportedInCurApiLevel（String action），
     * 类似于isSync的设计，然后统一到ExtensionManager中调用
     */
    //Camera.setDisplayOrientation函数在android 2.1上不支持，故暂不支持android 2.1
    private static final int apiLevel = android.os.Build.VERSION.SDK_INT;
    private XCallbackContext mCallbackCtx = null;

    @Override
    public XExtensionResult exec( String action,
           JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        Status status = Status.ERROR;
        if(COMMAND_START.equals(action) && !mLock && apiLevel >= 8)
        {
            mLock = true;
            mCallbackCtx = callbackCtx;
            Intent intent = new Intent();
            intent.setClass(getContext(), XCameraActivity.class);
            mExtensionContext.getSystemContext().startActivityForResult(this, intent, ZBAR_REQUEST_CODE);
            status = Status.NO_RESULT;
        }
        return new XExtensionResult(status);
    }

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        mLock = false;
        if(requestCode == ZBAR_REQUEST_CODE && mCallbackCtx !=null && mWebContext !=null) {
            if (resultCode ==  Activity.RESULT_OK && intent != null) {
                //返回的条形码数据
                String code = intent.getStringExtra("Code");
                mCallbackCtx.success(code);
            } else {
                mCallbackCtx.error();
            }
        }
    }
}
