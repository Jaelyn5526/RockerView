package jaelyn.blgproject;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Created by Jaelyn26 on 2015/12/4.
 */
public class RockerView {

    public static final int MOVE_FOLLOW_FIGER = 0;
    public static final int MOVE_ONLY_X = 1;
    public static final int MOVE_ONLY_Y = 2;

   /* private int left;
    private int top;
    private int width;*/
    private Bitmap bitmap;
    private Rect rect =new Rect();
    private int backX = 0, backY = 0;

    private setOnBack onBack;
    private int moveMode = MOVE_FOLLOW_FIGER;
    private int defaultX, defaultY;


    public RockerView(){
    }

    public RockerView(Bitmap bitmap, int moveMode){
        this.bitmap = bitmap;
        this.moveMode = moveMode;
    }

    public void initView(int left, int top, int width){
        /*this.left = left;
        this.top = top;
        this.width = width;*/
        this.moveMode = moveMode;
        defaultX = left;
        defaultY = top;
        rect = new Rect(left, top, left+width, top+width);
    }

    public void setMoveMode(int moveMode){
        this.moveMode = moveMode;
    }

    public void setBackXY(setOnBack onBack){
        this.onBack = onBack;
    }
    void onDraw(Canvas canvas){
        canvas.drawBitmap(bitmap, null, rect, null);
    }

    public int getBackX(){
        if (onBack != null){
            backX = onBack.getX();
        }
        return backX;
    }

    public int getBackY(){
        if (onBack != null){
            backY = onBack.getY();
        }
        return backY;
    }

    /**
     * @param x
     * @param y
     * 供外接调用，设置坐标，移动到该点
     */
    void moveTo(int x, int y){
        int left = 0;
        int top = 0;
        int width = rect.width();
        if (moveMode == MOVE_FOLLOW_FIGER){
            left = x - width/2;
            top = y - width/2;
        }else if (moveMode == MOVE_ONLY_X){
            left = x - width/2;
            top = defaultY;
        }else if (moveMode == MOVE_ONLY_Y){
            left = defaultX;
            top = y - width/2;
        }
        rect.offsetTo(left, top);
    }

    boolean isTouch(float x, float y){
        if ((x >= rect.left && x <= rect.left+rect.width()) && y >= rect.top && y <= rect.top + rect.width()){
            return true;
        }
        return false;
    }

    public interface setOnBack{
        public int getX();
        public int getY();
    }

    public int getLeft() {
        return rect.left;
    }


    public int getTop() {
        return rect.top;
    }



    public int getWidth() {
        return rect.width();
    }

    public void setLeft(int left){
        rect.offsetTo(left, rect.top);
    }

    public void setTop(int top){
        rect.offsetTo(rect.left, top);
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getDefaultX() {
        return defaultX;
    }

    public void setDefaultX(int defaultX) {
        this.defaultX = defaultX;
    }

    public int getDefaultY() {
        return defaultY;
    }

    public void setDefaultY(int defaultY) {
        this.defaultY = defaultY;
    }
}
