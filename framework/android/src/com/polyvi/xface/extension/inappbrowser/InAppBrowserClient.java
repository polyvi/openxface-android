package com.polyvi.xface.extension.inappbrowser;

import org.json.JSONArray;
import org.json.JSONException;

import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.util.XLog;

public class InAppBrowserClient extends WebChromeClient {
    private static final String CLASS_NAME = XInAppWebView.class.getSimpleName();
    private static final long MAX_QUOTA = 100 * 1024 * 1024;
    private static final String TAG_XFACE_IAP = "xface-iab://";
    private XCallbackContext callbackCtx;

    public InAppBrowserClient(XCallbackContext callbackCtx) {
        super();
        this.callbackCtx = callbackCtx;
    }

    /** 当超出数据库限额时的处理 */
    @Override
    public void onExceededDatabaseQuota(String url, String databaseIdentifier,
            long currentQuota, long estimatedSize, long totalUsedQuota,
            WebStorage.QuotaUpdater quotaUpdater) {
        XLog.d(CLASS_NAME,
                "onExceededDatabaseQuota estimatedSize: %d  currentQuota: %d  totalUsedQuota: %d",
                estimatedSize, currentQuota, totalUsedQuota);
        if (estimatedSize < MAX_QUOTA) {
            // increase for 1Mb
            long newQuota = estimatedSize;
            XLog.d(CLASS_NAME, "calling quotaUpdater.updateQuota newQuota: %d",
                    newQuota);
            quotaUpdater.updateQuota(newQuota);
        } else {
            // TODO: get docs on how to handle this properly
            quotaUpdater.updateQuota(currentQuota);
        }
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin,
            Callback callback) {
        super.onGeolocationPermissionsShowPrompt(origin, callback);
        callback.invoke(origin, true, false);
    }

    /** 拦截JS信息,然后把信息发送给上层回调 */
    @Override
    public boolean onJsPrompt(WebView view, String url, String message,
            String defaultValue, JsPromptResult result) {
        /** 如果prompt以gap-iab://开头，则向JS发送回调 */
        if (defaultValue != null && defaultValue.startsWith(TAG_XFACE_IAP)) {
            XExtensionResult scriptResult;
            String scriptCallbackId = defaultValue.substring(TAG_XFACE_IAP.length());
            if (scriptCallbackId.startsWith("InAppBrowser")) {
                if (message == null || message.length() == 0) {
                    scriptResult = new XExtensionResult(XExtensionResult.Status.OK, new JSONArray());
                } else {
                    try {
                        scriptResult = new XExtensionResult(XExtensionResult.Status.OK, new JSONArray(message));
                    } catch (JSONException e) {
                        scriptResult = new XExtensionResult(XExtensionResult.Status.JSON_EXCEPTION,e.getMessage());
                    }
                }
                callbackCtx.sendExtensionResult(scriptResult);
                result.confirm("");
                return true;
            }
        }
        return false;
    }
}
