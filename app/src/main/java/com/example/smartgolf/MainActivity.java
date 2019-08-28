package com.example.smartgolf;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.apache.poi.ss.formula.functions.T;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerSensor, linearAccelerSensor, gyroSensor;
    public float lAccX, lAccY, lAccZ;
    public float GyroX, GyroY, GyroZ;
    Button btn_swing, btn_excel, btn_reset, btn_tensor, btn_Force;
    TextView textView;

    private LineChart lineChart;
    LineDataSet lineDataSet;
    LineData lineData;

    public int count;
    private int time = 5000;

    float[][] Gyro = new float[time][3];
    float[][] GGap = new float[time][3];
    double[][] Pos = new double[time][3];
    double[][] Torque = new double[time][2];
    double[][] Force = new double[time][2];

    float[] SensorTime = new float[time];

    boolean isBtnOn = false;
    float FileSaveTime = System.currentTimeMillis() * 1000;

    int FileSaveTime_ver = 0;

    float dt, pitch, roll, yaw, radios;

    double totalRadios;
    double theta, pie;
    double Moment_Inertia;
    double AngAccX, AngAccY, AngAccZ;
    double theta_Acc, pie_Acc;

    File FilePath = new File(Environment.getExternalStorageDirectory() + "/Download");

    float[][][] inputGyro = new float[2][140][3];
    float[] outGyro = new float[]{};

    private static List<Float> x,y,z;

    private float[] results;
    public TensorFlowClassifier classifier;

    public Boolean P_S;
