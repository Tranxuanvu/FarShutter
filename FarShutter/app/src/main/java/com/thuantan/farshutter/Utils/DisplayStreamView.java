package com.thuantan.farshutter.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Created by choxu on 006/6/5/2016.
 */
public class DisplayStreamView extends SurfaceView {
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsAutoFullWidth = false;
    private boolean mIsResize = false;
    private boolean mIsBlockCanvas = false;
    private Matrix mMatrix = null;

    public DisplayStreamView(Context context) {
        super(context);

        mSurfaceHolder = getHolder();
    }

    public DisplayStreamView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mSurfaceHolder = getHolder();
    }

    public DisplayStreamView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mSurfaceHolder = getHolder();
    }

    public void UpdateView(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        if (bitmap != null) {
            if (!mIsResize && mIsAutoFullWidth) {
                ViewGroup.LayoutParams params = this.getLayoutParams();

                Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                params.width = display.getWidth();
                params.height = Math.round(params.width * (float) bitmap.getWidth() / bitmap.getHeight());
                this.setLayoutParams(params);

                mIsResize = true;
            }

            if (mMatrix == null) {
                mMatrix = new Matrix();
                mMatrix.preRotate(90, 0, bitmap.getHeight());
                mMatrix.preTranslate(-bitmap.getHeight(), 0);
                mMatrix.postScale((float) mSurfaceHolder.getSurfaceFrame().width() / bitmap.getHeight(), (float) mSurfaceHolder.getSurfaceFrame().height() / bitmap.getWidth(), 0, 0);
            }

            if (mSurfaceHolder.getSurface().isValid() && !mIsBlockCanvas) {
                mIsBlockCanvas = true;

                Canvas c = mSurfaceHolder.lockCanvas();
                c.drawBitmap(bitmap, mMatrix, null);
                mSurfaceHolder.unlockCanvasAndPost(c);

                mIsBlockCanvas = false;
            }

            bitmap.recycle();
        }
    }

    public boolean isAutoFullWidth() {
        return mIsAutoFullWidth;
    }

    public void setAutoFullWidth(boolean isAutoFullWidth) {
        this.mIsAutoFullWidth = isAutoFullWidth;
    }
}
