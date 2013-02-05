/**
 * TouchImageView.java 不支持在布局文件中使用
 */
package com.subway.ditu.view;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.subway.ditu.utils.image.ImageUtils;

/**
 * 支持缩放，拖动，还原的ImageView
 * 
 * @author Di Zhang
 */
public class TouchImageView extends View {

    private static final boolean DEBUG = false;
    
    public static final int EXIT = 0;

    private RectF mIdleRectF;
    private RectF mLastRectF;
    private RectF mDesRectF;

    private Bitmap mBitmap;

    private static final float MIN_SCALER = 1.0f; // 最小缩放比例
    private static final float MAX_SCALER = 16.0f; // 最大缩放比例

    // touch状态
    private static final int MODE_NONE = 0; // 初始状态
    private static final int MODE_PRESS = 1; // 按下
    private static final int MODE_DRAG = 2;// 拖动
    private static final int MODE_ZOOM = 3;// 缩放
    private int mTouchMode = MODE_NONE;

    private PointF mPrevPointF = new PointF();
    private PointF mMidPointF = new PointF();
    private float mPreDist = 0f;

    private boolean mFirstOnDraw = true;
    private int mViewWidth;
    private int mViewHeight;
    private int mBitmapWidth;
    private int mBitmapHeight;

    private String mFullPath;

    private RegionBitmap mRegionBitmap;
    
    private RectF mWindowRectF;

