package com.example.kikim.gesturecapture;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Camera View with point markers
 *
 * Created by kikim on 17. 2. 16.
 */

public class GestureCameraView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = "GCapture::GCameraView";
    private String mPictureFileName;

    public GestureCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public GestureCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;

        mCamera.setPreviewCallback(null);

        mCamera.takePicture(null, null, null);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera)
    {
        Log.i(TAG, "Saving a bitmap to File");

        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        try {
            FileOutputStream fos = new FileOutputStream((mPictureFileName));

            fos.write(data);
            fos.close();
        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }


    }
}
