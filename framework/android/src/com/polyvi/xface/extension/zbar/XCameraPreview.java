
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

package com.polyvi.xface.extension.zbar;

import java.io.IOException;

import com.polyvi.xface.util.XLog;

import android.view.SurfaceView;
import android.view.SurfaceHolder;

import android.annotation.TargetApi;
import android.content.Context;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;

/** A basic Camera preview class */
public class XCameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private final static String CLASS_NAME = XCameraPreview.class.getSimpleName();

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private PreviewCallback mPreviewCallback;
    private AutoFocusCallback mAutoFocusCallback;

    public XCameraPreview(Context context, Camera camera,
                         PreviewCallback previewCb,
                         AutoFocusCallback autoFocusCb) {
        super(context);
        mCamera = camera;
        mPreviewCallback = previewCb;
        mAutoFocusCallback = autoFocusCb;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            XLog.e(CLASS_NAME, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Camera preview released in activity
    }

    @TargetApi(8)
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        /*
         * If your preview can change or rotate, take care of those events here.
         * Make sure to stop the preview before resizing or reformatting it.
         */
        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        try {
            // Hard code camera surface rotation 90 degs to match Activity view in portrait
            //TODO:setDisplayOrientation在android 2.1上不支持
            mCamera.setDisplayOrientation(90);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.startPreview();
            mCamera.autoFocus(mAutoFocusCallback);
        } catch (Exception e){
            XLog.e(CLASS_NAME, "Error starting camera preview: " + e.getMessage());
        }
    }
}
