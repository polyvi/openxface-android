
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

package com.polyvi.xface.extension.xmlhttprequest;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtensionResult;

/**
 * ajax请求监听器 执行js通知应用层
 */
public class XAjaxRequestListener {

	private static final int ON_ABORT = 0;
	private static final int ON_ERROR = 1;
	private static final String TAG_PROGRESS_EVENT_TYPE = "eventType";

	private static final String TAG_READYSTATE = "readyState";
	private static final String TAG_STATUS = "status";
	private static final String TAG_RESPONSE_TEXT = "responseText";
	private static final String TAG_HEADERS = "headers";

	private XCallbackContext mCallbackContext; // 回调上下文 执行js
	private XIAjaxDataInterface mDataInterface;

	public XAjaxRequestListener(XCallbackContext callbackContext) {
		mCallbackContext = callbackContext;
	}

	public void setAjaxDataInterface(XIAjaxDataInterface dataInterface) {
		mDataInterface = dataInterface;
	}

	/**
	 * ajax状态改变 调用该回调函数
	 */
	public void onReadyStateChanged() {
		sendSuccessJSMessage();
	}

	/**
	 * ajax abort调用该回调函数
	 */
	public void onAbort() {
		sendErrorJSMessage(ON_ABORT);
	}

	/**
	 * ajax数据到达 调用该回调函数
	 * 
	 * @param dataInterface
	 *            数据接口 通过该
	 */
	public void onDataReceived() {
		sendSuccessJSMessage();
	}

	/**
	 * ajax网络异常 调用该回调函数
	 * 
	 * @param exception
	 */
	public void onNetworkError() {
		sendErrorJSMessage(ON_ERROR);
	}

	/**
	 * 发送成功消息给js 成功回调对应js onreadystatechange事件
	 */
	private void sendSuccessJSMessage() {
		try {
			JSONObject jsonObj = getAjaxData();
			XExtensionResult result = new XExtensionResult(
					XExtensionResult.Status.OK, jsonObj);
			result.setKeepCallback(true);
			mCallbackContext.sendExtensionResult(result);
		} catch (JSONException e) {
			e.printStackTrace();
			return;

		}

	}

	/**
	 * 错误回调对应ajax进度事件类型 目前仅仅支持onerror onabort
	 */
	private void sendErrorJSMessage(int eventType) {
		try {
			JSONObject jsonObj = getAjaxData();
			jsonObj.put(TAG_PROGRESS_EVENT_TYPE, eventType);
			XExtensionResult result = new XExtensionResult(
					XExtensionResult.Status.ERROR, jsonObj);
			result.setKeepCallback(true);
			mCallbackContext.sendExtensionResult(result);
		} catch (JSONException e) {
			e.printStackTrace();
			return;

		}
	}

	/**
	 * 获取ajax相关数据
	 * 
	 * @return
	 */
	private JSONObject getAjaxData() throws JSONException {
		JSONObject jsonObj = new JSONObject();
		JSONObject headers = this.getHeaders();
		jsonObj.put(TAG_HEADERS, headers);
		jsonObj.put(TAG_RESPONSE_TEXT, mDataInterface.getResponseText());
		jsonObj.put(TAG_READYSTATE, mDataInterface.getReadyState());
		jsonObj.put(TAG_STATUS, mDataInterface.getStatus());
		return jsonObj;
	}

	private JSONObject getHeaders() throws JSONException {
		JSONObject obj = new JSONObject();
		Header[] responseHeaders = mDataInterface.getAllResponseHeader();
		if (null == responseHeaders) {
			return obj;
		}
		for (Header header : responseHeaders) {
			obj.put(header.getName(), header.getValue());
		}
		return obj;
	}
}
