package com.example.smartgolf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerSensor;
    private Sensor linearAccelerSensor;
    private Sensor gyroSensor;
    public float lAccX, lAccY, lAccZ;
    public float GyroX, GyroY, GyroZ;
    TextView tvlXaxis, tvlYaxis, tvlZaxis, tvlTotal;
    Button button, btn_capture, btn_excel, btn_reset, btn_tensor;

    private LineChart lineChart;
    LineDataSet lineDataSet;
    LineData lineData;

    int count;
    private int time = 5000;

    float[][] lAcc = new float[time][3];
    float[][] Gyro = new float[time][3];
    float[][] GGap = new float[time][3];
    double[][] Pos = new double[time][3];
    double[][] Torque = new double[time][2];
    float[] SensorTime = new float[time];

    boolean isBtnOn = false;
    float FileSaveTime = System.currentTimeMillis() * 1000;

    float dt;

    int FileSaveTime_ver = 0;

    float pitch;
    float roll;
    float yaw;

    float radios;
    double theta;
    double pie;

    double Moment_Inertia;

    double AngAccX;
    double AngAccY;
    double AngAccZ;

    double theta_Acc;
    double pie_Acc;

    File FilePath = new File(Environment.getExternalStorageDirectory() + "/Download");

    float[][][] inputGyro = new float[2][140][3];
    float[] outGyro = new float[]{};

    //테스트
    private static final int N_SAMPLES = 200;
    private static List<Float> x;
    private static List<Float> y;
    private static List<Float> z;

    private float[] results;
    public TensorFlowClassifier classifier;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        classifier = new TensorFlowClassifier(getApplicationContext());
        //테스트
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();

        tvlXaxis = (TextView) findViewById(R.id.tvlXaxis);
        tvlYaxis = (TextView) findViewById(R.id.tvlYaxis);
        tvlZaxis = (TextView) findViewById(R.id.tvlZaxis);
        button = (Button) findViewById(R.id.button);
        btn_capture = (Button) findViewById(R.id.btn_capture);
        btn_excel = (Button) findViewById(R.id.btn_excel);
        btn_reset = (Button) findViewById(R.id.btn_reset);
        btn_tensor = (Button) findViewById(R.id.btn_tensor);
        lineChart = (LineChart) findViewById(R.id.chart);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linearAccelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        checkPermission();
        MakeChart(0, 0);

        count = 0;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBtnOn == true) {
                    isBtnOn = false;
                    onPause();
                    pitch = 0;
                    roll = 0;
                    yaw = 0;
                    AngAccX = 0;
                    AngAccY = 0;
                    AngAccZ = 0;
                } else if (isBtnOn == false) {
                    isBtnOn = true;
                    onResume();
                }
            }
        });

        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View rootView = getWindow().getDecorView();

                File screenShot = ScreenShot(rootView);
                if (screenShot != null) {
                    //갤러리에 추가
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(screenShot)));
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
//                TensorFlowLite();

            }
        });
    }

    public void clearGraph(){
        lineChart.invalidate();
        lineChart.clear();
        lineDataSet.clear();
        lineData.clearValues();
        count = 0;
        inputGyro = new float[][][]{};
        outGyro = new float[]{};
        MakeChart(0, 0);
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
//        // 가속도 파트 , Acc
//        if (event.sensor == linearAccelerSensor && isBtnOn == true) {
//            //exclude gravity
//            lAccX = event.values[0];
//            lAccY = event.values[1];
//            lAccZ = event.values[2];
//
//            tvlXaxis.setText("선형 X axis : " + String.format("%f", lAccX));
//            tvlYaxis.setText("선형 Y axis : " + String.format("%f", lAccY));
//            tvlZaxis.setText("선형 Z axis : " + String.format("%f", lAccZ));
//
//            lAcc[count] = new float[]{lAccX, lAccY, lAccZ};
//
//            count++;
//            updateMarker(count, lAccX);
//        }

        if (event.sensor == gyroSensor && isBtnOn == true) {
            GyroX = event.values[0];
            GyroY = event.values[1];
            GyroZ = event.values[2];

            //테스트
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

            for(int i=0; i<140; i++){
                for(int j=0; j<3; j++){
//                    inputGyro[0][i][j] = Gyro[i][j];
                }
            }
            count++;
            updateMarker(count, GyroX);
        }
    }

    //테스트
    private void activityPrediction() {
        if (x.size() == 140 && y.size() == 140 && z.size() == 140) {
            List<Float> data = new ArrayList<>();
            data.addAll(x);
            data.addAll(y);
            data.addAll(z);

            Log.d("be_predictProbabilities","predictProbabilities_ready");
            results = classifier.predictProbabilities(toFloatArray(data));
            Log.d("Petting", ""+ results[0]);
            Log.d("Swing", ""+results[1]);
            Log.d("af_predictProbabilities","predictProbabilities_complete");

            x.clear();
            y.clear();
            z.clear();
        }
    }

    //테스트
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
//        theta = pitch;
//        pie = yaw;
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
        Moment_Inertia = 1000;  //m단위 체계, 계산에 의하여.
        Torque[count][0] = Moment_Inertia * pie_Acc;    //횡회전
        Torque[count][1] = Moment_Inertia * theta_Acc;
    }

    List<Entry> entries = new ArrayList<>();

    public void MakeChart(float xValue, float yValue) {
        entries.add(new Entry(xValue, yValue));

        lineDataSet = new LineDataSet(entries, "속성명1");
        lineDataSet.setLineWidth(2);
        lineDataSet.setCircleRadius(6);
        lineDataSet.setCircleColor(Color.BLUE);
        lineDataSet.setCircleColorHole(Color.BLUE);
        lineDataSet.setColor(Color.BLUE);
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

    //csv또는 일반 text만들기 내부 메소드
    private boolean __makeCsvOrTxtFile(String[]... headers) {
        PrintWriter pw = null;
        StringBuilder sb = null;
        try {
            pw = new PrintWriter(new File(FilePath, "Golf_" + FileSaveTime_ver + ".csv"));

            String[] cellString = {"Time", "GyroX", "GyroY", "GyroZ", "PosX", "PosY", "PosZ", "TorPie", "TorThe"};

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
                    csvData.append(Torque[i][0]);
                    csvData.append(',');
                    csvData.append(Torque[i][1]);
                    csvData.append(',');
                }
                csvData.append('\n');
            }
            pw.write(csvData.toString());
            FileSaveTime_ver++;

            pw.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (sb != null) {
                sb = null;
            }
        }
        return true;
    }

    //화면 캡쳐하기
    public File ScreenShot(View view) {
        view.setDrawingCacheEnabled(true);  //화면에 뿌릴때 캐시를 사용하게 한다
        Bitmap screenBitmap = view.getDrawingCache();   //캐시를 비트맵으로 변환
        File file = new File(FilePath,  "Golf_" + FileSaveTime_ver + "_img.jpg");
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            screenBitmap.compress(Bitmap.CompressFormat.PNG, 90, os);   //비트맵을 PNG파일로 변환
            os.close();
            FileSaveTime_ver++;
            Toast.makeText(MainActivity.this, "이미지 캡쳐", Toast.LENGTH_SHORT).show();
            lineChart.invalidate();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("IOException", "" + e);
            return null;
        }
        view.setDrawingCacheEnabled(false);
        return file;
    }

    private void TensorFlowLite() {
//        // converted_model.tflite
//        float[][] input = new float[140][3];
//        float[] output = new float[2];
//
//        Interpreter tfile = getTfliteInterpreter("converted_model.tflite");
//        tfile.run(Gyro, output);
//
//        Log.e("output", ""+output[0]);
    }
}
