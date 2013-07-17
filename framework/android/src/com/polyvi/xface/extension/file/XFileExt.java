
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

package com.polyvi.xface.extension.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.polyvi.xface.exception.XEncodingException;
import com.polyvi.xface.exception.XFileExistsException;
import com.polyvi.xface.exception.XInvalidModificationException;
import com.polyvi.xface.exception.XNoModificationAllowedException;
import com.polyvi.xface.exception.XTypeMismatchException;
import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XBase64;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;

public class XFileExt extends XExtension {

    private static final String IOEXCEPTION_NO_FILESYSTEM_TYPE = "No filesystem of type requested";
    private static final String XINVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR="filePath is not in root directory";


    /**  File 提供给js用户的接口名字*/
    private static final String COMMAND_WRITE = "write";
    private static final String COMMAND_TRUNCATE = "truncate";
    private static final String COMMAND_GETFILE = "getFile";
    private static final String COMMAND_GETDIRECTORY= "getDirectory";
    private static final String COMMAND_GETFILEMETADATA = "getFileMetadata";
    private static final String COMMAND_COPYTO = "copyTo";
    private static final String COMMAND_MOVETO = "moveTo";
    private static final String COMMAND_REMOVE = "remove";
    private static final String COMMAND_GETPARENT = "getParent";
    private static final String COMMAND_REQUESTFILESYSTEM = "requestFileSystem";
    private static final String COMMAND_REMOVERECURSIVELY = "removeRecursively";
    private static final String COMMAND_READ_AS_TEXT = "readAsText";
    private static final String COMMAND_READ_AS_DATA_URL = "readAsDataURL";
    private static final String COMMAND_READ_AS_ARRAY_BUFFER = "readAsArrayBuffer";
    private static final String COMMAND_READ_AS_BINARY_STRING = "readAsBinaryString";
    private static final String COMMAND_READENTRIES = "readEntries";
    private static final String COMMAND_RESOLVELOCALFILESYSTEMURI = "resolveLocalFileSystemURI";
    private static final String COMMAND_GETMETADATA = "getMetadata";

    private static final int NOT_FOUND_ERR = 1;
    private static final int SECURITY_ERR = 2;
    private static final int ABORT_ERR = 3;

    private static final int NOT_READABLE_ERR = 4;
    private static final int ENCODING_ERR = 5;
    private static final int NO_MODIFICATION_ALLOWED_ERR = 6;
    private static final int INVALID_STATE_ERR = 7;
    private static final int SYNTAX_ERR = 8;
    private static final int INVALID_MODIFICATION_ERR = 9;
    private static final int QUOTA_EXCEEDED_ERR = 10;
    private static final int TYPE_MISMATCH_ERR = 11;
    private static final int PATH_EXISTS_ERR = 12;

    private static final int TEMPORARY = 0;
    private static final int PERSISTENT = 1;

    private XFile mFile;

