
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

package com.polyvi.xface.extension.xmlhttprequest;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.Header;
import org.apache.http.client.HttpResponseException;
import org.apache.http.message.BasicHeader;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.polyvi.xface.http.XAsyncHttpResponseHandler;
import com.polyvi.xface.http.XHttpWorker;

/**
 * 参照W3C规范 实现的ajax请求对象
 * 
 */
public class XXMLHttpRequest extends XAsyncHttpResponseHandler implements
        XIAjaxDataInterface {

    //FIXME:后续可以添加其他方法的支持
    private static final String GET = "GET";
    private static final String POST = "POST";

    private static String[] SupportMethods = { GET, POST };
    /*
                  状态                           名称                                        描述
        0       UNSEND               初始化状态, XMLHttpRequest 对象已创建或已被 abort() 方法重置；
        1       OPENED               open()方法已调用；
        2       HEADERS_RECEIVED     所有响应头部都已经接收到
        3       LOADING              正在接收服务器响应体数据
        4       DONE                 数据接收完毕或者请求被中断
      */
    static enum State {
        UNSEND, OPENED, HEADERS_RECEIVED, LOADING, DONE
    }

    private State mState;
    private Map<String, String> mRequestHeaders; // 请求头部
    private XHttpWorker mNetWork;  //网络请求服务对象
    private String mUrl;
    private String mMethod;
    private String mPostData;
    private XAjaxRequestListener mListener;
    private boolean mError;

    private int mHttpStatusCode;// http状态码
    private String mResponseText; // 响应文本
    private Header[] mResponseHeaders; // 响应头部

    private Context mContext;

    public XXMLHttpRequest(Context context) {
        super();
        mRequestHeaders = new ConcurrentHashMap<String, String>();
        mContext = context;
        mNetWork = new XHttpWorker();
        mResponseText = "";
        mState = State.UNSEND;

    }

    public void open(String method, String url) throws XAjaxException {
        // FIXME: 暂不支持用户名和密码
        if (!isMethodSupported(method)) {
            throw new XAjaxException(
                    XAjaxException.ErrorCode.METHOD_NOT_SUPPORT);
        }
        internalAbort();
        State previouseState = mState;
        mState = State.UNSEND;
        mError = false;
        clearRequest();
        clearResponse();
        if (mState.ordinal() != State.UNSEND.ordinal()) {
            throw new XAjaxException(XAjaxException.ErrorCode.INVALID_STATE_ERR);
        }

        mUrl = url;
        mMethod = method;
        // 检查前一个状态 防止应用多次调用open派发多次stateReadyChange事件
        if (previouseState.ordinal() != State.OPENED.ordinal()) {
            this.changeState(State.OPENED);
        } else {
            mState = State.OPENED;
        }

    }

    public void send(String data) throws XAjaxException {
        // FIXME:暂时不支持二进制的发送
        if (mState != State.OPENED) {
            throw new XAjaxException(XAjaxException.ErrorCode.INVALID_STATE_ERR);
        }

        mPostData = data;
        mError = false;
        doRequest();
    }

    public void setRequestHeader(String name, String value)
            throws XAjaxException {
        if (mState != State.OPENED) {
            throw new XAjaxException(XAjaxException.ErrorCode.INVALID_STATE_ERR);
        }

        if (!isHeaderValueValid(value)) {
            throw new XAjaxException(
                    XAjaxException.ErrorCode.INVALID_HEADER_VALUE);
        }
        mRequestHeaders.put(name, value);
    }

    /**
     * 中断请求
     */
    public void abort() {
        internalAbort();
        clearResponse();
        clearRequest();
        if (mListener != null)
            mListener.onAbort();
        if (mState.ordinal() > State.OPENED.ordinal()
                && mState.ordinal() != State.DONE.ordinal()) {
            changeState(State.DONE);
        }
        mState = State.UNSEND;
       
    }

    /**
     * 设置ajax请求监听器
     *
     * @param listener
     */
    public void setRequestListener(XAjaxRequestListener listener) {
        mListener = listener;
        if( null != mListener )
            mListener.setAjaxDataInterface(this);
    }

    public XAjaxRequestListener getRequestListener()
    {
        return mListener;
    }
    
    /**
     * 是否是有效的值
     * 
     * @param value
     */
    private boolean isHeaderValueValid(String value) {
        return !value.contains("\r") && !value.contains("\n");
    }

    /**
     * ajax状态改变
     * 
     * @param newState
     */
    private void changeState(State newState) {
        if (mState.ordinal() != newState.ordinal()) {
            mState = newState;
            if (null != mListener)
                mListener.onReadyStateChanged();
        }
    }

    /**
     * 发生网络错误
     */
    private void networkError() {
        clearRequest();
        clearResponse();
        mError = true;
       
        if (null != mListener)
            mListener.onNetworkError();
        changeState(State.DONE);
        internalAbort();
    }

    /**
     * 发送ajax请求
     * 
     * @return
     * @throws XAjaxException
     */
    private void doRequest() throws XAjaxException {
        addCookie(mUrl);
        if (mMethod.equalsIgnoreCase(GET)) {
            mNetWork.get(mUrl, this.convertHeaders(mRequestHeaders), this);

        } else if (mMethod.equalsIgnoreCase(POST)) {
            try {
                mNetWork.post(mUrl, this.convertHeaders(mRequestHeaders),
                        mPostData, null, this);
            } catch (IOException e) {
                throw new XAjaxException(
                        XAjaxException.ErrorCode.HTTP_REQUEST_ERROR);
            }
        } else {
            throw new XAjaxException(
                    XAjaxException.ErrorCode.METHOD_NOT_SUPPORT);
        }
    }

    /**
     * 增加cookie属性
     * 
     * @param url
     *            请求的url地址
     */
    private void addCookie(String url) {
        // 默认增加cookie的支持
        CookieSyncManager cookieSyncManager = CookieSyncManager
                .createInstance(mContext);
        cookieSyncManager.startSync();
        String cookie = CookieManager.getInstance().getCookie(url);
        if (cookie != null) {
            mRequestHeaders.put("Cookie", cookie);
        }
    }

    /**
     * 将hash表的头部转换为header[]
     * 
     * @return
     */
    private Header[] convertHeaders(Map<String, String> mapHeaders) {
        Header[] headers = new Header[mapHeaders.size()];
        Iterator<Entry<String, String>> iter = mapHeaders.entrySet().iterator();
        int i = 0;
        while (iter.hasNext()) {
            Entry<String, String> entry = iter.next();
            Header header = new BasicHeader(entry.getKey(), entry.getValue());
            headers[i++] = header;
        }
        return headers;
    }

    /**
     * 清空对象状态
     */
    private void internalAbort() {
        // 取消请求
        mNetWork.cancelRequest(true);
        mError = true;
    }

    /**
     * 清空请求头部
     */
    private void clearRequest() {
        mRequestHeaders.clear();
        mPostData = null;
        mMethod = null;
        mUrl = null;

    }

    /**
     * 清空响应
     */
    private void clearResponse() {
        mResponseHeaders = null;
        mResponseText = "";
        mHttpStatusCode = 0;
    }

    /**
     * 检查方法名的有效性
     * 
     * @param name
     * @return
     */
    private boolean isMethodSupported(String name) {
        for (String method : SupportMethods) {
            if (method.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getReadyState() {
        return mState.ordinal();
    }

    @Override
    public int getStatus() {
        return mHttpStatusCode;
    }

    @Override
    public String getResponseText() {
        return mResponseText;
    }

    public String getResponseHeader(String name) {
        if (null == mResponseHeaders)
            return null;
        for (Header header : mResponseHeaders) {
            if (header.getName().equals(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    @Override
    public Header[] getAllResponseHeader() {
        return mResponseHeaders;
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, String content) {
        // 如果已经出错 则不再处理
        if (mError) {
            return;
        }
        // FIXME:底层的数据是一次性返回的 无需考虑多次接收数据
        this.changeState(State.HEADERS_RECEIVED);
        mHttpStatusCode = statusCode;
        mResponseText = content;
        mResponseHeaders = headers;
        this.changeState(State.LOADING);
        this.changeState(State.DONE);
    }

    @Override
    public void onFailure(Throwable error, String content) {
        // 如果已经出错 则不再处理
        if (mError) {
            return;
        }
        error.printStackTrace();
        if (error instanceof HttpResponseException) {
            mHttpStatusCode = ((HttpResponseException) error).getStatusCode();
        }
        mResponseText = content;
        this.networkError();
    }

}
