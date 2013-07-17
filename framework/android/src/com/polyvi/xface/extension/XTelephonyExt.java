
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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.webkit.WebView;

import com.polyvi.xface.event.XEvent;
import com.polyvi.xface.event.XEventType;
import com.polyvi.xface.event.XSystemEventCenter;
import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XUtils;

public class XTelephonyExt extends XExtension {

    private static final String COMMAND_DELETE_ALL_CALL_RECORDS = "deleteAllCallRecords";
    private static final String COMMAND_DELETE_CALL_RECORD = "deleteCallRecord";
    private static final String COMMAND_GET_CALL_RECORD = "getCallRecord";
    private static final String COMMAND_FIND_CALL_RECORDS = "findCallRecords";
    private static final String COMMAND_GET_CALL_RECORD_COUNT = "getCallRecordCount";
    private static final String COMMAND_INITIATE_VOICE_CALL = "initiateVoiceCall";

    private final String CALL_RECORD_TYPE_RECEIVED = "RECEIVED";/** < 已接电话的通话记录类型 */
    private final String CALL_RECORD_TYPE_OUTGOING = "OUTGOING";/** < 已拨电话的通话记录类型 */
    private final String CALL_RECORD_TYPE_MISSED = "MISSED";/** < 未接电话的通话记录类型 */

    private ContentResolver mContentResolver;

    private final String CLASS_NAME = XTelephonyExt.class.getSimpleName();

    private BroadcastReceiver mIncomingCallBroadcastReceiver = null;

