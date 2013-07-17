
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

package com.polyvi.xface.extension.audio;

import com.polyvi.xface.extension.XCallbackContext;

/**
 * 此监听器用于监听 Audio 中音频播放状态的变化
 *
 */
public class XAudioStatusChangeListener {

    private static final String JSStatusChangeFuncStr = "xFace.require('xFace/extension/Media').onStatus";

    private XCallbackContext mCallbackCtx;

    public XAudioStatusChangeListener(
            XCallbackContext callbackCtx) {
        this.mCallbackCtx = callbackCtx;
    }

    /**
     * audio 状态发生改变时的回调
     *
     * @param playerId  JS 端的 player id
     * @param msgId     message id
     * @param status    状态
     */
    synchronized public void onStatusChange(String playerId, int msgId, int status) {
        String msg = String.format(JSStatusChangeFuncStr + "('%s', %d, %d);", playerId, msgId, status);
        sendMsg(msg);
    }

    /**
     * 发生错误时的回调
     *
     * @param playerId  JS 端的 player id
     * @param msgId        message id
     * @param errorMsg     错误信息
     */
    public void onError(String playerId, int msgId, int errorMsg) {
        String msg = String.format(JSStatusChangeFuncStr + "('%s', %d, {\"code\":%d});", playerId, msgId, errorMsg);
        sendMsg(msg);
    }

    /**
     * 向 JS 发送 duration 属性值
     *
     * @param playerId  JS 端的 player id
     * @param msgId     message id
     * @param duration
     */
    public void onGetDuration(String playerId, int msgId, float duration) {
        String msg = String.format(JSStatusChangeFuncStr + "('%s', %d, %f);", playerId, msgId, duration);
        sendMsg(msg);
    }

    /**
     * 向 JS 发送 position 属性值
     *
     * @param playerId  JS 端的 player id
     * @param msgId     message id
     * @param position
     */
    public void onGetPosition(String playerId, int msgId, int position) {
        String msg = String.format(JSStatusChangeFuncStr + "('%s', %d, %d);", playerId, msgId, position);
        sendMsg(msg);
    }

    /**
     * 向 JS 端发送状态变化信息
     *
     * @param jsMsg  JS 串
     */
    private void sendMsg(String jsMsg) {
        mCallbackCtx.sendExtensionResult(jsMsg);
    }
}
