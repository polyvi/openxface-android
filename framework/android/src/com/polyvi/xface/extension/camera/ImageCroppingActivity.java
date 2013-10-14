package com.polyvi.xface.extension.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.polyvi.xface.R;

/*
 * 此类完成图片的裁剪，裁剪后的图片将会保存到当前应用程序的工作空间中，
 * 并且将裁剪后的图片在工作空间中的URI（以“file:”开头）返回给调用
 * 此类的类。
 */
public class ImageCroppingActivity extends MonitoredActivity {
    private int mAspectX, mAspectY;  // 裁剪图片参数
    private final Handler mHandler = new Handler();
    private boolean mCircleCrop = false;
    boolean isSaveButtonClicked;       // 裁剪图片后是否点击了“保存”按钮.
    private CropImageView mImageView;
    private Bitmap mBitmap;
    private HighlightView mCrop;
    private Uri targetUri;
    private Uri cropped_image_uri = null; //此URI由用户提供，亦即用户希望将裁剪后的图片保存到哪个空图片文件中 ，在启动本Activity的Intent中获取
    private HighlightView hv;
    private ContentResolver mContentResolver;
    private static final int DEFAULT_WIDTH = 512;
    private static final int DEFAULT_HEIGHT = 384;
    private int width;
    private int height;
    private int sampleSize = 1;

    public final static String SOURCE_IMAGE_URI = "source_image_uri"; // Intent key word for getting URI of the cropping image.
    public final static String CROPPED_IMAGE_URI = "cropped_image_uri"; // Intent key word for setting URI of the cropped image;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Make UI fullscreen.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.image_crop_activity);
        initViews();  // 初始化图片裁剪Activity的界面按钮响应

        Intent intent = getIntent();
        targetUri = intent.getParcelableExtra(SOURCE_IMAGE_URI); //获取将被裁剪图片的URI
        cropped_image_uri = intent.getParcelableExtra(CROPPED_IMAGE_URI); //获取裁剪后图片的URI
        mContentResolver = getContentResolver();

