package com.thuantan.farshutter.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by choxu on 006/6/5/2016.
 */
public class DisplayStreamView extends SurfaceView {
    private SurfaceHolder mSurfaceHolder;

    public DisplayStreamView(Context context) {
        super(context);

        mSurfaceHolder = getHolder();
    }

    public void UpdateView(byte[] data){
        if (mSurfaceHolder.getSurface().isValid()){
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

            Canvas c = mSurfaceHolder.lockCanvas();
            c.drawBitmap(bitmap, new Rect(0,0, bitmap.getWidth(), bitmap.getHeight()), mSurfaceHolder.getSurfaceFrame(), null);
            mSurfaceHolder.unlockCanvasAndPost(c);

            bitmap.recycle();
        }
    }
}
