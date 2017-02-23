package jaelyn.blgproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        final TextView orienTv = (TextView) findViewById(R.id.orien_tv);
        final TextView powerTv = (TextView) findViewById(R.id.power_tv);

        PlaneControlView powerCtl = (PlaneControlView) findViewById(R.id.power_rocker);
        PlaneControlView orienCtl = (PlaneControlView) findViewById(R.id.orien_rocker);

        powerCtl.setOnLocaListener(new PlaneControlView.OnLocaListener() {
            @Override
            public void getLocation(float x, float y) {
                DecimalFormat fnum = new DecimalFormat("##0.00");
                powerTv.setText("power1: x-" + fnum.format(x) + "  y-" + fnum.format(y));
            }
        });

        orienCtl.setOnLocaListener(new PlaneControlView.OnLocaListener() {
            @Override
            public void getLocation(float x, float y) {
                DecimalFormat fnum = new DecimalFormat("##0.00");
                orienTv.setText("orien1: x-" + fnum.format(x) + "  y-" + fnum.format(y));
            }
        });
    }
}
