
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

import com.polyvi.xface.extension.XExtensionContext;

/**
 * 该类主要用于启动push的service
 */
public final class XServiceManager {

    private Context mContext;

    public static final String PACKAGE_NAME = "packageName";

    public static final String HOST = "host";

    public static final String PORT = "port";

    private static  boolean isServiceRunning = false;

    public XServiceManager(XExtensionContext extensionContext) {
        XNotificationService notificationService = new XNotificationService();
        notificationService.setExtensionContext(extensionContext);
        mContext = extensionContext.getSystemContext().getContext();
    }

    /**
     * 启动push的service
     */
    public void startService() {
        Thread serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = getIntent();
                mContext.startService(intent);
                isServiceRunning = true;
            }
        });
        serviceThread.start();
    }

    /**
     * 启动push的service
     */
    public void startService(final String host,final String port) {
        Thread serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = getIntent(host,port);
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
     * 获取service的意图
     * @return service的意图
     */
    private Intent getIntent(String host,String port) {
        Intent intent = new Intent(mContext, XNotificationService.class);
        intent.putExtra(PACKAGE_NAME, mContext.getPackageName());
        intent.putExtra(HOST,host);
        intent.putExtra(PORT, port);
        return intent;
    }

    /**
     * 停止service
     */
    public void stopService() {
        if(isServiceRunning) {
            Intent intent = getIntent();
            mContext.stopService(intent);
            isServiceRunning = false;
        }
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
