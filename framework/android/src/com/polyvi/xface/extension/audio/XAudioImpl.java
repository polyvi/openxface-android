
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;

import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XConstant;

/**
 * 此类实现了音频相关的各种操作，实现了系统的相关监听器，用于监听音频的状态变化
 * 调用者为 XAudioExt
 * 每个类实例仅操作一个音频文件
 *
 */
public class XAudioImpl implements OnCompletionListener, OnPreparedListener, OnErrorListener {
    private final String CLASS_NAME = XAudioImpl.class.getSimpleName();

    private enum StatesCode {
        MEDIA_NONE,
        MEDIA_STARTING,
        MEDIA_RUNNING,
        MEDIA_PAUSED,
        MEDIA_STOPPED
    }

    private enum MsgId {
        MEDIA_NONE,  // 为了与 JS 端的对 msgId 的定义相匹配而定义的占位符
        MEDIA_STATE,
        MEDIA_DURATION,
        MEDIA_POSITION,
        MEDIA_ERROR
    }

    private enum ErrorCode {
        MEDIA_ERR_NONE_ACTIVE,
        MEDIA_ERR_ABORTED,
        MEDIA_ERR_NETWORK,
        MEDIA_ERR_DECODE,
        MEDIA_ERR_NONE_SUPPORTED
    }

    private StatesCode mState = StatesCode.MEDIA_NONE;     // 播放状态

    private static final String HTTP_SCHEME = "http://";
    private static final String HTTPS_SCHEME = "https://";

    private MediaPlayer mPlayer = null;     // Audio player 对象
    private String mPlayerId;               // Audio player Id，用于识别 JS 端的 audio 对象
    private float mDuration = -1;           // Audio clip 的长度
    private boolean mPrepareOnly = false;   // 主要用于区分在准备好 audio 后，是否需要马上进行播放
    private XAudioStatusChangeListener mListener;  // 用于监听播放状态变化的 listener

    private MediaRecorder mRecorder = null;            // Audio recording object
    private String mWorkspacePath;

    public XAudioImpl(String playerId, String workspacePath, XAudioStatusChangeListener listener) {
        mPlayerId = playerId;
        mListener = listener;
        mWorkspacePath = workspacePath;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        XLog.e(CLASS_NAME, "AudioPlayer.onError(" + what + ", " + extra+")");
        this.mPlayer.stop();
        this.mPlayer.release();
        //FIXME: what 的值不与ErrorCode完全匹配
        mListener.onError(this.mPlayerId, MsgId.MEDIA_ERROR.ordinal(), what);
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // 监听 audio 播放完成
        this.mPlayer.setOnCompletionListener(this);

        // 需要马上进行播放
        if (!this.mPrepareOnly) {
            this.setState(StatesCode.MEDIA_RUNNING);
            this.mPlayer.start();
        }

        // 获取 audio clip 的长度
        this.mDuration = getDurationInSeconds();
        this.mPrepareOnly = false;
        mListener.onGetDuration(mPlayerId,
                MsgId.MEDIA_DURATION.ordinal(), this.mDuration);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        this.setState(StatesCode.MEDIA_STOPPED);
    }

