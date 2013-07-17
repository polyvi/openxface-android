
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.net.Uri;

import com.polyvi.xface.exception.XEncodingException;
import com.polyvi.xface.exception.XFileExistsException;
import com.polyvi.xface.exception.XInvalidModificationException;
import com.polyvi.xface.exception.XNoModificationAllowedException;
import com.polyvi.xface.exception.XTypeMismatchException;
import com.polyvi.xface.util.XFileUtils;
import com.polyvi.xface.util.XPathResolver;

public class XFileImpl implements XFile {

    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int DEFAULT_READ_SIZE = 1000;
    private static final String ENCODING_TYPE = "UTF-8";
    private static final String FILE_SCHEME = "file";

    private static final String ENCODING_EXCEPTION_NAME_CONTAINS_COLON = "This file has a : in its name";
    private static final String FILE_EXISTS_EXCEPTION_CREATE_OR_EXCLUSIVE_FAILS = "create/exclusive fails";
    private static final String FILE_EXISTS_EXCEPTION_CREATE_FAILS = "create fails";
    private static final String FILE_NOTFOUND_EXCEPTION_PATH_NOT_EXISTS = "path does not exist";
    private static final String TYPE_MISMATCH_EXCEPTION_PATH_NOT_EXISTS_OR_IS_FILE = "path doesn't exist or is file";
    private static final String TYPE_MISMATCH_EXCEPTION_PATH_NOT_EXISTS_OR_IS_DIR = "path doesn't exist or is directory";
    private static final String INVALID_MODIFICATION_EXCEPTION_COPY_ERROR = "Can't copy a file onto itself";
    private static final String NO_MODIFICATION_ALLOWED_EXCEPTION_CREATE_DEST_ERR = "Couldn't create the destination direcotry";
    private static final String INVALID_MODIFICATION_EXCEPTION_DIR_RENAME_TO_FILE = "Can't rename a directory to a file";
    private static final String INVALID_MODIFICATION_EXCEPTION_FILE_RENAME_TO_DIR = "Can't rename a file to a directory";
    private static final String INVALID_MODIFICATION_EXCEPTION_DIR_IS_NOT_EMPTY = "directory is not empty";
    private static final String INVALID_MODIFICATION_EXCEPTION_DELETE_ROOT_DIR_ERR = "You can't delete the root directory";
    private static final String INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR = "You can't get file that is not in the root directory";

    @Override
    public long write(String appWorkSpace, String filePath, String data, int position)
            throws FileNotFoundException, IOException, XInvalidModificationException {
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        return XFileUtils.write(file.getAbsolutePath(), data, position);
    }

    @Override
    public long truncateFile(String appWorkSpace, String filePath, long size)
            throws FileNotFoundException, IOException ,XInvalidModificationException {
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        return XFileUtils.truncateFile(file.getAbsolutePath(), size);
    }

    @Override
    public File getFileObject(String appWorkSpace, String dirPath, String fileName, boolean create,
            boolean exclusive, boolean directory) throws XFileExistsException, IOException,
            XTypeMismatchException, XEncodingException, XInvalidModificationException {
        if (fileName.contains(":")) {
            throw new XEncodingException(ENCODING_EXCEPTION_NAME_CONTAINS_COLON);
        }

        File fp = new File(appWorkSpace, dirPath);
        File file = null;
        if(fileName.startsWith(File.separator)) {
            //当传进来的文件名是以"/"开头时，在unix下认为是根路径开始
            file = new File(appWorkSpace, fileName);
        } else {
            file = new File(fp, fileName);
        }
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        if (create) {
            if (exclusive && file.exists()) {
                throw new XFileExistsException(FILE_EXISTS_EXCEPTION_CREATE_OR_EXCLUSIVE_FAILS);
            }
            if (directory) {
                file.mkdir();
            } else {
                file.createNewFile();
            }
            if (!file.exists()) {
                throw new XFileExistsException(FILE_EXISTS_EXCEPTION_CREATE_FAILS);
            }
        }
        else {
            if (!file.exists()) {
                throw new FileNotFoundException(FILE_NOTFOUND_EXCEPTION_PATH_NOT_EXISTS);
            }
            if (directory) {
                if (file.isFile()) {
                    throw new XTypeMismatchException(TYPE_MISMATCH_EXCEPTION_PATH_NOT_EXISTS_OR_IS_FILE);
                }
            }
            else if (file.isDirectory()) {
                    throw new XTypeMismatchException(TYPE_MISMATCH_EXCEPTION_PATH_NOT_EXISTS_OR_IS_DIR);
            }
        }
        return file;
    }

