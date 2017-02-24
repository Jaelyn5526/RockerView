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
 * Created by jaelyn on 16-06-29.
 * 摇杆view
 */
public class PlaneControlView extends View {
    //手放开后，球回归到y轴。
    public static final int BACK_MODE_LINE_Y = 1;
    //手放开后，球回归到中心点。
    public static final int BACK_MODE_CENTER = 0;

    //球的控制模式 0-手控  1-重力感应控制
    public static final int CONTROL_BY_TOUCH = 0;
    public static final int CONTROL_BY_ORIENTATION = 1;

    //onTouchEvent的状态
    private static final int TOUCH_DOWN = 0;
    private static final int TOUCH_MOVE = 1;
    private static final int TOUCH_UP = 2;
    private static final int TOUCH_CENTER = 3;

    //缩放系数：球离控件的边距
    private static final float PagerMarginsScale = 0.09f;

    private Drawable mDrawBg;
    private Drawable mDrawRocker;
    private int mPagerMargins = 0;

    private int mBgWight, mBgR;

    //球的宽、半径
    private int mRockerWidth, mRockerR;
    //球的坐标
    private int mRockerX, mRockerY;
    private Rect mRockerRect = new Rect();
    //rocker 默认位置 0-center， 1-bottom
    private int mRockerGravide = 0;
    private int mRockeBackMode = BACK_MODE_LINE_Y;

    //控制模式
    private int mTouchMode = TOUCH_CENTER;

    private Paint paint = new Paint();
    //球坐标的回掉监听
    private OnLocaListener onLocaListener;
    //微调后的中心值;
    private byte trimX = (byte) 128, trimY = (byte) 128;
    //传感器工具
    private SensorUtil sensorUtil;

    private int mControlMode = CONTROL_BY_TOUCH;

    private float mRockerXPer, mRockerYPer;

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
        //根据模式初始化小球位置
        if (mRockerGravide == 0) {
            //居中
            mRockerXPer = 0;
            mRockerYPer = 0;
        } else if (mRockerGravide == 1) {
            //底部
            mRockerXPer = 0;
            mRockerYPer = -1;
        }
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
        mBgR = parentWidth / 2 - mPagerMargins - mRockerR;
        mRockerWidth = mDrawRocker.getIntrinsicWidth();
        mRockerR = mRockerWidth / 2;
        setMeasuredDimension(parentWidth, parentHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制背景
        mDrawBg.setBounds(0, 0, mBgWight, mBgWight);
        mDrawBg.draw(canvas);
        //计算小球的位置
        mDrawRocker.setBounds(getRockerRect());
        //绘制小球
        mDrawRocker.draw(canvas);
    }

