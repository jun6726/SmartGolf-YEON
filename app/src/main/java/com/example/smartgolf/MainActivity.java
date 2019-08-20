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
import java.util.Locale;

import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

class MyVector{
    public float x,y,z;

    MyVector(){
        this.x = 0.f;
        this.y = 0.f;
        this.z = 0.f;
    }
    public MyVector(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    ArrayList<MyVector> MyVector;

    private SensorManager sensorManager;
    private Sensor accelerSensor;
    private Sensor linearAccelerSensor;
    public float lAccX, lAccY, lAccZ;
    TextView tvlXaxis, tvlYaxis, tvlZaxis, tvlTotal;
    Button button;

    private LineChart lineChart;

    int count;
    private int time = 5000;
    private float dt = 0.1f;   // 임시
    int stepnum = (int)(time / dt);
    float five_second;

    float[][] Acc = new float[stepnum][3];
    float[][] Vel = new float[stepnum][3];
    float[][] Loc = new float[stepnum][3];

    float start_time, end_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvlXaxis = (TextView) findViewById(R.id.tvlXaxis);
        tvlYaxis = (TextView) findViewById(R.id.tvlYaxis);
        tvlZaxis = (TextView) findViewById(R.id.tvlZaxis);
        button = (Button) findViewById(R.id.button);
        lineChart = (LineChart) findViewById(R.id.chart);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linearAccelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // 기능
        MakeChart(0, 0);

        count = 0;
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == motionEvent.ACTION_DOWN) {
                    start_time = System.currentTimeMillis()  / 1000;
                    end_time = start_time + 5000;
                    five_second = end_time - start_time;
                    Toast.makeText(MainActivity.this, "시간차"+five_second, Toast.LENGTH_SHORT).show();
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
        // 가속도 파트 , Acc
        if (event.sensor == linearAccelerSensor) {
            //exclude gravity
            lAccX = event.values[0];
            lAccY = event.values[1];
            lAccZ = event.values[2];

            tvlXaxis.setText("선형 X axis : " + String.format("%.2f", lAccX));
            tvlYaxis.setText("선형 Y axis : " + String.format("%.2f", lAccY));
            tvlZaxis.setText("선형 Z axis : " + String.format("%.2f", lAccZ));

            Acc[count] = new float[]{lAccX, lAccY, lAccZ};

            if(count>1) {
                Vel = Numeric_Integration(Acc);

            }
            if(count>2) {
                Loc = Numeric_Integration(Vel);
                updateMarker(count, Loc[count][1]);
            }
            count++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    List<Entry> entries = new ArrayList<>();

    public void MakeChart(float xValue, float yValue) {
        entries.add(new Entry(xValue, yValue));

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
        MyMarkerView marker = new MyMarkerView(this, R.layout.markerview);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);
    }

    private void updateMarker(float addXValue, float addYValue) {

        LineData lineData = lineChart.getData();
        lineData.addEntry(new Entry(addXValue,addYValue),0);
        lineData.notifyDataChanged();

        lineChart.notifyDataSetChanged();
        lineChart.moveViewToX(lineData.getEntryCount());

        MyMarkerView marker = new MyMarkerView(this, R.layout.markerview);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);
    }

    private float[][] Numeric_Integration(float[][] paraArray){
        float[][] Temp = new float[stepnum][3];

        // temp 초기화 첫행은 속도와 위치 모두 0
        for(int i = 0 ; i<3 ; i++){
            Temp[0][i] = 0;
        }

        for(int j = 1 ; j < stepnum ; j++) {
            for (int k = 0; k < 3; k++) {
                Temp[j][k] = Temp[j - 1][k] + ((paraArray[j][k] + paraArray[j - 1][k]) * dt / 2);
            }
        }
        return Temp;
    }
}