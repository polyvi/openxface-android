
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

package com.polyvi.xface.extension.camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import com.polyvi.xface.core.XConfiguration;
import com.polyvi.xface.extension.XActivityResultListener;
import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.util.XBase64;
import com.polyvi.xface.util.XConstant;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XNotification;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XStringUtils;
import com.polyvi.xface.util.XUtils;

public class XCameraExt extends XExtension implements XActivityResultListener {
    private static final String COMMAND_TAKE_PICTURE = "takePicture";

    private static final long DURATION = 4000;//alert提示框显示的时间

    private static final int DATA_URL = 0; // 返回图片数据
    private static final int FILE_URI = 1; // 返回图片路径
    private static final int NATIVE_URI = 2;  // 在android上和FILE_URI用法一样

    private static final int PHOTOLIBRARY = 0; // 选择一个 picture library
                                                // (android平台与SAVEDPHOTOALBUM一致)
    private static final int CAMERA = 1; // 通过camera照一张图片
    private static final int SAVEDPHOTOALBUM = 2; // 选择一个 picture library
                                                    // (android平台与PHOTOLIBRARY一致)

    private static final int PICTURE = 0;
    private static final int VIDEO = 1;
    private static final int ALLMEDIA = 2;

    private static final int JPEG = 0; // JPEG类型标志
    private static final int PNG = 1; // PNG类型标志
    private static final String GET_PICTURE = "Get Picture";
    private static final String GET_VIDEO = "Get Video";
    private static final String GET_All = "Get All";

    private static final String CLASS_NAME = "Camera";

    private static final String RESIZED_PIC_NAME = "Resize.jpg";
    private static final String MEDIA_MIME_TYPE = "image/jpeg";

    private static final int CAMERA_REQUEST_CODE = XUtils.genActivityRequestCode();
    private static final int PHOTO_REQUEST_CODE = XUtils.genActivityRequestCode();
    private static final int PICTURE_CUT_REQUEST_CODE = XUtils.genActivityRequestCode();

    private int mQuality; // 图像质量 (0-100)
    private int mTargetWidth; // 图像宽度
    private int mTargetHeight; // 图像高度
    private Uri mImageUri; // 图像路径
    private int mEncodingType; // 图像类型（JPEG, PNG)
    private int mMediaType; // 媒体文件类型(PICTURE, VIDEO, ALLMEDIA)
    private int mSrcType; // 图像资源类型(PHOTOLIBRARY, CAMERA, SAVEDPHOTOALBUM)
    private int mDestType; // 目标图像的数据类型(DATA_URL, FILE_URI，NATIVE_URI)
    private boolean mSaveToPhotoAlbum; // 图像是否保存到设备的相册
    private boolean mAllowEdit;//是否允许图像进行裁剪

    private XCallbackContext mCallbackCtx;
    private int mNumPics;

    //启动非系统自带图片裁剪程序的Intent参数
    private final static String CROP_IMAGE_ACTION = "android.intent.action.XFACE_IMAGE_CROP";

    private Uri mCroppedImageUri;  // 如果需要对图片进行裁剪，此Uri用于指向裁剪后图片的路径，此路径在工作空间中，不在相册路径中

    @Override
    public void sendAsyncResult(String result) {

    }

    @Override
    public boolean isAsync(String action) {
        return false;
    }

    @Override
    public XExtensionResult exec(String action, JSONArray args, XCallbackContext callbackCtx)
            throws JSONException {

        XExtensionResult.Status status = XExtensionResult.Status.OK;
        String result = "";
        mCallbackCtx = callbackCtx;

        try {
            if (COMMAND_TAKE_PICTURE.equals(action)) {
                mSrcType = CAMERA;
                mDestType = FILE_URI;
                mTargetHeight = 0;
                mTargetWidth = 0;
                mEncodingType = JPEG;
                mMediaType = PICTURE;
                mQuality = 80;
                mSaveToPhotoAlbum = false;
                mAllowEdit = false;

                mQuality = args.getInt(0);
                mDestType = args.getInt(1);
                mSrcType = args.getInt(2);
                mTargetWidth = args.getInt(3);
                mTargetHeight = args.getInt(4);
                mEncodingType = args.getInt(5);
                mMediaType = args.getInt(6);
                mAllowEdit = args.getBoolean(7);
                mSaveToPhotoAlbum = args.getBoolean(9);

                if (mSrcType == CAMERA) {  // 图片源是照相机
                    takePicture(mEncodingType);  // 启动照相机拍摄图片
                } else if ((mSrcType == PHOTOLIBRARY)||(mSrcType == SAVEDPHOTOALBUM)) {//图片源是手机相册
                    getImage();
                }
                XExtensionResult r = new XExtensionResult(XExtensionResult.Status.NO_RESULT);
                r.setKeepCallback(true);
                return r;
            }
            return new XExtensionResult(status, result);
        } catch (IllegalArgumentException e) {
            XLog.e(CLASS_NAME, "Take picture arguments error!");
            return new XExtensionResult(XExtensionResult.Status.ERROR);
        }
    }

