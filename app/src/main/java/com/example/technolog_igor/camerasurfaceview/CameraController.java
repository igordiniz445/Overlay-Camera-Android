package com.example.technolog_igor.camerasurfaceview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;


public class CameraController extends SurfaceView implements SurfaceHolder.Callback, GestureDetector.OnGestureListener {

    private android.hardware.Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean previewing = false;
    private float mDist;
    private Activity activity;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;
    public static int CAMERA_SELECIONADA = 0;
    public static final String CAMERA_FRONT = "1";
    public static final String CAMERA_BACK = "0";

    private String cameraId = CAMERA_BACK;

    public CameraController(Activity atividade, int surfaceView) {
        super(atividade.getApplicationContext());
        this.surfaceView = (SurfaceView) atividade.findViewById(surfaceView);
        surfaceHolder = this.surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        activity = atividade;

    }


    /**
     * Isto é chamado imediatamente após o SurfaceHolder ser criado.
     *
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = android.hardware.Camera.open();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        this.surfaceView.setLayoutParams(params);
        camera.setDisplayOrientation(90);

    }

    /**
     * Isto é chamado imediatamente após as alterações estruturais (o formato ou
     * tamanho) foram feitos ao SurfaceHolder.
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (previewing) {
            pararVisualizacao();
        }
        if (camera != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                iniciarVisualizacao();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Isto é chamado imediatamente antes do SurfaceHolder ser destruído.
     *
     * @param holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pararVisualizacao();
        camera.release();
        camera = null;
    }

    public void tirarFoto(Camera.ShutterCallback shutter, Camera.PictureCallback raw, Camera.PictureCallback jpeg) {
        camera.takePicture(shutter, raw, jpeg);
    }


    @SuppressLint("ClickableViewAccessibility")
    public void iniciarVisualizacao() {
        previewing = true;
        camera.startPreview();
        camera.getParameters().setPreviewFrameRate(60);
        camera.getParameters().setVideoStabilization(true);
        camera.getParameters().setAutoWhiteBalanceLock(true);



        this.surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Camera.Parameters params = camera.getParameters();
                int action = event.getAction();
                Log.wtf("Pointer Count",""+event.getPointerCount());
                if (event.getPointerCount() > 1) {
                    if (action == MotionEvent.ACTION_POINTER_DOWN) {
                        mDist = getFingerSpacing(event);
                    } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                        camera.cancelAutoFocus();
                        handleZoom(event, params);
                    }
                } else {
                    // handle single touch events
                    if (action == MotionEvent.ACTION_UP) {
                        try{
                            handleFocus(event, params);
                        }catch(Exception e){
                            Log.wtf("Erro Foco:",""+e.getMessage());
                        }
                    }
                }

                return true;
            }
        });

    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        camera.setParameters(params);
    }

    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch
                }
            });
        }
    }

    /** Determine the space between the first two fingers */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    public void pararVisualizacao() {
        camera.stopPreview();
        previewing = false;
    }

    public Camera getCameraControler() {
        return camera;
    }

    public void hideView(){
        this.surfaceView.setVisibility(View.GONE);
    }


    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {


        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    public boolean botaoFlash(boolean value){
        Camera.Parameters parameter = camera.getParameters();

        if(value){
            parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameter);
        }else{
            parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameter);
        }
        return false;
    }

    public void switchCamera() {

        if (cameraId.equals(CAMERA_FRONT)) {
            cameraId = CAMERA_BACK;
            camera.stopPreview();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            this.surfaceView.setLayoutParams(params);
            camera.release();
            camera = android.hardware.Camera.open(0);
            camera.setDisplayOrientation(90);
            CAMERA_SELECIONADA = 0;
            //switchCameraButton.setImageResource(R.drawable.ic_camera_front);
        } else if (cameraId.equals(CAMERA_BACK)) {
            try{
                cameraId = CAMERA_FRONT;
                camera.stopPreview();
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,1000);
                this.surfaceView.setLayoutParams(params);
                camera.release();
                camera = android.hardware.Camera.open(1);
                camera.setDisplayOrientation(90);
                CAMERA_SELECIONADA = 1;
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        try {
            camera.setPreviewDisplay(surfaceHolder);
            //"this" is a SurfaceView which implements SurfaceHolder.Callback,
            //as found in the code examples
            camera.startPreview();
        } catch (IOException exception) {
            camera.release();
            camera = null;
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        mSupportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }

        if (mPreviewSize!=null) {
            float ratio;
            if(mPreviewSize.height >= mPreviewSize.width)
                ratio = (float) mPreviewSize.height / (float) mPreviewSize.width;
            else
                ratio = (float) mPreviewSize.width / (float) mPreviewSize.height;

            // One of these methods should be used, second method squishes preview slightly
            setMeasuredDimension(width, (int) (width * ratio));
            // setMeasuredDimension((int) (width * ratio), height);
        }
    }

}