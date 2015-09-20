package com.proxy.shadowsocksr.qrscan_not_use;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.Arrays;
//TODO: Not Complete Code
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class QRScanActivityNew extends Activity implements TextureView.SurfaceTextureListener
{
    private TextureView tv;

    private Size previewSize;
    private CaptureRequest.Builder previewBuilder;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        tv = new TextureView(this);
        setContentView(tv);
        tv.setSurfaceTextureListener(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            new AlertDialog.Builder(this)
                    .setTitle("Warning!")
                    .setMessage("Your device have not camera!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        @Override public void onClick(DialogInterface dialog, int which)
                        {
                            finish();
                        }
                    })
                    .show();
        }

        threadHandler = new HandlerThread("CAMERA2");
        threadHandler.start();
        handler = new Handler(threadHandler.getLooper());
    }

    @Override protected void onResume()
    {
        super.onResume();
    }

    @Override protected void onPause()
    {
        super.onPause();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {//TODO: Android 6 not test
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK && requestCode == 0)
        {
            new AlertDialog.Builder(this)
                    .setTitle("Warning!")
                    .setMessage("You must agree grant use camera permission to scan QR code!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        @Override public void onClick(DialogInterface dialog, int which)
                        {
                            finish();
                        }
                    })
                    .show();
        }
    }

    private Handler handler;
    private HandlerThread threadHandler;

    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try
        {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics("0");
            //支持的STREAM CONFIGURATION
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //显示的size
            previewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            //打开相机
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat
                        .requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
                return;
            }
            manager.openCamera("0", cameraStateCallback, handler);
        }
        catch (Exception e)
        {
            //
        }
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {

    }

    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        return false;
    }

    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {

    }

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback()
    {
        @Override public void onOpened(@NonNull CameraDevice camera)
        {
            SurfaceTexture texture = tv.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            try
            {
                previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewBuilder.addTarget(surface);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
            }

            try
            {
                camera.createCaptureSession(Arrays.asList(surface), sessionStateCallback, handler);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        @Override public void onDisconnected(@NonNull CameraDevice camera)
        {
        }

        @Override public void onError(@NonNull CameraDevice camera, int error)
        {
        }
    };

    private CameraCaptureSession.StateCallback sessionStateCallback
            = new CameraCaptureSession.StateCallback()
    {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session)
        {
            try
            {
                session.setRepeatingRequest(previewBuilder.build(), null, handler);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session)
        {
        }
    };
}
