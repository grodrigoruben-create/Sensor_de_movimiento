package com.example.sensor;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int CODIGO_SOLICITUD_PERMISO = 100;
    private static final float VARIABLE_VIVRACION = 12.0f;
    private static final String NOMBRE_CONTA = "PreferenciasContadorPasos";
    private static final String CLAVE_DESP = "PASOS";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor accelSensor;
    private Vibrator vibrator;
    private TextView tvSteps;
    private TextView tvAccel;
    private Button btnReset;
    private float totalSteps = 0;
    private float previousStepOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvSteps = findViewById(R.id.tvSteps);
        tvAccel = findViewById(R.id.tvAccel);
        btnReset = findViewById(R.id.btnReset);

        // Cargar los pasos de referencia previamente guardados
        loadData();

        // Inicializar Gestor de Sensores
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Inicializar Vibrador
        VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        if (vibratorManager != null) {
            vibrator = vibratorManager.getDefaultVibrator();
        }

        // Configurar acción del botón Reiniciar
        btnReset.setOnClickListener(v -> {
            previousStepOffset = totalSteps; // Establecer el valor actual como nuevo cero
            saveData();
            tvSteps.setText(getString(R.string.steps_initial));
            Toast.makeText(MainActivity.this, getString(R.string.toast_reset), Toast.LENGTH_SHORT).show();
        });

        // Solicitar permisos en tiempo de ejecución
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    CODIGO_SOLICITUD_PERMISO);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (stepCounterSensor != null) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
            } else {
                tvSteps.setText(getString(R.string.steps_not_available));
            }

            if (accelSensor != null) {
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = event.values[0];

            // Si es la primera vez que inicia la app y no hay offset, fijar la referencia actual
            if (previousStepOffset == 0) {
                previousStepOffset = totalSteps;
                saveData();
            }

            int currentSteps = (int) (totalSteps - previousStepOffset);
            if (currentSteps < 0) {
                currentSteps = 0;
            }

            tvSteps.setText(getString(R.string.steps_format, currentSteps));
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            tvAccel.setText(getString(R.string.accel_format, x, y, z));

            float gForce = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

            if (gForce > VARIABLE_VIVRACION) {
                triggerVibration();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed
    }

    private void triggerVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(NOMBRE_CONTA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(CLAVE_DESP, previousStepOffset);
        editor.apply();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(NOMBRE_CONTA, Context.MODE_PRIVATE);
        previousStepOffset = sharedPreferences.getFloat(CLAVE_DESP, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_SOLICITUD_PERMISO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }
}