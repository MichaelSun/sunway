/**
 * TouchImageView.java 不支持在布局文件中使用
 */
package com.subway.ditu.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

/**
 * 支持缩放，拖动，还原的ImageView
 * 
 * @author Di Zhang
 */
public class TouchImageView extends View {

    public static final int EXIT = 0;

    private RectF mIdleRectF;
    private RectF mLastRectF;
    private RectF mDesRectF;

    private Bitmap mBitmap;

    private static final float MIN_SCALER = 1.0f; // 最小缩放比例
    private static final float MAX_SCALER = 8.0f; // 最大缩放比例

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
            if (mTouchMode == MODE_ZOOM)
                checkZoomSize();
            mTouchMode = MODE_NONE;
            break;
        case MotionEvent.ACTION_MOVE:
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
        if (mBitmap != null && !mBitmap.isRecycled()) {
            Rect src = new Rect(0, 0, mBitmapWidth, mBitmapHeight);
            canvas.drawBitmap(mBitmap, src, mDesRectF, paint);
        }
        canvas.restore();
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
        }

        invalidate();
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

}