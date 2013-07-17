
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

package com.polyvi.xface.extension.capture;

import org.json.JSONObject;

public class XCaptureScreenOptions {
    /**用于标识目标图像的数据类型*/
    private static final  int    TAG_DATA_URL = 0;//base64编码格式的数据
    private static final  int    TAG_FILE_URI = 1;//文件url
    private static final  String TAG_X = "x";
    private static final  String TAG_Y = "y";
    private static final  String TAG_WIDTH = "width";
    private static final  String TAG_HEIGHT = "height";
    private static final  String TAG_DESTINATION_FILE = "destionationFile";
    private static final  String TAG_DESTINATION_TYPE = "destinationType";
    private int x = 0;
    private int y = 0;
    private int width  = 0;
    private int height = 0;
    private String destionationFile = "";
    private int destinationType = TAG_FILE_URI;

    public XCaptureScreenOptions(JSONObject object) {
        if(null == object) {
            return;
        }
        x = object.optInt(TAG_X, x);
        y = object.optInt(TAG_Y, y);
        width = object.optInt(TAG_WIDTH, width);
        height = object.optInt(TAG_HEIGHT, height);
        destionationFile = object.optString(TAG_DESTINATION_FILE, "");
        destinationType = object.optInt(TAG_DESTINATION_TYPE, TAG_DATA_URL);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDestinationType() {
        return destinationType;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getDestionationFile() {
        return destionationFile;
    }

    public void setDestionationFile(String destionationFile) {
        this.destionationFile = destionationFile;
    }

    public static int getTagDataUrl() {
        return TAG_DATA_URL;
    }

    public static int getTagFileUri() {
        return TAG_FILE_URI;
    }

    /**
     * 标示是否是默认情况：即用户没有输入x,y,width,height
     * @return true:默认情况
     */
    public boolean iSDefault() {
        return (x == 0) && (y == 0) && (width ==0)  && (height == 0);
    }

    /**
     * 标示是否用户输入了x,y,但没输入width和height
     * @return true:默认情况
     */
    public boolean iSOnlyXAndYInput() {
        return (width == 0) && (height == 0);
    }
}
