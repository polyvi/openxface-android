
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

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XUtils;

public class XVideoExt extends XExtension {

    /**  Video 提供给js用户的接口名字*/
    private static final String COMMAND_PLAY = "play";

    public static final int PLAY_VIDEO_REQUEST_CODE = XUtils.genActivityRequestCode();     // play video 的 request code

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
        XExtensionResult.Status status = XExtensionResult.Status.NO_RESULT;

        if (action.equals(COMMAND_PLAY)) {
            play(args.getString(0), callbackCtx);
        }

        return new XExtensionResult(status);
    }

    private void play(String filePath, XCallbackContext callbackCtx) {
        XPathResolver pathResolver = new XPathResolver(filePath, mWebContext.getWorkSpace());
        String path = pathResolver.resolve();
        XPathResolver.Scheme scheme = pathResolver.getSchemeType();
        Boolean isPathValid = false;

        //判断路径的合法性
        if (null != path) {
            if (scheme == XPathResolver.Scheme.NONE
             || scheme == XPathResolver.Scheme.FILE
             || scheme == XPathResolver.Scheme.CONTENT) {//本地文件，检测文件是否存在
                File file = new File(path);
                if(file.exists())
                {
                    //开放文件的读权限
                    XFileUtils.setPermission(XFileUtils.READABLE_BY_OTHER, path);
                    isPathValid = true;
                }
            } else {//若路径不是本地文件，则判断为有效路径
                isPathValid = true;
            }
        }

        if(isPathValid) {
             Intent intent = new Intent(Intent.ACTION_VIEW);
             intent.setDataAndType(pathResolver.getUri(), "video/*");
             mExtensionContext.getSystemContext().startActivityForResult(
                     new XVideoActResultListener(callbackCtx),
                     intent, PLAY_VIDEO_REQUEST_CODE);
        } else {
            callbackCtx.error("video file not found error");
        }
    }
}
