package jaelyn.blgproject;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by zaric on 16-06-29.
 */
public class PlaneControlView extends View {
    public static final int BACK_MODE_LINE_Y = 1;
    public static final int BACK_MODE_CENTER = 0;

    public static final int CONTROL_BY_TOUCH = 0;
    public static final int CONTROL_BY_ORIENTATION = 1;

    private static final int TOUCH_DOWN = 0;
    private static final int TOUCH_MOVE = 1;
    private static final int TOUCH_UP = 2;
    private static final int TOUCH_CENTER = 3;

    private static final float PagerMarginsScale = 0.09f;

    private Drawable mDrawBg;
    private Drawable mDrawRocker;
    private int mPagerMargins = 0;

    private int mBgWight, mBgR;
    private int mRockerWidth, mRockerR;
    private int mRockerX, mRockerY;
    private Rect mRockerRect = new Rect();
    //rocker 默认位置 0-center， 1-bottom
    private int mRockerGravide = 0;
    private int mRockeBackMode = BACK_MODE_LINE_Y;

    private int mTouchMode = TOUCH_CENTER;
    private float mTouchX, mTouchY, mTouchBeforeX, mTouchBeforeY;

    private Paint paint = new Paint();

    private OnLocaListener onLocaListener;

    private byte trimX = (byte) 128, trimY = (byte) 128; //微调后的中心值;
    private SensorUtil sensorUtil;

    private int mControlMode = CONTROL_BY_TOUCH;

    private float percenX, percenY;

    private Runnable moveBackRunnable = new Runnable() {
        @Override
        public void run() {
            moveBack();
        }
    };

    public PlaneControlView(Context context) {
        super(context);
        init(null, 0);
    }