//    public DataFilter_Cal dataFilter_cal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_swing = (Button) findViewById(R.id.btn_swing);
        btn_excel = (Button) findViewById(R.id.btn_excel);
        btn_reset = (Button) findViewById(R.id.btn_reset);
        btn_tensor = (Button) findViewById(R.id.btn_tensor);
        lineChart = (LineChart) findViewById(R.id.chart);
        btn_Force = (Button) findViewById(R.id.btn_Force);
        textView = (TextView) findViewById(R.id.textView);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linearAccelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        classifier = new TensorFlowClassifier(getApplicationContext());
        count = 0;

        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();

        checkPermission();
        MakeChart(0, 0);

        btn_swing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBtnOn == true) {
                    isBtnOn = false;
                    onPause();
                } else if (isBtnOn == false) {
                    isBtnOn = true;
                    onResume();
                }
            }
        });

        btn_excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                __makeCsvOrTxtFile();
                clearGraph();
            }
        });

        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearGraph();
            }
        });

        btn_tensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogAnimation(P_S);
            }
        });

        btn_Force.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "힘 계산 완료 : " + Force[50][0], Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 마시멜로우 버전과 같거나 이상이라면
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "외부 저장소 사용을 위해 읽기/쓰기 필요", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
                //마지막 인자는 체크해야될 권한 갯수
            } else {
                Toast.makeText(this, "권한 승인되었음", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAccelerSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == gyroSensor && isBtnOn == true) {
            GyroX = event.values[0];
            GyroY = event.values[1];
            GyroZ = event.values[2];

            activityPrediction();
            x.add(event.values[0]);
            y.add(event.values[1]);
            z.add(event.values[2]);

            // nanoTime = 1/1000000
            SensorTime[count] = System.nanoTime();
            Gyro[count] = new float[]{GyroX, GyroY, GyroZ};

            if (count > 2) {
                dt = SensorTime[count - 1] - SensorTime[count - 2];
                //#### 잔류각도 고정 필터 준비
                for (int i = 0; i < 2; i++) {
                    float temp1 = Gyro[count - 1][i];
                    float temp2 = Gyro[count - 2][i];
                    GGap[count - 1][i] = temp1 - temp2;
                }
            }
            Filtering();
            Calculate_Position();
            if (count == 0) {
                Torque[count][0] = 0;
                Torque[count][1] = 0;
            } else {
                Calculate_Force();
            }

            count++;
            updateMarker(count, GyroX);
        }
    }

    //테스트
    private void activityPrediction() {
        if (x.size() == 150 && y.size() == 150 && z.size() == 150) {
            List<Float> data = new ArrayList<>();
            data.addAll(x);
            data.addAll(y);
            data.addAll(z);
            results = classifier.predictProbabilities(toFloatArray(data));

            textView.setText("  putting : "+results[0]+ "\n  swing : "+results[1]);

            if(results[0]>results[1]){P_S = false;}
            else if(results[0]<results[1]){P_S = true;}

            x.clear();
            y.clear();
            z.clear();
        }
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void Filtering() {
        //#### 잔류 각도 고정,
        //threshold 0.05 ; 의식적으로 가만히 있고자 노력할 때 각속도가 0으로 고정됨.
        //threshold 1 ; 잔류고정각속도가 0인 지점에서 0이 아닌 값이 발생하게 됨. 이는 적분시 치명적 오류야기
        if (count > 0) {
            if (Math.abs(GyroX - Gyro[count - 1][0]) < 0.05) {
                GyroX = Gyro[count - 1][0];
                Gyro[count][0] = GyroX;
            } else if (Math.abs(GyroX - Gyro[count - 1][1]) < 0.05) {
                GyroY = Gyro[count - 1][1];
                Gyro[count][1] = GyroY;
            } else if (Math.abs(GyroX - Gyro[count - 1][2]) < 0.05) {
                GyroZ = Gyro[count - 1][2];
                Gyro[count][2] = GyroZ;
            }
        }

        //#### angle 단순 계산
        pitch += (GyroX) * (dt * Math.pow(10, -9));
        roll += (GyroY) * (dt * Math.pow(10, -9));
        yaw += (GyroZ) * (dt * Math.pow(10, -9));
        double DT = dt * Math.pow(10, -9);

        //###### Complemantary Filtering
        //가속도계로부터 얻은 값을 Arctan 적용하여 2%비율로 반영(by 누군가)
        double forceMagnitude = Math.abs(lAccX) + Math.abs(lAccY) + Math.abs(lAccZ);
        if (forceMagnitude > 4.9 && forceMagnitude < 19.6) {
            float pitchAcc = (float) (Math.atan2(lAccZ, lAccY) * 180 / (Math.PI));
            pitch = (float) (pitch * 0.98 + pitchAcc * 0.02);

            float rollAcc = (float) (Math.atan2(lAccX, lAccZ) * 180 / (Math.PI));
            roll = (float) (pitch * 0.98 + rollAcc * 0.02);

            float yawAcc = (float) (Math.atan2(lAccY, lAccX) * 180 / (Math.PI));
            yaw = (float) (pitch * 0.98 + yawAcc * 0.02);
        }
    }

    public void Calculate_Position() {
        radios = 0.6f; //m 단위, 팔길이 + 골프채 길이(잡고 있을 때)

        //####### Roll 에 의한 Theta, pie 계산 모델링
        //골프 상황에서만 적용됨
        //roll이 45 dgree일때 theta, pie가 90도가 넘어가는 문제가 있다. 단순 덧셈으로 모델링 할 수 없다.
        theta = (yaw * Math.cos(roll) + pitch * Math.sin(roll));
        pie = (pitch * Math.cos(roll) + yaw * Math.sin(roll));

//        theta = pitch; pie = yaw;
        Pos[count][0] = radios * Math.sin(theta) * Math.sin(pie);
        Pos[count][1] = radios * Math.sin(theta) * Math.cos(pie);
        Pos[count][2] = radios * Math.cos(theta);
    }

    public void Calculate_Force() {
        //count가 1이상에서 작동
        //#### 단순 각가속도 계산
        AngAccX = (Gyro[count][0] - Gyro[count - 1][0]) / (dt * Math.pow(10, -9));
        AngAccZ = (Gyro[count][2] - Gyro[count - 1][2]) / (dt * Math.pow(10, -9));

        //roll 상황에 따른 pie와 theta 각가속도 변화
        theta_Acc = (AngAccZ * Math.cos(roll) + AngAccX * Math.sin(roll));
        pie_Acc = (AngAccX * Math.cos(roll) + AngAccZ * Math.sin(roll));

        //토크 계산
        Moment_Inertia = 0.185;  //m단위 체계, 계산에 의하여.
        Torque[count][0] = Moment_Inertia * pie_Acc;    //횡회전
        Torque[count][1] = Moment_Inertia * theta_Acc;

        //타격점 힘 계산
        totalRadios = (radios + 0.93);  //7번 club 기준, MKS 단위계 기준
        Force[count][0] = Torque[count][0] / totalRadios;    //횡회전
        Force[count][1] = Torque[count][1] / totalRadios;
    }

    public void clearGraph(){
        lineChart.invalidate();
        lineChart.clear();
        lineDataSet.clear();
        lineData.clearValues();
        pitch = 0;
        roll = 0;
        yaw = 0;
        AngAccX = 0;
        AngAccY = 0;
        AngAccZ = 0;
        count = 0;
        inputGyro = new float[][][]{};
        outGyro = new float[]{};
        textView.setText("");
        MakeChart(0, 0);
    }

    List<Entry> entries = new ArrayList<>();

    public void MakeChart(float xValue, float yValue) {
        entries.add(new Entry(xValue, yValue));

        lineDataSet = new LineDataSet(entries, "GyroX");
        lineDataSet.setLineWidth(1);
        lineDataSet.setCircleRadius(2);
        lineDataSet.setCircleColor(Color.BLACK);
        lineDataSet.setCircleColorHole(Color.BLACK);
        lineDataSet.setColor(Color.BLACK);
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawHorizontalHighlightIndicator(false);
        lineDataSet.setDrawHighlightIndicators(false);
        lineDataSet.setDrawValues(true);

        lineData = new LineData(lineDataSet);
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
        lineData.addEntry(new Entry(addXValue, addYValue), 0);
        lineData.notifyDataChanged();

        lineChart.notifyDataSetChanged();
        lineChart.moveViewToX(lineData.getEntryCount());

        MyMarkerView marker = new MyMarkerView(this, R.layout.markerview);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);
    }

    //csv또는 일반 text만들기 내부 메소드
    private boolean __makeCsvOrTxtFile(String[]... headers) {
        PrintWriter pw = null;
        StringBuilder sb = null;
        try {
            pw = new PrintWriter(new File(FilePath, "Golf_" + FileSaveTime_ver + ".csv"));

            String[] cellString = {"Time", "GyroX", "GyroY", "GyroZ", "PosX", "PosY", "PosZ", "ForPie", "ForThe"};

            StringBuffer csvHeader = new StringBuffer("");
            StringBuffer csvData = new StringBuffer("");
            for (int i = 0; i < cellString.length; i++) {
                csvHeader.append(cellString[i]);
                csvHeader.append(',');
            }
            csvHeader.append('\n');

            // write header
            pw.write(csvHeader.toString());

            // write data
            csvData.append("Started:");
            csvData.append('\n');

            for (int i = 0; i < count; i++) {
                for (int k = 0; k < 1; k++) {
                    if (count < 2) {
                        csvData.append("");
                        csvData.append(',');
                    } else {
                        csvData.append(dt);
                        csvData.append(',');
                    }
                }
                for (int j = 0; j < 3; j++) {
                    csvData.append(Gyro[i][j]);
                    csvData.append(',');
                }
                for (int j = 0; j < 3; j++) {
                    csvData.append(Pos[i][j]);
                    csvData.append(',');
                }
                if (count == 0) {
                    csvData.append('0');
                    csvData.append(',');
                    csvData.append('0');
                    csvData.append(',');
                } else {
                    csvData.append(Force[i][0]);
                    csvData.append(',');
                    csvData.append(Force[i][1]);
                    csvData.append(',');
                }
                csvData.append('\n');
            }
            pw.write(csvData.toString());
            FileSaveTime_ver++;
            pw.close();
        } catch (FileNotFoundException e) { e.printStackTrace();
        } finally {
            if (pw != null) { pw.close(); }
            if (sb != null) { sb = null; }
        }
        return true;
    }

    // 다이어로그
    public void DialogAnimation(Boolean P_S){
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.custom_dialog);

        ImageView image = (ImageView) dialog.findViewById(R.id.gif_image);
        TextView textView2 = (TextView) dialog.findViewById(R.id.textView2);

        GlideDrawableImageViewTarget gifImage = new GlideDrawableImageViewTarget(image);

        if(P_S == false) {
            Glide.with(this).load(R.drawable.putting).into(gifImage);
            textView2.setText("퍼팅입니다~");
        }
        else if(P_S == true) {
            Glide.with(this).load(R.drawable.swing).into(gifImage);
            textView2.setText("스윙입니다~");
        }
        dialog.show();
    }
}