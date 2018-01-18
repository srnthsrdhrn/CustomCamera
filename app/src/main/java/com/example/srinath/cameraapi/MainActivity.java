package com.example.srinath.cameraapi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MainActivity extends Activity {
    private static final String TAG = "CustomCamera";
    private Camera mCamera;
    private CustomCamera mPreview;
    private int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private MediaRecorder mMediaRecorder;
    private CountDownTimer timer;
    private TextView time_count;
    private long MAX_VIDEO_RECORDING_TIME = 300000;
    private ImageButton captureButton,captureButtonVideo;
    private DrawingView drawingView;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            mCamera = getCameraInstance();
            mPreview = new CustomCamera(this, mCamera);
            FrameLayout preview = findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            captureButton = findViewById(R.id.button_capture_image);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // get an image from the camera
                            mCamera.takePicture(null, null, mPicture);
                        }
                    }
            );
            final boolean[] isRecording = {false};
            captureButtonVideo = findViewById(R.id.button_capture_video);
            captureButtonVideo.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isRecording[0]) {
                                // stop recording and release camera
                                mMediaRecorder.stop();  // stop the recording
                                releaseMediaRecorder(); // release the MediaRecorder object
                                mCamera.lock();         // take camera access back from MediaRecorder

                                // inform the user that recording has stopped

                                isRecording[0] = false;
                            } else {
                                // initialize video camera
                                if (prepareVideoRecorder()) {
                                    // Camera is available and unlocked, MediaRecorder is prepared,
                                    // now you can start recording
                                    mMediaRecorder.start();
                                    time_count.setVisibility(View.VISIBLE);
                                    timer.start();

                                    // inform the user that recording has started

                                    isRecording[0] = true;
                                } else {
                                    // prepare didn't work, release the camera
                                    releaseMediaRecorder();
                                    // inform user
                                }
                            }
                        }
                    }
            );

        }
    }

    final boolean[] isRecording = {false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        drawingView = new DrawingView(this);
        ViewGroup.LayoutParams layoutParamsDrawing
                = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT);
        this.addContentView(drawingView,layoutParamsDrawing);
        time_count = findViewById(R.id.timer);
        time_count.setVisibility(View.GONE);
        time_count.setText("5:00");
        timer = new CountDownTimer(MAX_VIDEO_RECORDING_TIME, 1000) {

            public void onTick(long millisUntilFinished) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                time_count.setText(minutes + ":" + seconds);
            }

            public void onFinish() {
                Toast.makeText(MainActivity.this, "Saved Video", Toast.LENGTH_SHORT).show();
                isRecording[0] = false;
                releaseMediaRecorder();
                timer.cancel();
                time_count.setText("5:00");
                time_count.setVisibility(View.GONE);

            }
        };
        if (checkCameraHardware(this)) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)
                        this, Manifest.permission.CAMERA)) {


                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO},
                            MY_PERMISSIONS_REQUEST_CAMERA);
                }

            } else {
                mCamera = getCameraInstance();
                mPreview = new CustomCamera(this, mCamera);
                FrameLayout preview = findViewById(R.id.camera_preview);
                preview.addView(mPreview);
                mCamera.startPreview();

                captureButton = findViewById(R.id.button_capture_image);
                captureButton.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // get an image from the camera
                                try {
                                    mCamera.takePicture(ShutterCallBack, null, null, mPicture);

                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Please Wait a second between clicks", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                );


                captureButtonVideo = findViewById(R.id.button_capture_video);
                captureButtonVideo.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isRecording[0]) {
                                    // stop recording and release camera
                                    timer.onFinish();

                                    // inform the user that recording has stopped

                                    isRecording[0] = false;
                                } else {
                                    // initialize video camera
                                    if (prepareVideoRecorder()) {
                                        // Camera is available and unlocked, MediaRecorder is prepared,
                                        // now you can start recording
                                        mMediaRecorder.start();
                                        time_count.setVisibility(View.VISIBLE);
                                        timer.start();
                                        // inform the user that recording has started

                                        isRecording[0] = true;
                                    } else {
                                        // prepare didn't work, release the camera
                                        releaseMediaRecorder();
                                        // inform user
                                    }
                                }
                            }
                        }
                );

            }
        }
        // Create our Preview view and set it as the content of our activity.

    }

    private boolean prepareVideoRecorder() {

        mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mMediaRecorder.setMaxDuration(10000);
        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private File getOutputMediaFile(int type) {

        File mediaStorageDir = new File(getDir("MGCameraApp", MODE_PRIVATE).getAbsolutePath());

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.i("MgCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        switch (type) {
            case MEDIA_TYPE_IMAGE:
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "IMG_" + timeStamp + ".jpg");
                Log.i("MGCameraApp", mediaFile.getAbsolutePath());
                return mediaFile;


            case MEDIA_TYPE_VIDEO:
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_" + timeStamp + ".wav");
                return mediaFile;

            default:
                return null;
        }
    }

    private Camera.ShutterCallback ShutterCallBack = new Camera.ShutterCallback() {

        @Override
        public void onShutter() {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            switch (audio.getRingerMode()) {
                case AudioManager.RINGER_MODE_NORMAL:
                    MediaActionSound sound = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        sound = new MediaActionSound();
//                        sound.play(MediaActionSound.SHUTTER_CLICK);
                    }

                    break;
                case AudioManager.RINGER_MODE_SILENT:
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    break;
            }
        }
    };


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            Log.i("MGCameraApp", pictureFile.getAbsolutePath());
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");

                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            camera.startPreview();

        }
    };


    public Camera getCameraInstance() {
        Camera c = null;

        try {
            c = Camera.open(0);
            setCameraDisplayOrientation(this, 0, c);
            //set camera to continually auto-focus
            Camera.Parameters params = c.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            c.setParameters(params);

            // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);

    }

    Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback(){

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            // TODO Auto-generated method stub
            if (arg0){
                captureButton.setEnabled(true);
                captureButtonVideo.setEnabled(true);
                mCamera.cancelAutoFocus();
            }

            float focusDistances[] = new float[3];
            arg1.getParameters().getFocusDistances(focusDistances);
            Toast.makeText(MainActivity.this,"Optimal Focus Distance(meters): "+ focusDistances[Camera.Parameters.FOCUS_DISTANCE_OPTIMAL_INDEX],Toast.LENGTH_LONG).show();

        }};

    public void touchFocus(final Rect tfocusRect){



        mCamera.cancelAutoFocus();

        //Convert from View's width and height to +/- 1000
        final Rect targetFocusRect = new Rect(
                tfocusRect.left ,
                tfocusRect.top ,
                tfocusRect.right ,
                tfocusRect.bottom );

        final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
        Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
        focusList.add(focusArea);

        Camera.Parameters para = mCamera.getParameters();
        int areas = para.getMaxNumFocusAreas();
        if(areas !=0) {
            para.setFocusAreas(focusList);

            mCamera.setParameters(para);

            mCamera.autoFocus(myAutoFocusCallback);

            drawingView.setHaveTouch(true, tfocusRect);
            drawingView.invalidate();
        }else{
            Toast.makeText(MainActivity.this,"Camera Doesnt Support Manual Focus",Toast.LENGTH_LONG).show();
        }
    }

    private class DrawingView extends View{

        boolean haveFace;
        Paint drawingPaint;

        boolean haveTouch;
        Rect touchArea;

        public DrawingView(Context context) {
            super(context);
            haveFace = false;
            drawingPaint = new Paint();
            drawingPaint.setColor(Color.GREEN);
            drawingPaint.setStyle(Paint.Style.STROKE);
            drawingPaint.setStrokeWidth(2);

            haveTouch = false;
        }

        public void setHaveTouch(boolean t, Rect tArea){
            haveTouch = t;
            touchArea = tArea;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            if(haveTouch){
                drawingPaint.setColor(Color.BLUE);
                canvas.drawRect(
                        touchArea.left, touchArea.top, touchArea.right, touchArea.bottom,
                        drawingPaint);
            }
        }

    }

}
