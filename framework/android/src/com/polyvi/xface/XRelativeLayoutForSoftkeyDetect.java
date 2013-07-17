
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

package com.polyvi.xface;

import com.polyvi.xface.util.XLog;

import android.content.Context;
import android.widget.RelativeLayout;

/**
 * 继承RelativeLayout，通过重写onMeasure去检查view的高度变化，用于检测软键盘是否弹出。
 * */
public class XRelativeLayoutForSoftkeyDetect extends RelativeLayout {

    private static final String CLASS_NAME = XRelativeLayoutForSoftkeyDetect.class
            .getSimpleName();

    private int mOldHeight = 0;
    private int mOldWidth = 0;
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;

    private long mCurrentSysTimeWhenSoftKeyboardHidden = 0;

    private static final int CRITICAL = 100;/**< 标记是否需要传播按键事件的临界值，单位是毫秒*/

    public XRelativeLayoutForSoftkeyDetect(Context ctx, int width, int height) {
        super(ctx);
        mScreenWidth = width;
        mScreenHeight = height;
    }

    @Override
    /**
     * 监听measurement事件，根据view的高度判断软键盘是否弹出
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width, height;

        height = MeasureSpec.getSize(heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);

        //初始状态
        if (mOldHeight == 0 || mOldHeight == height) {
            XLog.d(CLASS_NAME, "Ignore this event");
        }
        //转屏
        else if (mScreenHeight == width) {
            int tmp_var = mScreenHeight;
            mScreenHeight = mScreenWidth;
            mScreenWidth = tmp_var;
            XLog.i(CLASS_NAME, "Orientation Change");
        }
        //软键盘隐藏了
        else if (height > mOldHeight) {
            XLog.i(CLASS_NAME, "Soft Keyboard Hidden");
            mCurrentSysTimeWhenSoftKeyboardHidden = System.currentTimeMillis();
        }

        //软键盘弹出
        else if (height < mOldHeight) {
            XLog.i(CLASS_NAME, "Soft Keyboard Shown");
        }
        mOldHeight = height;
        mOldWidth = width;
    }

    /**
     * 是否传播按键事件。
     * 在软键盘隐藏的时候记录系统时间，当点击手机按键的时候，再取一次系统时间，比较两次时间差
     * 如果差值小于某一值（一个经验值，目前是100毫秒）就认为是在很短时间内响应了手机按键，此时
     * 认为手机按键的操作是关闭软键盘，则不需要再传播事件，如果大于该值，则认为是需要执行手机
     * 按键事件，则需要传播以便执行js
     *
     * 为什么要根据临界值去判断？
     * 因为，在软键盘弹出后，我们点击返回键，需要经过一下流程：
     * 软键盘关闭->软键盘将返回事件继续传播->xFace响应返回键事件
     * 那么这一段事件是系统在处理，所以关闭软键盘到响应返回键这个时间很短
     *
     * 如果点击屏幕空白处将软键盘关闭，再点返回键，在这一流程中，由于是人为的关闭了软键盘和人为
     * 的点击了返回键，所以这个时间比起系统自己来处理要长得多。
     * 所以才根据临界时间来判断是否是关闭软键盘的操作。
     *
     * @return true 传播 false 不传播
     * */
    public boolean isPropagateEvent() {
        long currentSystime = 0;
        currentSystime = System.currentTimeMillis();
        XLog.d(CLASS_NAME, "The time difference is : "
                + (currentSystime - mCurrentSysTimeWhenSoftKeyboardHidden));
        // 计算两次时间差，小于临界值就认为，本次事件是关闭软键盘，那么就不传播事件了
        if (currentSystime - mCurrentSysTimeWhenSoftKeyboardHidden < CRITICAL) {
            return false;
        }
        return true;
    }
}
