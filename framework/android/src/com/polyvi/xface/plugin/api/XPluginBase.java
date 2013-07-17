
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

package com.polyvi.xface.plugin.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.polyvi.xface.extension.XActivityResultListener;
import com.polyvi.xface.extension.XExtensionContext;

/**
 * 所有外部插件基类
 *
 */
public class XPluginBase implements XActivityResultListener {

	protected XExtensionContext mExtContext;
	private XIWebContext mApp;

	public XPluginBase() {

	}

	/**
	 * 初始化
	 *
	 * @param ctx
	 */
	public void initialize(XExtensionContext ctx, XIWebContext app) {
		mExtContext = ctx;
		mApp = app;
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

	}


	/**
	 * 执行js函数
	 *
	 * @param jsonStr
	 *            需要执行的js语句
	 */
	final public void callJavaScriptFunction(String jsonStr) {
		mApp.sendJavascript(jsonStr);
	}

	/**
	 * 启动系统activity 并返回结果
	 *
	 * @param intent
	 * @param reqeustCode
	 */
	final public void startActivityForResult(Intent intent, int reqeustCode) {
		mExtContext.getSystemContext().startActivityForResult(this, intent,
				reqeustCode);
	}

	/**
	 * 启动系统activity
	 *
	 * @param intent
	 */
	final public void startActivity(Intent intent) {
		getContext().startActivity(intent);
	}

	/**
	 * 当系统抛出pause事件的时候被调用
	 *
	 */
	public void onPause() {
	}

	/**
	 * 当系统抛出resume事件的时候被调用
	 *
	 */
	public void onResume() {
	}

	/**
	 * 当系统抛出destroy事件的时候被调用
	 */
	public void onDestroy() {
	}

	/**
	 * 当一个消息被发送给扩展的时候被调用
	 */
	public Object onMessage(String id, Object data) {
		return null;
	}

	/**
	 * 获取context
	 */
	public Context getContext() {
		return mExtContext.getSystemContext().getContext();
	}

	/**
	 * 获取context
	 */
	public Activity getActivity() {
		return mExtContext.getSystemContext().getActivity();
	}

	/**
	 * 获得app的工作空间
	 */
	protected String getAppWorksacpe() {
		return mApp.getWorkSpace();
	}

	/**
	 * 对单引号进行处理， 否则会导致js报错
	 *
	 * @param str
	 * @return
	 */
	protected String handleSingleQuotes(String str) {

		if (str.contains("'")) {
			str = str.replaceAll("'", "\\\\'");
		}
		return str.toString();
	}

	/**
	 * 对转义字符进行处理,目前处理了\n和\"， 否则会导致js报错
	 *
	 * @param str
	 * @return
	 */
	protected String handleEscapeCharacters(String str) {
		if (str.contains("\n")) {
			str = str.replaceAll("\n", "\\\\n");
		}
		if (str.contains("\"")) {
			str = str.replaceAll("\"", "\\\\\"");
		}
		return str.toString();
	}
}