    /**
     * 以摇杆当前的坐标为中心点，计算Rocker绘画的范围
     *
     * @return
     */
    private Rect getRockerRect() {
        int rockerX = (int) (mBgR * mRockerXPer + mBgWight / 2);
        int rockerY = (int) (-mBgR * mRockerYPer + mBgWight / 2);
        mRockerRect.set(rockerX - mRockerR, rockerY - mRockerR, rockerX + mRockerR,
                rockerY + mRockerR);
        return mRockerRect;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mControlMode == CONTROL_BY_ORIENTATION) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (isTouchRocker((int) event.getX(), (int) event.getY())) {
                mTouchMode = TOUCH_DOWN;
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
            if (mTouchMode == TOUCH_MOVE) {
                moveBack();
                mTouchMode = TOUCH_UP;
            }
            break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * x,y是否在小球的范围。
     * 判断是否触碰到小球
     * @param x
     * @param y
     * @return
     */
    public boolean isTouchRocker(int x, int y) {
        return mRockerRect.contains(x, y);
    }

    /**
     * 移动小球到 x,y
     * @param x
     * @param y
     */
    private void moveRocker(float x, float y) {
        if (!isRange(x, y)) {
            setCircleXY(x, y);
        } else {
            mRockerX = (int) x;
            mRockerY = (int) y;
        }
        setLinstenerData();
        postInvalidate();
    }

    /**
     * 判断小球是否在范围
     *
     * @param x
     * @param y
     * @return
     */
    private boolean isRange(float x, float y) {
        float dx = mBgWight / 2 - x;
        float dy = mBgWight / 2 - y;
        return mBgR >= Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    }

    private void setCircleXY(float x, float y) {
        float dx = mBgWight / 2 - x;
        float dy = mBgWight / 2 - y;
        double touchR = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        double cosAngle = Math.acos(dx / touchR);
        if (dy > 0) {
            cosAngle = -cosAngle;
        }
        mRockerX = (int) (mBgR * Math.cos(Math.PI - cosAngle)) + mBgWight / 2;
        mRockerY = (int) (mBgR * Math.sin(cosAngle)) + mBgWight / 2;
    }

    /**
     * 回调小球位置
     */
    private void setLinstenerData() {
        if (mBgR == 0) {
            return;
        }
        mRockerXPer = (mRockerX - mBgWight / 2f) / (float) mBgR;
        mRockerYPer = (mBgWight / 2f - mRockerY) / (float) mBgR;
        if (onLocaListener != null) {
            onLocaListener.getLocation(mRockerXPer, mRockerYPer);
        }
    }

    public interface OnLocaListener {
        /**
         * @param x 方向偏移百分比
         * @param y 方向偏移百分比
         */
        void getLocation(float x, float y);
    }

    public void setOnLocaListener(OnLocaListener onLocaListener) {
        this.onLocaListener = onLocaListener;
    }

    /**
     * 动画类
     * 小球回归时使用的动画
     */
    private class AnimToBack implements Runnable {

        private long startTime;
        private float duration;
        private int startX, startY;
        private int dx, dy;

        public void start(int startX, int startY, int endX, int endY, int duration) {
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
            if (progress >= 1) {
                progress = 1;
            } else {
                post(this);
            }
            moveRocker(dx * progress + startX, dy * progress + startY);
        }
    }

    private void setBackMode(int baceMode, boolean moveToBack) {
        this.mRockeBackMode = baceMode;
        if (moveToBack) {
            moveBack();
        }
    }

    /**
     * 将摇杆移动到回归点
     */
    public void moveBack() {
        int backX = mBgWight / 2;
        int backY = mBgWight / 2;
        if (mRockeBackMode == BACK_MODE_CENTER) {
            backY = backX = mBgWight / 2;
        } else if (mRockeBackMode == BACK_MODE_LINE_Y) {
            backX = mBgWight / 2;
            backY = mBgWight;
        }
        new AnimToBack().start(mRockerX, mRockerY, backX, backY, 150);
    }

    public void moveToPoint(float percentX, float percentY) {
        float x = percentX * mBgR + mBgWight / 2;
        float y = percentY * mBgR + mBgWight / 2;
        new AnimToBack().start((int) mRockerX, (int) mRockerY, (int) x, (int) y, 10);
    }

    public void moveToPointDura(float percentX, float percentY, int duration) {
        float x = percentX * mBgR + mBgWight / 2;
        float y = percentY * mBgR + mBgWight / 2;
        new AnimToBack().start((int) mRockerX, (int) mRockerY, (int) x, (int) y, 10);
        Handler handler = new Handler();
        handler.removeCallbacks(moveBackRunnable);
        handler.postDelayed(moveBackRunnable, duration);
    }

    /**
     * 设置控制模式
     * @param type PlaneControlView.CONTROL_BY_ORIENTATION PlaneControlView.CONTROL_BY_TOUCH
     */
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
                    Log.d("sensor", dx + "--" + dy);
                    float x = dx * mBgR + (float) mBgWight / 2;
                    float y = dy * mBgR + (float) mBgWight / 2;
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

    public float getmRockerXPer() {
        return mRockerXPer;
    }

    public float getmRockerYPer() {
        return mRockerYPer;
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
