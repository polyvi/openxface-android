
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

package com.polyvi.xface.extension;

import org.json.JSONArray;
import org.json.JSONException;

public class XIdleWatcherExt extends XExtension {
    private static final String COMMAND_START = "start";
    private static final String COMMAND_STOP = "stop";
    //默认的等待无操作时间是5分钟
    private static final long DEFAULT_INTERVAL = 300000;
    //标识应用是否在后台运行
    private boolean mRunningInBackground = false;
    //用于标识应用是否切换到前台
    private int mCountNotifiedInBackground = 0;
    //用于记录等待时间
    private long mInterval = DEFAULT_INTERVAL;
    private Runnable mTask;
    private XCallbackContext mCallbackCtx;

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return false;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, final XCallbackContext callbackCtx) throws JSONException {
        mCallbackCtx = callbackCtx;
        if (COMMAND_START.equals(action)) {
            String timeout = args.getString(0);
            if (null != timeout) {
                mInterval = Long.parseLong(timeout) * 1000;
            }
            if (mInterval < 0) {
                return new XExtensionResult(
                        XExtensionResult.Status.ERROR);
            }
            mTask = new Runnable() {
                @Override
                public void run() {
                    if (isRunningInBackground()) {
                        // 处于后台状态
                        mCountNotifiedInBackground ++;
                    } else {
                        XExtensionResult result = new XExtensionResult(
                                XExtensionResult.Status.PROGRESS_CHANGING);
                        result.setKeepCallback(true);
                        mCallbackCtx.sendExtensionResult(result);
                    }
                }
            };
            mWebContext.getApplication().startIdleWatcher(mInterval, mTask);
        } else if (COMMAND_STOP.equals(action)) {
            mWebContext.getApplication().stopIdleWatcher();
        }
        XExtensionResult result = new XExtensionResult(
                XExtensionResult.Status.OK);
        result.setKeepCallback(true);
        return result;
    }

    /**
     * 实时获取是否在后台状态的值
     * @return
     */
    private boolean isRunningInBackground() {
        return mRunningInBackground;
    }

    @Override
    public void onPause() {
        //app已经运行在后台
        mRunningInBackground = true;
    }

    @Override
    public void onResume() {
        //app切换在前台
        //程序首次启动会走这里
        mRunningInBackground = false;
        if (mCountNotifiedInBackground > 0 ) {
            mCountNotifiedInBackground = 0;
            XExtensionResult result = new XExtensionResult(
                    XExtensionResult.Status.PROGRESS_CHANGING);
            result.setKeepCallback(true);
            mCallbackCtx.sendExtensionResult(result);
            //从后台切换到前台时，从新计时
            mWebContext.getApplication().startIdleWatcher(mInterval, mTask);
        }
    }

}
