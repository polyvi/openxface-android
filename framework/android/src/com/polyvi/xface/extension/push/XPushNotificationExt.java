
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

package com.polyvi.xface.extension.push;

import org.json.JSONArray;
import org.json.JSONException;

import android.provider.Settings;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.plugin.api.XIWebContext;

/**
 *dependent-libs: asmack.jar
 */
public class XPushNotificationExt extends XExtension {

    private static final String COMMAND_GETDEVICETOKEN = "getDeviceToken";

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return false;
    }

    @Override
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        super.init(extensionContext, webContext);
        XServiceManager serviceManager = new XServiceManager();
        if (serviceManager.isOpenPush()) {
            serviceManager.startService();
        }
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        XExtensionResult.Status status = XExtensionResult.Status.OK;
        String result = "";
        if (action.equals(COMMAND_GETDEVICETOKEN)) {
            return new XExtensionResult(status, getUuid());
        }
        return new XExtensionResult(status, result);
    }

    /**
     * 获得device的Universally Unique Identifier (UUID).
     */
    private String getUuid() {
        String uuid = Settings.Secure.getString(getContext()
                .getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        return uuid;
    }
}
