
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

package com.polyvi.xface.extension.filetransfer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XConstant;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XPathResolver;

public class XFileTransferExt extends XExtension {

    private static final String CLASS_NAME = XFileTransferExt.class.getSimpleName();

    private static final String ENCODING_TYPE = "UTF-8";
    private static final int CONNECTION_TIME_OUT_MILLISECONDS = 5000;

    private static final String JSON_EXCEPTION_MISSING_SOURCE_OR_TARGET = "Missing source or target";
    private static final String ILLEGAL_ARGUMENT_EXCEPTION_NOT_IN_ROOT_DIR = "filePath is not in root directory";
    private static final String ILLEGAL_ARGUMENT_EXCEPTION_NAME_CONTAINS_COLON = "This file has a : in its name";
    private static final String JSON_EXCEPTION_MISSING_OBJECT_ID = "Missing objectId";
    private static final String ABORT_EXCEPTION_DOWNLOAD_ABORTED = "download aborted";
    private static final String ABORT_EXCEPTION_UPLOAD_ABORTED = "upload aborted";


    private static final String COMMAND_DOWNLOAD = "download";
    private static final String COMMAND_UPLOAD = "upload";
    private static final String COMMAND_ABORT = "abort";

    private static final int FILE_NOT_FOUND_ERR = 1;
    private static final int INVALID_URL_ERR = 2;
    private static final int CONNECTION_ERR = 3;
    private static final int ABORTED_ERR = 4;

    private static final String LINE_START = "--";
    private static final String LINE_END = "\r\n";
    private static final String BOUNDARY =  "*****";

    private static HashSet<String> abortTriggered = new HashSet<String>();

    private SSLSocketFactory mDefaultSSLSocketFactory = null;
    private HostnameVerifier mDefaultHostnameVerifier = null;

    private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private static class AbortException extends Exception {
        private static final long serialVersionUID = 1L;
        public AbortException(String str) {
            super(str);
        }
    }

