package com.proxy.shadowsocksr.qrscan_not_use;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Gravity;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class QRScanActivityOld extends Activity implements TextureView.SurfaceTextureListener
{
    private TextureView tv;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        tv = new TextureView(this);
        setContentView(tv);
        tv.setSurfaceTextureListener(this);
    }

    private Camera camera;

    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        camera = Camera.open();
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                previewSize.width, previewSize.height, Gravity.CENTER));
        try
        {
            camera.setPreviewTexture(surface);
        }
        catch (IOException t)
        {
        }
        camera.startPreview();
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {

    }

    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        if (camera != null)
        {
            camera.stopPreview();
            camera.release();
        }
        return true;
    }

    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
    }
}