    private Paint paint;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case TAP:
                if (!mIsSingleTap && mOnTapListener != null) {
                    mOnTapListener.onTap();
                }
                break;
            }
        }
    };

    private onTapListener mOnTapListener = null;

    public static interface onTapListener {
        public void onTap();

        public void onDoubleTap(MotionEvent event);

        public void onScaleTap(MotionEvent event);

        public void onDragTap(MotionEvent event);
    }

    private static final int TAP = 1;
    private static final int DOUBLE_TAP_TIMEOUT = 200;

    private boolean mIsDoubleTap = false;
    private boolean mIsSingleTap = false; // 单击

    public TouchImageView(Context context) {
        super(context);

        mDesRectF = new RectF();
        mLastRectF = new RectF();
        mIdleRectF = new RectF();
        paint = new Paint();

    }

    public TouchImageView(Context context, Bitmap bitmap) {
        super(context);

        mDesRectF = new RectF();
        mLastRectF = new RectF();
        mIdleRectF = new RectF();
        paint = new Paint();

        initBitmap(bitmap);
    }

    private boolean initBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmapWidth = mBitmap.getWidth();
            mBitmapHeight = mBitmap.getHeight();
            return true;
        }
        return false;
    }

    public void setImageFullPath(String fullPath) {
        mFullPath = fullPath;
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (initBitmap(bitmap)) {
            mFirstOnDraw = true;
            mDesRectF = new RectF();
            mLastRectF = new RectF();
            mIdleRectF = new RectF();
            invalidate();
        }
    }

    public void setOnTapListener(onTapListener listener) {
        mOnTapListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        // 主点按下
        case MotionEvent.ACTION_DOWN:
            mIsSingleTap = true;
            boolean hadTapMessage = mHandler.hasMessages(TAP);
            if (hadTapMessage) {
                mHandler.removeMessages(TAP);
            }
            if (hadTapMessage && isIdlePosition()) {
                mIsDoubleTap = true;
                adapterHeight();
                if (mOnTapListener != null) {
                    mOnTapListener.onDoubleTap(event);
                }
            } else {
                // This is a first tap
                mHandler.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT);
            }

            mLastRectF.set(mDesRectF);
            mPrevPointF.set(event.getX(), event.getY());
            mTouchMode = MODE_PRESS;
            break;
        // 副点按下
        case MotionEvent.ACTION_POINTER_DOWN:
            mPreDist = getSpace(event);
            // 如果连续两点距离大于10，则判定为多点模式
            if (getSpace(event) > 10f) {
                mLastRectF.set(mDesRectF);
                mMidPointF = getMidPointF(event);
                mTouchMode = MODE_ZOOM;
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mTouchMode == MODE_PRESS) {
                if (mIsDoubleTap) {
                    mIsDoubleTap = false;
                }
                mIsSingleTap = false;
            } else {
                checkPosition();
            }
            mTouchMode = MODE_NONE;
            break;
        case MotionEvent.ACTION_POINTER_UP:
            if (mTouchMode == MODE_ZOOM) {
                checkZoomSize();
            }
            mTouchMode = MODE_NONE;
            break;
        case MotionEvent.ACTION_MOVE:
            mRegionBitmap = null;
            if (mTouchMode == MODE_DRAG || mTouchMode == MODE_PRESS) {
                float dragX = event.getX() - mPrevPointF.x;
                float dragY = event.getY() - mPrevPointF.y;

                float dragLength = FloatMath.sqrt(dragX * dragX + dragY * dragY);
                if (dragLength > 10f) {
                    mTouchMode = MODE_DRAG;
                    if (isDragable()) {
                        mDesRectF.set(mLastRectF.left + dragX, mLastRectF.top + dragY, mLastRectF.right + dragX,
                                mLastRectF.bottom + dragY);
                        invalidate();
                    }

                    if (mOnTapListener != null) {
                        mOnTapListener.onDragTap(event);
                    }
                }
            } else if (mTouchMode == MODE_ZOOM) {
                float newDist = getSpace(event);
                if (newDist > 10f) {

                    float tScale = newDist / mPreDist;

                    float deltaLeft = (float) ((tScale - 1.0) * (mLastRectF.left - mMidPointF.x));
                    float deltaRight = (float) ((tScale - 1.0) * (mLastRectF.right - mMidPointF.x));
                    float deltaTop = (float) ((tScale - 1.0) * (mLastRectF.top - mMidPointF.y));
                    float deltaBottom = (float) ((tScale - 1.0) * (mLastRectF.bottom - mMidPointF.y));
                    mDesRectF.set(mLastRectF.left + deltaLeft, mLastRectF.top + deltaTop,
                            mLastRectF.right + deltaRight, mLastRectF.bottom + deltaBottom);
                    invalidate();
                    if (mOnTapListener != null) {
                        mOnTapListener.onScaleTap(event);
                    }
                }
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            mIsDoubleTap = false;
            mIsSingleTap = false;
            mTouchMode = MODE_NONE;
            mHandler.removeMessages(TAP);
            break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFirstOnDraw) {
            mViewWidth = this.getWidth();
            mViewHeight = this.getHeight();

            adapterWidth();
            mIdleRectF.set(mDesRectF);

            mFirstOnDraw = false;
        }

        canvas.save();
        paint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        canvas.drawPaint(paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC));
        if (mRegionBitmap == null && mBitmap != null && !mBitmap.isRecycled()) {
            Rect src = new Rect(0, 0, mBitmapWidth, mBitmapHeight);
            canvas.drawBitmap(mBitmap, src, mDesRectF, paint);
        } else if (mRegionBitmap != null && mRegionBitmap.bitmapDrawRect != null && mRegionBitmap.btDraw != null
                && !mRegionBitmap.btDraw.isRecycled()) {
            Rect src = new Rect(0, 0, mRegionBitmap.btDraw.getWidth(), mRegionBitmap.btDraw.getHeight());
            canvas.drawBitmap(mRegionBitmap.btDraw, src, mRegionBitmap.bitmapDrawRect, paint);
            if (DEBUG) {
                Log.d(">>>>>>>", " onDraw : src = " + src + " targetDraw = " + mRegionBitmap.bitmapDrawRect);
            }
        }
        canvas.restore();
    }

    private RectF getWindowRectF() {
        if (mWindowRectF == null) {
            mWindowRectF = new RectF();
        }
        if (mWindowRectF.width() == 0 || mWindowRectF.height() == 0) {
            mWindowRectF.left = 0;
            mWindowRectF.top = 0;
            mWindowRectF.right = mWindowRectF.left + getWidth();
            mWindowRectF.bottom = mWindowRectF.top + getHeight();
        }
        
        return mWindowRectF;
    }
    
    /**
     * 限制最大最小缩放比例，自动居中 缩小后尺寸 < 原始尺寸 还原
     */
    private void checkZoomSize() {
        mLastRectF.set(mDesRectF);

        float scaleX = mDesRectF.width() / mIdleRectF.width();
        float scaleY = mDesRectF.height() / mIdleRectF.height();

        if (mTouchMode == MODE_ZOOM) {
            if (scaleX < MIN_SCALER || scaleY < MIN_SCALER) {
                mDesRectF.set(mIdleRectF);
            } else if (scaleX > MAX_SCALER || scaleY > MAX_SCALER) {
                float dx = MAX_SCALER / scaleX;
                float dY = MAX_SCALER / scaleY;

                float deltaLeft = (float) ((dx - 1.0) * (mLastRectF.left - mMidPointF.x));
                float deltaRight = (float) ((dx - 1.0) * (mLastRectF.right - mMidPointF.x));
                float deltaTop = (float) ((dY - 1.0) * (mLastRectF.top - mMidPointF.y));
                float deltaBottom = (float) ((dY - 1.0) * (mLastRectF.bottom - mMidPointF.y));
                mDesRectF.set(mLastRectF.left + deltaLeft, mLastRectF.top + deltaTop, mLastRectF.right + deltaRight,
                        mLastRectF.bottom + deltaBottom);
            }

            if (mRegionBitmap != null && mRegionBitmap.btDraw != null && !mRegionBitmap.btDraw.isRecycled()) {
                mRegionBitmap.btDraw.recycle();
            }
            mRegionBitmap = null;
            RectF windowsRectF = getWindowRectF();

            if (DEBUG) {
                Log.d("}}}}", "mDesRectF " + mDesRectF);
            }
            if (RectF.intersects(mDesRectF, windowsRectF)) {
                mRegionBitmap = ImageRegionDecodeUtils.getRegionBitmap(this.mFullPath, mDesRectF,
                        intersect(mDesRectF, windowsRectF));
                if (DEBUG) {
                    Log.d(">>>> ", " mRegionBitmap = " + mRegionBitmap);
                }
            }
        }

        invalidate();
    }

    private RectF intersect(RectF one, RectF two) {
        RectF ret = new RectF();
        if (RectF.intersects(one, two)) {
            ret.left = Math.max(one.left, two.left);
            ret.top = Math.max(one.top, two.top);
            ret.right = Math.min(one.right, two.right);
            ret.bottom = Math.min(one.bottom, two.bottom);

            return ret;
        }

        return ret;
    }

    /*
     * 检查超出位置
     */
    private void checkPosition() {
        mLastRectF.set(mDesRectF);
        float deltaX = 0.0f;
        float deltaY = 0.0f;
        if ((mLastRectF.left < 0 && mLastRectF.right < mViewWidth)
                || (mLastRectF.left > 0 && mLastRectF.right > mViewWidth)) {
            float absX1 = Math.abs(0 - mLastRectF.left);
            float absX2 = Math.abs(mViewWidth - mLastRectF.right);
            deltaX = absX1 < absX2 ? 0 - mLastRectF.left : mViewWidth - mLastRectF.right;
        }
        if ((mLastRectF.top < 0 && mLastRectF.bottom < mViewHeight)
                || (mLastRectF.top > 0 && mLastRectF.bottom > mViewHeight)) {
            float absY1 = Math.abs(0 - mLastRectF.top);
            float absY2 = Math.abs(mViewHeight - mLastRectF.bottom);
            deltaY = absY1 < absY2 ? 0 - mLastRectF.top : mViewHeight - mLastRectF.bottom;
        }
        mDesRectF.set(mLastRectF.left + deltaX, mLastRectF.top + deltaY, mLastRectF.right + deltaX, mLastRectF.bottom
                + deltaY);

        if (mDesRectF.width() > getWindowRectF().width() || mDesRectF.height() > getWindowRectF().height()) {
            if (DEBUG) {
                Log.d("}}}", "[[checkPosition]] mDesRectF = " + mDesRectF);
            }
            if (mRegionBitmap != null && mRegionBitmap.btDraw != null && !mRegionBitmap.btDraw.isRecycled()) {
                mRegionBitmap.btDraw.recycle();
            }
            mRegionBitmap = null;
            RectF windowsRectF = getWindowRectF();

            if (RectF.intersects(mDesRectF, windowsRectF)) {
                mRegionBitmap = ImageRegionDecodeUtils.getRegionBitmap(this.mFullPath, mDesRectF,
                        intersect(mDesRectF, windowsRectF));
                if (DEBUG) {
                    Log.d(">>>> ", " mRegionBitmap = " + mRegionBitmap);
                }
            }
        }
        
        invalidate();
    }

    /**
     * 是否可以拖动
     * 
     * @return
     */
    private boolean isDragable() {

        if (mDesRectF.top < 0 || mDesRectF.left < 0 || mDesRectF.right > mViewWidth || mDesRectF.bottom > mViewHeight) {
            return true;
        }
        return false;
    }

    /**
     * 横向、纵向居中
     */
    private void center(boolean horizontal, boolean vertical) {

        mLastRectF.set(mDesRectF);

        float deltaX = 0, deltaY = 0;

        if (vertical) {
            deltaY = ((0 - mLastRectF.top) + (mViewHeight - mLastRectF.bottom)) / 2;
        }

        if (horizontal) {
            deltaX = ((0 - mLastRectF.left) + (mViewWidth - mLastRectF.right)) / 2;
        }

        mDesRectF.set(mLastRectF.left + deltaX, mLastRectF.top + deltaY, mLastRectF.right + deltaX, mLastRectF.bottom
                + deltaY);

        invalidate();

    }

    /**
     * 缩放至适应宽度
     */
    public void adapterWidth() {
        mDesRectF.set(0, 0, mBitmapWidth, mBitmapHeight);
        mLastRectF.set(mDesRectF);
        float dX = mViewWidth / mLastRectF.width();
        float dY = mViewHeight / mLastRectF.height();
        float dScale = 0f;
        dScale = dX < dY ? dX : dY;
        mDesRectF.set(mLastRectF.left * dScale, mLastRectF.top * dScale, mLastRectF.right * dScale, mLastRectF.bottom
                * dScale);

        invalidate();

        center(true, true);
    }

    /**
     * 缩放至适应高度
     */
    private void adapterHeight() {
        mDesRectF.set(0, 0, mBitmapWidth, mBitmapHeight);
        mLastRectF.set(mDesRectF);
        float dX = mViewWidth / mLastRectF.width();
        float dY = mViewHeight / mLastRectF.height();
        float dScale = 0f;
        dScale = dX < dY ? dY : dX;
        mDesRectF.set(mLastRectF.left * dScale, mLastRectF.top * dScale, mLastRectF.right * dScale, mLastRectF.bottom
                * dScale);

        invalidate();

        center(true, true);
    }

    /**
     * 初始位置
     * 
     * @return
     */
    public boolean isIdlePosition() {
        return mBitmap != null && mDesRectF.equals(mIdleRectF);
    }

    /**
     * 两点的距离
     */
    private float getSpace(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    /**
     * 两点的中点
     */
    private PointF getMidPointF(MotionEvent event) {
        PointF point = new PointF();
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
        return point;
    }

    private static final class RegionBitmap {
        RectF bitmapDrawRect;
        Bitmap btDraw;

        @Override
        public String toString() {
            return "RegionBitmap [bitmapDrawRect=" + bitmapDrawRect + ", btDraw=" + btDraw + "]";
        }
    }

    private static final class ImageRegionDecodeUtils {
        static RegionBitmap getRegionBitmap(String btFullPath, RectF btCurRect, RectF drawRect) {
            if (!TextUtils.isEmpty(btFullPath) && btCurRect != null && drawRect != null && btCurRect.width() > 0
                    && btCurRect.height() > 0 && drawRect.width() > 0 && drawRect.height() > 0) {
                if (btCurRect.width() > drawRect.width() || btCurRect.height() > drawRect.height()
                        || btCurRect.contains(drawRect)) {
                    try {
                        BitmapFactory.Options opts = ImageUtils.getBitmapHeaderInfo(btFullPath);
                        if (opts != null) {
                            int btWidth = opts.outWidth;
                            int btHeight = opts.outHeight;

                            float scaleX = (float) ((btWidth * 1.0) / btCurRect.width());
                            float scaleY = (float) ((btHeight * 1.0) / btCurRect.height());

                            Rect decodeRect = new Rect();
                            int decodeWidth = (int) (drawRect.width() * scaleX);
                            int decodeHeight = (int) (drawRect.height() * scaleY);
                            decodeRect.left = (int) (Math.abs((btCurRect.left - drawRect.left)) * scaleX);
                            decodeRect.top = (int) (Math.abs(btCurRect.top - drawRect.top) * scaleY);
                            decodeRect.right = decodeRect.left + decodeWidth;
                            decodeRect.bottom = decodeRect.top + decodeHeight;

                            RegionBitmap ret = new RegionBitmap();
                            ret.bitmapDrawRect = drawRect;
                            ret.btDraw = ImageUtils.loadBitmapRectDecode(new File(btFullPath), decodeRect);

                            Log.d("::::::::", "decodeRect = " + decodeRect.toString() + " btCurRect = " + btCurRect
                                    + " drawRect = " + drawRect + " bt size = (" + btWidth + ", " + btHeight + ")"
                                    + " scaleX = " + scaleX + " scaleY = " + scaleY + " ret.btDraw size = ("
                                    + (ret.btDraw != null ? ret.btDraw.getWidth() : 0) + ", "
                                    + (ret.btDraw != null ? ret.btDraw.getHeight() : 0) + ")");

                            return ret;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }
    }

}