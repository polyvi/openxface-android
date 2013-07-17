
/*
 This file was modified from or inspired by Apache Cordova.

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied. See the License for the
 specific language governing permissions and limitations
 under the License.
*/

package com.polyvi.xface.extension;

import org.json.JSONArray;
import org.json.JSONObject;

import com.polyvi.xface.util.XBase64;

public class XExtensionResult {

	private final int mStatus;
	/** < 执行js代码后的状态值 */
	private final int messageType;
	private String mMessage;
	/** < 执行js代码后对应的状态消息 */
	private boolean mKeepCallBack;
	/** < 该值用于通知js层是否保留js代码执行后的回调函数 */
	private String mEncodedMessage;

	public static final int MESSAGE_TYPE_STRING = 1;
	public static final int MESSAGE_TYPE_JSON = 2;
	public static final int MESSAGE_TYPE_NUMBER = 3;
	public static final int MESSAGE_TYPE_BOOLEAN = 4;
	public static final int MESSAGE_TYPE_NULL = 5;
	public static final int MESSAGE_TYPE_ARRAYBUFFER = 6;
	public static final int MESSAGE_TYPE_BINARYSTRING = 7;

	// 表示执行js代码后的状态
	public enum Status {
		NO_RESULT, PROGRESS_CHANGING, OK, CLASS_NOT_FOUND_EXCEPTION, ILLEGAL_ACCESS_EXCEPTION, INSTANTIATION_EXCEPTION, MALFORMED_URL_EXCEPTION, IO_EXCEPTION, INVALID_ACTION, JSON_EXCEPTION, ERROR
	};

	// 执行js代码状态所对应的描述串
	public static String[] StatusMessages = new String[] { "No Result",
			"Progress Changing", "OK", "Class Not Found", "Illegal access",
			"Instantiation", "Malformed", "IO error", "Invalid action",
			"JSON error", "Error" };

	/**
	 * 构造函数
	 *
	 * @param status
	 *            扩展结果的状态值
	 */
	public XExtensionResult(Status status) {
		this(status, XExtensionResult.StatusMessages[status.ordinal()]);
	}

	/**
	 * 构造函数
	 *
	 * @param status
	 *            扩展结果的状态值
	 * @param message
	 *            扩展结果具体的数据
	 */
	public XExtensionResult(Status status, JSONArray message) {
		this.mStatus = status.ordinal();
		this.messageType = MESSAGE_TYPE_JSON;
		this.mEncodedMessage = message.toString();
	}

	/**
	 * 构造函数
	 *
	 * @param status
	 *            扩展结果的状态值
	 * @param message
	 *            扩展结果具体的数据
	 */
	public XExtensionResult(Status status, JSONObject message) {
		this.mStatus = status.ordinal();
		this.messageType = MESSAGE_TYPE_JSON;
		this.mEncodedMessage = message.toString();
	}

	/**
	 * 构造函数
	 *
	 * @param status
	 *            扩展结果的状态值
	 * @param message
	 *            扩展结果具体的数据
	 */
	public XExtensionResult(Status status, String message) {
		this.mStatus = status.ordinal();
		this.messageType = message == null ? MESSAGE_TYPE_NULL
				: MESSAGE_TYPE_STRING;
		this.mMessage = message;
	}

	public XExtensionResult(Status status, int i) {
		this.mStatus = status.ordinal();
		this.messageType = MESSAGE_TYPE_NUMBER;
		this.mEncodedMessage = "" + i;
	}

	public XExtensionResult(Status status, float f) {
		this.mStatus = status.ordinal();
		this.messageType = MESSAGE_TYPE_NUMBER;
		this.mEncodedMessage = "" + f;
	}

	public XExtensionResult(Status status, boolean b) {

		this.mStatus = status.ordinal();
		this.messageType = MESSAGE_TYPE_BOOLEAN;
		this.mEncodedMessage = Boolean.toString(b);
	}

	public XExtensionResult(Status status, byte[] data) {
		this(status, data, false);
	}

	public XExtensionResult(Status status, byte[] data, boolean binaryString) {
		this.mStatus = status.ordinal();
		this.messageType = binaryString ? MESSAGE_TYPE_BINARYSTRING
				: MESSAGE_TYPE_ARRAYBUFFER;
		this.mEncodedMessage = XBase64.encodeToString(data, XBase64.NO_WRAP);
	}

	/**
	 * 获得结果具体的信息
	 * 
	 * @return
	 */
	public String getMessage() {
		if (mEncodedMessage == null) {
			mEncodedMessage = JSONObject.quote(mMessage);
		}
		return this.mEncodedMessage;
	}

	/**
	 * 如果 messageType == MESSAGE_TYPE_STRING, 返回消息字符串. 否则返回 null
	 */
	public String getStrMessage() {
		return mMessage;
	}

	public int getMessageType() {
		return messageType;
	}

	/**
	 * 获取是否继续保存js用户设置的回调标志
	 * 
	 * @return
	 */
	public boolean getKeepCallback() {
		return this.mKeepCallBack;
	}

	/**
	 * 设置是否保存js设置的回调函数标志
	 * 
	 * @param keepCallBack
	 */
	public void setKeepCallback(boolean keepCallBack) {
		this.mKeepCallBack = keepCallBack;
	}

	/**
	 * 获得js执行结果的json串
	 * 
	 * @return
	 */
	public String getJSONString() {
		return "{status:" + this.mStatus + ",message:" + this.getMessage()
				+ ",keepCallback:" + this.mKeepCallBack + "}";
	}

	/**
	 * 获得扩展结果的执行状态
	 * 
	 * @return
	 */
	public int getStatus() {
		return mStatus;
	}

	/**
	 * 表示js对应的本地扩展执行成功后，需要执行的js语句
	 *
	 * @param callbackId
	 *            js用户设置的回调函数对应的回调id
	 * @return 可执行的js语句
	 */
	public String toSuccessCallbackString(String callbackId) {
		return "xFace.callbackSuccess('" + callbackId + "',"
				+ this.getJSONString() + ");";
	}

	/**
	 * 表示js对应的本地扩展执行失败后，需要执行的js语句
	 *
	 * @param callbackId
	 *            js用户设置的回调函数对应的回调id
	 * @return 可执行的js语句
	 */
	public String toErrorCallbackString(String callbackId) {
		return "xFace.callbackError('" + callbackId + "',"
				+ this.getJSONString() + ");";
	}

	/**
	 * 表示js在执行扩展的过程中，需要执行的js语句
	 *
	 * @param callbackId
	 *            js用户设置的回调函数对应的回调id
	 * @return 可执行的js语句
	 */
	public String toStatusChangeCallbackString(String callbackId) {
		return "xFace.callbackStatusChanged('" + callbackId + "',"
				+ this.getJSONString() + ");";
	}
}
