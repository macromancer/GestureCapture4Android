package com.example.kikim.gesturecapture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public class CaptureActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener{
    private static final String TAG = "GCapture::Activity";

    private GestureCameraView mOpenCVCameraView;
    private List<Camera.Size> mResolutionList;
    private SubMenu mColorEffectsMenu;
    private SubMenu mResolutionMenu;
    private SubMenu mGesturesMenu;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCVCameraView.enableView();
                    mOpenCVCameraView.setOnTouchListener(CaptureActivity.this);
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public CaptureActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called  onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

        setContentView(R.layout.activity_capture);

        mOpenCVCameraView = (GestureCameraView) findViewById((R.id.gesture_camera_view));
        mOpenCVCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("Pointing Thumb Up");
        //tv.setText(stringFromJNI());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected((LoaderCallbackInterface.SUCCESS));
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat img = inputFrame.rgba();
        Imgproc.circle(img, new Point(200, 100), 20, new Scalar(200));
        return img;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        List<String> effects = mOpenCVCameraView.getEffectList();

        if (effects == null){
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

        mColorEffectsMenu = menu.addSubMenu("Color Effect");

        int idx = 0;
        for(String effect : effects) {
            mColorEffectsMenu.add(1, idx, Menu.NONE, effect);
            idx++;
        }


        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCVCameraView.getResolutionList();

        idx = 0;
        for(Camera.Size element : mResolutionList) {
            mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" +
                    Integer.valueOf(element.height).toString());
            idx++;
        }

        mGesturesMenu = menu.addSubMenu("Gesture");
        mGesturesMenu.add(3,  0, Menu.NONE, "Pointing Thumb Up");
        mGesturesMenu.add(3,  1, Menu.NONE, "Pointing Thumb Fold");

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item.getGroupId() == 1) {
            mOpenCVCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCVCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2) {
            int id = item.getItemId();
            Camera.Size resolution = mResolutionList.get(id);
            mOpenCVCameraView.setResolution(resolution);
            resolution = mOpenCVCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" +
                             Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 3) {
            String gesture = item.toString();
            Toast.makeText(this, gesture, Toast.LENGTH_SHORT).show();

            TextView tv = (TextView) findViewById(R.id.sample_text);
            tv.setText(gesture);
        }

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() +
                "/sample_picture_" + currentDateandTime + ".jpg";
        mOpenCVCameraView.takePicture(fileName);
        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        return false;
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
