
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

package com.polyvi.xface.extension.video;

import android.app.Activity;
import android.content.Intent;

import com.polyvi.xface.extension.XActivityResultListener;
import com.polyvi.xface.extension.XCallbackContext;

public class XVideoActResultListener implements XActivityResultListener {

    private XCallbackContext mCallbackCtx = null;

    public XVideoActResultListener(XCallbackContext callbackCtx) {
        mCallbackCtx = callbackCtx;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // 从实测来看，无论是正常播放完视频还是中途退出，都返回Activity.RESULT_CANCELED
        if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == XVideoExt.PLAY_VIDEO_REQUEST_CODE) {
                mCallbackCtx.success("play video end");
            }
        } else {
            mCallbackCtx.error("play video error");
        }
    }

}
