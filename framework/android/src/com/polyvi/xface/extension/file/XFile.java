
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

import android.content.Context;

import com.polyvi.xface.exception.XEncodingException;
import com.polyvi.xface.exception.XFileExistsException;
import com.polyvi.xface.exception.XInvalidModificationException;
import com.polyvi.xface.exception.XNoModificationAllowedException;
import com.polyvi.xface.exception.XTypeMismatchException;

/**
 * 封装file的各种操作
 */
public interface XFile {

    /**
     * 写文件
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     要写入的文件的路径
     * @param data         要写入的数据
     * @return             开始写入数据的位置
     */
    public abstract long write(String appWorkSpace, String filePath, String data, int position)
            throws FileNotFoundException, IOException, XInvalidModificationException;

    /**
     * 清除指定长度后的文件内容
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     要清除的文件的路径
     * @param size         清除后剩下的文件大小
     * @return             剩下的文件长度
     */
    public abstract long truncateFile(String appWorkSpace, String filePath, long size)
            throws FileNotFoundException, IOException, XInvalidModificationException;

    /**
     * 创建或者查找一个文件（夹）对象.
     * @param appWorkSpace 当前应用工作目录
     * @param dirPath   文件（夹）所在的文件夹路径
     * @param fileName  文件（夹）的名字
     * @param create    标识是否创建文件（夹）。true表示当文件（夹）不存在时创建，false表示当文件（夹）不存在时不创建
     * @param exclusive 标识当文件（夹）存在但create又为true的时候是否创建成功
     * @param directory true:创建或查找文件夹对象, false:创建或查找文件对象
     * @return 代表文件（夹）的对象
     */
    public abstract File getFileObject(String appWorkSpace, String dirPath, String fileName, boolean create,
            boolean exclusive, boolean directory) throws XFileExistsException, IOException,
            XTypeMismatchException, XEncodingException, XInvalidModificationException;

    /**
     * 文件（夹）的移动或者复制
     * @param appWorkSpace 当前应用工作空间
     * @param oldPath      文件（夹）原始路径
     * @param newPath      文件（夹）新路径
     * @param move         true表示移动文件（夹），false表示复制文件（夹）
     * @return             目的文件流对象
     */
    public abstract File transferTo(String appWorkSpace, String oldPath, String newPath, String newName, boolean move)
            throws XNoModificationAllowedException, IOException, XInvalidModificationException, XEncodingException;

    /**
     * 删除一个文件（夹）（不能删除一个不为空的文件夹，也不能删除文件根目录）
     * @param appWorkSpace 当前应用工作空间
     * @param filePath     要删除的文件（夹）路径
     * @return         true:删除成功,false:删除失败
     */
    public abstract boolean remove(String appWorkSpace, String filePath)
            throws IOException, XNoModificationAllowedException, XInvalidModificationException;

    /**
     * 获取当前文件（夹）的父目录
     * @param appWorkSpace 当前应用工作空间
     * @param filePath     当前文件（夹）的路径
     * @return             父目录的路径
     */
    public abstract String getParentPath(String appWorkSpace, String filePath)
            throws IOException, XInvalidModificationException;

    /**
     * 删除文件夹中的所有文件及文件夹
     * @param appWorkSpace 当前应用工作空间
     * @param filePath     文件夹的路径
     * @return         true:删除成功,false:删除失败
     */
    public abstract boolean removeRecursively(String appWorkSpace, String filePath)
            throws IOException, XInvalidModificationException;

    /**
     * 同步读取文件的二进制数据
     * @param appWorkSpace  当前应用工作目录
     * @param filePath      要读取的文件的路径
     * @param start         slice块的起始位置
     * @param end           slice块的结束位置
     * @return              读取到的 文件二进制数据
     * @throws FileNotFoundException,IOException,XInvalidModificationException
     */
    public abstract byte[] readAsBinary(String appWorkSpace, String filePath,int start,int ends)
            throws FileNotFoundException, IOException, XInvalidModificationException;

    /**
     * 获取指定文件夹中的文件流对象数组
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     文件夹路径
     * @return             文件流对象数组
     */
    public abstract File[] getFileEntries(String appWorkSpace, String filePath)
            throws FileNotFoundException, IOException, XInvalidModificationException;

    /**
     * 获取文件的元数据（最后修改时间）
     * @param appWorkSpace 当前应用工作目录
     * @param filePath     文件的路径
     * @return             最后修改时间
     */
    public abstract long getMetadata(String appWorkSpace, String filePath)
            throws FileNotFoundException, IOException, XInvalidModificationException;

    /**
     * 从URL格式中获取文件流对象
     * @param appWorkSpace 当前应用工作目录
     * @param url     文件的URL格式
     * @return        文件流对象
     */
    public abstract File getFileStreamFromURL(String appWorkSpace, String url, Context cxt)
            throws IOException, XInvalidModificationException, MalformedURLException;
}