    private static final String ACTION_PHONE_STATE_CHANGED = TelephonyManager.ACTION_PHONE_STATE_CHANGED;
    @Override
    public void init(XExtensionContext extensionContext,  XIWebContext webContext) {
        super.init(extensionContext, webContext);
        mContentResolver = getContext().getContentResolver();
        genIncomingCallBroadcastReceiver();
        getContext().registerReceiver(
                mIncomingCallBroadcastReceiver,
                new IntentFilter(ACTION_PHONE_STATE_CHANGED));
    }

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        if (action.equals(COMMAND_INITIATE_VOICE_CALL)) {
            return false;
        }
        return true;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        XExtensionResult.Status status = XExtensionResult.Status.OK;
        String result = "";
        try {
            if (action.equals(COMMAND_DELETE_ALL_CALL_RECORDS)) {
                boolean delAllSuccess = deleteAllCallRecords(args.getString(0));
                return new XExtensionResult(status, delAllSuccess);
            } else if (action.equals(COMMAND_DELETE_CALL_RECORD)) {
                boolean delSuccess = deleteCallRecord(args.getString(0),
                        args.getLong(1));
                return new XExtensionResult(status, delSuccess);
            } else if (action.equals(COMMAND_FIND_CALL_RECORDS)) {
                JSONArray matchedCallRecord = findCallRecords(
                        args.getJSONObject(0), args.getInt(1), args.getInt(2));
                return new XExtensionResult(status, matchedCallRecord);
            } else if (action.equals(COMMAND_GET_CALL_RECORD)) {
                JSONObject callRecord = getCallRecord(args.getString(0),
                        args.getInt(1));
                return new XExtensionResult(status, callRecord);
            } else if (action.equals(COMMAND_GET_CALL_RECORD_COUNT)) {
                int callCount = getCallRecordCount(args.getString(0));
                return new XExtensionResult(status, callCount);
            } else if (action.equals(COMMAND_INITIATE_VOICE_CALL)) {
                boolean isCallSuccess = false;
                boolean mobileNetAccessible = isSimCardAvailable(getContext());
                isCallSuccess = initiateVoiceCall(args.getString(0));
                if ( !mobileNetAccessible || !isCallSuccess) {
                    status = Status.ERROR;
                }
                return new XExtensionResult(status, result);
            }
            return new XExtensionResult(status, result);
        } catch (JSONException e) {
            return new XExtensionResult(XExtensionResult.Status.ERROR, result);
        }
    }

    @Override
    public void destroy(){
        if(null != mIncomingCallBroadcastReceiver){
            getContext().unregisterReceiver(mIncomingCallBroadcastReceiver);
            mIncomingCallBroadcastReceiver = null;
        }
    }
    /**
     * 删除所有通话记录
     *
     * @param[in] callRecordType 通话记录类型
     *
     * @return 全部删除通话记录是否成功
     * */
    private boolean deleteAllCallRecords(String callRecordType) {
        int callType = getCallRecordType(callRecordType);
        mContentResolver.delete(CallLog.Calls.CONTENT_URI, CallLog.Calls.TYPE
                + "=" + callType, null);
        return true;
    }

    /**
     * 删除指定id和通话记录类型的通话记录
     *
     * @param[in] callRecordType 通话记录类型
     * @param[in] callRecordId 通话记录id
     *
     * @return 删除通话记录是否成功
     * */
    private boolean deleteCallRecord(String callRecordType, long recordId) {
        int callType = getCallRecordType(callRecordType);
        mContentResolver.delete(CallLog.Calls.CONTENT_URI, CallLog.Calls._ID
                + "=" + recordId + " and " + CallLog.Calls.TYPE + "="
                + callType, null);
        return true;
    }

    /**
     * 得到指定通话记录类型和id的通话记录
     *
     * @param[in] callRecordType 通话记录类型
     * @param[in] callRecordIndex 通话记录的索引
     *
     * @return 返回通话记录的JSON对象
     * */
    private JSONObject getCallRecord(String callRecordType, int recordIndex) {
        int callType = getCallRecordType(callRecordType);
        Cursor c = mContentResolver.query(CallLog.Calls.CONTENT_URI,
                new String[] { CallLog.Calls.NUMBER, CallLog.Calls._ID,
                        CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE,
                        CallLog.Calls.DATE, CallLog.Calls.DURATION },
                CallLog.Calls.TYPE + "=" + callType, null,
                CallLog.Calls.DEFAULT_SORT_ORDER);
        String callRecordAddress = "";
        String callRecordId = "";
        String callRecordName = "";
        long durationSeconds = 0;
        long startTime = 0;
        JSONObject callRecord = new JSONObject();
        if (null != c) {
            if (c.moveToPosition(recordIndex)) {
                callRecordAddress = c.getString(0);
                callRecordId = c.getString(1);
                callRecordName = c.getString(2);
                startTime = c.getLong(4);
                durationSeconds = c.getLong(5);
            }
            c.close();
            try {
                callRecord.put("callRecordAddress", callRecordAddress);
                callRecord.put("callRecordId", callRecordId);
                callRecord.put("callRecordName", callRecordName);
                callRecord.put("durationSeconds", durationSeconds);
                callRecord.put("startTime", startTime);
                callRecord.put("callRecordType", callRecordType);
            } catch (JSONException e) {
                XLog.e(CLASS_NAME, e.getMessage());
            }
        }
        return callRecord;
    }

    /**
     * 查找指定匹配的通话记录
     *
     * @param[in] comparisonCallRecord 待匹配的通话记录
     * @param[in] startIndex 要查找的开始索引
     * @param[in] endIndex 要查找的结束位置的索引
     *
     * @return 返回按照匹配的通话记录查找的所有通话记录的JSON数组
     * */
    private JSONArray findCallRecords(JSONObject comparisonCallRecord,
            int startIndex, int endIndex) {
        JSONArray result = new JSONArray();
        if (null != comparisonCallRecord) {
            try {
                String callRecordId = comparisonCallRecord
                        .getString("callRecordId");
                String callRecordAddress = comparisonCallRecord
                        .getString("callRecordAddress");
                String callRecordName = comparisonCallRecord
                        .getString("callRecordName");
                String callRecordType = comparisonCallRecord
                        .getString("callRecordType");
                long startTime = comparisonCallRecord.getLong("startTime");
                long durationSeconds = comparisonCallRecord
                        .getLong("durationSeconds");

                ArrayList<String> projections = new ArrayList<String>();
                projections.add(CallLog.Calls._ID);
                projections.add(CallLog.Calls.NUMBER);
                projections.add(CallLog.Calls.CACHED_NAME);

                ArrayList<String> projectionsValue = new ArrayList<String>();
                projectionsValue.add(callRecordId);
                projectionsValue.add(callRecordAddress);
                projectionsValue.add(callRecordName);

                StringBuilder selection = XUtils.constructSelectionStatement(
                        projections, projectionsValue);
                String selectionStr = buildSelectionStr(selection, startTime,
                        durationSeconds,callRecordType);
                Cursor cursor = mContentResolver.query(
                        CallLog.Calls.CONTENT_URI, null, selectionStr, null,
                        CallLog.Calls.DEFAULT_SORT_ORDER);
                if (null == cursor) {
                    return result;
                }
                int count = endIndex - startIndex + 1;
                if (count > 0) {
                    if (cursor.moveToPosition(startIndex)) {
                        do {
                            JSONObject callRecord = getCallRecordFromCursor(cursor);
                            result.put(callRecord);
                            count--;
                        } while (cursor.moveToNext() && count > 0);
                    }
                }
                cursor.close();
            } catch (JSONException e) {
                XLog.e(CLASS_NAME, e.toString());
            }
        }
        return result;
    }

    /**
     * 获取指定通话记录类型的通话记录个数
     *
     * @param[in] callRecordType 通话记录类型
     *
     * @return 返回通话记录个数
     * */
    private int getCallRecordCount(String callRecordType) {
        int callType = getCallRecordType(callRecordType);
        Cursor c = mContentResolver.query(CallLog.Calls.CONTENT_URI,
                new String[] { CallLog.Calls._ID }, CallLog.Calls.TYPE + "="
                        + callType, null, CallLog.Calls.DEFAULT_SORT_ORDER);
        int count = 0;
        if (null != c) {
            count = c.getCount();
        }
        c.close();
        return count;
    }

    /**
     * 拨打指定号码的电话
     *
     * @param[in] phoneNumber 要拨打的电话号码
     * @return 拨打电话是否成功
     * */
    private boolean initiateVoiceCall(String phoneNumber) {
        try {
            if (isLegalPhoneNum(phoneNumber)) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse(WebView.SCHEME_TEL + phoneNumber));
                getContext().startActivity(intent);
                return true;
            }
		} catch (ActivityNotFoundException e) {
			XLog.e(CLASS_NAME, e.toString());
		} catch (SecurityException e) {
			XLog.e(CLASS_NAME, e.toString());
		}
        return false;
    }

    /**
     * 检测sim卡是否可用
     * @param context
     * @return
     */
    public boolean isSimCardAvailable(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return (info != null && info.isConnected() && checkSimCardState(tm));
    }

    /**
     * 检查sim卡的状态
     * @param tm
     * @return
     */
    private boolean checkSimCardState(TelephonyManager tm){
        int simState = tm.getSimState();
        switch (simState) {
        case TelephonyManager.SIM_STATE_ABSENT:
        	return false;
        case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
        	return true;
        case TelephonyManager.SIM_STATE_PIN_REQUIRED:
        	return true;
        case TelephonyManager.SIM_STATE_PUK_REQUIRED:
        	return true;
        case TelephonyManager.SIM_STATE_READY:
        	return true;
        case TelephonyManager.SIM_STATE_UNKNOWN:
        	return false;
        default:
            return false;
        }
    }

    private void genIncomingCallBroadcastReceiver(){
        if(null == mIncomingCallBroadcastReceiver){
            mIncomingCallBroadcastReceiver = new BroadcastReceiver(){

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if(ACTION_PHONE_STATE_CHANGED.equals(action)){
                        incomingCallResponse(intent);
                    }
                }
            };
        }
    }

    private void incomingCallResponse(Intent intent){
        Context ctx = getContext();
        TelephonyManager tm = (TelephonyManager) ctx
                .getSystemService(Service.TELEPHONY_SERVICE);
        XEvent evt = XEvent.createEvent(XEventType.CALL_RECEIVED, tm.getCallState());
        XSystemEventCenter.getInstance().sendEventAsync(evt);
    }

    /**
     * 验证是否是合法的电话号码
     * @param phoneNumber 待验证的电话号码
     * @return 返回验证结果  -true 电话号码合法 -false 电话号码非法
     * */
    private boolean isLegalPhoneNum(String phoneNumber) {
        String regExpression = "[+*#\\d]+";
        return phoneNumber.matches(regExpression);
    }

    /**
     * 根据js传进来的通话记录类型，获取native的通话记录类型
     *
     * @param[in] callRecordType 通话记录类型
     *
     * @return 返回native的通话记录类型
     * */
    private int getCallRecordType(String callRecordType) {
        int callType = CallLog.Calls.INCOMING_TYPE;
        if (CALL_RECORD_TYPE_RECEIVED.equals(callRecordType)) {
            callType = CallLog.Calls.INCOMING_TYPE;
        } else if (CALL_RECORD_TYPE_OUTGOING.equals(callRecordType)) {
            callType = CallLog.Calls.OUTGOING_TYPE;
        } else if (CALL_RECORD_TYPE_MISSED.equals(callRecordType)) {
            callType = CallLog.Calls.MISSED_TYPE;
        }
        return callType;
    }

    /**
     * 根据通话记录类型（整型）的到JS需要的字符串通话记录类型
     * */
    private String getCallRecordTypeStr(int type) {
        String typeStr = CALL_RECORD_TYPE_RECEIVED;
        switch (type) {
        case CallLog.Calls.INCOMING_TYPE:
            typeStr = CALL_RECORD_TYPE_RECEIVED;
            break;
        case CallLog.Calls.OUTGOING_TYPE:
            typeStr = CALL_RECORD_TYPE_OUTGOING;
            break;
        case CallLog.Calls.MISSED_TYPE:
            typeStr = CALL_RECORD_TYPE_MISSED;
            break;
        }
        return typeStr;
    }

    /**
     * 通过Cursor得到通话记录的JSON对象
     * */
    private JSONObject getCallRecordFromCursor(Cursor cursor) {
        String callRecordId = cursor.getString(cursor
                .getColumnIndex(CallLog.Calls._ID));
        String callRecordAddress = cursor.getString(cursor
                .getColumnIndex(CallLog.Calls.NUMBER));
        String callRecordName = cursor.getString(cursor
                .getColumnIndex(CallLog.Calls.CACHED_NAME));
        int callRecordTypeInt = cursor.getInt(cursor
                .getColumnIndex(CallLog.Calls.TYPE));
        long startTime = cursor.getLong(cursor
                .getColumnIndex(CallLog.Calls.DATE));
        long durationSeconds = cursor.getLong(cursor
                .getColumnIndex(CallLog.Calls.DURATION));
        JSONObject callRecord = new JSONObject();
        try {
            callRecord.put("callRecordId", callRecordId);
            callRecord.put("callRecordAddress", callRecordAddress);
            callRecord.put("callRecordName", callRecordName);
            callRecord.put("callRecordType",
                    getCallRecordTypeStr(callRecordTypeInt));
            callRecord.put("startTime", startTime);
            callRecord.put("durationSeconds", durationSeconds);
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.toString());
        }
        return callRecord;
    }

    /**
     * 根据已经得到的selection和电话开始时间和持续时间构建SQL语句字符串
     *
     * @param selection
     *            初始构建的查询语句
     * @param startTime
     *            通话开始时间
     * @param durationSeconds
     *            通话持续时间
     *
     * @return SQL查询的条件语句字符串
     * */
    private String buildSelectionStr(StringBuilder selection, long startTime,
            long durationSeconds,String type) {
        if (-1 != startTime) {
            if (null == selection) {
                selection = new StringBuilder();
            } else {
                selection.append(" AND ");
            }
            selection.append(CallLog.Calls.DATE);
            selection.append("=");
            selection.append(startTime);
        }
        if (-1 != durationSeconds) {
            if (null == selection) {
                selection = new StringBuilder();
            } else {
                selection.append(" AND ");
            }
            selection.append(CallLog.Calls.DURATION);
            selection.append("=");
            selection.append(durationSeconds);
        }
        if(!"".equals(type)){
            if (null == selection) {
                selection = new StringBuilder();
            } else {
                selection.append(" AND ");
            }
            selection.append(CallLog.Calls.TYPE);
            selection.append("=");
            selection.append(getCallRecordType(type));
        }
        String selectionStr = null;
        if (null != selection) {
            selectionStr = selection.toString();
        }
        return selectionStr;
    }
}


