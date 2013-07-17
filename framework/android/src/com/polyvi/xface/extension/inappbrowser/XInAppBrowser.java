
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

package com.polyvi.xface.extension.inappbrowser;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.plugin.api.XIWebContext;

/**
 *内部浏览器类，封装了WebView，实现了构建和页面加载行为的回调接口
 */
public class XInAppBrowser implements XIBrowserBuilder , XIBrowserListener{
    private static final int BUTTON_CONTAINER_ID = 1;
    private static final int BUTTON_BACKBUTTON_ID = 2;
    private static final int BUTTON_FORWARDBUTTON_ID = 3;
    private static final int EDITTEXT_ID = 4;
    private static final int BUTTON_CLOSE_ID = 5;

    private Dialog mDialog;
    private Context mContext;
    private EditText mEditText;
    private XIBrowserListener mBrowserListener;
    private String mUrl;
    private WebView mWebView;
    private boolean mIsShowToolBar;
    private XIWebContext mWebContext;


    public XInAppBrowser(Context context, XIBrowserListener listener,
            String url, XIWebContext webContext, boolean isShowToolBar)
    {
        mContext = context;
        mBrowserListener = listener;
        mUrl = url;
        mWebContext = webContext;
        mIsShowToolBar = isShowToolBar;
    }

    /**
     * 创建MainLayout布局
     * @return
     */
    protected LinearLayout buildMainContainerLayout() {
        return createMainContainerLayout(LinearLayout.VERTICAL);
    }
    /**
     * 构建toolBar布局
     * @return
     */
    protected RelativeLayout buildToolbarLayout() {
        return createToolbarLayout(Gravity.LEFT, Gravity.TOP);
    }

