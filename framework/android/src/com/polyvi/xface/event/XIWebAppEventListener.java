
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

package com.polyvi.xface.event;

import com.polyvi.xface.view.XAppWebView;

public interface XIWebAppEventListener {

    /**
     * webview页面加载完毕通知
     *
     * @param view
     *            加载页面的webview
     */
    public void onPageLoadingFinished(XAppWebView view);

    /**
     * js初始化完毕的通知
     *
     * @param view
     *            加载js的appview
     */
    public void onJsInitFinished(XAppWebView view);

    /**
     * 重写backbutton的通知
     *
     * @param view
     *            要通知的view
     * @param overrideBackbutton
     *            是否重写backbutton
     * */
    public void onOverrideBackbutton(XAppWebView view, boolean overrideBackbutton);

    /**
     * 重写volume button down的通知
     *
     * @param view
     *            要通知的view
     * @param overrideVolumeButtonDown
     *            是否重写volume button down
     * */
    public void onOverrideVolumeButtonDown(XAppWebView view,
            boolean overrideVolumeButtonDown);

    /**
     * 重写volume button up的通知
     *
     * @param view
     *            要通知的view
     * @param overrideVolumeButtonUp
     *            是否重写volume button up
     * */
    public void onOverrideVolumeButtonUp(XAppWebView view,
            boolean overrideVolumeButtonUp);

    /**
     * 请求关闭当前的Application
     *
     * @pram viewId 需要关闭app对应的viewid
     */
    public void onCloseApplication(int viewId);

    /**
     *  页面开始加载
     * @param view
     */
    public void onPageStarted(XAppWebView view);

    /**
     * 处理app消息
     *
     * @param view
     *            发送消息的app对应的view
     * @param msgData
     *            消息数据
     */
    public void onXAppMessageReceived(XAppWebView view, String msgData);
}
