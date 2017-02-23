package jaelyn.blgproject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Jaelyn26 on 2015/12/5.
 */
public class SensorUtil implements SensorEventListener{

    private Context context;
    private SensorManager sm;
    private Sensor aSensor;
    private Sensor mSensor;
    private float valueX;
    private float valueY;
    private float[] aValues = new float[3];
    private float[] mValues = new float[3];
    private OnChangeListener onChangeListener;

    public SensorUtil(Context context, OnChangeListener onChangeListener){
        this.context = context;
        this.onChangeListener = onChangeListener;
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void registerListener(){
        sm.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void unRegisterListener(){
        sm.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            for (int i = 0; i < 3; i++) {
                aValues[i] = event.values[i];
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            for (int i = 0; i < 3; i++) {
                mValues[i] = event.values[i];
            }
        }
        calculateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void calculateOrientation(){
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, aValues, mValues);
        SensorManager.getOrientation(R, values);
        valueX = values[1]*0.1f + valueX*0.9f; // 缓冲
        valueY = values[2]*0.1f + valueY*0.9f;

        float x = (float) degressToCoor(valueX / (50 * Math.PI / 180));
        float y = (float) degressToCoor(valueY / (50 * Math.PI / 180));
        onChangeListener.setXY(x, y);
    }

    private double degressToCoor(double v){
        v=-v;
        if (v>0){
            v-=1;
            v=-v*v;
            v+=1;
        } else {
            v+=1;
            v=v*v;
            v-=1;
        }
        return v;
    }

    public interface OnChangeListener{
        public void setXY(float dx, float dy);
    }
}