        //下面判断可能是多余的，因为执行此Activity时，mBitmap必然为null
        if (mBitmap == null) {
            getBitmapSize(); // 获取图片的宽和高，其值保存在本类的height和width属性中
            getBitmap();  //为sampleSize和mBitmap赋值，即此时mBitmap不再为空
        }
        if (mBitmap == null) {
            finish();
            return;
        }
        startFaceDetection(); // 显示图片，实现裁剪工作
    }

    /**
     * 初始化裁剪Activity
     */
    private void initViews(){
          mImageView = (CropImageView) findViewById(R.id.imgcutimageview);
          mImageView.mContext = this;
          findViewById(R.id.imgcutdiscard).setOnClickListener(
                  new View.OnClickListener() {
                      public void onClick(View v) {
                          setResult(RESULT_CANCELED);
                          finish();
                      }
                  });  // 选择好裁剪区域后，响应“舍弃”按钮
          findViewById(R.id.imgcutsave).setOnClickListener(
                  new View.OnClickListener() {
                      public void onClick(View v) {
                          onSaveClicked();
                      }
                  });  // 选择好裁剪区域后，响应“保存”按钮
    }

    /**
     * 获取图片分辨率，即图片的宽和高的值
     */
    private void getBitmapSize(){
        InputStream is = null;
        try {
            is = getInputStream(targetUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            width = options.outWidth;
            height = options.outHeight;
        }catch(IOException e) {
            e.printStackTrace();
        }finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 获取图片文件中存储的数据
     */
    private void getBitmap(){
        InputStream is = null;
        try {
            try {
                is = getInputStream(targetUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while ((width / sampleSize > DEFAULT_WIDTH * 2) ||
                   (height / sampleSize > DEFAULT_HEIGHT * 2)) {
                sampleSize *= 2;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            mBitmap = BitmapFactory.decodeStream(is, null, options);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 获取图片文件的输入流
     *
     * @param mUri
     * @return InputStream
     */
    private InputStream getInputStream(Uri mUri) throws IOException{
        try {
            if (mUri.getScheme().equals("file")) {
                return new java.io.FileInputStream(mUri.getPath());
            } else {
                return mContentResolver.openInputStream(mUri);
            }
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    /**
     * 根据图片文件的Uri获取文件的路径
     *
     * @param mUri  文件的URI
     *
     * @return 文件的存储路径
     */
    private String getFilePath(Uri mUri){
        try {
            if (mUri.getScheme().equals("file")) {
                return mUri.getPath();
            } else {
                return getFilePathByUri(mUri);
            }
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    /**
     * 根据图片文件的Uri获取文件的路径，文件URL以“content”开头
     *
     * @param mUri  文件的URI
     *
     * @return 文件的存储路径
     */
    private String getFilePathByUri(Uri mUri)
            throws FileNotFoundException{
        String imgPath ;
        Cursor cursor = mContentResolver.
                query(mUri, null, null,null, null);
        cursor.moveToFirst();
        imgPath = cursor.getString(1);
        return imgPath;
    }

    /**
     * 显示图片和图片裁剪框，以允许对图片进行裁剪
     */
    private void startFaceDetection() {
        if (isFinishing()) {
            return;
        }
        mImageView.setImageBitmapResetBase(mBitmap, true);
        startBackgroundJob(this, null, getResources().getString(
                R.string.runningFaceDetection), new Runnable() {
            public void run() {
                final CountDownLatch latch = new CountDownLatch(1);
                mHandler.post(new Runnable() {
                    public void run() {
                        final Bitmap b = mBitmap;
                        if (b != mBitmap && b != null) {
                            mImageView.setImageBitmapResetBase(b, true);
                            mBitmap.recycle();
                            mBitmap = b;
                        }
                        if (mImageView.getScale() == 1F) {
                            mImageView.center(true, true);
                        }
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                mRunFaceDetection.run();
            }
        }, mHandler);
    }

    private static class BackgroundJob extends
            MonitoredActivity.LifeCycleAdapter implements Runnable {
        private final MonitoredActivity mActivity;
        private final ProgressDialog mDialog;
        private final Runnable mJob;
        private final Handler mHandler;
        private final Runnable mCleanupRunner = new Runnable() {
            public void run() {
                mActivity.removeLifeCycleListener(BackgroundJob.this);
                if (mDialog.getWindow() != null)
                    mDialog.dismiss();
            }
        };

        public BackgroundJob(MonitoredActivity activity, Runnable job,
                ProgressDialog dialog, Handler handler) {
            mActivity = activity;
            mDialog = dialog;
            mJob = job;
            mActivity.addLifeCycleListener(this);
            mHandler = handler;
        }

        public void run() {
            try {
                mJob.run();
            } finally {
                mHandler.post(mCleanupRunner);
            }
        }

        @Override
        public void onActivityDestroyed(MonitoredActivity activity) {
            // We get here only when the onDestroyed being called before
            // the mCleanupRunner. So, run it now and remove it from the
            // queue
            mCleanupRunner.run();
            mHandler.removeCallbacks(mCleanupRunner);
        }

        @Override
        public void onActivityStopped(MonitoredActivity activity) {
            mDialog.hide();
        }

        @Override
        public void onActivityStarted(MonitoredActivity activity) {
            mDialog.show();
        }
    }

    private static void startBackgroundJob(MonitoredActivity activity,
            String title, String message, Runnable job, Handler handler) {
        // Make the progress dialog uncancelable, so that we can gurantee
        // the thread will be done before the activity getting destroyed.
        ProgressDialog dialog = ProgressDialog.show(activity, title,
                message, true, false);
        new Thread(new BackgroundJob(activity, job, dialog, handler)).
                  start();
    }

    Runnable mRunFaceDetection = new Runnable() {
        float mScale = 1F;
        Matrix mImageMatrix;

        // Create a default HightlightView if we found no face in the
        // picture.
        private void makeDefault() {
            if(hv != null){
                mImageView.remove(hv);
            }
            hv = new HighlightView(mImageView);
            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();
            Rect imageRect = new Rect(0, 0, width, height);

            // make the default size about 4/5 of the width or height
            int cropWidth = Math.min(width, height) * 4 / 5;
            int cropHeight = cropWidth;
            if (mAspectX != 0 && mAspectY != 0) {
                if (mAspectX > mAspectY) {
                    cropHeight = cropWidth * mAspectY / mAspectX;
                } else {
                    cropWidth = cropHeight * mAspectX / mAspectY;
                }
            }
            int x = (width - cropWidth) / 2;
            int y = (height - cropHeight) / 2;
            RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
            hv.setup(mImageMatrix, imageRect, cropRect, mCircleCrop,
                    mAspectX != 0 && mAspectY != 0);
            mImageView.add(hv);
        }
        public void run() {
            mImageMatrix = mImageView.getImageMatrix();

            mScale = 1.0F / mScale;
            mHandler.post(new Runnable() {
                public void run() {
                    makeDefault();

                    mImageView.invalidate();
                    if (mImageView.mHighlightViews.size() == 1) {
                        mCrop = mImageView.mHighlightViews.get(0);
                        mCrop.setFocus(true);
                    }
                }
            });
        }
    };

    /**
     * 点击保存按钮后，完成对裁剪后的图片的处理。操作成功后，向调用本裁剪程序的程序传回裁剪后图片的URI
     *
     * 注意：Android系统默认裁剪程序是传回的是一个bitmap对象，如果此bitmap数据比较大的话就会引
     * 起系统出错，并抛出异常：android.os.transactiontoolargeexception。为了避免出现
     * 异常，所以在这里采取先保存裁剪后图片的数据到应用程序的工作空间，再将此图片URI传回，应用程序就可
     * 以根据这个URI就可以拿到裁剪后的图片。
     *
     */
    private void onSaveClicked() {
        if (mCrop == null) {
            return;
        }
        if (isSaveButtonClicked)
            return;
        isSaveButtonClicked = true;
        final Bitmap croppedImage;
        Rect r = mCrop.getCropRect();
        int width = r.width();
        int height = r.height();

        // If we are circle cropping, we want alpha channel, which is the
        // third param here.
        croppedImage = Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(croppedImage);
        Rect dstRect = new Rect(0, 0, width, height);
        canvas.drawBitmap(mBitmap, r, dstRect, null);

        // Release bitmap memory as soon as possible
        mImageView.clear();
        mBitmap.recycle();
        mBitmap = null;
        mImageView.setImageBitmapResetBase(croppedImage, true);
        mImageView.center(true, true);
        mImageView.mHighlightViews.clear();

        String imgPath = getFilePath(targetUri);// 原图片的路径
        String tempStr = null;
        if(cropped_image_uri != null){
            tempStr = getFilePath(cropped_image_uri);
        } else {
            tempStr = imgPath + "_crop.jpg";
        }
        final String cropPath = tempStr; //裁剪后图片的路径
        mHandler.post(new Runnable(){
            @Override
            public void run() {
                 saveDrawableToCache(croppedImage,cropPath);
            }
        });
        Uri cropUri = Uri.fromFile(new File(cropPath));

        //这里将裁剪后图片的URI传回给调用者，其实也可以不传，因为，裁剪后图片的URI和调用者传来的URI是一样的。
        Intent intent = new Intent("inline-data");
        intent.putExtra(CROPPED_IMAGE_URI, cropUri);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 将Bitmap对象中的图片数据放入指定的图片文件中
     *
     * @param bitmap    //图片对象的数据
     * @param filePath  //图片要存放的绝对路径
     *
     */
    private void saveDrawableToCache(Bitmap bitmap,String filePath){
        try {
            File file = new File(filePath);
            OutputStream outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
