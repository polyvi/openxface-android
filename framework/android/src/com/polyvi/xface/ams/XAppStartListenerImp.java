package com.polyvi.xface.ams;

import org.json.JSONException;
import org.json.JSONObject;

import com.polyvi.xface.ams.XAMSError.AMS_ERROR;
import com.polyvi.xface.extension.XCallbackContext;

public class XAppStartListenerImp implements XAppStartListener {

    private final static String TAG_APP_ID = "appid";
    private final static String TAG_ERROR_CODE = "errorcode";

    /** js回调上下文环境 */
    private XCallbackContext mCallbackCtx;

    public XAppStartListenerImp(XCallbackContext callbackCtx) {
        mCallbackCtx = callbackCtx;
    }

    @Override
    public void onError(String appId, AMS_ERROR errorState) {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(TAG_APP_ID, appId);
            jsonObj.put(TAG_ERROR_CODE, errorState.ordinal());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mCallbackCtx.error(jsonObj);

    }

    @Override
    public void onSuccess(String appId) {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(TAG_APP_ID, appId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mCallbackCtx.success(jsonObj);

    }

}
