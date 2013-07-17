
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

import android.content.Context;
import android.content.Intent;

/**
 * 该类主要用于启动push的service
 */
public final class XServiceManager {

    private Context mContext;

    public static final String PACKAGE_NAME = "packageName";

    /**
     * 启动push的service
     */
    public void startService() {
        Thread serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = getIntent();
                mContext.startService(intent);
            }
        });
        serviceThread.start();
    }

    /**
     * 获取service的意图
     * @return service的意图
     */
    private Intent getIntent() {
        Intent intent = new Intent(mContext, XNotificationService.class);
        intent.putExtra(PACKAGE_NAME, mContext.getPackageName());
        return intent;
    }

    /**
     * 停止service
     */
    public void stopService() {
        Intent intent = getIntent();
        mContext.stopService(intent);
    }

    /**
     * 在程序启动的时候判断是否需要push
     * @return true:开启push,false:不开启push
     */
    public boolean isOpenPush() {
        XNotificationService notificationService = new XNotificationService();
        return notificationService.isOpenPush(mContext);
    }
}