    @Override
    public File transferTo(String appWorkSpace, String oldPath, String newPath, String newName,
            boolean move) throws XNoModificationAllowedException, IOException,
            XInvalidModificationException, XEncodingException {
        if(null != newName && newName.contains(":")) {
            throw new XEncodingException(ENCODING_EXCEPTION_NAME_CONTAINS_COLON);
        }
        File source = new File(appWorkSpace, oldPath);

        if (!source.exists()) {
            throw new FileNotFoundException(FILE_NOTFOUND_EXCEPTION_PATH_NOT_EXISTS);
        }

        File destinationDir = new File(appWorkSpace, newPath);
        if (!destinationDir.exists()) {
            throw new FileNotFoundException(FILE_NOTFOUND_EXCEPTION_PATH_NOT_EXISTS);
        }

        if(!XFileUtils.isFileAncestorOf(appWorkSpace, source.getCanonicalPath()) ||
                !XFileUtils.isFileAncestorOf(appWorkSpace, destinationDir.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        File destination = createDestination(newName, source, destinationDir);

        if (source.getAbsolutePath().equals(destination.getAbsolutePath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_COPY_ERROR);
        }

        if (source.isDirectory()) {
            if (move) {
                return moveDirectory(source, destination);
            } else {
                return copyDirectory(source, destination);
            }
        } else {
            if (move) {
                return moveFile(source, destination);
            } else {
                return copyFile(source, destination);
            }
        }
    }

    @Override
    public boolean remove(String appWorkSpace, String filePath) throws IOException, XNoModificationAllowedException,
            XInvalidModificationException {
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        // 不能删除根目录.
        if (isRootDirectory(appWorkSpace, file.getAbsolutePath())) {
            throw new XNoModificationAllowedException(INVALID_MODIFICATION_EXCEPTION_DELETE_ROOT_DIR_ERR);
        }

        // 不能删除不为空的文件夹
        if (file.isDirectory() && file.list().length > 0) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_DIR_IS_NOT_EMPTY);
        }

        return file.delete();
    }

    /**
     * 根据源文件对象，目的文件对象和新的文件名字创建新的目的文件对象
     * @param newName      新的文件名字
     * @param fp           源文件对象
     * @param destination  目的文件对象
     * @return
     */
    private File createDestination(String newName, File fp, File destination) {
        File destFile = null;
        if ("null".equals(newName) || "".equals(newName) ) {
            newName = null;
        }

        if (null != newName) {
            destFile = new File(destination.getAbsolutePath(), newName);
        } else {
            destFile = new File(destination.getAbsolutePath(), fp.getName());
        }
        return destFile;
    }

