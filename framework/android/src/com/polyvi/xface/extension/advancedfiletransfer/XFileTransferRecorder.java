
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

package com.polyvi.xface.extension.advancedfiletransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XXmlUtils;

// 该类用于操作配置文件(包括配置文件的初始化，读取配置文件，写配置文件，更新配置文件和删除配置文件)
public class XFileTransferRecorder {
    private static final String CLASS_NAME = XFileTransferRecorder.class.getSimpleName();

    private static final String TAG_FILETRANSFER_INFO = "\n<filetransfer_info>\n</filetransfer_info>\n";
    private static final String FILETRANSFER_CONFIG_FILE_NAME = "filetransfer_info.xml";
    private static final String CONFIG_FILE_TAG_ROOT = "filetransfer_info";
    private static final String CONFIG_FILE_TAG_ID = "id";
    private static final String CONFIG_FILE_TAG_TOTAL_SIZE = "totalSize";
    private static final String CONFIG_FILE_TAG_COMPLETE_SIZE = "completeSize";
    private static final String CONFIG_FILE_TAG_SOURCE_ID = "sourceid";

    /** 配置文件xml内容对应的Document对象 */
    private Document mDocument;

    /** 配置文件xml的路径 */
    private String mConfigPath;

    public XFileTransferRecorder(XIWebContext webContext) {
        mConfigPath = webContext.getApplication().getDataDir() + File.separator + FILETRANSFER_CONFIG_FILE_NAME;
        File file = new File(mConfigPath);
        if (!file.exists()) {
            try {
                file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                out.write(TAG_FILETRANSFER_INFO.getBytes());
                out.close();
            } catch (IOException e) {
                XLog.e(CLASS_NAME, e.getMessage());
            }
        }
        mDocument = XXmlUtils.parseXml(mConfigPath);
    }

    /**
     * 查看配置文件中是否有该记录
     * @param url 要查找的路径
     */
    public synchronized boolean hasDownloadInfo(String url) {
        if (null != mDocument) {
            Element urlElement = mDocument.getElementById(url);
            return urlElement != null;
        }
        return false;
    }

    /**
     * 保存 下载的具体信息
     * @param info 要存储的下载信息，包括文件的地址，文件的大小和已下载的大小
     */
    public synchronized void saveDownloadInfo(XFileDownloadInfo info) {
        if(null != mDocument) {
            Element downloadInfoElement = (Element) mDocument.getElementsByTagName(
                    CONFIG_FILE_TAG_ROOT).item(0);
            Element downloadElement = mDocument.createElement("download");
            downloadInfoElement.appendChild(downloadElement);

            downloadElement.setAttribute(CONFIG_FILE_TAG_ID, info.getUrl());
            downloadElement.setAttribute(CONFIG_FILE_TAG_COMPLETE_SIZE, String.valueOf(info.getCompleteSize()));
            downloadElement.setAttribute(CONFIG_FILE_TAG_TOTAL_SIZE, String.valueOf(info.getTotalSize()));

            XXmlUtils.saveDocToFile(mDocument, mConfigPath,false);
        }
    }

    /**
     * 得到下载具体信息
     * @param url 要获取的路径
     */
    public synchronized XFileDownloadInfo getDownloadInfo(String url) {
        XFileDownloadInfo info = null;
        if(null != mDocument) {
            Element downloadElement = mDocument.getElementById(url);
            if (downloadElement != null) {
                info = new XFileDownloadInfo(Integer.parseInt(downloadElement
                        .getAttribute(CONFIG_FILE_TAG_TOTAL_SIZE)), Integer.parseInt(downloadElement
                        .getAttribute(CONFIG_FILE_TAG_COMPLETE_SIZE)), url);
            }
        }
        return info;
    }

    /**
     * 更新配置文件中的下载信息
     */
    public synchronized void updateDownloadInfo(int compeleteSize, String url) {
        if(null != mDocument) {
            Element downloadElement = mDocument.getElementById(url);
            if (downloadElement != null) {
                downloadElement.setAttribute(CONFIG_FILE_TAG_COMPLETE_SIZE,
                        String.valueOf(compeleteSize));
            }
            XXmlUtils.saveDocToFile(mDocument, mConfigPath,false);
        }
    }

    /**
     * 下载完成后删除配置文件中的数据
     */
    public synchronized void deleteDownloadInfo(String url) {
        if(null != mDocument) {
            Element downloadInfoElement = (Element) mDocument.getElementsByTagName(
            CONFIG_FILE_TAG_ROOT).item(0);
            Element downloadElement = mDocument.getElementById(url);
            if (downloadElement != null) {
                downloadInfoElement.removeChild(downloadElement);
            }
            XXmlUtils.saveDocToFile(mDocument, mConfigPath,false);
        }
    }

    /**
     * 保存 上传的具体信息
     * @param sourceid filePath对应的唯一标示符
     * @param filePath 要上传的文件地址
     */
    public synchronized void saveUploadInfo(String sourceid, String filePath, String totalSize) {
        if (null != mDocument) {
            Element uploadInfoElement = (Element) mDocument
                    .getElementsByTagName(CONFIG_FILE_TAG_ROOT).item(0);
            Element uploadElement = mDocument.createElement("upload");
            uploadInfoElement.appendChild(uploadElement);
            uploadElement.setAttribute(CONFIG_FILE_TAG_ID, filePath);
            uploadElement.setAttribute(CONFIG_FILE_TAG_SOURCE_ID, sourceid);
            uploadElement.setAttribute(CONFIG_FILE_TAG_TOTAL_SIZE, totalSize);
            XXmlUtils.saveDocToFile(mDocument, mConfigPath,false);
        }
    }

    /**
     * 删除上传的具体信息
     * @param filePath 要上传的文件地址
     */
    public synchronized void deleteUploadInfo(String filePath) {
        if (null != mDocument) {
            Element uploadInfoElement = (Element) mDocument
                    .getElementsByTagName(CONFIG_FILE_TAG_ROOT).item(0);
            Element uploadElement = mDocument.getElementById(filePath);
            if (uploadElement != null) {
                uploadInfoElement.removeChild(uploadElement);
            }
            XXmlUtils.saveDocToFile(mDocument, mConfigPath,false);
        }
    }

    /**
     * 获取filePath对应的唯一标示符
     * @param filePath 要上传的文件地址
     */
    public synchronized String getSourceId(String filePath,String totalSize) {
        if (null != mDocument) {
            Element uploadElement = mDocument.getElementById(filePath);
            if (uploadElement != null) {
                // 如果要上传的文件大小与记录的文件大小一致则认为是同一个文件
                //TODO:以后用更好的方式区分文件
                if (totalSize == uploadElement
                        .getAttribute(CONFIG_FILE_TAG_TOTAL_SIZE)) {
                    return uploadElement
                            .getAttribute(CONFIG_FILE_TAG_SOURCE_ID);
                }
            }
        }
        return null;
    }
}