    public void takePicture(int mEncodingType) {
        Cursor cursor = queryImgDB();
        if(null != cursor) {
            mNumPics = cursor.getCount();// 当前手机中图片数量
        } else {
            mNumPics = 0;//当系统相册没有照片时，Cursor为空
        }

        // 准备启动camera的参数
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        File photo = createCaptureFile(mEncodingType);  // 在工作空间中创建一个空文件

        // 将上面空文件的URI传给照相机，照相机会把拍摄图片的数据保存到此空文件中
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
        mImageUri = Uri.fromFile(photo);

        // 启动系统Camera，照相机拍摄后将回到此类的onActivityResult函数执行后续工作
        mExtensionContext.getSystemContext().startActivityForResult(this, intent, CAMERA_REQUEST_CODE);
    }

    public void getImage() {
        Intent intent = new Intent();
        String title = GET_PICTURE;
        if (mMediaType == PICTURE) {
            intent.setType("image/*");
        } else if (mMediaType == VIDEO) {
            intent.setType("video/*");
            title = GET_VIDEO;
        } else if (mMediaType == ALLMEDIA) {
            intent.setType("*/*");
            title = GET_All;
        }

        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // 启动系统图片管理工具
        mExtensionContext.getSystemContext().startActivityForResult(this,
                    Intent.createChooser(intent, new String(title)), PHOTO_REQUEST_CODE);
    }

    private Cursor queryImgDB() {
        Uri contentUri = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            contentUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else {
            contentUri = android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI;
        }

        return mExtensionContext.getSystemContext().getContext().getContentResolver()
                .query(contentUri,new String[]{MediaStore.Images.Media._ID },null,null,null);
    }