    @Override
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        super.init(extensionContext, webContext);
        mFile = (XFile) getNativeWorker();
    }

    @Override
    protected Object getNativeWorker() {
        return new XFileImpl();
    }

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }

    @Override
    public XExtensionResult exec(String action, JSONArray args,
            XCallbackContext callbackCtx) throws JSONException{
        XExtensionResult.Status status = XExtensionResult.Status.OK;
        String appWorkSpace = mWebContext.getWorkSpace();
        String result = "";
        int errorType = 0;
        try{
            if(action.equals(COMMAND_WRITE)) {
                long size = write(appWorkSpace, args.getString(0), args.getString(1), args.getInt(2));
                return new XExtensionResult(status, size);
            }
            else if(action.equals(COMMAND_TRUNCATE)) {
                long size = truncateFile(appWorkSpace, args.getString(0), args.getLong(1));
                return new XExtensionResult(status, size);
            }
            else if (action.equals(COMMAND_GETFILE)) {
                JSONObject obj = getFile(appWorkSpace, args.getString(0), args.getString(1), args.optJSONObject(2), false);
                return new XExtensionResult(status, obj);
            }
            else if(action.equals(COMMAND_GETFILEMETADATA)) {
                JSONObject obj = getFileMetadata(appWorkSpace, args.getString(0));
                return new XExtensionResult(status, obj);
            }
            else if(action.equals(COMMAND_COPYTO)) {
                JSONObject obj = transferTo(appWorkSpace, args.getString(0), args.getString(1), args.optString(2), false);
                return new XExtensionResult(status, obj);
            }
            else if(action.equals(COMMAND_MOVETO)) {
                JSONObject obj = transferTo(appWorkSpace, args.getString(0), args.getString(1), args.optString(2), true);
                return new XExtensionResult(status, obj);
            }
            else if(action.equals(COMMAND_REMOVE)) {
                boolean success = remove(appWorkSpace, args.getString(0));
                return new XExtensionResult(status, success);
            }
            else if(action.equals(COMMAND_REQUESTFILESYSTEM)) {
                long size = args.optLong(1);
                //TODO:当请求的size不为0时我们需要判断该size是否超出了SD卡或者内存的大小，
                //如果超出要抛出QUOTA_EXCEEDED_ERR异常
                JSONObject obj = requestFileSystem(appWorkSpace, args.getInt(0));
                return new XExtensionResult(status, obj);
            }
            else if(action.equals(COMMAND_GETDIRECTORY)) {
                JSONObject obj = getFile(appWorkSpace, args.getString(0), args.getString(1), args.optJSONObject(2), true);
                return new XExtensionResult(status, obj);
            }
            else if(action.equals(COMMAND_GETPARENT)) {
                JSONObject obj = getParent(appWorkSpace, args.getString(0));
                return new XExtensionResult(status, obj);
            }
            else if(action.equals(COMMAND_REMOVERECURSIVELY)) {
                boolean success = removeRecursively(appWorkSpace, args.getString(0));
                if(success) {
                    return new XExtensionResult(status);
                } else {
                    return new XExtensionResult(XExtensionResult.Status.ERROR, NO_MODIFICATION_ALLOWED_ERR);
                }
            }
            else if(action.equals(COMMAND_READ_AS_TEXT)) {
                String filePath = args.getString(0);
                String encoding = args.getString(1);
                int start= args.getInt(2);
                int end = args.getInt(3);
                readFileAs(appWorkSpace,filePath, encoding ,start,end, callbackCtx, XExtensionResult.MESSAGE_TYPE_STRING);
            }
            else if(action.equals(COMMAND_READ_AS_DATA_URL)) {
                String filePath = args.getString(0);
                int start= args.getInt(2);
                int end = args.getInt(3);
                readFileAs(appWorkSpace,filePath, null , start,end, callbackCtx, -1);
            }
            else if(action.equals(COMMAND_READ_AS_ARRAY_BUFFER)) {
                String filePath = args.getString(0);
                int start= args.getInt(2);
                int end = args.getInt(3);
                readFileAs(appWorkSpace,filePath, null , start,end, callbackCtx, XExtensionResult.MESSAGE_TYPE_ARRAYBUFFER);
            }
            else if(action.equals(COMMAND_READ_AS_BINARY_STRING)) {
                String filePath = args.getString(0);
                int start= args.getInt(2);
                int end = args.getInt(3);
                readFileAs(appWorkSpace,filePath, null ,  start,end, callbackCtx,XExtensionResult.MESSAGE_TYPE_BINARYSTRING);
            }
            else if(action.equals(COMMAND_READENTRIES)) {
                JSONArray array = readEntries(appWorkSpace, args.getString(0));
                return new XExtensionResult(status, array);
            }
            else if(action.equals(COMMAND_RESOLVELOCALFILESYSTEMURI)) {
                JSONObject obj = resolveLocalFileSystemURI(appWorkSpace, args.getString(0));
                return new XExtensionResult(status, obj);
            }
            else if(action.equals(COMMAND_GETMETADATA)) {
                return new XExtensionResult(status, getMetadata(appWorkSpace, args.getString(0)));
            }
            return new XExtensionResult(status, result);
            }catch (FileNotFoundException e) {
                errorType = NOT_FOUND_ERR;
            }catch (MalformedURLException e) {
                errorType = ENCODING_ERR;
            }catch(IOException e) {
                errorType = INVALID_MODIFICATION_ERR;
            } catch (XFileExistsException e) {
                errorType = PATH_EXISTS_ERR;
            } catch (XTypeMismatchException e) {
                errorType = TYPE_MISMATCH_ERR;
            } catch (XEncodingException e) {
                errorType = ENCODING_ERR;
            } catch (XNoModificationAllowedException e) {
                errorType = NO_MODIFICATION_ALLOWED_ERR;
            } catch (XInvalidModificationException e) {
                errorType = INVALID_MODIFICATION_ERR;
            } catch (Exception e) {
                errorType = TYPE_MISMATCH_ERR;
            }
            return new XExtensionResult(XExtensionResult.Status.ERROR, errorType);
    }

    /**
     * 创建或者查找一个文件（夹）.
     * 接口使用方式DirectoryEntry.getFile(fileName, {create: true, exclusive: false}, win, fail);
     * @param appWorkSpace 当前应用工作目录
     * @param dirPath      文件（夹）所在的文件夹路径
     * @param fileName     文件（夹）的名字
     * @param options      指定创建或者不创建,传入的形式为{create: true(false), exclusive: (true)false},
     *                     create标识是否创建文件（夹），exclusive标识当文件（夹）存在但create又为true的时候是否创建成功
     * @param directory    true:创建或查找文件夹, false:创建或查找文件
     * @return             代表文件（夹）的JSON对象
     */
    private JSONObject getFile(String appWorkSpace, String dirPath,
            String fileName, JSONObject options, boolean directory)
            throws XFileExistsException, IOException, XTypeMismatchException,
            XEncodingException, JSONException, XInvalidModificationException {
        boolean create = false;
        boolean exclusive = false;
        if (null != options) {
            create = options.optBoolean("create");
            if (create) {
                exclusive = options.optBoolean("exclusive");
            }
        }
        File file = mFile.getFileObject(appWorkSpace, dirPath, fileName, create, exclusive, directory);
        return XFileUtils.getEntry(appWorkSpace, file);
    }

    /**
     * 返回一个代表文件（夹）的JSON对象
     * @param  filePath 文件路径
     * @return          代表文件（夹）的JSON对象
     * @throws JSONException
     */
    private JSONObject getEntry(String appWorkSpace, String filePath) throws JSONException {
        return XFileUtils.getEntry(appWorkSpace, new File(filePath));
    }

    /**
     * 写文件
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     要写入的文件的路径
     * @param data         要写入的数据
     * @return             开始写入数据的位置
     */
    private long write(String appWorkSpace, String filePath, String data, int position)
            throws FileNotFoundException, IOException, XInvalidModificationException {
        return mFile.write(appWorkSpace, filePath, data, position);
    }

    /**
     * 清除指定长度后的文件内容
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     要清除的文件的路径
     * @param size         清除后剩下的文件大小
     * @return             剩下的文件长度
     */
    private long truncateFile(String appWorkSpace, String filePath, long size)
            throws FileNotFoundException, IOException, XInvalidModificationException {
        return mFile.truncateFile(appWorkSpace, filePath, size);
    }

    /**
     * 获取文件的元数据信息
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     文件的路径
     * @return             文件的元数据信息组成的JSON对象
     */
    private JSONObject getFileMetadata(String appWorkSpace, String filePath)
            throws IOException, FileNotFoundException, JSONException, XInvalidModificationException{
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(XINVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }

        if (!file.exists()) {
            throw new FileNotFoundException("File: " + filePath + " does not exist.");
        }

        JSONObject metadata = new JSONObject();
        metadata.put("size", file.length());
        metadata.put("type", XFileUtils.getMimeType(filePath));
        String fileName;
        String fullPath;
        if(file.getAbsolutePath().equals(appWorkSpace)) {
            fileName = File.separator;
            fullPath = File.separator;
        }
        else {
            fullPath = file.getAbsolutePath().substring(appWorkSpace.length());
            fileName = file.getName();
        }
        metadata.put("name", fileName);
        metadata.put("fullPath", fullPath);
        metadata.put("lastModifiedDate", file.lastModified());

        return metadata;
    }

    /**
     * 文件（夹）的移动或者复制
     * @param appWorkSpace 当前应用工作目录
     * @param oldPath      文件（夹）原始路径
     * @param newPath      文件（夹）新路径
     * @param move         true表示移动文件（夹），false表示复制文件（夹）
     * @return             目的文件流对象
     */
    private JSONObject transferTo(String appWorkSpace, String oldPath, String newPath, String newName, boolean move)
            throws XNoModificationAllowedException, IOException,
            XInvalidModificationException, XEncodingException, JSONException {
        File file = mFile.transferTo(appWorkSpace, oldPath, newPath, newName, move);
        return XFileUtils.getEntry(appWorkSpace, file);
    }

    private boolean remove(String appWorkSpace, String filePath) throws IOException, XInvalidModificationException,
            XNoModificationAllowedException {
        return mFile.remove(appWorkSpace, filePath);
    }

    /**
     * 请求一个文件系统来存储应用数据
     * @param appWorkSpace 当前应用工作目录
     * @param type         文件系统的类型
     * @return             代表文件系统的JSON对象
     * @throws IOException
     * @throws JSONException
     */
    private JSONObject requestFileSystem(String appWorkSpace, int type) throws IOException, JSONException {
        JSONObject fileSystem = new JSONObject();
        if (TEMPORARY == type) {
            fileSystem.put("name", "temporary");
        }
        else if (PERSISTENT == type) {
            fileSystem.put("name", "persistent");
        }
        else {
            throw new IOException(IOEXCEPTION_NO_FILESYSTEM_TYPE);
        }
        fileSystem.put("root", getEntry(appWorkSpace, appWorkSpace));
        return fileSystem;
    }

    /**
     * 获取当前文件（夹）的父目录
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     当前文件（夹）的路径
     * @return             父目录的JSON对象
     */
    private JSONObject getParent(String appWorkSpace,String filePath)
            throws IOException, JSONException, XInvalidModificationException{
        String parentPath = mFile.getParentPath(appWorkSpace, filePath);
        return getEntry(appWorkSpace, parentPath);
    }

    /**
     * 删除文件夹中的所有文件及文件夹目录
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     文件夹的路径
     * @return         true:删除成功,false:删除失败
     */
    private boolean removeRecursively(String appWorkSpace, String filePath)
            throws IOException, XInvalidModificationException {
        return mFile.removeRecursively(appWorkSpace, filePath);
    }

    /**
     * 读取文件内容,并回调读取结果
     * TODO:在后台线程中实现
     *
     * @param appWorkSpace      当前应用工作目录
     * @param filePath          要读取的文件的路径
     * @param start             slice块的起始位置
     * @param end               slice块的结束位置
     * @param callbackContext   回调函数.
     * @param encoding          返回的Sting数据编码(仅用于resultType为String类型的时候),通常应该指定为utf-8.
     * @param resultType        返回的数据类型,如String,二进制buffer,二进制String,Base64,默认为Base64
     * @return
     */
    private void readFileAs(String appWorkSpace, String filePath,  String encoding,  int start, int end,
            XCallbackContext callbackContext,int resultType){
        int errorType = 0;
        try {
            byte[] bytes = mFile.readAsBinary(appWorkSpace, filePath, start, end);
            XExtensionResult result;
            switch (resultType) {
                case XExtensionResult.MESSAGE_TYPE_STRING:
                    result = new XExtensionResult(XExtensionResult.Status.OK, new String(bytes, encoding));
                    break;
                case XExtensionResult.MESSAGE_TYPE_ARRAYBUFFER:
                    result = new XExtensionResult(XExtensionResult.Status.OK, bytes);
                    break;
                case XExtensionResult.MESSAGE_TYPE_BINARYSTRING:
                    result = new XExtensionResult(XExtensionResult.Status.OK, bytes, true);
                    break;
                default: // Base64.
                    String contentType = XFileUtils.getMimeType(filePath);
                    byte[] base64 = XBase64.encode(bytes, XBase64.NO_WRAP);
                    String s = "data:" + contentType + ";base64," + new String(base64, "US-ASCII");
                    result = new XExtensionResult(XExtensionResult.Status.OK, s);
            }
            callbackContext.sendExtensionResult(result);
            return;
        } catch (FileNotFoundException e) {
            errorType = NOT_FOUND_ERR;
        } catch (MalformedURLException e) {
            errorType = ENCODING_ERR;
        } catch(IOException e) {
            errorType = INVALID_MODIFICATION_ERR;
        } catch (XInvalidModificationException e) {
            errorType = INVALID_MODIFICATION_ERR;
        } catch (Exception e) {
            errorType = TYPE_MISMATCH_ERR;
        }
        callbackContext.error(errorType);
    }

    /**
     * 获取指定文件夹中的所有文件实体
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     文件夹路径
     * @return             实体对象的JSON数组
     */
    private JSONArray readEntries(String appWorkSpace, String filePath)
            throws FileNotFoundException, JSONException, IOException, XInvalidModificationException {
        File[] files = mFile.getFileEntries(appWorkSpace, filePath);
        JSONArray entries = new JSONArray();

        for(File file : files){
            entries.put(XFileUtils.getEntry(appWorkSpace, file));
        }

        return entries;
    }

    /**
     * 获取文件的元数据（最后修改时间）
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     文件的路径
     * @return             最后修改时间
     */
    private long getMetadata(String appWorkSpace, String filePath)
            throws FileNotFoundException, IOException, XInvalidModificationException {
        return mFile.getMetadata(appWorkSpace, filePath);
    }

    /**
     * 解析URI格式的文件路径
     * @param appWorkSpace 当前应用工作目录
     * @param url          要解析的文件URI
     * @return             解析后的文件JSON对象
     */
    private JSONObject resolveLocalFileSystemURI(String appWorkSpace, String url)
            throws IOException, JSONException, XInvalidModificationException, MalformedURLException{
        File file = mFile.getFileStreamFromURL(appWorkSpace, url, getContext());
        return XFileUtils.getEntry(appWorkSpace, file);
    }
}