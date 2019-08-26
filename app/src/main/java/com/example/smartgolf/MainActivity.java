package com.example.smartgolf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

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
    float[] SensorTime = new float[time];

    boolean isBtnOn = false;
    float FileSaveTime = System.currentTimeMillis() * 1000;

    float dt;

    int FileSaveTime_ver = 0;

    float pitch;
    float roll;
    float yaw;

    File FilePath = new File(Environment.getExternalStorageDirectory() + "/Download");

    float[][][] inputGyro = new float[2][140][3];
    float[] outGyro = new float[]{};

    TensorFlowInferenceInterface inferenceInterface;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                lineChart.invalidate();
                lineChart.clear();
                lineDataSet.clear();
                lineData.clearValues();
                count = 0;
                MakeChart(0, 0);
            }
        });

        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lineChart.invalidate();
                lineChart.clear();
                lineDataSet.clear();
                lineData.clearValues();
                count = 0;
                inputGyro = new float[][][]{};
                outGyro = new float[]{};
                MakeChart(0, 0);
            }
        });

        btn_tensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TensorFlowLite();
            }
        });
    }

    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(MainActivity.this, modelPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
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

            // nanoTime = 1/1000000
            SensorTime[count] = System.nanoTime();

            Gyro[count] = new float[]{GyroX, GyroY, GyroZ};
//            Log.e("Gyro",""+Gyro[1][1]);
            if (count > 2) {
                dt = SensorTime[count - 1] - SensorTime[count - 2];
//                for(int i=0; i<10; i++){
//                    Log.d("SensorTime["+i+"]",""+SensorTime[i]);
//                }
            }
            Filtering();

            count++;
            updateMarker(count, GyroX);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void Filtering() {
        // angle 단순 계산
        pitch += (GyroX) * (dt * Math.pow(10, -9));
        roll += (GyroY) * (dt * Math.pow(10, -9));
        yaw += (GyroZ) * (dt * Math.pow(10, -9));

        // Filtering
        double forceMagnitude = Math.abs(lAccX) + Math.abs(lAccY) + Math.abs(lAccZ);
        if (forceMagnitude > 4.9 && forceMagnitude < 19.6) {
            float pitchAcc = (float) (Math.atan2(lAccY, lAccZ) * 180 / (Math.PI));
            pitch = (float) (pitch * 0.98 + pitchAcc * 0.02);

            float rollAcc = (float) (Math.atan2(lAccX, lAccZ) * 180 / (Math.PI));
            roll = (float) (pitch * 0.98 + rollAcc * 0.02);

            float yawAcc = (float) (Math.atan2(lAccX, lAccY) * 180 / (Math.PI));
            yaw = (float) (pitch * 0.98 + yawAcc * 0.02);
        }
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
            pw = new PrintWriter(new File(FilePath, "test.csv"));

            String[] cellString = {"Time", "AccX", "AccY", "AccZ"};

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
                csvData.append('\n');
            }
            pw.write(csvData.toString());
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
        File file = new File(FilePath, FileSaveTime + "_" + FileSaveTime_ver + "_img.jpg");
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