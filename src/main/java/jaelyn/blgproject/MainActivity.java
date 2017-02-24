package jaelyn.blgproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private PlaneControlView powerCtl;
    private PlaneControlView orienCtl;
    private boolean isSensorMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        final TextView orienTv = (TextView) findViewById(R.id.orien_tv);
        final TextView powerTv = (TextView) findViewById(R.id.power_tv);

        powerCtl = (PlaneControlView) findViewById(R.id.power_rocker);
        orienCtl = (PlaneControlView) findViewById(R.id.orien_rocker);

        powerCtl.setOnLocaListener(new PlaneControlView.OnLocaListener() {
            @Override
            public void getLocation(float x, float y) {
                DecimalFormat fnum = new DecimalFormat("##0.00");
                powerTv.setText("power1: x,y(" + fnum.format(x) +","+ fnum.format(y)+")");
            }
        });

        orienCtl.setOnLocaListener(new PlaneControlView.OnLocaListener() {
            @Override
            public void getLocation(float x, float y) {
                DecimalFormat fnum = new DecimalFormat("##0.00");
                orienTv.setText("orien1: x,y(" + fnum.format(x) +","+ fnum.format(y)+")");
            }
        });

        findViewById(R.id.sensor_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isSensorMode) {
                    orienCtl.setControlMode(PlaneControlView.CONTROL_BY_ORIENTATION);
                    isSensorMode = true;
                } else {
                    orienCtl.moveBack();
                    orienCtl.setControlMode(PlaneControlView.CONTROL_BY_TOUCH);
                    isSensorMode = false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //注册传感器
        orienCtl.registerListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //取消注册传感器
        orienCtl.unregisterListener();
    }
}
