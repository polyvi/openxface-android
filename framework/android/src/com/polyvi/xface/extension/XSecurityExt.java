
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
import java.io.UnsupportedEncodingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.polyvi.xface.exception.XCryptionException;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XBase64;
import com.polyvi.xface.util.XCryptor;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XStringUtils;

public class XSecurityExt extends XExtension {

    private static final String CLASS_NAME = XSecurityExt.class.getSimpleName();

    /** Security 提供给js用户的接口名字 */
    private static final String COMMAND_ENCRYPT = "encrypt";
    private static final String COMMAND_DECRYPT = "decrypt";
    private static final String COMMAND_ENCRYPT_FILE = "encryptFile";
    private static final String COMMAND_DECRYPT_FILE = "decryptFile";
    private static final String COMMAND_DIGEST = "digest";

    /** 加解密过程中的错误 */
    private static final int FILE_NOT_FOUND_ERR = 1;
    private static final int PATH_ERR = 2;
    private static final int OPERATION_ERR = 3;

    /**加解密报错*/
    private static final String KEY_EMPTY_ERROR  = "Error:key null or empty";
    private static final String FILE_NOT_FOUND_ERROR  = "Error: file not found";
    private static final String CRYPTION_ERROR = "Error:cryption error";

    /**加密算法选择*/
    private static final int DES_ALOGRITHEM = 1;       //DES方式加解密
    private static final int TRIPLE_DES_ALOGRITHEM = 2;//3DES方式加解密
    private static final int RSA_ALOGRITHEM = 3;       //RSA方式加解密
    /**返回数据类型选择*/
    private static final int ENCODE_TYPE_STRING = 0;       //返回数据为String
    private static final int ENCODE_TYPE_BASE64 = 1;//返回的数据以Base64编码格式
    private static final int ENCODE_TYPE_HEX = 2;//返回的数据以16进制编码格式
    /** 加解密配置选项的属性名称  */
    private static final String KEY_CRYPT_ALGORITHM = "CryptAlgorithm";
    private static final String KEY_ENCODE_DATA_TYPE = "EncodeDataType";
    private static final String KEY_ENCODE_KEY_TYPE = "EncodeKeyType";