    /**
     * 使用指定文件名开始录制.
     *
     * @param context           上下文环境
     * @param outputFileName    输出音频文件的file name，由JS端指定
     */
    public void startRecording(Context context, String outputFileName) {

        if (this.mPlayer != null) {
            XLog.e(CLASS_NAME, "AudioPlayer Error: Can't record in play mode.");
            this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_ABORTED);
        }
        // 确认没有开始录音
        else if (this.mRecorder == null) {
            this.mRecorder = new MediaRecorder();
            this.mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            this.mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT); // THREE_GPP);
            this.mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); //AMR_NB);
            XPathResolver pathResolver = new XPathResolver(outputFileName, mWorkspacePath);
            String path = pathResolver.resolve();

            //输出文件创建成功才能录制
            if(XFileUtils.createFile(path))
            {
                try {
                    this.mRecorder.setOutputFile(path);
                    this.mRecorder.prepare();
                    this.mRecorder.start();
                    this.setState(StatesCode.MEDIA_RUNNING);
                    return;
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_ABORTED);
        }
        else {
            XLog.e(CLASS_NAME, "AudioPlayer Error: Already recording.");
            this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_ABORTED);
        }
    }

    /**
     * 停止录音
     */
    public void stopRecording() {
        if (this.mRecorder != null) {
            try{
                if (this.mState == StatesCode.MEDIA_RUNNING) {
                    //stop 需要百毫秒级的时间
                    this.mRecorder.reset();
                    this.setState(StatesCode.MEDIA_STOPPED);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Start 或者 resume 音频文件.
     *
     * @param fileAbsPath    音频文件的绝对路径
     */
    public void start(String fileAbsPath) {
        if (this.mRecorder != null) {
            XLog.e(CLASS_NAME, "AudioPlayer Error: Can't play in record mode.");
            this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_ABORTED);
        }
        // 如果此时是新播放音频或者状态处于stop
        else if ((this.mPlayer == null) || (this.mState == StatesCode.MEDIA_STOPPED)) {
             try {
                 // 如果已经停止,则重置 player
                 if (this.mPlayer != null) {
                     this.mPlayer.reset();
                 }
                 // 创建一个新的 player
                 else {
                     this.mPlayer = new MediaPlayer();
                 }
                 // 如果是流媒体
                 if (this.isStreaming(fileAbsPath)) {
                     this.mPlayer.setDataSource(fileAbsPath);
                     this.mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                     this.mPlayer.setOnErrorListener(this);
                     this.mPlayer.setOnPreparedListener(this);
                     this.setState(StatesCode.MEDIA_STARTING);
                     this.mPlayer.prepareAsync();    // 异步准备
                 }
                 // 如果是本地文件
                 else {
                     XPathResolver pathResolver =new XPathResolver(fileAbsPath, mWorkspacePath);
                     try {
                         FileInputStream fileInputStream = new FileInputStream(pathResolver.resolve());
                         this.mPlayer.setDataSource(fileInputStream.getFD());
                         this.mPlayer.setOnErrorListener(this);
                         this.mPlayer.setOnPreparedListener(this);
                         this.setState(StatesCode.MEDIA_STARTING);
                         this.mPlayer.prepare();    // 同步准备
                         this.mDuration = getDurationInSeconds();
                     } catch (FileNotFoundException e) {
                         //src无效, 返回MEDIA_ERR_ABORTED，与标准html5不一致，与phone gap一致
                         this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_ABORTED);
                         return;
                     }
                 }
             }
             catch (Exception e) {
                 e.printStackTrace();
                 this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_ABORTED);
             }
         }
         // 如果已经创建了一个 audio player
         else {
             // 如果 audio 被暂停，则继续播放
             if ((this.mState == StatesCode.MEDIA_PAUSED)
                     || (this.mState == StatesCode.MEDIA_STARTING)) {
                 try {
                     this.mPlayer.start();
                     this.setState(StatesCode.MEDIA_RUNNING);
                 } catch (IllegalStateException e) {
                     XLog.e(CLASS_NAME, "AudioPlayer Error: start() called during invalid state: " + this.mState);
                     this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_NONE_ACTIVE);
                 }
             } else {
                 XLog.e(CLASS_NAME, "AudioPlayer Error: startPlaying() called during invalid state: " + this.mState);
                 this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_ABORTED);
             }
         }
    }

    /**
     * 是否是流媒体.
     * @param path  绝对路径
     * @return  是返回 true，不是返回 false
     */
    private boolean isStreaming(String path) {
        return (path.startsWith(HTTP_SCHEME) || path.startsWith(HTTPS_SCHEME)) ? true : false;
    }

    /**
     * 以秒的形式获取 duration.
     * @return  duration 的秒表示
     */
    private float getDurationInSeconds() {
        return (this.mPlayer.getDuration() / 1000.0f);
    }

    /**
     * 停止正在播放的 audio.
     */
    public void stop() {
        if ((this.mState == StatesCode.MEDIA_RUNNING) || (this.mState == StatesCode.MEDIA_PAUSED)) {
            try {
                this.mPlayer.stop();
                this.setState(StatesCode.MEDIA_STOPPED);
            } catch (IllegalStateException e) {
                XLog.e(CLASS_NAME, "AudioPlayer Error: stopPlaying() called during invalid state: " + this.mState);
                this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_NONE_ACTIVE);
            }

        } else {
            XLog.e(CLASS_NAME, "AudioPlayer Error: stopPlaying() called during invalid state: " + this.mState);
            this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_NONE_ACTIVE);
        }
    }

    /**
     * 暂停播放.
     */
    public void pause() {
        if (this.mState == StatesCode.MEDIA_RUNNING) {
            try {
                this.mPlayer.pause();
                this.setState(StatesCode.MEDIA_PAUSED);
            } catch (IllegalStateException e) {
                XLog.e(CLASS_NAME, "AudioPlayer Error: pause() called during invalid state: " + this.mState);
                this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_NONE_ACTIVE);
            }
        } else {
            XLog.e(CLASS_NAME, "AudioPlayer Error: pausePlaying() called during invalid state: " + this.mState);
            this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_NONE_ACTIVE);
        }
    }

    /**
     * 跳到 audio 的某个位置.
     *
     * @param miliseconds 需要调整到新位置的毫秒值
     */
    public void seekTo(int milliseconds) {
        if (this.mPlayer != null) {
            try {
                this.mPlayer.seekTo(milliseconds);
            } catch (IllegalStateException e) {
                XLog.e(CLASS_NAME, "AudioPlayer Error: seekTo() called during invalid state: " + this.mState);
                this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_NONE_ACTIVE);
            }
            XLog.d(CLASS_NAME, "Send a onStatus update for the new seek");
            sendPosition(milliseconds / XConstant.MILLISECONDS_PER_SECOND);
        }
    }

    /**
     * 获取 playback 的当前位置.
     *
     * @return    position 或者 -1
     */
    public int getCurrentPosition() {
        try {
            if ((this.mState == StatesCode.MEDIA_RUNNING) || (this.mState == StatesCode.MEDIA_PAUSED)) {
                int curPos = this.mPlayer.getCurrentPosition() / XConstant.MILLISECONDS_PER_SECOND;
                sendPosition(curPos);
                return curPos;
             }
        } catch (IllegalStateException e) {
            XLog.e(CLASS_NAME, "AudioPlayer Error: getCurrentPosition() called during invalid state: " + this.mState);
            this.sendErrorMsg(MsgId.MEDIA_ERROR, ErrorCode.MEDIA_ERR_NONE_ACTIVE);
        }
        return -1;
    }

    /**
     * 设置player的音量
     *
     * @param value   要设置的音量值
     */
    public void setVolume(float value) {
        if (null != this.mPlayer) {
            this.mPlayer.setVolume(value, value);
        }
    }

    /**
     * 停止播放并销毁 player.
     */
    public void destroy() {
        // 停止所有播放
        if (this.mPlayer != null) {
            if ((this.mState == StatesCode.MEDIA_RUNNING)
                    || (this.mState == StatesCode.MEDIA_PAUSED)) {
                try {
                    this.mPlayer.stop();
                    this.setState(StatesCode.MEDIA_STOPPED);
                } catch (IllegalStateException e) {
                    XLog.e(CLASS_NAME, "AudioPlayer Error: stopPlaying() called during invalid state: " + this.mState);
                }
                this.setState(StatesCode.MEDIA_STOPPED);
            }
            this.mPlayer.release();
            this.mPlayer = null;
        }

        if (this.mRecorder != null) {
            this.stopRecording();
            this.mRecorder.release();
            this.mRecorder = null;
        }
    }

    /**
     * 设置状态并发送给 JS.
     *
     * @param state    状态码
     */
    private void setState(StatesCode state) {
        if (this.mState != state) {
            this.mState = state;
            mListener.onStatusChange(this.mPlayerId,
                    MsgId.MEDIA_STATE.ordinal(), state.ordinal());
        }
    }

    /**
     * 向 JS 端发送错误信息
     *
     * @param id     error id
     * @param code   error code
     */
    private void sendErrorMsg(MsgId id, ErrorCode code) {
        mListener.onError(this.mPlayerId, MsgId.MEDIA_ERROR.ordinal(),
                code.ordinal());
    }

    /**
     * 向 JS 端发送 audio 的 position 信息
     *
     * @param position   audio 的当前位置，单位为秒
     */
    private void sendPosition(int position) {
        mListener.onGetPosition(this.mPlayerId,
                MsgId.MEDIA_POSITION.ordinal(), position);
    }
}
