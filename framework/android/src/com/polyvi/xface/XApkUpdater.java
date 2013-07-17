
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

package com.polyvi.xface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.polyvi.xface.util.XConstant;
import com.polyvi.xface.util.XLog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * 负责apk的更新
 *
 */
public class XApkUpdater  extends AsyncTask<String, Void, Boolean>{

    /** buffer无效长度 */
    public static final int BUFFER_INVALID_LEN = -1;

    private static final String CLASS_NAME = XApkUpdater.class.getSimpleName();;

    /** activity运行上下文 */
    private Context mContext;

    private String mDownloadAddress;

    public XApkUpdater(Context ctx) {
        mContext = ctx;
    }

    /**
     * 后台获取最新版本的apk下载地址
     *
     * @return 是否获取到apk下载地址，这个值会传给onPostExecute
     */
    @Override
    protected Boolean doInBackground(String... serverAddress) {
        mDownloadAddress = checkNewVersion(serverAddress[0], getCurrentVersionCode());
        return ((null == mDownloadAddress)? false : true);
    }

    /**
     * 提示用户，调用浏览器下载
     *
     * @param isDownload
     *                 是否需要下载，这个值为doInBackground的返回值
     * @return
     */
    @Override
    protected void onPostExecute(Boolean isDownload) {
        super.onPostExecute(isDownload);
        if (isDownload) {
            notifyHasNewVersion();
        }
    }

    /**
     * 通知有最新的版本，询问用户是否新版本的升级
     */
    private void notifyHasNewVersion() {
        //TODO 规范化提示信息，显示新版本的发布日志
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle("软件更新");
        dialog.setMessage("发现最新版本, 是否更新?");
        dialog.setPositiveButton("更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(null != mDownloadAddress) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri uri = Uri.parse(mDownloadAddress);
                    intent.setData(uri);
                    mContext.startActivity(intent);
                    }
            }
        });

        dialog.setNegativeButton("暂不更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });

        dialog.create();
        dialog.show();
    }

    /**
     * 检测是否有新版本的apk
     *
     * @param serverAddress
     *                      服务器地址
     * @param int currentVersionCode
     *                      当前程序的版本号
     * @return 有新版本的apk则返回apk的下载地址，否则返回null
     */
    private String checkNewVersion(String serverAddress, int currentVersionCode) {
        serverAddress = serverAddress + "?platform=android&currentVersionCode=" + currentVersionCode;
        String downloadAddress = null;
        try {
            URL myURL = new URL(serverAddress);
            URLConnection urlConnection = myURL.openConnection();
            // 设定连接超时30秒
            urlConnection.setConnectTimeout(30 * XConstant.MILLISECONDS_PER_SECOND);
            // 使用InputStream，从URLConnection读取数据
            InputStream iStream = urlConnection.getInputStream();

            byte buffer[] = new byte[64];
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();
            int len = BUFFER_INVALID_LEN;
            while ((len  = iStream.read(buffer)) != BUFFER_INVALID_LEN) {
                oStream.write(buffer, 0, len);
            }
            downloadAddress = oStream.toString();

            iStream.close();
            oStream.close();
        } catch (IOException  e) {
            XLog.e(CLASS_NAME, "Error when check new version: " + e.getMessage(), e);
            e.printStackTrace();
        }
        return downloadAddress;
    }

    /**
     * 获得当前程序的版本号
     *
     * @return 当前程序的版本号
     */
    private int getCurrentVersionCode() {
        int versionCode = -1;
        try {
            PackageManager  packageManager = mContext.getPackageManager();
            String packageName = mContext.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            versionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return versionCode;
    }
}
