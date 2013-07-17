
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

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.util.XLog;

public class XAudioExt extends XExtension {
    private static final String CLASS_NAME = "XAudioExt";

    /**  Audio 提供给js用户的接口名字*/
    private static final String COMMAND_PLAY = "play";
    private static final String COMMAND_STOP = "stop";
    private static final String COMMAND_PAUSE = "pause";
    private static final String COMMAND_SEEKTO = "seekTo";
    private static final String COMMAND_RELEASE = "release";
    private static final String COMMAND_GETCURRENTPOSITION = "getCurrentPosition";
    private static final String COMMAND_STARTRECORDING = "startRecording";
    private static final String COMMAND_STOPRECORDING = "stopRecording";
    private static final String COMMAND_SETVOLUME = "setVolume";

    private HashMap<String,XAudioImpl> mPlayersMap;    // 存在多 app 时，根据 appId 保存不同 app 对应的 map

    // TODO:处理电话接入时，暂停正在播放的audio;电话挂断后恢复被暂停的audio

    public XAudioExt() {
        mPlayersMap = new HashMap<String ,XAudioImpl>();
    }

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        if (action.equals(COMMAND_GETCURRENTPOSITION)) {
            return false;
        }
        return true;
    }

    @Override
    public void onPageStarted() {
        destroy();
    }

    @Override
    public void destroy() {
        if (!mPlayersMap.isEmpty()) {
                for (XAudioImpl audio : mPlayersMap.values()) {
                    audio.destroy();
            }
            mPlayersMap.clear();
        }
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        String appId = mWebContext.getApplication().getAppId();
        XExtensionResult.Status status = XExtensionResult.Status.NO_RESULT;
        if (action.equals(COMMAND_PLAY)) {
            XAudioStatusChangeListener listener = new XAudioStatusChangeListener(
                    callbackCtx);
            play(args.getString(0), args.getString(1), mWebContext.getWorkSpace(), listener);
        } else if (action.equals(COMMAND_STOP)) {
            stop(args.getString(0));
        } else if (action.equals(COMMAND_PAUSE)) {
            pause(args.getString(0));
        } else if (action.equals(COMMAND_SEEKTO)) {
            seekTo(args.getString(0), args.getInt(1));
        } else if (action.equals(COMMAND_RELEASE)) {
            boolean ret = release(args.getString(0));
            return new XExtensionResult(status, ret);
        } else if (action.equals(COMMAND_GETCURRENTPOSITION)) {
            int pos = getCurrentPosition(args.getString(0), appId);
            return new XExtensionResult(XExtensionResult.Status.OK, pos);
        } else if (action.equals(COMMAND_STARTRECORDING)) {
            XAudioStatusChangeListener listener = new XAudioStatusChangeListener(
                    callbackCtx);
            startRecording(args.getString(0), args.getString(1), mWebContext.getWorkSpace(), listener);
        } else if (action.equals(COMMAND_STOPRECORDING)) {
            stopRecording(args.getString(0));
        } else if (action.equals(COMMAND_SETVOLUME)) {
            setVolume(args.getString(0), Float.parseFloat(args.getString(1)));
        }

        return new XExtensionResult(status);
    }

    /**
     * Start 或者 resume 音频文件.
     *
     * @param playerId          audio player 对象的 id
     * @param fileAbsPath       音频文件的绝对路径
     * @param workspacePath     workspcae 路径
     */
    private void play(String playerId, String fileAbsPath,
            String workspacePath, XAudioStatusChangeListener listener) {
        XAudioImpl audio = mPlayersMap.get(playerId);
        if (audio == null) {
            audio = new XAudioImpl(playerId, workspacePath, listener);
            mPlayersMap.put(playerId, audio);
        }
        audio.start(fileAbsPath);
    }

    /**
     * 停止正在播放的 audio.
     *
     * @param playerId    audio player 对象的 id
     * @param appId       执行此扩展的 app 的 id
     */
    private void stop(String playerId) {
        XAudioImpl audio = mPlayersMap.get(playerId);
        if (audio != null) {
            audio.stop();
        }
    }

    /**
     * 暂停播放.
     *
     * @param playerId    audio player 对象的 id
     * @param appId       执行此扩展的 app 的 id
     */
    private void pause(String playerId) {
        XAudioImpl audio = mPlayersMap.get(playerId);
        if (audio != null) {
            audio.pause();
        }
    }

    /**
     * 跳到 audio 的某个位置.
     *
     * @param playerId    audio player 对象的 id
     * @param miliseconds 需要调整到新位置的毫秒值
     * @param appId       执行此扩展的 app 的 id
     */
    private void seekTo(String playerId, int milliseconds) {
        XAudioImpl audio = mPlayersMap.get(playerId);
        if (audio != null) {
            audio.seekTo(milliseconds);
        }
    }

    /**
     * 获取 playback 的当前位置.
     *
     * @param playerId    audio player 对象的 id
     * @param miliseconds 需要调整到新位置的毫秒值
     * @param appId       执行此扩展的 app 的 id
     * @return            position 或者 -1
     */
    public int getCurrentPosition(String playerId, String appId) {
        XAudioImpl audio = mPlayersMap.get(playerId);
        if (audio != null) {
            return audio.getCurrentPosition();
        }
        return -1;
    }

    /**
     * 释放 audio 实例.
     *
     * @param playerId    audio player 对象的 id
     * @param appId       执行此扩展的 app 的 id
     */
    private boolean release(String playerId) {
        XAudioImpl audio = mPlayersMap.get(playerId);
        if (audio != null) {
            mPlayersMap.remove(playerId);
            audio.destroy();
            return true;
        }
        return false;
    }

    /**
     * 开始录音，并保存指定的文件.
     *
     * @param playerId          audio player 对象的 id
     * @param outputFileNanem    输出音频文件的file name，由JS端指定
     * @param appId             执行此扩展的 app 的 id
     * @param workspacePath     workspace 的绝对路径
     */
    private void startRecording(String playerId, String outputFileNanem,
            String workspacePath, XAudioStatusChangeListener listener) {
        // 如果已经开始 recording，则返回；
        XAudioImpl audio = mPlayersMap.get(playerId);
        if (audio != null) {
            return;
        }

        audio = new XAudioImpl(playerId, workspacePath, listener);
        mPlayersMap.put(playerId, audio);
        audio.startRecording(getContext(), outputFileNanem);
    }

    /**
     * 停止录音，并使用用户指定的名称将文件保存在workspace中.
     *
     * @param playerId        audio player 对象的 id
     * @param appId           执行此扩展的 app 的 id
     */
    private void stopRecording(String playerId) {
        XAudioImpl audio = mPlayersMap.get(playerId);

        if (audio != null) {
            audio.stopRecording();
            mPlayersMap.remove(playerId);
        }
    }

    /**
     * 设置播放的音量
     *
     * @param playerId   audio player 对象的id
     * @param appId      执行此扩展的app 的id
     * @param value      播放的音量大小
     */
    private void setVolume(String playerId, float value) {
        XAudioImpl audio = mPlayersMap.get(playerId);
        if (audio != null) {
            audio.setVolume(value);
        } else {
            XLog.e(CLASS_NAME,"setVolume() Error: Unknown Audio Player " + playerId);
        }
    }
}