    @Override
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        super.init(extensionContext,webContext);
    }

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
        if(action.equals(COMMAND_DOWNLOAD) || action.equals(COMMAND_UPLOAD)) {
            String source = null;
            String target = null;
            String appWorkSpace = mWebContext.getWorkSpace();
            try {
                source = args.getString(0);
                target = args.getString(1);
            } catch (JSONException e) {
                XLog.d(CLASS_NAME, JSON_EXCEPTION_MISSING_SOURCE_OR_TARGET);
                return new XExtensionResult(XExtensionResult.Status.JSON_EXCEPTION, JSON_EXCEPTION_MISSING_SOURCE_OR_TARGET);
            }
            if (action.equals(COMMAND_DOWNLOAD)) {
                return download(appWorkSpace, source, target, args, callbackCtx);
            }
            else if (action.equals(COMMAND_UPLOAD)) {
                return upload(appWorkSpace, source, target, args, callbackCtx);
            }
        } else if (action.equals(COMMAND_ABORT)) {
            return abort(args);
        }
        return new XExtensionResult(XExtensionResult.Status.INVALID_ACTION);
    }

    /**
     * 返回一个代表文件的JSON对象
     * @param appWorkSpace 当前应用工作目录
     * @param source       服务器的URL
     * @param target       设备上的路径
     * @param args         下载需要的参数
     * @param callbackCtx   native端js回调的上下文环境
     * @return             代表文件的JSON对象
     * @throws IOException
     * @throws JSONException
     */
    private XExtensionResult download(String appWorkSpace, String source,
            String target, JSONArray args, XCallbackContext callbackCtx)
            throws JSONException {
        HttpURLConnection connection = null;
        try {
            boolean trustEveryone = args.optBoolean(2);
            String objectId = args.getString(3);
            if(target.contains(":")) {
                JSONObject error = createFileTransferError(FILE_NOT_FOUND_ERR, source, target, connection);
                XLog.e(CLASS_NAME, ILLEGAL_ARGUMENT_EXCEPTION_NAME_CONTAINS_COLON);
                return new XExtensionResult(XExtensionResult.Status.ERROR, error);
            }
            File file = new File(appWorkSpace, target);

            if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
                JSONObject error = createFileTransferError(FILE_NOT_FOUND_ERR, source, target, connection);
                XLog.e(CLASS_NAME, ILLEGAL_ARGUMENT_EXCEPTION_NOT_IN_ROOT_DIR);
                return new XExtensionResult(XExtensionResult.Status.ERROR, error);
            }
            file.getParentFile().mkdirs();

            // 连接服务器
            URL url = new URL(source);
            //TODO:这里只是简单的实现，以后可能会有更加复杂的需求
            connection = getURLConnection(url, trustEveryone);;
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIME_OUT_MILLISECONDS);
            setCookieProperty(connection, source);
            connection.connect();
            XLog.d(CLASS_NAME, "Download file:" + url);

            InputStream inputStream = connection.getInputStream();
            byte[] buffer = new byte[XConstant.BUFFER_LEN];
            int bytesRead = 0;
            long totalBytes = 0;

            FileTransferProgress progress = new FileTransferProgress();
            if (connection.getContentEncoding() == null) {
                progress.setLengthComputable(true);
                progress.setTotal(connection.getContentLength());
            }

            FileOutputStream outputStream = new FileOutputStream(file);
            while ( (bytesRead = inputStream.read(buffer)) > 0 ) {
                outputStream.write(buffer,0, bytesRead);
                totalBytes += bytesRead;
                if (objectId != null) {
                    //只有js层传送过来一个object ID我们才会更新进度回调
                    progress.setLoaded(totalBytes);
                    XExtensionResult progressResult = new XExtensionResult(XExtensionResult.Status.OK, progress.toJSONObject());
                    progressResult.setKeepCallback(true);
                    callbackCtx.sendExtensionResult(progressResult);
                }
                synchronized (abortTriggered) {
                    if (objectId != null && abortTriggered.contains(objectId)) {
                        abortTriggered.remove(objectId);
                        throw new AbortException(ABORT_EXCEPTION_DOWNLOAD_ABORTED);
                    }
                }
            }
            outputStream.close();
            inputStream.close();
            XLog.d(CLASS_NAME, "Saved file: " + target);
            JSONObject entry = XFileUtils.getEntry(appWorkSpace, file);
            // 还原设置
            if (trustEveryone && url.getProtocol().toLowerCase().equals("https")) {
                ((HttpsURLConnection) connection).setHostnameVerifier(mDefaultHostnameVerifier);
                HttpsURLConnection.setDefaultSSLSocketFactory(mDefaultSSLSocketFactory);
            }
            return new XExtensionResult(XExtensionResult.Status.OK, entry);
        } catch (AbortException e) {
            JSONObject error = createFileTransferError(ABORTED_ERR, source, target, connection);
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (FileNotFoundException e) {
            JSONObject error = createFileTransferError(FILE_NOT_FOUND_ERR, source, target, connection);
            XLog.e(CLASS_NAME, error.toString());
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (MalformedURLException e) {
            JSONObject error = createFileTransferError(INVALID_URL_ERR, source, target, connection);
            XLog.e(CLASS_NAME, error.toString());
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (Exception e) {
            JSONObject error = createFileTransferError(CONNECTION_ERR, source, target, connection);
            XLog.e(CLASS_NAME, error.toString());
            return new XExtensionResult(XExtensionResult.Status.IO_EXCEPTION, error);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 上传指定文件到指定服务器
     * @param appWorkspace  当前应用工作目录
     * @param source        要上传的本地文件路径
     * @param target        接收文件的服务器地址
     * @param args          JSONArray
     * @param callbackCtx   native端js回调的上下文环境
     *
     * args[2] fileKey       表单元素的name值，如果没有设置默认为 “file”
     * args[3] fileName      希望文件存储到服务器所用的文件名，如果没有设置默认为 “image.jpg”
     * args[4] mimeType      正在上传数据所使用的mime类型，如果没有设置默认为“image/jpeg”
     * args[5] params        通过HTTP请求发送到服务器的一系列可选键/值对
     * args[6] trustEveryone 信任所有的主机
     * args[7] chunkedMode   数据是否以块流模式上传，如果没有这个参数，默认该值为true
     * @return FileUploadResult
     */
    private XExtensionResult upload(String appWorkspace, String source,
            String target, JSONArray args, XCallbackContext callbackCtx) {
        XLog.d(CLASS_NAME, "upload " + source + " to " +  target);

        HttpURLConnection conn = null;
        try {
            String fileKey = getArgument(args, 2, "file");
            String fileName = getArgument(args, 3, "image.jpg");
            String mimeType = getArgument(args, 4, "image/jpeg");
            JSONObject params = args.optJSONObject(5);
            if (params == null) {
                params = new JSONObject();
            }
            boolean trustEveryone = args.optBoolean(6);
            boolean chunkedMode = args.optBoolean(7) || args.isNull(7);
            JSONObject headers = args.optJSONObject(8);
            if (headers == null && params != null) {
                headers = params.optJSONObject("headers");
            }
            String objectId = args.getString(9);
            //------------------ 客户端请求
            URL url = new URL(target);
            conn = getURLConnection(url, trustEveryone);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
            setCookieProperty(conn, target);
            // 处理头部信息
            handleRequestHeader(headers, conn);
            byte[] extraBytes = extraBytesFromParams(params, fileKey);

            String midParams = "\"" + LINE_END + "Content-Type: " + mimeType + LINE_END + LINE_END;
            String tailParams = LINE_END + LINE_START + BOUNDARY + LINE_START + LINE_END;
            byte[] fileNameBytes = fileName.getBytes(ENCODING_TYPE);

            FileInputStream fileInputStream = (FileInputStream)getPathFromUri(appWorkspace, source);
            int maxBufferSize = XConstant.BUFFER_LEN;

            if (chunkedMode) {
                conn.setChunkedStreamingMode(maxBufferSize);
            } else {
                int stringLength = extraBytes.length + midParams.length()
                        + tailParams.length() + fileNameBytes.length;
                XLog.d(CLASS_NAME, "String Length: " + stringLength);
                int fixedLength = (int) fileInputStream.getChannel().size() + stringLength;
                XLog.d(CLASS_NAME, "Content Length: " + fixedLength);
                conn.setFixedLengthStreamingMode(fixedLength);
            }
            // 开始向服务器上传数据
            OutputStream  ouputStream = conn.getOutputStream();
            DataOutputStream dos = new DataOutputStream( ouputStream);
            dos.write(extraBytes);
            dos.write(fileNameBytes);
            dos.writeBytes(midParams);
            XFileUploadResult result = new XFileUploadResult();
            FileTransferProgress progress = new FileTransferProgress();
            int bytesAvailable = fileInputStream.available();
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];
            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            long totalBytes = 0;

            while (bytesRead > 0) {
                totalBytes += bytesRead;
                result.setBytesSent(totalBytes);
                dos.write(buffer, 0, bytesRead);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                if (objectId != null) {
                    //只有js层传送过来一个object ID我们才会更新进度回调
                    progress.setTotal(bytesAvailable);
                    XLog.d(CLASS_NAME, "total="+bytesAvailable);
                    progress.setLoaded(totalBytes);
                    progress.setLengthComputable(true);
                    XExtensionResult progressResult = new XExtensionResult(XExtensionResult.Status.OK, progress.toJSONObject());
                    progressResult.setKeepCallback(true);
                    callbackCtx.sendExtensionResult(progressResult);
                }
                synchronized (abortTriggered) {
                    if (objectId != null && abortTriggered.contains(objectId)) {
                        abortTriggered.remove(objectId);
                        throw new AbortException(ABORT_EXCEPTION_UPLOAD_ABORTED);
                    }
                }
            }
            dos.writeBytes(tailParams);
            fileInputStream.close();
            dos.flush();
            dos.close();
            checkConnection(conn);
            setUploadResult(result, conn);

            // 还原设置
            if (trustEveryone && url.getProtocol().toLowerCase().equals("https")) {
                ((HttpsURLConnection) conn).setHostnameVerifier(mDefaultHostnameVerifier);
                HttpsURLConnection.setDefaultSSLSocketFactory(mDefaultSSLSocketFactory);
            }

            XLog.d(CLASS_NAME, "****** About to return a result from upload");
            return new XExtensionResult(XExtensionResult.Status.OK, result.toJSONObject());

        } catch (AbortException e) {
            JSONObject error = createFileTransferError(ABORTED_ERR, source, target, conn);
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (FileNotFoundException e) {
            JSONObject error = createFileTransferError(FILE_NOT_FOUND_ERR, source, target, conn);
            XLog.e(CLASS_NAME, error.toString());
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (MalformedURLException e) {
            JSONObject error = createFileTransferError(INVALID_URL_ERR, source, target, conn);
            XLog.e(CLASS_NAME, error.toString());
            return new XExtensionResult(XExtensionResult.Status.ERROR, error);
        } catch (IOException e) {
            JSONObject error = createFileTransferError(CONNECTION_ERR, source, target, conn);
            XLog.e(CLASS_NAME, error.toString());
            return new XExtensionResult(XExtensionResult.Status.IO_EXCEPTION, error);
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage());
            return new XExtensionResult(XExtensionResult.Status.JSON_EXCEPTION);
        } catch (Throwable t) {
            JSONObject error = createFileTransferError(CONNECTION_ERR, source, target, conn);
            XLog.e(CLASS_NAME, error.toString());
            return new XExtensionResult(XExtensionResult.Status.IO_EXCEPTION, error);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 处理请求头部
     * @param headers       头部信息
     * @param conn          Http连接
     */
    private void handleRequestHeader(JSONObject headers, HttpURLConnection conn) {
        if (headers != null) {
            try {
                for (Iterator iter = headers.keys(); iter.hasNext(); ) {
                    String headerKey = iter.next().toString();
                    JSONArray headerValues = headers.optJSONArray(headerKey);
                    if (headerValues == null) {
                        headerValues = new JSONArray();
                        headerValues.put(headers.getString(headerKey));
                    }
                    conn.setRequestProperty(headerKey, headerValues.getString(0));
                    for (int index = 1; index < headerValues.length(); ++index) {
                        conn.addRequestProperty(headerKey, headerValues.getString(index));
                    }
                }
            } catch (JSONException e1) {
                XLog.d(CLASS_NAME, "No headers to be manipulated!");
            }
        }
    }

    /**
     * 获取Http连接
     * @param url           服务器地址
     * @param trustEveryone 信任所有的主机
     */
    private HttpURLConnection getURLConnection(URL url, boolean trustEveryone) throws IOException {
        HttpURLConnection conn = null;
        // 根据URL的协议打开一个HTTP连接
        if (url.getProtocol().toLowerCase().equals("https")) {
            // 使用标准的HTTPS连接. 不允许自己签名
            if (!trustEveryone) {
                conn = (HttpsURLConnection) url.openConnection();
            }
            else {
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                // 保存当前的 hostnameVerifier以备还原
                mDefaultHostnameVerifier = https.getHostnameVerifier();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            }
        }
        // 使用标准的HTTP连接
        else {
            conn = (HttpURLConnection) url.openConnection();
        }
        return conn;
    }

    /**
     * 从params中提取出Bytes
     * @param params        js传过来的参数
     * @param fileKey       表单元素的name值
     * @return   Bytes[]
     */
    private byte[] extraBytesFromParams(JSONObject params, String fileKey) throws UnsupportedEncodingException {
        StringBuilder extraParams = new StringBuilder();
        try {
            for (Iterator iter = params.keys(); iter.hasNext();) {
                Object key = iter.next();
                if(!String.valueOf(key).equals("headers")) {
                  extraParams.append(LINE_START + BOUNDARY + LINE_END);
                  extraParams.append("Content-Disposition: form-data; name=\"" +  key.toString() + "\";");
                  extraParams.append(LINE_END + LINE_END);
                  extraParams.append(params.getString(key.toString()));
                  extraParams.append(LINE_END);
                }
            }
        } catch (JSONException e) {
            XLog.d(CLASS_NAME, e.getMessage());
        }

        extraParams.append(LINE_START + BOUNDARY + LINE_END);
        extraParams.append("Content-Disposition: form-data; name=\"" + fileKey + "\";" + " filename=\"");
        return extraParams.toString().getBytes(ENCODING_TYPE);
    }

    /**
     * 将服务器发来的响应设置到XFileUploadResult中
     * @param result        返回给js端
     * @param conn          Http连接
     */
    private void setUploadResult(XFileUploadResult result, HttpURLConnection conn) {
        StringBuffer responseString = new StringBuffer("");
        DataInputStream inStream = null;
        try {
            inStream = new DataInputStream( conn.getInputStream() );
            String line = null;
            while (( line = inStream.readLine()) != null) {
                responseString.append(line);
            }

            // 设置XFileUploadResult的数据
            result.setResponseCode(conn.getResponseCode());
            result.setResponse(responseString.toString());
            inStream.close();
        } catch(FileNotFoundException e) {
            XLog.e(CLASS_NAME, e.toString());
        } catch (IOException e) {
            XLog.e(CLASS_NAME, e.toString());
        }
    }
    /**
     * 安装一个信任所有SSL证书的TrustManager。增加这个函数的原因是使开发者能使用自己签名的SSL证书。
     * 标准的HttpsURLConnection类在使用自己签名的证书时如果没有这段代码会抛出异常
     */
    private void trustAllHosts() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
            }
        } };

        // 安装all-trusting TrustManager
        try {
            // 备份当前的SSL套接字工厂
            mDefaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            // 安装自己的TrustManager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            XLog.e(CLASS_NAME, e.getMessage());
        }
    }

    /**
     * 从JSONArray中读取参数.
     * @param args          js端传过来的JSONArray
     * @param position      要获取的参数位置
     * @param defaultString 默认值
     * @return 获取到的参数值
     */
    private String getArgument(JSONArray args, int position, String defaultString) {
        String arg = defaultString;
        if (args.length() >= position) {
            arg = args.optString(position);
            if (arg == null || "null".equals(arg)) {
                arg = defaultString;
            }
        }
        return arg;
    }

    /**
     *  从文件路径或者content://uri路径获取InputStream
     *
     * @param  path 本地路径
     * @return an input stream
     * @throws FileNotFoundException
     */
    private InputStream getPathFromUri(String appWorkspace, String path) throws FileNotFoundException {
        XPathResolver pathResolver = new XPathResolver(path, appWorkspace, getContext());
        String filepath = pathResolver.resolve();
        if(null == filepath) {
            throw new FileNotFoundException();
        }
        return new FileInputStream(filepath);
    }

    /**
     * 创建FileTransferError对象
     * @param errorCode     错误码
     * @return JSONObject   包含错误的JSON对象
     */
    private JSONObject createFileTransferError(int errorCode, String source,
            String target, HttpURLConnection connection) {
        Integer httpStatus = null;
        if (connection != null) {
            try {
                httpStatus = connection.getResponseCode();
            } catch (IOException e) {
                XLog.e(CLASS_NAME, "Error getting HTTP status code from connection.");
            }
        }

        JSONObject error = null;
        try {
            error = new JSONObject();
            error.put("code", errorCode);
            error.put("source", source);
            error.put("target", target);
            if (httpStatus != null) {
                error.put("http_status", httpStatus);
            }
        } catch (JSONException e) {
            XLog.e(CLASS_NAME, e.getMessage());
        }
        return error;
    }

    /**
     * 终止正在上传或下载的任务
     *
     * @param args          函数执行的参数
     */
    private XExtensionResult abort(JSONArray args) {
        String objectId;
        try {
            objectId = args.getString(0);
        } catch (JSONException e) {
            XLog.d(CLASS_NAME, JSON_EXCEPTION_MISSING_OBJECT_ID);
            return new XExtensionResult(XExtensionResult.Status.JSON_EXCEPTION, "Missing objectId");
        }
        synchronized (abortTriggered) {
            abortTriggered.add(objectId);
        }
        return new XExtensionResult(XExtensionResult.Status.OK);
    }

    /**
     * 设置connection的Cookie
     * @param connection   Http连接
     * @param propert       cookie要设置的属性
     */
    private void setCookieProperty(HttpURLConnection connection, String propert) {
        //Add cookie support
        CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(getContext());
        cookieSyncManager.startSync();
        String cookie = CookieManager.getInstance().getCookie(propert);
        if(cookie != null)
        {
          connection.setRequestProperty("cookie", cookie);
        }
    }

    /**
     * 检查连接是否成功
     *
     * @param  con    要检查的连接
     * @throws IOException
     */
    private void checkConnection(HttpURLConnection con)
            throws MalformedURLException, IOException {
        int responseCode = con.getResponseCode();
        if (HttpURLConnection.HTTP_OK != responseCode) {
            throw new MalformedURLException("" + responseCode);
        }
    }
}