    private File createCaptureFile(int encodingType) {
        Context context = getContext();
        String picName = "";
        File photo = null;
        long time = Calendar.getInstance().getTimeInMillis();
        if (mEncodingType == JPEG) {
            picName = String.valueOf(time) + ".jpg";
        } else if (mEncodingType == PNG) {
            picName = String.valueOf(time) + ".png";
        } else {
            throw new IllegalArgumentException("Invalid Encoding Type: " + mEncodingType);
        }

        photo = context.getFileStreamPath(picName);
        if (!photo.exists()) {
            try {
                FileOutputStream outStream = context.openFileOutput(picName,
                        Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
                outStream.close();
                photo = context.getFileStreamPath(picName);
            } catch (FileNotFoundException e) {
                XLog.e(CLASS_NAME, "Create picture failed!");
            } catch (IOException e) {
                XLog.e(CLASS_NAME, "Close picture fileStream failed!");
            }
        }
        return photo;
    }

    /**
     * 返回媒体入口
     *
     * @return uri
     */
    private Uri getUriFromMediaStore() {
        Uri uri = null;
        ContentValues values = new ContentValues();
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,
                MEDIA_MIME_TYPE);
        try {
            uri = getContext().getContentResolver()
                  .insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (UnsupportedOperationException e) {
            XLog.d(CLASS_NAME, "Can't write to external media storage.");
            try {
                uri = getContext().getContentResolver()
                      .insert(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);
            } catch (UnsupportedOperationException ex) {
                XLog.d(CLASS_NAME, "Can't write to internal media storage.");
                mCallbackCtx.error("Error capturing image - no media storage found.");
                return null;
            }
        }
        return uri;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (CAMERA_REQUEST_CODE == requestCode) {
            if (resultCode != Activity.RESULT_OK) {
                XLog.i(CLASS_NAME, "Camera cancelled.");
                mCallbackCtx.error("Camera cancelled.");
                return;
            }
            if (!mAllowEdit) {
                cameraSucess(intent);
            } else {
                startCutPhotoAfterCamera(intent);
            }
        } else if (PHOTO_REQUEST_CODE == requestCode) {
            if (resultCode != Activity.RESULT_OK || null == intent) {
                XLog.e(CLASS_NAME, "Selection cancelled.");
                mCallbackCtx.error("Selection cancelled.");
                return;
            }
            if (!mAllowEdit) {
                photoSucess(intent);
            } else {
                mImageUri = intent.getData();
                startCutPhoto(mImageUri);
            }
        } else {
            if (resultCode != Activity.RESULT_OK || null == intent) {
                XLog.e(CLASS_NAME, "cut picture cancelled.");
                mCallbackCtx.error("cut picture cancelled.");
                return;
            }
            if (mSrcType == CAMERA) {
                cameraSucess(intent);
            } else if ((mSrcType == PHOTOLIBRARY) || (mSrcType == SAVEDPHOTOALBUM)) {
                photoSucess(intent);
            }
        }
    }

    /**
     * 在拍照过后启动裁剪 注意由于裁剪的图片可能来自不同的位置
     *
     * @param intent
     */
    private void startCutPhotoAfterCamera(Intent intent) {
        // 当mDstType为FILE_URI时，intent为null，拍照的图片存放在mImageUri中
        if (null != intent && null != intent.getData()) {
            startCutPhoto(intent.getData());
        } else {
             startCutPhoto(mImageUri);
        }
    }

    /**
     * 裁剪图片
     *
     * 由于在图片裁剪时，裁剪长宽是由用户决定的，所以这里不必将用户指定要裁剪图片的长宽值传给裁剪程序
     *
     * @param uri
     */
    private boolean startCutPhoto(Uri uri) {
        if (!(mTargetHeight > 0 && mTargetWidth > 0)) {
            XLog.e(CLASS_NAME, "Width and height must be larger than 1 when you want to crop image.");
            mCallbackCtx.error("you must give target width and height for crop photo.");
            return false;
        }

        // 为裁剪后的图片在程序工作空间中创建一个空文件，裁剪程序会将裁剪后的图片数据填入此文件中
        Uri cropped_image_uri = createEmptyFileForCroppedImageBeforeCrop();
        Intent intent = new Intent();
        intent.putExtra(ImageCroppingActivity.SOURCE_IMAGE_URI, uri);
        intent.putExtra(ImageCroppingActivity.CROPPED_IMAGE_URI, cropped_image_uri);
        intent.setClass(getContext(), ImageCroppingActivity.class);
        mExtensionContext.getSystemContext().startActivityForResult(this, intent, PICTURE_CUT_REQUEST_CODE);
        return true;

    }

    /**
     * 选择图片或者直接提供图片路径，在程序工作空间创建一个空文件，裁剪程序将会把裁剪后的图片数据放到
     * 这个文件中，同时得到裁剪后图片的URI，因为此文件在裁剪前后的URI都不会改变。
     *
     * @return 裁剪后图片的URI
     */
    private Uri createEmptyFileForCroppedImageBeforeCrop(){
        String cropped_image_name = null;
        if (mEncodingType == JPEG) {
            String tempStr = System.currentTimeMillis()+".jpg";
            cropped_image_name = tempStr + "_cropped.jpg";
        } else if (mEncodingType == PNG) {
            String tempStr = System.currentTimeMillis()+".png";
            cropped_image_name = tempStr + "_cropped.png";
        } else {
            throw new IllegalArgumentException("Invalid Encoding Type: " + mEncodingType);
        }

        File cropped_image = new File(mWebContext.getWorkSpace(),cropped_image_name);
        mCroppedImageUri = null;
        if(cropped_image.exists()){
            cropped_image.delete();
        }
        try {
            cropped_image.createNewFile();
            mCroppedImageUri = Uri.fromFile(cropped_image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mCroppedImageUri;
    }

    /**
     * 计算图像区域
     *
     * @param bitmap   源图像.
     * @return Bitmap  目标图像.
     */
    public Bitmap scaleBitmap(Bitmap bitmap) {
        int newWidth = mTargetWidth;
        int newHeight = mTargetHeight;
        int origWidth = bitmap.getWidth();
        int origHeight = bitmap.getHeight();

        // 计算目标图像区域
        if (newWidth <= 0 && newHeight <= 0) {
            return bitmap;
        } else if (newWidth > 0 && newHeight <= 0) {
            newHeight = (newWidth * origHeight) / origWidth;
        } else if (newWidth <= 0 && newHeight > 0) {
            newWidth = (newHeight * origWidth) / origHeight;
        } else {
            double newRatio = newWidth / (double) newHeight;
            double origRatio = origWidth / (double) origHeight;

            if (origRatio > newRatio) {
                newHeight = (newWidth * origHeight) / origWidth;
            } else if (origRatio < newRatio) {
                newWidth = (newHeight * origWidth) / origHeight;
            }
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * 去掉重复的图像
     *
     * @param type   FILE_URI，NATIVE_URI或者 DATA_URL
     */
    private void checkForDuplicateImage(int type) {
        int diff = 1;
        Cursor cursor = queryImgDB();
        int currentNumOfImages = 0;
        if(null != cursor){
            currentNumOfImages = cursor.getCount();
        }

        if (type == FILE_URI || type == NATIVE_URI) {
            diff = 2;
        }

        // 删除重复的图片
        if ((currentNumOfImages - mNumPics) == diff) {
            cursor.moveToLast();
            int id = Integer.
                    valueOf(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID))) - 1;
            Uri uri = Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + id);
            getContext().getContentResolver().delete(uri, null, null);
        }
    }

    private void cameraSucess(Intent intent) {
        try {
            Bitmap bitmap = null;
            try {
                //获取裁剪过后image的bitmap
                if(mAllowEdit) {
                    //裁剪后，裁剪程序返回的是裁剪图片的数据，Android系统自带的裁剪程序就是返回图片数据
                    //有时图片数据可能很大，所以下面返回图片的URI更好些
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        bitmap = extras.getParcelable("data");
                    }

                    //裁剪后，裁剪程序返回的是裁剪图片的URI
                    if((bitmap == null)||(extras == null)){
                        bitmap = getCroppedBitmap(intent);
                    }
                } else { // 处理不需要裁剪的图片
                    bitmap = android.provider.MediaStore.Images.Media
                        .getBitmap(getContext().getContentResolver(), mImageUri);
                }
            } catch (FileNotFoundException e) {
                Uri uri = intent.getData();
                android.content.ContentResolver resolver = getContext().getContentResolver();
                bitmap = android.graphics.BitmapFactory.decodeStream(resolver.openInputStream(uri));
            } catch (IOException e) {
                mCallbackCtx.error("Can't open image");
            } catch (OutOfMemoryError e) {
                XNotification notification = new XNotification(mExtensionContext.getSystemContext());
                notification.alert("Size of Image is too large!", "Save Image Error", "OK", null, DURATION);
                mCallbackCtx.error("Size of image is too large");
                return;
            }

            // 对图片重新量化，以减少其体积
            Bitmap scaleBitmap = scaleBitmap(bitmap);
            Uri uri = null;
            if (mDestType == DATA_URL) {
                processPicture(scaleBitmap);
                checkForDuplicateImage(DATA_URL);
            } else if (mDestType == FILE_URI || mDestType == NATIVE_URI) {
                if (!this.mSaveToPhotoAlbum) {
                    String suffixName = null;
                    if (mEncodingType == JPEG) {
                        suffixName = ".jpg";
                    } else if (mEncodingType == PNG) {
                        suffixName = ".png";
                    } else {
                        throw new IllegalArgumentException("Invalid Encoding Type: " + mEncodingType);
                    }
                    String photoName = System.currentTimeMillis() + suffixName;
                    uri = Uri.fromFile(new File(mWebContext.getWorkSpace(),photoName));
                } else {
                    uri = getUriFromMediaStore();
                }
                if (uri == null) {
                    mCallbackCtx.error("Error capturing image - no media storage found.");
                }

                // 压缩图像
                OutputStream os = getContext().getContentResolver().openOutputStream(uri);
                scaleBitmap.compress(Bitmap.CompressFormat.JPEG, mQuality, os);
                os.close();

                // 将图像路径作为参数，调用success callback
                XPathResolver pathResolver = new XPathResolver(uri.toString(), "", getContext());
                mCallbackCtx.success(XConstant.FILE_SCHEME + pathResolver.resolve());
            }
            scaleBitmap.recycle();
            scaleBitmap = null;
            cleanup(FILE_URI, mImageUri, uri, bitmap);
        } catch (IOException e) {
            mCallbackCtx.error("Error capturing image.");
        }
    }

    /**
     * 根据图片URI获取图片Bitmap对象
     *
     * @param intent  包含有裁剪后图片URI的intent对象
     * @return Bitmap 上面intent对象中的URI所对应的图片的数据对象
     */
    private Bitmap getCroppedBitmap(Intent intent){
        Bitmap bitmap = null;
        InputStream is = null;
        Uri uri = null;  //裁剪后图片的URI，此图片在应用程序工作空间中

        if (intent == null) {
            uri = mCroppedImageUri;
        }else {
            uri = intent.getParcelableExtra(ImageCroppingActivity.CROPPED_IMAGE_URI);
        }

        try {
            is = getInputStream(uri);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return bitmap;
    }

    private InputStream getInputStream(Uri mUri) throws IOException{
        try {
            if (mUri.getScheme().equals("file")) {
                return new java.io.FileInputStream(mUri.getPath());
            } else {
                return getContext().getContentResolver().openInputStream(mUri);
            }
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    /**
     * 照相后完成旧图片的删除等回收工作.
     */
    private void cleanup(int imageType, Uri oldImage, Uri newImage, Bitmap bitmap) {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        // Clean up initial camera-written image file.
        String filePath = oldImage.toString();
        if (filePath.startsWith("file://")) {
            filePath = filePath.substring(7);
        }
        (new File(filePath)).delete();

        checkForDuplicateImage(imageType);
        System.gc();
    }

    /**
     * 采用jpeg格式压缩图片，然后将其传换成base64编码的字符串
     *
     * @param bitmap   位图
     *
     **/
    private void processPicture(Bitmap bitmap) {
        ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
        try {
            if (bitmap.compress(CompressFormat.JPEG, mQuality, jpeg_data)) {
                byte[] code = jpeg_data.toByteArray();
                byte[] output = XBase64.encode(code, XBase64.DEFAULT);
                String js_out = new String(output);
                mCallbackCtx.success(js_out);
                js_out = null;
                output = null;
                code = null;
            }
        } catch (Exception e) {
            mCallbackCtx.error("Error compressing image.");
        }
        jpeg_data = null;
    }

    private void photoSucess(Intent intent) {
        //此处的URI是不不进行裁剪，直接从相册中选择图片后的URI，亦即，此URI是指向相册中的某张图片
        //由于裁剪程序本身也需要执行此函数，所以在try-catch中需要进行另外处理
        Uri uri = intent.getData();
        if(null == uri ) {
            uri = mImageUri;
        }
        ContentResolver resolver = getContext().getContentResolver();
        XPathResolver pathResolver = new XPathResolver(null == uri ? null : uri.toString(), ""
                ,getContext());
        Bitmap bitmap = null;
        try {
            if(!mAllowEdit) {
                String path = pathResolver.resolve();
                if(!XStringUtils.isEmptyString(path)) {
                    bitmap = XUtils.decodeBitmap(path);
                }
            }else{
                //裁剪后，裁剪程序返回的是裁剪图片的数据，Android系统自带的裁剪程序就是返回图片数据
                bitmap = intent.getExtras().getParcelable("data");

                //裁剪后，裁剪程序返回的是裁剪图片的URI
                if(bitmap == null){
                    bitmap = getCroppedBitmap(intent);
                }
            }
        } catch (OutOfMemoryError e) {
            mCallbackCtx.error("OutOfMemoryError when decode image.");
            return;
        }
        if (mDestType == DATA_URL) {
                int rotate = 0;
                String[] cols = { MediaStore.Images.Media.ORIENTATION };
                Cursor cursor = resolver.query(uri, cols, null, null, null);
                if (null != cursor) {
                    cursor.moveToPosition(0);
                    rotate = cursor.getInt(0);
                    cursor.close();
                }
                if (0 != rotate) {
                    Matrix matrix = new Matrix();
                    matrix.setRotate(rotate);
                    bitmap = bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                    bitmap.getHeight(), matrix, true);
                }
                bitmap = scaleBitmap(bitmap);
                processPicture(bitmap);
                bitmap.recycle();
                bitmap = null;
                System.gc();
        } else if (mTargetHeight > 0 && mTargetWidth > 0) {
            try {
                Bitmap scaleBitmap = scaleBitmap(bitmap);

                String fileName = XConfiguration.getInstance().getWorkDirectory() + RESIZED_PIC_NAME;
                OutputStream os = new FileOutputStream(fileName);
                scaleBitmap.compress(Bitmap.CompressFormat.JPEG, mQuality, os);
                os.close();

                bitmap.recycle();
                bitmap = null;
                scaleBitmap.recycle();
                scaleBitmap = null;

                mCallbackCtx.success("file://" + fileName + "?" + System.currentTimeMillis());
                System.gc();
            } catch (Exception e) {
                mCallbackCtx.error("Error retrieving image.");
                return;
            }
        } else {
            mCallbackCtx.success(XConstant.FILE_SCHEME + pathResolver.resolve());
        }
    }
}
