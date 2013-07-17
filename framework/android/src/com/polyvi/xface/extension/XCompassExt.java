
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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.polyvi.xface.util.XLog;

public class XCompassExt extends XExtension implements SensorEventListener {

    /** XCompassExt 提供给js用户的接口名字 */
    private static final String COMMAND_GET_HEADING = "getHeading";

    private static final int STOPPED = 0;
    private static final int STARTING = 1;
    private static final int RUNNING = 2;
    private static final int ERROR_FAILED_TO_START = 3;

    private static final long TIMEOUT = 30000;    //sensor若经过了TIMEOUT指定的时间，还未访问，则关闭sensor节省电量
    private int mStatus;                          //用于记录sensor的状态
    private float mLatestHeading;                 //用于记录最后一次的heading
    private long mTimeStamp;                      //最后一次的heading的时间戳
    private long mLastAccessTime;                 //最后一次使用sensor的时间

    private SensorManager mSensorManager = null;        //用于获得Compass Sensor
    private Sensor mSensor = null;                      //Compass Sensor

    public XCompassExt() {
        mLatestHeading = 0;
        mTimeStamp = 0;
        mStatus = STOPPED;
        mLastAccessTime = 0;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) {
        XExtensionResult.Status status = XExtensionResult.Status.OK;
        if (action.equals(COMMAND_GET_HEADING)) {
            if (mStatus != RUNNING) {
                int sensorStatus = start();
                if (sensorStatus == ERROR_FAILED_TO_START) {
                    return new XExtensionResult(XExtensionResult.Status.IO_EXCEPTION, ERROR_FAILED_TO_START);
                }
                //等待sensor启动，最多等待2s
                long timeout = 2000;
                while ((mStatus == STARTING) && (timeout > 0)) {
                    timeout = timeout - 100;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (timeout == 0) {
                    return new XExtensionResult(XExtensionResult.Status.IO_EXCEPTION, ERROR_FAILED_TO_START);
                }
            }
            return new XExtensionResult(status, getCompassHeading());
        }
        else {
            return new XExtensionResult(XExtensionResult.Status.INVALID_ACTION);
        }
    }

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        if (action.equals(COMMAND_GET_HEADING)) {
            //若sensor正在运行，则采用同步
            if (mStatus == RUNNING) {
                return false;
            }
        }
        return true;
    }

    /**
     * 开始监听compass sensor
     */
    private int start() {
        if ((mStatus == RUNNING) || (mStatus == STARTING)) {
            return mStatus;
        }

        //FIXME:若将获得SENSOR_SERVICE的行为放在主线程当中，则在模拟器上大部分情况下会造成主线程hang的状态
        //      在真机上不会有问题。
        if(null == mSensorManager){
            mSensorManager = (SensorManager) getContext()
                    .getSystemService(Context.SENSOR_SERVICE);
        }

        List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);

        if (list != null && list.size() > 0) {
            mSensor = list.get(0);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mLastAccessTime = System.currentTimeMillis();
            mStatus = STARTING;
        }
        else {
            mStatus = ERROR_FAILED_TO_START;
        }

        return mStatus;
    }

    /**
     * 停止监听compass sensor
     */
    private void stop() {
        if (mStatus != STOPPED) {
            mSensorManager.unregisterListener(this);
        }
        mStatus = STOPPED;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    /** 处理SenserEvent事件
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        float heading = event.values[0];

        mTimeStamp = System.currentTimeMillis();
        mLatestHeading = heading;
        mStatus = RUNNING;

        // 如果经过TIMEOUT指定的时间后，还未被读取数据，那么停止监听compass sensor以节省电量
        if ((mTimeStamp - mLastAccessTime) > TIMEOUT) {
            stop();
        }
    }

    /**
     * 创建CompassHeading的JSON对象，用于返回给JavaScript
     *
     * @return 返回CompassHeading的JSON对象
     */
    private JSONObject getCompassHeading() {
        mLastAccessTime = System.currentTimeMillis();
        JSONObject obj = new JSONObject();

        try {
            obj.put("magneticHeading", mLatestHeading);
            obj.put("trueHeading", mLatestHeading);
            //accuracy的定义为true与magnetic之间的区别。
            //在此处取出的magnetic与true是相同的，所以accuracy一直为0
            obj.put("headingAccuracy", 0);
            obj.put("timestamp", mTimeStamp);
        } catch (JSONException e) {
            String className = XCompassExt.class.getSimpleName();
            XLog.e(className, e.getMessage());
        }
        return obj;
    }
}