    /**加解密工具类*/
    XCryptor mCryptor = new XCryptor();

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
        XExtensionResult.Status status = XExtensionResult.Status.OK;
        String result = "Unsupported Operation: " + action;
        try {
            if (action.equals(COMMAND_ENCRYPT)) {
                result = encrypt(args.getString(0), args.getString(1),args.optJSONObject(2));
            } else if (action.equals(COMMAND_DECRYPT)) {
                result = decrypt(args.getString(0), args.getString(1), args.optJSONObject(2));
            } else if (action.equals(COMMAND_ENCRYPT_FILE)) {
                result = encryptFile(mWebContext, args.getString(0), args.getString(1),
                        args.getString(2));
            } else if (action.equals(COMMAND_DECRYPT_FILE)) {
                result = decryptFile(mWebContext, args.getString(0), args.getString(1),
                        args.getString(2));
            } else if (action.equals(COMMAND_DIGEST)) {
                result = digest(args.getString(0));
            } else {
                status = XExtensionResult.Status.INVALID_ACTION;
            }
            return new XExtensionResult(status, result);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new XExtensionResult(XExtensionResult.Status.ERROR, PATH_ERR);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            XLog.e(CLASS_NAME, FILE_NOT_FOUND_ERROR, e);
            return new XExtensionResult(XExtensionResult.Status.ERROR,
                    FILE_NOT_FOUND_ERR);
        } catch (XCryptionException e) {
            e.printStackTrace();
            XLog.e(CLASS_NAME, CRYPTION_ERROR, e);
            return new XExtensionResult(XExtensionResult.Status.ERROR,
                    OPERATION_ERR);
        }
    }

    /**
     * 对称加密字节数组并返回
     *
     * @param sKey
     *            密钥
     * @param sourceData
     *            需要加密的数据
     * @param options
     *            加解密配置选项
     * @return 经过加密的数据
     */
    private String encrypt(String sKey, String sourceData, JSONObject options)
            throws XCryptionException {
        int cryptAlgorithm = DES_ALOGRITHEM;
        int encodeDataType = ENCODE_TYPE_STRING;
        int encodeKeyType = ENCODE_TYPE_STRING;
        if (options != null) {
            cryptAlgorithm = options.optInt(KEY_CRYPT_ALGORITHM, DES_ALOGRITHEM);
            encodeDataType = options.optInt(KEY_ENCODE_DATA_TYPE, ENCODE_TYPE_BASE64);
            encodeKeyType = options.optInt(KEY_ENCODE_KEY_TYPE, ENCODE_TYPE_STRING);
        }
        byte[] keyBytes = null;
        keyBytes = getBytesEncode(encodeKeyType, sKey);
        switch (cryptAlgorithm) {
        case TRIPLE_DES_ALOGRITHEM:
            switch (encodeDataType) {
            case ENCODE_TYPE_HEX:
                return XStringUtils.hexEncode(mCryptor.encryptBytesFor3DES(
                        sourceData.getBytes(), keyBytes));
            default:
                return XBase64.encodeToString((mCryptor.encryptBytesFor3DES(
                        sourceData.getBytes(), keyBytes)), XBase64.NO_WRAP);
            }
        case RSA_ALOGRITHEM:
            switch (encodeDataType) {
            case ENCODE_TYPE_HEX:
                return XStringUtils.hexEncode(mCryptor.encryptRSA(
                        sourceData.getBytes(), keyBytes));
            default:
                return XBase64.encodeToString((mCryptor.encryptRSA(
                        sourceData.getBytes(), keyBytes)), XBase64.NO_WRAP);
            }
        default:
            switch (encodeDataType) {
            case ENCODE_TYPE_HEX:
                return XStringUtils.hexEncode(mCryptor.encryptBytesForDES(
                        sourceData.getBytes(), keyBytes));
            default:
                return XBase64.encodeToString((mCryptor.encryptBytesForDES(
                        sourceData.getBytes(), keyBytes)), XBase64.NO_WRAP);
            }
        }
    }

    /**
     * 对称加密文件并返回
     *
     * @param webContext
     *            具体应用
     * @param sKey
     *            密钥
     * @param sourceFilePath
     *            需要加密的文件的路径（只支持相对路径）
     * @param targetFilePath
     *            经过加密得到的文件的路径
     * @return 加密后文件的相对路径
     * @throws XCryptionException
     * @throws FileNotFoundException
     */
    private String encryptFile(XIWebContext webContext, String sKey,
            String sourceFilePath, String targetFilePath)
            throws FileNotFoundException, XCryptionException {
        return doFileCrypt(webContext, sKey, sourceFilePath, targetFilePath, true);
    }

    /**
     * 对称解密字节数组并返回
     *
     * @param sKey
     *            密钥
     * @param sourceData
     *            需要解密的数据
     * @param options
     *            加解密配置选项
     * @return 经过解密的数据
     */
    private String decrypt(String sKey, String sourceData,  JSONObject options)
            throws XCryptionException {
        int cryptAlgorithm = DES_ALOGRITHEM;
        int encodeDataType = ENCODE_TYPE_STRING;
        int encodeKeyType = ENCODE_TYPE_STRING;
        if (options != null) {
            cryptAlgorithm = options.optInt(KEY_CRYPT_ALGORITHM, DES_ALOGRITHEM);
            encodeDataType = options.optInt(KEY_ENCODE_DATA_TYPE, ENCODE_TYPE_STRING);
            encodeKeyType = options.optInt(KEY_ENCODE_KEY_TYPE, ENCODE_TYPE_STRING);
        }
        byte[] keyBytes = null;
        keyBytes = getBytesEncode(encodeKeyType, sKey);
        switch (cryptAlgorithm) {
        case TRIPLE_DES_ALOGRITHEM:
            switch (encodeDataType) {
            case ENCODE_TYPE_HEX:
                return new String(mCryptor.decryptBytesFor3DES(
                        XStringUtils.hexDecode(sourceData), keyBytes));
            default:
                return new String(mCryptor.decryptBytesFor3DES(
                        XBase64.decode(sourceData,XBase64.NO_WRAP),  keyBytes));
            }
        case RSA_ALOGRITHEM:
            switch (encodeDataType) {
            case ENCODE_TYPE_HEX:
                return new String(mCryptor.decryptRSA(
                        XStringUtils.hexDecode(sourceData), keyBytes));
            default:
                return new String(mCryptor.decryptRSA(
                        XBase64.decode(sourceData, XBase64.NO_WRAP),  keyBytes));
            }
        default:
            switch (encodeDataType) {
            case ENCODE_TYPE_HEX:
                return new String(mCryptor.decryptBytesForDES(
                        XStringUtils.hexDecode(sourceData), keyBytes));
            default:
                return new String(mCryptor.decryptBytesForDES(
                        XBase64.decode(sourceData,XBase64.NO_WRAP), keyBytes));
            }
        }
    }

    /**
     * 对称解密文件并返回
     *
     * @param webContext
     *            具体应用
     * @param sKey
     *            密钥
     * @param sourceFilePath
     *            需要解密的文件的路径(只支持相对路径)
     * @param targetFilePath
     *            经过解密得到的文件的路径
     * @return 解密后文件的相对路径
     * @throws FileNotFoundException
     */
    private String decryptFile(XIWebContext webContext, String sKey,
            String sourceFilePath, String targetFilePath)
            throws XCryptionException, FileNotFoundException {
        return doFileCrypt(webContext, sKey, sourceFilePath, targetFilePath, false);
    }

    private String doFileCrypt(XIWebContext webContext, String sKey,
            String sourceFilePath, String targetFilePath, boolean isEncrypt)
            throws XCryptionException, FileNotFoundException {
        // 检查要加解密的key为空
        if (XStringUtils.isEmptyString(sKey)) {
            XLog.e(CLASS_NAME, KEY_EMPTY_ERROR);
            throw new XCryptionException(KEY_EMPTY_ERROR);
        }
        // 检查传入文件路径是否为空
        if (XStringUtils.isEmptyString(targetFilePath)
                || XStringUtils.isEmptyString(sourceFilePath)) {
            throw new IllegalArgumentException();
        }
        // 对要解密的文件作路径解析和检测
        String appWorkSpace = webContext.getWorkSpace();
        File sourceFile = new File(appWorkSpace,sourceFilePath);
        String absSourceFilePath =  getAbsFilePath(sourceFile);
        if((null == absSourceFilePath) || !XFileUtils.isFileAncestorOf(appWorkSpace, absSourceFilePath)) {
            throw new IllegalArgumentException();
        }
        if (!sourceFile.exists()) {
            throw new FileNotFoundException();
        }
        // 对解密后要生成的文件作路径解析和检测
        File targetFile = new File(appWorkSpace, targetFilePath);
        String absTargetFilePath =  getAbsFilePath(targetFile);
        if(null == absTargetFilePath ||
                !XFileUtils.isFileAncestorOf(appWorkSpace, absTargetFilePath) ||
                (null == new XPathResolver(targetFilePath,
                        appWorkSpace).resolve())) {
            throw new IllegalArgumentException();
        }
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (!XFileUtils.createFile(absTargetFilePath)) {
            throw new FileNotFoundException();
        }
        byte[] keyBytes = getBytesEncode(ENCODE_TYPE_STRING, sKey);
        if( isEncrypt ? mCryptor.encryptFileForDES(keyBytes, absSourceFilePath,
                absTargetFilePath) : mCryptor.decryptFileForDES(keyBytes, absSourceFilePath,
                        absTargetFilePath)){
            return targetFilePath;
        }
        throw new XCryptionException(CRYPTION_ERROR);
    }

    private String digest(String data) {
        XCryptor cryptor = new XCryptor();
        try {
            return cryptor.calMD5Value(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            XLog.e(CLASS_NAME, "digest failed: UnsupportedEncodingException");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取文件的绝对路径
     * @param targetFile
     * @return
     */
    private String getAbsFilePath (File targetFile) throws
    IllegalArgumentException{
        try {
            return targetFile.getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * 获取按直接编码解码后的keyBytes
     * @param encodeKeyType:编码类型
     * @param keyString :解码前的keyString
     * @return 解码后的二进制串
     */
    private byte[] getBytesEncode(int encodeKeyType, String keyString) {
        if(XStringUtils.isEmptyString(keyString)) {
            return null;
        }
        switch (encodeKeyType) {
        case ENCODE_TYPE_BASE64:
            return XBase64.decode(keyString, XBase64.NO_WRAP);
        case ENCODE_TYPE_HEX:
            return XStringUtils.hexDecode(keyString);
        default:
            return keyString.getBytes();
        }
    }
}
