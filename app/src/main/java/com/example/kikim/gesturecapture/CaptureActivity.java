package com.example.kikim.gesturecapture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
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
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CaptureActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "GCapture::Activity";

    private GestureCameraView mOpenCVCameraView;
    private List<Camera.Size> mResolutionList;
    private SubMenu mColorEffectsMenu;
    private SubMenu mResolutionMenu;
    private SubMenu mGesturesMenu;

    private int marker_x = 400;
    private int marker_y = 200;
    private int radius = 50;
    private int period = 3600;

    private Random rand = new Random();

    private Timer timer;
    private long startTime;

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
        tv.setText("Pointing_Thumb_Up");
        //tv.setText(stringFromJNI());
    }

    private void setNewMarker()
    {
        if (mOpenCVCameraView == null) return;

        Camera.Size resol = mOpenCVCameraView.getResolution();

        marker_x = rand.nextInt(resol.width + 1);
        marker_y = rand.nextInt(resol.height + 1);
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

        Imgproc.circle(img, new Point(marker_x, marker_y), radius, new Scalar(128, 128, 128), 10);

        double start_deg = 270;
        double add_deg = 0;
        if (timer != null)
            add_deg = 360 * (System.currentTimeMillis() - startTime) / period;

        Imgproc.ellipse(img, new Point(marker_x, marker_y), new Size(radius + 10, radius + 10), 0,
                start_deg, start_deg + add_deg,
                new Scalar(255, 255, 255), 10);

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
        mGesturesMenu.add(3,  0, Menu.NONE, "No_Gesture");
        mGesturesMenu.add(3,  1, Menu.NONE, "Pointing_Thumb_Up");
        mGesturesMenu.add(3,  2, Menu.NONE, "Pointing_Thumb_Fold");

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

        if (timer == null) {
            Log.i(TAG, "Start Gesture Capture");
            timer = new Timer();
            timer.schedule(new TimerTask() {
                boolean isFirst = true;

                @Override
                public void run() {
                    if (!isFirst) {
                        capture();
                    } else {
                        isFirst = false;
                    }

                    setNewMarker();
                    startTime = System.currentTimeMillis();
                    Log.d(TAG, "Center (" + marker_x + "," + marker_y + ")");
                }
            }, period, period);

            TextView tv = (TextView) findViewById(R.id.sample_text);
            String gestureType = tv.getText().toString();
            Toast.makeText(this, "[" + gestureType + "] Capture Started", Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "Stop Gesture Capture");
            timer.cancel();
            timer = null;

            Toast.makeText(this, "Capture Stopped", Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    private void capture()
    {
        TextView tv = (TextView) findViewById(R.id.sample_text);
        String gestureType = tv.getText().toString();

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
        String now = df.format(new Date());

        String marker = marker_x + "_" + marker_y + "_" + radius;

        String fileName = gestureType + "_" + marker + "_" + now + ".jpg";

        File rootDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //Log.i(TAG, "SD card Mounted");
            rootDir = Environment.getExternalStorageDirectory();
        } else {
            //Log.i(TAG, "SD card Not mounted");
            rootDir = Environment.getRootDirectory();
        }

        String fileDir = rootDir.getAbsolutePath() + "/gesture_capture/";

        File dir = new File(fileDir);
        if (! dir.isDirectory())
            dir.mkdirs();

        String filePath = fileDir + fileName;

        mOpenCVCameraView.takePicture(filePath);

        Log.i(TAG, "saved: " + filePath);
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