    /**
     * 构建ButtonContainer布局
     * @return
     */
    protected RelativeLayout buildActionButtonContainerLayout() {
        return createActionButtonContainer(BUTTON_CONTAINER_ID,
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT), Gravity.LEFT,
                Gravity.CENTER_VERTICAL);
    }
    /**
     * 构建回退键
     * @return
     */
    protected Button buildBackButton() {
        RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
        Button back = createButton(BUTTON_BACKBUTTON_ID, backLayoutParams,
                "Back Button", "<", new View.OnClickListener() {
                    public void onClick(View v) {
                        goBack();
                    }
                });
        return back;
    }
    /**
     * 构建前进键
     * @return
     */
    protected Button buildForwardButton() {
        RelativeLayout.LayoutParams forwardLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        forwardLayoutParams.addRule(RelativeLayout.RIGHT_OF, 2);
        Button forward = createButton(BUTTON_FORWARDBUTTON_ID,
                forwardLayoutParams, "Forward Button", ">",
                new View.OnClickListener() {
                    public void onClick(View v) {
                       goForward();
                    }
                });
        return forward;
    }

    /**
     * 构建关闭键
     * @return
     */
    protected Button buildCloseButton() {
        RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        Button close = createButton(BUTTON_CLOSE_ID, closeLayoutParams,
                "Close Button", "Done", new View.OnClickListener() {
                    public void onClick(View v) {
                        closeDialog();
                    }
                });
        return close;
    }

    /**
     * 构建文本框
     */
    protected void buildEditText() {
        RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
        textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
        OnKeyListener keyListener = new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    navigate(mEditText.getText().toString());
                    return true;
                }
                return false;
            }
        };
        mEditText =  createEditText(EDITTEXT_ID, textLayoutParams, true, mUrl,
                InputType.TYPE_TEXT_VARIATION_URI, EditorInfo.IME_ACTION_GO,
                keyListener);
    }

    /**
     * 根据参数创建对话框
     * @param theme 主题
     * @param animation 动画
     * @param windowFeature 窗口特性
     * @return dialog对象
     */
    protected Dialog createDialog(int theme, int animation, int windowFeature) {
        Dialog dialog = new Dialog(mContext, theme);
        dialog.getWindow().getAttributes().windowAnimations = animation;
        dialog.requestWindowFeature(windowFeature);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                mBrowserListener.onDismiss(mWebContext);
            }
        });
        return dialog;
    }
    /**
     * 根据布局参数创建Dialog的主布局
     * @param layout 布局参数
     * @return
     */
    protected LinearLayout createMainContainerLayout(int layout) {
        LinearLayout mainLayout = new LinearLayout(mContext);
        mainLayout.setOrientation(layout);
        return mainLayout;
    }
    /**
     * 根据指定参数创建工具栏的布局
     * @param horizontalGravity 水平重力参数
     * @param verticalGravity 垂直重力参数
     * @return
     */
    protected RelativeLayout createToolbarLayout(int horizontalGravity,
            int verticalGravity) {
        RelativeLayout toolbarLayOut = new RelativeLayout(mContext);
        toolbarLayOut.setLayoutParams(new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dpToPixels(44)));
        toolbarLayOut.setPadding(dpToPixels(2), dpToPixels(2), dpToPixels(2),
                dpToPixels(2));
        toolbarLayOut.setHorizontalGravity(horizontalGravity);
        toolbarLayOut.setVerticalGravity(verticalGravity);
        return toolbarLayOut;
    }
    /**
     * 根据指定参数创建button的容器
     * @param containerId 容器id，用来唯一标识该容器
     * @param params 布局参数
     * @param horizontalGravity 水平重力参数
     * @param verticalGravity 垂直重力参数
     * @return
     */
    protected RelativeLayout createActionButtonContainer(int containerId,
            RelativeLayout.LayoutParams params, int horizontalGravity,
            int verticalGravity) {
        RelativeLayout actionButtonContainer = new RelativeLayout(mContext);
        actionButtonContainer.setLayoutParams(params);
        actionButtonContainer.setHorizontalGravity(horizontalGravity);
        actionButtonContainer.setVerticalGravity(verticalGravity);
        actionButtonContainer.setId(containerId);
        return actionButtonContainer;
    }
    /**
     * 根据指定的参数创建按钮
     * @param id 按钮的唯一标识
     * @param layout 布局参数
     * @param description 描述
     * @param text button的文字内容
     * @param listener 点击事件的监听对象
     * @return
     */
    protected Button createButton(int id, RelativeLayout.LayoutParams layout,
            String description, String text, OnClickListener listener) {
        Button button = new Button(mContext);
        button.setLayoutParams(layout);
        button.setContentDescription(description);
        button.setText(text);
        button.setId(id);
        button.setOnClickListener(listener);
        return button;
    }
    /**
     * 创建文本编辑框
     * @param id 文本编辑框的唯一标识
     * @param layoutParams 布局参数
     * @param isSingleLine 是否是单行显示
     * @param text 文本框的内容
     * @param inputType 输入类型
     * @param imeOption ime选项
     * @param listener 事件监听器
     * @return
     */
    protected EditText createEditText(int id,
            RelativeLayout.LayoutParams layoutParams, boolean isSingleLine,
            String text, int inputType, int imeOption,
            View.OnKeyListener listener) {
        EditText editText = new EditText(mContext);
        editText.setLayoutParams(layoutParams);
        editText.setId(id);
        editText.setSingleLine(true);
        editText.setText(text);
        editText.setInputType(inputType);
        editText.setImeOptions(imeOption);
        editText.setOnKeyListener(listener);
        return editText;
    }

    /**
     * 创建WebView
     * @param url WebView显示的url
     * @return
     */
    protected void createWebView(String url,LinearLayout mainLayout) {
        mWebView =   new XInAppWebView(mContext, this, url, mWebContext);
        mainLayout.addView(mWebView);
    }

    /**
     * 将DIP转化为像素
     *
     * @param dipValue
     *            dip的值
     * @return int
     */
    protected int dpToPixels(int dipValue) {
        int value = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, (float) dipValue, mContext
                        .getResources().getDisplayMetrics());

        return value;
    }

    /**
     * 构建浏览器
     */
    public XInAppBrowser buildBrowser()
    {
        mDialog = createDialog(android.R.style.Theme_NoTitleBar,
                 android.R.style.Animation_Dialog, Window.FEATURE_NO_TITLE);
        LinearLayout mainLayout = buildMainContainerLayout();

        if(mIsShowToolBar)
        {
            buildToolbar(mainLayout);
        }

        createWebView( mUrl,mainLayout);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(mDialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mDialog.getWindow().setAttributes(layoutParams);

        mDialog.setContentView(mainLayout);

        return this;
    }

    /**
     * 构建ToolBar
     * @param mainLayout dialog的布局
     */
    private void buildToolbar(LinearLayout mainLayout)
    {
        RelativeLayout toolBarLayout = buildToolbarLayout();
        RelativeLayout buttonContainerLayout =  buildActionButtonContainerLayout();
        Button backButton = buildBackButton();
        Button forwardButton = buildForwardButton();
        Button closeButton = buildCloseButton();
        buttonContainerLayout.addView(backButton);
        buttonContainerLayout.addView(forwardButton);
        buildEditText();
        toolBarLayout.addView(buttonContainerLayout);
        toolBarLayout.addView(mEditText);
        toolBarLayout.addView(closeButton);
        mainLayout.addView(toolBarLayout);
    }

    /**
     * 显示
     */
    public void show()
    {
        mDialog.show();
    }

    /**
     * 回退到前一个页面
     */
    private void goBack() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        }
    }

    /**
       * 前进到下一个页面
     */
    private void goForward() {
        if (mWebView.canGoForward()) {
            mWebView.goForward();
        }
    }

    /**
     * 导航到新页面
     * @param url
     *            要加载的页面的Url
     */
    private void navigate(String url) {
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        if (isCompleteFormUrl(url)) {
            mWebView.loadUrl(url);
        } else {
            mWebView.loadUrl("http://" + url);
        }
        mWebView.requestFocus();
    }

    /**
     * 关闭对话框
     */
    public void closeDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mBrowserListener.onDismiss(mWebContext);
        }
    }

    @Override
    public void onLoadFinished(String url, XIWebContext webContext) {
        mBrowserListener.onLoadFinished(url, webContext);
    }

    @Override
    public void onLoadStart(String url, XIWebContext webContext) {
          String newloc;
          if (isCompleteFormUrl(url)) {
              newloc = url;
          } else {
              newloc = "http://" + url;
          }
          if (mEditText != null && !newloc.equals(mEditText.getText().toString())) {
              mEditText.setText(newloc);
          }
        mBrowserListener.onLoadStart(url, webContext);

    }

    @Override
    public void onDismiss(XIWebContext webContext) {
        mBrowserListener.onDismiss(webContext);

    }

    /**
     * 判断url是不是完整格式，以http或者file开头
     * @param url
     * @return
     */
    private boolean isCompleteFormUrl(String url)
    {
        return (url.startsWith("http") || url.startsWith("file"));
    }

    public void setInjectCallbackContext(XCallbackContext callbackCtx) {
        mWebView.setWebChromeClient(new InAppBrowserClient(callbackCtx));
    }
    /**
     * 提供统一的方法给InAppBrowser的WebView注入一个对象(script or style)
     *
     *如果提供了包装数据，源字符串会被格式化JSON形式并且被jsWrapper封装
     *
     * @param source      要注入到文档中的源对象(filename or script/style text)
     * @param jsWrapper   用来封装源数据串的JS字符串, 以便对象通过合适的方式注入，如果该串为null并且源串为JS代码，则直接执行
     */
    public void injectDeferredObject(String source, String jsWrapper) {
        String scriptToInject;
        if (jsWrapper != null) {
            org.json.JSONArray jsonEsc = new org.json.JSONArray();
            jsonEsc.put(source);
            String jsonRepr = jsonEsc.toString();
            String jsonSourceString = jsonRepr.substring(1, jsonRepr.length()-1);
            scriptToInject = String.format(jsWrapper, jsonSourceString);
        } else {
            scriptToInject = source;
        }
        mWebView.loadUrl("javascript:" + scriptToInject);
    }
}
