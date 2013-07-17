
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;

import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XZipper;

public class XZipExt extends XExtension{

    private enum ErrorCode {
        NONE,
        // 文件不存在.
        FILE_NOT_EXIST,
        // 压缩文件出错.
        COMPRESS_FILE_ERROR,
        // 解压文件出错.
        UNZIP_FILE_ERROR,
        // 文件路径错误
        FILE_PATH_ERROR,
        // 文件类型错误,不支持的文件类型
        FILE_TYPE_ERROR,
    }

    private static final String COMMAND_ZIP = "zip";
    private static final String COMMAND_ZIP_FILES = "zipFiles";
    private static final String COMMAND_UNZIP = "unzip";

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
        Status status = Status.ERROR;
        ErrorCode errorCode = ErrorCode.NONE;
        if(COMMAND_ZIP.equals(action)){
            try{
                boolean zipSuccess = zip(mWebContext, args.getString(0), args.getString(1));
                if(!zipSuccess){
                    errorCode = ErrorCode.FILE_PATH_ERROR;
                } else {
                    status = Status.OK;
                    return new XExtensionResult(status);
                }
            } catch (FileNotFoundException e) {
                errorCode = ErrorCode.FILE_NOT_EXIST;
            } catch (IOException e) {
                errorCode = ErrorCode.COMPRESS_FILE_ERROR;
            } catch (IllegalArgumentException e){
                errorCode = ErrorCode.COMPRESS_FILE_ERROR;
            }
        } else if(COMMAND_ZIP_FILES.equals(action)){
            try{
                boolean zipSuccess = zipFiles(mWebContext, args.getJSONArray(0), args.getString(1));
                if(!zipSuccess){
                    errorCode = ErrorCode.FILE_PATH_ERROR;
                }else {
                    status = Status.OK;
                    return new XExtensionResult(status);
                }
            } catch (FileNotFoundException e) {
                errorCode = ErrorCode.FILE_NOT_EXIST;
            } catch (IOException e) {
                errorCode = ErrorCode.COMPRESS_FILE_ERROR;
            } catch (IllegalArgumentException e){
                errorCode = ErrorCode.COMPRESS_FILE_ERROR;
            }
        } else if (COMMAND_UNZIP.equals(action)) {
            try{
                boolean zipSuccess = unzip(mWebContext, args.getString(0), args.getString(1));
                if(!zipSuccess){
                    errorCode = ErrorCode.FILE_PATH_ERROR;
                }else {
                    status = Status.OK;
                    return new XExtensionResult(status);
                }
            } catch (FileNotFoundException e) {
                errorCode = ErrorCode.FILE_NOT_EXIST;
            } catch (IOException e) {
                errorCode = ErrorCode.UNZIP_FILE_ERROR;
            } catch (IllegalArgumentException e){
                errorCode = ErrorCode.UNZIP_FILE_ERROR;
            }
        }
        return new XExtensionResult(status, errorCode.ordinal());
    }

    /**
     * zip压缩方法(都限定在app的workspace下面)
     * @param webContext app对象
     * @param srcEntry 要压缩的源文件，可以是文件也可以是文件夹
     * @param destZipFile 压缩成的目标文件，可以是test.zip也可以是a/b/c/test.zip
     * @return 返回压缩是否成功
     *                          true 成功
     *                          false 失败, 路径非法
     * */
    private boolean zip(XIWebContext webContext, String srcEntry, String destZipFile)
            throws NullPointerException, FileNotFoundException, IOException,
            IllegalArgumentException {
        XPathResolver srcPathResolver = new XPathResolver(srcEntry, webContext.getWorkSpace());
        XPathResolver desPathResolver = new XPathResolver(destZipFile, webContext.getWorkSpace());
        String srcPath = srcPathResolver.resolve();
        String desPath = desPathResolver.resolve();
        if(null == srcPath || null == desPath) {
            return false;
        }
        new XZipper().zipDir(srcPath, desPath);
        return true;
    }

    /**
     * zip压缩多个可选文件方法(都限定在app的workspace下面)
     * @param webContext app对象
     * @param srcEntries 要压缩的源文件列表，可以是文件也可以是文件夹
     * @param destZipFile 压缩成的目标文件，可以是test.zip也可以是a/b/c/test.zip
     * @return 返回压缩是否成功
     *                          true 成功
     *                          false 失败, 路径非法
     * @throws JSONException
     * */
    private boolean zipFiles(XIWebContext webContext, JSONArray  srcEntries, String destZipFile)
            throws NullPointerException, FileNotFoundException, IOException,
            IllegalArgumentException, JSONException {

        String[] paths = new String[srcEntries.length()];
        for (int i = 0; i < srcEntries.length(); i++) {
            XPathResolver pathResolver = new XPathResolver(srcEntries.getString(i), webContext.getWorkSpace());
            paths[i] = pathResolver.resolve();
            if(null == paths[i]) {
                return false;
            }
            File srcFile = new File(paths[i]);
            if (!srcFile.exists()) {
                throw new FileNotFoundException();
            }
        }
        XPathResolver desPathResolver = new XPathResolver(destZipFile, webContext.getWorkSpace());
        String desPath = desPathResolver.resolve();
        if(null == desPath) {
            return false;
        }
        new XZipper().zipFiles(paths, desPath);
        return true;
    }
    /**
     * unzip解压缩方法(都限定在app的workspace下面)
     * @param app app对象
     * @param zipFilePath 要解压的源文件
     * @param destPath 要解压的目标路径
     * @return 返回解压缩是否成功
     *                          true 成功
     *                          false 失败, 路径非法
     * */
    private boolean unzip(XIWebContext webContext, String zipFilePath, String destPath)
            throws FileNotFoundException, IOException {
        XPathResolver srcPathResolver = new XPathResolver(zipFilePath, webContext.getWorkSpace());
        XPathResolver desPathResolver = new XPathResolver(destPath, webContext.getWorkSpace());
        String srcPath = srcPathResolver.resolve();
        String desPath = desPathResolver.resolve();
        if(null == srcPath || null == desPath) {
            return false;
        }
        new XZipper().unzipFile( desPath, srcPath);
        return true;
    }
}
