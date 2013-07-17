
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

package com.polyvi.xface.extension;

import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class XCalendarExt extends XExtension {

    private static final String COMMAND_GET_TIME = "getTime";
    private static final String COMMAND_GET_DATE = "getDate";

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {

        Calendar calendar = Calendar.getInstance();

        if (COMMAND_GET_TIME.equals(action)) {
            // 若没有输入，则使用当前时间，反之亦然
            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);

            if (2 == args.length()) {
                hours = args.getInt(0);
                minutes = args.getInt(1);
            }

            getTime(hours, minutes, callbackCtx);
        } else if (COMMAND_GET_DATE.equals(action)) {
            // 若没有输入，则使用当前日期，反之亦然
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            if (3 == args.length()) {
                year = args.getInt(0);
                month = args.getInt(1);
                day = args.getInt(2);
            }

            getDate(year, month, day, callbackCtx);
        }

        XExtensionResult er = new XExtensionResult(
                XExtensionResult.Status.NO_RESULT);
        return er;
    }

    /**
     * 初始化时间控件显示的时间并通过时间控件获取系统的时间
     *
     * @param hour
     *            初始设置时间控件显示的小时
     * @param minute
     *            初始设置时间控件显示的分钟
     * */
    private void getTime(final int hour, final int minute,
            final XCallbackContext callbackCtx) {
        mExtensionContext.getSystemContext().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new TimePickerDialog(getContext(),
                                new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(TimePicker view,
                                            int hourOfDay, int minute) {
                                        setTime(hourOfDay, minute, callbackCtx);
                                    }
                                }, hour, minute, true).show();
                    }
                });
    }

    /**
     * 初始化日期控件显示的日期通过日期控件获取系统的日期
     *
     * @param year
     *            初始设置日期控件显示的年份
     * @param month
     *            初始设置日期控件显示的月份
     * @param day
     *            初始设置日期控件显示的天
     * */
    private void getDate(final int year, final int month, final int day,
            final XCallbackContext callbackCtx) {
        mExtensionContext.getSystemContext().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new DatePickerDialog(getContext(),
                                new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker view,
                                            int year, int monthOfYear,
                                            int dayOfMonth) {
                                        setDate(year, monthOfYear, dayOfMonth,
                                                callbackCtx);
                                    }
                                }, year, (month - 1), day).show();// 由于系统计算月份是以0开始，所以系统在日期选择器
                                                                  // 控件中会自动把设置的初始月份加1，为了保证月份准确
                                                                  // 显示，所以这里将js传来的月份数减1
                    }
                });
    }

    /**
     * 根据时间控件选择的时间结果，设置需要返回给js的时间
     *
     * @param hour
     *            通过时间控件设置的小时
     * @param minute
     *            通过时间控件设置的分钟
     * */
    private void setTime(int hour, int minute,
            XCallbackContext callbackCtx) {
        JSONObject time = new JSONObject();
        try {
            time.put("hour", hour);
            time.put("minute", minute);
            callbackCtx.success(time);
        } catch (JSONException e) {
            callbackCtx.error();
        }
    }

    /**
     * 根据日期控件选择的日期结果，设置需要返回给js的日期
     *
     * @param year
     *            通过日期控件设置的年份
     * @param month
     *            通过日期控件设置的月份
     * @param day
     *            通过日期控件设置的天
     * */
    private void setDate(int year, int month, int day,
            XCallbackContext callbackCtx) {
        JSONObject date = new JSONObject();
        try {
            date.put("year", year);
            date.put("month", (month + 1));// 由于系统返回的月份是以0开始，所以在返回给js月份的时候加1
            date.put("day", day);
            callbackCtx.success(date);
        } catch (JSONException e) {
            callbackCtx.error();
        }
    }
}