    public PlaneControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PlaneControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    public void init(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PlaneControlView,
                defStyleAttr, 0);
        mDrawBg = a.getDrawable(R.styleable.PlaneControlView_control_bg);
        mDrawRocker = a.getDrawable(R.styleable.PlaneControlView_control_rocker);
        mRockerGravide = a.getInteger(R.styleable.PlaneControlView_rocker_gravide, 0);
        mRockeBackMode = a.getInteger(R.styleable.PlaneControlView_rocker_back_mode, 0);
        a.recycle();
        paint.setColor(0xaaff0000);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        parentHeight = parentWidth = Math.min(parentWidth, parentHeight);
        mBgWight = parentWidth;
        mPagerMargins = (int) (parentWidth * PagerMarginsScale);
        mBgR = (int) (parentWidth/2 - mPagerMargins - mRockerR);
        mRockerWidth = mDrawRocker.getIntrinsicWidth();
        mRockerR = mRockerWidth / 2;
        Log.d("onMeasure-", "onMeasure");
        if (mRockerGravide == 0) {
            mRockerX = mBgWight / 2;
            mRockerY = mBgWight / 2;
        } else if (mRockerGravide == 1) {
            mRockerX = mBgWight / 2;
            mRockerY = mBgWight - mPagerMargins - mRockerR;
        }
        setMeasuredDimension(parentWidth, parentHeight);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mDrawBg.setBounds(0, 0, mBgWight, mBgWight);
        mDrawBg.draw(canvas);
        mDrawRocker.setBounds(getRockerRect());
        mDrawRocker.draw(canvas);
        //canvas.drawCircle(mBgWight / 2, mBgWight / 2, mBgR, paint);
    }

    /**
     * 以摇杆当前的坐标为中心点，计算Rocker绘画的范围
     *
     * @return
     */
    private Rect getRockerRect() {
        mRockerRect.set(mRockerX - mRockerR, mRockerY - mRockerR, mRockerX + mRockerR,
                mRockerY + mRockerR);
        Log.d("mRoce-", mRockerRect.toShortString()+"  mRockerX="+mRockerX +"  mRockerR="+mRockerR);
        return mRockerRect;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mControlMode == CONTROL_BY_ORIENTATION){
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (isTouchRocker((int) event.getX(), (int) event.getY())) {
                mTouchMode = TOUCH_DOWN;
                mTouchBeforeX = mTouchX = event.getX();
                mTouchBeforeY = mTouchY = event.getY();
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (mTouchMode == TOUCH_DOWN || mTouchMode == TOUCH_MOVE) {
                moveRocker(event.getX(), event.getY());
                mTouchMode = TOUCH_MOVE;
                return true;
            }
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            if (mTouchMode == TOUCH_MOVE){
                moveBack();
                mTouchMode = TOUCH_UP;
            }
            break;
        }
        return super.onTouchEvent(event);
    }

    public boolean isTouchRocker(int x, int y) {
        return mRockerRect.contains(x, y);
    }

    private void moveRocker(float x, float y) {
        if (!isRange(x, y)) {
            setCircleXY(x, y);
        } else {
            mRockerX = (int)x;
            mRockerY = (int)y;
        }
        setLinstenerData();
        postInvalidate();
    }

    /**
     * 判断是否在范围
     *
     * @param x
     * @param y
     * @return
     */
    public boolean isRange(float x, float y) {
        float dx = mBgWight / 2 - x;
        float dy = mBgWight / 2 - y;
        return mBgR >= Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    }

    public void setCircleXY(float x, float y){
        float dx = mBgWight / 2 - x;
        float dy = mBgWight / 2 - y;
        double touchR = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        double cosAngle = Math.acos(dx / touchR);
        if (dy > 0){
            cosAngle = -cosAngle;
        }
        mRockerX = (int) (mBgR * Math.cos(Math.PI-cosAngle)) + mBgWight /2;
        mRockerY = (int) (mBgR * Math.sin(cosAngle)) + mBgWight /2;
    }

    public void setLinstenerData(){
        if (mBgR == 0){
            return;
        }
        percenX = (mRockerX - mBgWight / 2f) / (float)mBgR;
        percenY = (mBgWight / 2f - mRockerY) / (float)mBgR;
        Log.d("mRocker--XY", percenX + "---   "+percenY+"---"+ mRockerX + " --- "+ mRockerY +"---"+mBgWight / 2f +"----"+mBgR );
        if (onLocaListener != null){
            onLocaListener.getLocation(percenX, percenY);
        }
    }

    public interface OnLocaListener{
        /**
         * @param x 方向偏移百分比
         * @param y 方向偏移百分比
         */
        void getLocation(float x, float y);
    }

    public void setOnLocaListener(OnLocaListener onLocaListener){
        this.onLocaListener = onLocaListener;
    }

    private class AnimToBack implements Runnable{

        private long startTime;
        private float duration;
        private int startX, startY;
        private int dx, dy;
        public void start(int startX, int startY, int endX, int endY, int duration){
            this.duration = duration;
            dx = endX - startX;
            dy = endY - startY;
            this.startX = startX;
            this.startY = startY;
            startTime = SystemClock.uptimeMillis();
            post(this);
        }

        @Override
        public void run() {
            float progress = (SystemClock.uptimeMillis() - startTime) / duration;
            if (progress >= 1){
                progress = 1;
            }else {
                post(this);
            }
            Log.d("progress--", progress+"");
            moveRocker(dx * progress + startX, dy * progress + startY);
        }
    }

    public void setBackMode(int baceMode, boolean moveToBack){
        this.mRockeBackMode = baceMode;
        if (moveToBack){
            moveBack();
        }
    }

    /**
     * 将摇杆移动到回归点
     */
    public void moveBack(){
        int backX = mBgWight / 2;
        int backY = mBgWight / 2;
        if (mRockeBackMode == BACK_MODE_CENTER){
            backY = backX = mBgWight / 2;
        }else if (mRockeBackMode == BACK_MODE_LINE_Y){
            backX = mBgWight / 2;
            backY = mBgWight;
        }
        new AnimToBack().start(mRockerX, mRockerY, backX, backY, 150);
    }

    public void moveToPoint(float percentX, float percentY){
        float x = percentX * mBgR + mBgWight / 2;
        float y = percentY * mBgR + mBgWight / 2;
        new AnimToBack().start((int) mRockerX, (int) mRockerY,  (int)x,  (int)y, 10);
    }


    public void moveToPointDura(float percentX, float percentY, int duration){
        float x = percentX * mBgR + mBgWight / 2;
        float y = percentY * mBgR + mBgWight / 2;
        new AnimToBack().start((int) mRockerX, (int) mRockerY,  (int)x,  (int)y, 10);
        Handler handler = new Handler();
        handler.removeCallbacks(moveBackRunnable);
        handler.postDelayed(moveBackRunnable, duration);
    }

    public void setControlMode(int type) {
        final double stickyCenter = 0.09;
        if (type == CONTROL_BY_TOUCH) {
            mControlMode = CONTROL_BY_TOUCH;
            if (sensorUtil != null) {
                sensorUtil.unRegisterListener();
                sensorUtil = null;
            }
        } else if (type == CONTROL_BY_ORIENTATION) {
            mControlMode = CONTROL_BY_ORIENTATION;
            sensorUtil = new SensorUtil(getContext(), new SensorUtil.OnChangeListener() {
                @Override
                public void setXY(float dx, float dy) {
                    Log.d("sensor", dx +"--"+dy);
                    float x = dx * mBgR +  (float)mBgWight / 2;
                    float y = dy * mBgR + (float)mBgWight / 2;
                    moveRocker(x, y);
                }
            });
            sensorUtil.registerListener();
        }

    }

    public void registerListener() {
        if (sensorUtil != null) {
            sensorUtil.registerListener();
        }
    }

    public void unregisterListener() {
        if (sensorUtil != null) {
            sensorUtil.unRegisterListener();
        }
    }

    public float getPercenX() {
        return percenX;
    }

    public float getPercenY() {
        return percenY;
    }

    public byte getTrimX() {
        return trimX;
    }

    public void setTrimX(byte trimX) {
        this.trimX = trimX;
    }

    public byte getTrimY() {
        return trimY;
    }

    public void setTrimY(byte trimY) {
        this.trimY = trimY;
    }
}