    /**
     * 复制文件
     * @param srcFile   原始文件流
     * @param destFile  目的文件流
     * @return          目的文件流
     */
    private File copyFile(File srcFile, File destFile)
            throws IOException, XInvalidModificationException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_DIR_RENAME_TO_FILE);
        }

        FileChannel input = new FileInputStream(srcFile).getChannel();
        FileChannel output = new FileOutputStream(destFile).getChannel();

        input.transferTo(0, input.size(), output);

        input.close();
        output.close();

        return destFile;
    }

    /**
     * 复制文件夹
     * @param srcFile   原始文件流
     * @param destFile  目的文件流
     * @return                目的文件流
     */
    private File copyDirectory(File srcDir, File destDir)
            throws IOException, XNoModificationAllowedException, XInvalidModificationException {

        if (destDir.exists() && destDir.isFile()) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_FILE_RENAME_TO_DIR);
        }

        if (isCopyOnItself(srcDir.getAbsolutePath(), destDir.getAbsolutePath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_COPY_ERROR);

        }

        if (!destDir.exists()) {
            if (!destDir.mkdir()) {
                throw new XNoModificationAllowedException(NO_MODIFICATION_ALLOWED_EXCEPTION_CREATE_DEST_ERR);
            }
        }

        for (File file : srcDir.listFiles()) {
            if (file.isDirectory()) {
                copyDirectory(file, destDir);
            } else {
                File destination = new File(destDir.getAbsoluteFile(), file.getName());
                copyFile(file, destination);
            }
        }
        return destDir;
    }

    /**
     * 移动文件
     * @param srcFile   原始文件流
     * @param destFile  目的文件流
     * @return          true:移动成功，false:移动失败
     */
    private File moveFile(File srcFile, File destFile) throws XInvalidModificationException{
        if (destFile.exists() && destFile.isDirectory()) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_DIR_RENAME_TO_FILE);
        }

        if (!srcFile.renameTo(destFile)) {

        }

        return destFile;
    }

    /**
     * 移动文件夹
     * @param srcFile   原始文件流
     * @param destFile  目的文件流
     * @return          true:移动成功，false:移动失败
     */
    private File moveDirectory(File srcDir, File destDir) throws XInvalidModificationException{
        if (destDir.exists() && destDir.isFile()) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_FILE_RENAME_TO_DIR);
        }

        if (isCopyOnItself(srcDir.getAbsolutePath(), destDir.getAbsolutePath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_COPY_ERROR);
        }

        if (destDir.exists()) {
            if (destDir.list().length > 0) {
                throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_DIR_IS_NOT_EMPTY);
            }
        }

        if (!srcDir.renameTo(destDir)) {
        }

        return destDir;
    }

    /**
     * 判断目的路径是否是原始路径的子路径
     * @param src   原始路径
     * @param dest  目的路径
     * @return      true:是，false:否
     */
    private boolean isCopyOnItself(String src, String dest) {
        if(src.equals(dest)) {
            return true;
        }
        if(!src.endsWith(File.separator)) {
            src += File.separator;
        }
        return dest.startsWith(src);
    }

    /**
     * 是否为根目录
     * @param appWorkSpace 当前应用工作空间
     * @param filePath     文件路径
     * @return true:根目录，false:不是根目录.
     */
    private boolean isRootDirectory(String appWorkSpace, String filePath) {
        if(filePath.endsWith(File.separator)) {
            filePath = filePath.substring(0, filePath.length() - 1);
        }
        if (filePath.equals(appWorkSpace)) {
            return true;
        }
        return false;
    }

    @Override
    public String getParentPath(String appWorkSpace, String filePath) throws IOException, XInvalidModificationException {
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        if (isRootDirectory(appWorkSpace, file.getAbsolutePath())) {
            return appWorkSpace;
        }
        filePath =  file.getParent();
        return filePath;
    }

    @Override
    public boolean removeRecursively(String appWorkSpace, String filePath)
            throws IOException, XInvalidModificationException{
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        if(isRootDirectory(appWorkSpace, file.getAbsolutePath())) {
            return false;
        }
        return XFileUtils.deleteFileRecursively(file.getAbsolutePath());
    }

    private long getFileSliceLength(int start,int end,long fileLength)
    {
         long abosoluteStart = getAbosolutePosition(start,fileLength);
         long abosoluteEnd = 0;

         //end为0表示没有设置slice块的结束点，默认为整个文件的结尾
         if( end == 0 )
         {
             abosoluteEnd = fileLength;
         }else//表示设置了结束点，转化为绝对位置
         {
             abosoluteEnd = getAbosolutePosition(end,fileLength);
         }

         //当设置了slice块的起点或者终点时，需要重新计算要读取文件块的长度
         if(abosoluteStart > 0 || abosoluteEnd > 0)
         {
             return abosoluteEnd - abosoluteStart;
         }else{
             return fileLength;
         }

    }

    /**
     * 用来获取文件块的绝对位置
     * @param relativePosition 文件块的相对位置，如-5，则表示里文件结尾距离为5的位置
     * @param size 文件大小
     * @return
     */
    private long getAbosolutePosition(long relativePosition,long size)
    {
         return (relativePosition < 0) ?  Math.max(size+relativePosition, 0) : Math.min(relativePosition, size);
    }

    @Override
    public byte[] readAsBinary(String appWorkSpace, String filePath, int start, int end)
             throws FileNotFoundException, IOException, XInvalidModificationException{
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }
        /**要读取的文件长度*/
        long length = file.length();
        byte[] bytes = new byte[DEFAULT_READ_SIZE];
        InputStream inputStream = new FileInputStream(file.getAbsolutePath());
        BufferedInputStream bis = new BufferedInputStream(inputStream, DEFAULT_BUFFER_SIZE);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        length = getFileSliceLength(start, end, file.length());
        int numRead = 0;
        long abosoluteStart = getAbosolutePosition(start, file.length());
        if (abosoluteStart > 0) {
            bis.skip(abosoluteStart);
        }
        while (length > 0 && (numRead = bis.read(bytes, 0, (int)Math.min(DEFAULT_READ_SIZE, length))) >= 0) {
            length -= numRead;
            bos.write(bytes, 0, numRead);
        }
        byte[] content = bos.toByteArray();
        inputStream.close();
        bis.close();
        bos.close();
        return content;
    }

    @Override
    public File[] getFileEntries(String appWorkSpace, String filePath)
            throws FileNotFoundException, IOException, XInvalidModificationException {
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }

        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            return files;
        }
        return null;
    }

    @Override
    public File getFileStreamFromURL(String appWorkSpace, String url, Context cxt)
            throws IOException, XInvalidModificationException ,MalformedURLException {
        String decoded = URLDecoder.decode(url, ENCODING_TYPE);

        Uri uri = Uri.parse(decoded);
        String scheme = uri.getScheme();
        if(null == scheme) {
          //没有协议开头的url都是非法的，throw MalformedURLException
            throw new MalformedURLException();
        } else if (scheme.equals(FILE_SCHEME)) {
          //对于file接口，所有的文件路径都是相对workspace的，所以file协议的文件路径也转为相对workspace的
            decoded = uri.getPath();
        }

        XPathResolver pathResolver = new XPathResolver(decoded, appWorkSpace, cxt);

        String path = pathResolver.resolve();
        if(null == path) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        if (!file.canRead()) {
            throw new IOException();
        }
        return file;
    }

    @Override
    public long getMetadata(String appWorkSpace, String filePath)
            throws FileNotFoundException, IOException, XInvalidModificationException {
        File file = new File(appWorkSpace, filePath);
        if(!XFileUtils.isFileAncestorOf(appWorkSpace, file.getCanonicalPath())) {
            throw new XInvalidModificationException(INVALID_MODIFICATION_EXCEPTION_NOT_IN_ROOT_DIR);
        }

        if (!file.exists()) {
            throw new FileNotFoundException(FILE_NOTFOUND_EXCEPTION_PATH_NOT_EXISTS);
        }

        return file.lastModified();
    }
}
