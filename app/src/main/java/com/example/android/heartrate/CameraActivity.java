package com.example.android.heartrate;

import android.Manifest;
import android.content.Context;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;



import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.Arrays;


public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private TextureView textureView; //TextureView to deploy camera data
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private static final int REQUEST_CAMERA_PERMISSION = 1;


    public static final String  EXTRA_NUMBER="com.example.android.heartrate.EXTRA_NUMBER";

    // Thread handler member variables
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    //Heart rate detector member variables
    int hrtratebpm=60;
    private int mCurrentRollingAverage;
    private int mLastRollingAverage;
    private int mLastLastRollingAverage;
    private long [] mTimeArray;
    private int numCaptures = 0;
    private int mNumBeats = 0;

    TextView tv;

    private LineGraphSeries<DataPoint> series;
    private int lastX=0;

    public int dataMaxPoint=100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        textureView =  findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        mTimeArray = new long [30];
        tv = (TextView)findViewById(R.id.neechewalatext);

        //we get graph
        GraphView graph =(GraphView)findViewById(R.id.graph);


          //data
        series=new LineGraphSeries<DataPoint>();

        graph.addSeries(series);

        //customer viewreport
        Viewport viewport=graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0);
        viewport.setMaxY(40);
        viewport.setMinX(0);
        viewport.setMaxX(dataMaxPoint);
        viewport.setScrollable(true);
        viewport.setScalable(true);
        //GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        //gridLabel.setHorizontalAxisTitle("Time");
        //gridLabel.setVerticalAxisTitle("mmHg");
        //StaticLabelsFormatter staticLabelsFormatter =new StaticLabelsFormatter(graph);
        //staticLabelsFormatter.setVerticalLabels(new String[]{"4","6","8","10","12","14","16"});
        //staticLabelsFormatter.setHorizontalLabels(new String[]{"1","2","3","4","5"});
        //graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridStyle( GridLabelRenderer.GridStyle.NONE );

    }


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.d(TAG, "onSurfaceTextureUpdated");
            Bitmap bmp = textureView.getBitmap();
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int imgSize=width*height;
            int[] pixels = new int[imgSize];

            bmp.getPixels(pixels, 0, width, width / 2, height / 2, width / 20, height / 20);


            int sum = 0;

            addEntry();

            for (int i = 0; i < imgSize; i++) {
                int red = (pixels[i] >> 16) & 0xFF;
                sum = sum + red;

            }
            if(sum>90000){
            //RedValue=sum/(imgSize);
            // Waits 20 captures, to remove startup noise.
            if (numCaptures == 20) {
                mCurrentRollingAverage = sum;
            }
            // Next 18 averages needs to incorporate the sum with the correct N multiplier
            // in rolling average.
            else if (numCaptures > 20 && numCaptures < 49) {
                mCurrentRollingAverage = (mCurrentRollingAverage*(numCaptures-20) + sum)/(numCaptures-19);
            }
            // From 49 on, the rolling average incorporates the last 30 rolling averages.
            else if (numCaptures >= 49) {
                mCurrentRollingAverage = (mCurrentRollingAverage*29 + sum)/30;
                if (mLastRollingAverage > mCurrentRollingAverage && mLastRollingAverage > mLastLastRollingAverage && mNumBeats < 30) {
                    mTimeArray[mNumBeats] = System.currentTimeMillis();
                    //tv.setText("Beats="+mNumBeats+"\nsum:"+sum+"\nTime="+mTimeArray[mNumBeats]);
                    tv.setText("Beats = "+mNumBeats+"\nTry not to move your finger\nWaiting for measure 30 beats");
                    //tv.setText("Beats = "+sum+"\nTry not to move your finger");

                    mNumBeats++;
                    addHeart_beat();
                    if (mNumBeats == 30) {
                        calcBPM();
                    }
                }
                else addEntry();
            }

            // Another capture

            numCaptures++;


            // Save previous two values
            mLastLastRollingAverage = mLastRollingAverage;
            mLastRollingAverage = mCurrentRollingAverage;

            //onPause();
            } else {
                numCaptures=0;
                mNumBeats=0;
                //tv.setText("Your finger is not cover the camera");
                tv.setText(sum+"\nTime="+mTimeArray[mNumBeats]);
            }
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            if (cameraDevice != null)
                cameraDevice.close();
            cameraDevice = null;
        }
    };

    // onResume
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    // onPause
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void calcBPM() {
        int med;
        long [] timedist = new long [29];
        for (int i = 0; i < 29; i++) {
            timedist[i] = mTimeArray[i+1] - mTimeArray[i];
        }
        Arrays.sort(timedist);
       // med = ((int)timedist[14]+(int)timedist[15]+(int)timedist[16])/3;
         med = ((int)timedist[11]+(int)timedist[12]+(int)timedist[13]+(int)timedist[14])/4;
        hrtratebpm= 60000/med;

        TextView tv = (TextView)findViewById(R.id.neechewalatext);
        tv.setText("Heart Rate = "+hrtratebpm+" BPM");
        //tv.setText("Heart Rate = "+(int)timedist[13]+"   time dist 14: "+(int)timedist[14]+"   15: "+(int)timedist[15]+"   16: "+(int)timedist[16]+" BPM");

    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // Opening the camera for use
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if (null == cameraDevice) {
           // Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(CameraActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
       // stopBackgroundThread();
        super.onPause();
        /*Intent intent=new Intent(getApplicationContext(),ResultActivity.class);
        intent.putExtra(EXTRA_NUMBER,hrtratebpm);
        startActivity(intent);*/
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        closeCamera();
        stopBackgroundThread();
        super.onStop();
    }

    private void addEntry(){
        //chose to display max 10 point on the viewport and scroll to the end
        //series.appendData(new DataPoint(lastX++, RANDOM.nextDouble()*10d),true,1000);

        series.appendData(new DataPoint(lastX++, 10),true,dataMaxPoint);
    }


    private void addHeart_beat(){
        series.appendData(new DataPoint(lastX++,12),true,dataMaxPoint);
        // series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);

        series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);
        //series.appendData(new DataPoint(lastX++,12),true,dataMaxPoint);
        // series.appendData(new DataPoint(lastX++,13),true,dataMaxPoint);

        series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);


        // series.appendData(new DataPoint(lastX++,13),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,8),true,dataMaxPoint);
        // series.appendData(new DataPoint(lastX++,11),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,18),true,dataMaxPoint);
        // series.appendData(new DataPoint(lastX++,9),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,28),true,dataMaxPoint);
        //series.appendData(new DataPoint(lastX++,7),true,dataMaxPoint);
        //series.appendData(new DataPoint(lastX++,15),true,dataMaxPoint);

        series.appendData(new DataPoint(lastX++,6),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);

        series.appendData(new DataPoint(lastX++,12),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,11),true,dataMaxPoint);
    }
    /*private void addHeart_beat(){
        series.appendData(new DataPoint(lastX++,9),true,dataMaxPoint);
       // series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);

        series.appendData(new DataPoint(lastX++,11),true,dataMaxPoint);
        //series.appendData(new DataPoint(lastX++,12),true,dataMaxPoint);
       // series.appendData(new DataPoint(lastX++,13),true,dataMaxPoint);

        series.appendData(new DataPoint(lastX++,14),true,dataMaxPoint);


       // series.appendData(new DataPoint(lastX++,13),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,12),true,dataMaxPoint);
       // series.appendData(new DataPoint(lastX++,11),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);
       // series.appendData(new DataPoint(lastX++,9),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,8),true,dataMaxPoint);
        //series.appendData(new DataPoint(lastX++,7),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,6),true,dataMaxPoint);

     //   series.appendData(new DataPoint(lastX++,7),true,dataMaxPoint);
        //series.appendData(new DataPoint(lastX++,8),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,9),true,dataMaxPoint);
        //series.appendData(new DataPoint(lastX++,10),true,dataMaxPoint);
        series.appendData(new DataPoint(lastX++,11),true,dataMaxPoint);
    }*/


}











