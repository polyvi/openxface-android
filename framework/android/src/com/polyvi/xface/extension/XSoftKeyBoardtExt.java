package com.polyvi.xface.extension;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.extension.XExtensionResult.Status;

public class XSoftKeyBoardtExt  extends XExtension {

    /**
    *   Show soft keyboard
    */
    private void showSoftKeyboard(){
        WebView webView = (WebView) mWebContext.getApplication().getView();
        InputMethodManager imm = (InputMethodManager)webView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(webView, InputMethodManager.SHOW_FORCED);
    }

    /**
    *   Hide soft keyboard
    */
    private void hideSoftKeyboard(){
        View webView = (View) mWebContext.getApplication().getView();
        InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(webView.getWindowToken(), 0);
    }

    /**
    *   Show state of soft keyboard
    */
    private boolean isSoftKeyboardShowing(){
        View webView = (View) mWebContext.getApplication().getView();
        int heightDiff = webView.getRootView().getHeight() - webView.getHeight();
        return (100 < heightDiff); // if more than 100 pixels, it's probably a keyboard.
    }

    @Override
    public XExtensionResult exec(String action, JSONArray args,
            XCallbackContext callbackCtx) throws JSONException {
        Status status = Status.ERROR;
        if("show".equals(action)){
            showSoftKeyboard();
            status = Status.OK;
        }else if ("hide".equals(action)){
            hideSoftKeyboard();
            status = Status.OK;
        }else if ("isShowing".equals(action)) {
            boolean isShowing =  isSoftKeyboardShowing();
            status = Status.OK;
            return new XExtensionResult(status,isShowing);
        }else {
             status = Status.INVALID_ACTION;
        }
        return new XExtensionResult(status);
    }

    @Override
    public void sendAsyncResult(String result) {

    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }
}
