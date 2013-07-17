
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

import com.polyvi.xface.extension.XExtensionResult.Status;

public class XAccelerometerExt extends XExtension implements
        SensorEventListener {

    private final static int STOPPED = 0;
    private final static int STARTING = 1;
    private final static int RUNNING = 2;
    private final static int ERROR_FAILED_TO_START = 3;

    private final static String COMMAND_START = "start";
    private final static String COMMAND_STOP = "stop";

    private float mX, mY, mZ; // 最近一次获得的acceleration值
    private long mTimestamp; // 最近一次获得的acceleration的时间戳
    private int mStatus; // 监听器所处的状态
    private int mAccuracy; // 获取到的数据的精度

    private SensorManager mSensorManager; // 用于获得Acceleration sensor
    private Sensor mSensor; // Acceleration sensor
    private XCallbackContext mCallbackCtx;

    public XAccelerometerExt() {
        mX = 0;
        mY = 0;
        mZ = 0;
        mTimestamp = 0;
        mStatus = STOPPED;
        mAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    }

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return action.equals(COMMAND_START) && mStatus != RUNNING;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) {
        XExtensionResult.Status extensionStatus = Status.NO_RESULT;
        mCallbackCtx = callbackCtx;
        if (action.equals(COMMAND_START)) {
            if (mStatus != RUNNING) {
                start();
            }
        } else if (action.equals(COMMAND_STOP)) {
            if (RUNNING == mStatus) {
                stop();
            }
            extensionStatus = Status.OK;
        } else {
            extensionStatus = Status.INVALID_ACTION;
        }
        return new XExtensionResult(extensionStatus);
    }

    /**
     * 开始监听accelerometer sensor
     */
    private void start() {
        if ((mStatus == RUNNING) || (mStatus == STARTING)) {
            return;
        }

        mStatus = STARTING;
        if (null == mSensorManager) {
            mSensorManager = (SensorManager) getContext()
                    .getSystemService(Context.SENSOR_SERVICE);
        }

        List<Sensor> list = mSensorManager
                .getSensorList(Sensor.TYPE_ACCELEROMETER);

        if ((list != null) && (list.size() > 0)) {
            mSensor = list.get(0);
            mSensorManager.registerListener(this, mSensor,
                    SensorManager.SENSOR_DELAY_UI);
        } else {
            mStatus = ERROR_FAILED_TO_START;
            mCallbackCtx.error("No sensors found to register accelerometer listening to.");
        }

        // Wait until running
        long timeout = 2000;
        while ((this.mStatus == STARTING) && (timeout > 0)) {
            timeout = timeout - 100;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (timeout == 0) {
            mStatus = ERROR_FAILED_TO_START;
            mCallbackCtx.error("Accelerometer could not be started.");
        }
    }

    /**
     * 停止监听accelerometer sensor
     */
    private void stop() {
        if (STOPPED != mStatus && null != mSensorManager) {
            mSensorManager.unregisterListener(this);
        }
        mStatus = STOPPED;
        mAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    }

    private void win() {
        XExtensionResult result = new XExtensionResult(Status.OK,
                getAccelerationJSON());
        result.setKeepCallback(true);
        mCallbackCtx.sendExtensionResult(result);
    }

    /**
     * 获取json格式的重力感应数据
     */
    private JSONObject getAccelerationJSON() {
        JSONObject r = new JSONObject();
        try {
            r.put("x", mX);
            r.put("y", mY);
            r.put("z", mZ);
            r.put("timestamp", mTimestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return r;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Only look at accelerometer events
        if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        // If not running, then just return
        if (mStatus == STOPPED) {
            return;
        }
        mAccuracy = accuracy;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Only look at accelerometer events
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        // If not running, then just return
        if (mStatus == STOPPED) {
            return;
        }
        mStatus = RUNNING;
        if (mAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
            mTimestamp = System.currentTimeMillis();
            mX = event.values[0];
            mY = event.values[1];
            mZ = event.values[2];

            win();
        }
    }

    @Override
    public void destroy() {
        stop();
        super.destroy();
    }
}
