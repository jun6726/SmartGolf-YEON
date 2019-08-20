package com.example.smartgolf;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerSensor;
    private Sensor linearAccelerSensor;
    private float gAccX, gAccY, gAccZ, accX, accY, accZ, lAccX, lAccY, lAccZ;
    private float tempX, tempY, tempZ;
    private double gTotal, total, lTotal;
    private float alpha = 0.8f;
    TextView tvgXaxis, tvgYaxis, tvgZaxis, tvXaxis, tvYaxis, tvZaxis, tvlXaxis, tvlYaxis, tvlZaxis, tvgTotal, tvTotal, tvlTotal;
    Button button;

    private LineChart lineChart;

    int count;
    private int time = 5;
    private float dt = 0.5f;   // 임시
    int stepnum = (int)(time / dt);
//    float[][] Time, Acc, Vel, Loc;
    float[][] Acc;
    float[][] Vel = new float[stepnum][3];
    float[][] Dis = new float[stepnum][3];

    float start_time, end_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        tvgXaxis = (TextView) findViewById(R.id.tvgXaxis);
//        tvgYaxis = (TextView) findViewById(R.id.tvgYaxis);
//        tvgZaxis = (TextView) findViewById(R.id.tvgZaxis);
//        tvXaxis = (TextView) findViewById(R.id.tvXaxis);
//        tvYaxis = (TextView) findViewById(R.id.tvYaxis);
//        tvZaxis = (TextView) findViewById(R.id.tvZaxis);
        tvlXaxis = (TextView) findViewById(R.id.tvlXaxis);
        tvlYaxis = (TextView) findViewById(R.id.tvlYaxis);
        tvlZaxis = (TextView) findViewById(R.id.tvlZaxis);
//        tvgTotal = (TextView) findViewById(R.id.tvgTotal);
//        tvTotal = (TextView) findViewById(R.id.tvTotal);
        tvlTotal = (TextView) findViewById(R.id.tvlTotal);
        button = (Button) findViewById(R.id.button);
        lineChart = (LineChart) findViewById(R.id.chart);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linearAccelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // 기능
        MakeChart(1, 1);

        count = 0;
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == motionEvent.ACTION_DOWN) {
//                    start_time = System.currentTimeMillis();
////                    end_time = start_time + 5000;
                }
                if (motionEvent.getAction() == motionEvent.ACTION_UP) {
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAccelerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerSensor) {
            //include gravity
            gAccX = event.values[0];
            gAccY = event.values[1];
            gAccZ = event.values[2];

//            tempX = alpha * tempX + (1 - alpha) * event.values[0];
//            tempY = alpha * tempY + (1 - alpha) * event.values[1];
//            tempZ = alpha * tempZ + (1 - alpha) * event.values[2];
//
//
//            accX = event.values[0] - tempX;
//            accY = event.values[1] - tempY;
//            accZ = event.values[2] - tempZ;
//
//            gTotal = Math.sqrt(Math.pow(gAccX, 2) + Math.pow(gAccY, 2) + Math.pow(gAccZ, 2));
//            total = Math.sqrt(Math.pow(accX, 2) + Math.pow(accY, 2) + Math.pow(accZ, 2));
//
//            tvgXaxis.setText("중력 가속도 X axis : " + String.format("%.2f", gAccX));
//            tvgYaxis.setText("중력 가속도 Y axis : " + String.format("%.2f", gAccY));
//            tvgZaxis.setText("중력 가속도 Z axis : " + String.format("%.2f", gAccZ));
//            tvgTotal.setText("Total Gravity : " + String.format("%.2f", gTotal) + " m/s\u00B2");
//
//            tvXaxis.setText("X axis : " + String.format("%.2f", accX));
//            tvYaxis.setText("Y axis : " + String.format("%.2f", accY));
//            tvZaxis.setText("Z axis : " + String.format("%.2f", accZ));
//            tvTotal.setText("Total Gravity : " + String.format("%.2f", total) + " m/s\u00B2");
        }

        // 가속도 파트 , Acc
        if (event.sensor == linearAccelerSensor) {
            //exclude gravity
            lAccX = event.values[0];
            lAccY = event.values[1];
            lAccZ = event.values[2];

            lTotal = Math.sqrt(Math.pow(lAccX, 2) + Math.pow(lAccY, 2) + Math.pow(lAccZ, 2));

            tvlXaxis.setText("선형 X axis : " + String.format("%.2f", lAccX));
            tvlYaxis.setText("선형 Y axis : " + String.format("%.2f", lAccY));
            tvlZaxis.setText("선형 Z axis : " + String.format("%.2f", lAccZ));
            tvlTotal.setText("Total Gravity : " + String.format("%.2f", lTotal) + " m/s\u00B2");

            Acc = new float[5000][3];
            Acc[count][0] = lAccX;
            Acc[count][1] = lAccY;
            Acc[count][2] = lAccZ;

            updateMarker(count, lAccX);
            count++;
//            Log.d("초당 센서 갯수 확인용", ""+lAccX);
            Log.d("Acc배열 확인","count"+count+"  Acc[count]"+Acc[count]+"  Acc[count][0]"+Acc[count][0]+ "   lAccx"+lAccX);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    List<Entry> entries = new ArrayList<>();

    public void MakeChart(float xValue, float yValue) {
        entries.add(new Entry(xValue, yValue));
        entries.add(new Entry(xValue + 1, yValue + 1));
        entries.add(new Entry(xValue + 2, yValue + 2));

        LineDataSet lineDataSet;
        lineDataSet = new LineDataSet(entries, "속성명1");
        lineDataSet.setLineWidth(2);
        lineDataSet.setCircleRadius(6);
        lineDataSet.setCircleColor(Color.parseColor("#FFA1B4DC"));
        lineDataSet.setCircleColorHole(Color.BLUE);
        lineDataSet.setColor(Color.parseColor("#FFA1B4DC"));
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawHorizontalHighlightIndicator(false);
        lineDataSet.setDrawHighlightIndicators(false);
        lineDataSet.setDrawValues(true);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.enableGridDashedLine(8, 24, 0);

        YAxis yLAxis = lineChart.getAxisLeft();
        yLAxis.setTextColor(Color.BLACK);

        YAxis yRAxis = lineChart.getAxisRight();
        yRAxis.setDrawLabels(false);
        yRAxis.setDrawAxisLine(false);
        yRAxis.setDrawGridLines(false);

        Description description = new Description();
        description.setText("");

        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setDescription(description);
//        lineChart.animateY(2000, Easing.EasingOption.EaseInCubic);
        MyMarkerView marker = new MyMarkerView(this, R.layout.markerview);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);
    }

    private void updateMarker(float addXValue, float addYValue) {

        LineData lineData = lineChart.getData();
        lineData.addEntry(new Entry(addXValue,addYValue),0);
        lineData.notifyDataChanged();

        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(120);
        lineChart.moveViewToX(lineData.getEntryCount());

        MyMarkerView marker = new MyMarkerView(this, R.layout.markerview);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);
    }

    // 가속도 -> 속도 -> 위치
    // v1 = (a0 + a1)(t1-t0) / 2
    private float Numerical_Integration(float val0, float val1, float time0, float time1) {
            float transform_V =  ((val0 + val1)*(time1 - time0) / 2);
        return transform_V;
    }
}