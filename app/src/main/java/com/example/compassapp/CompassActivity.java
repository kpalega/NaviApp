package com.example.compassapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {
    private ImageView imageArrow;
    private TextView txtAzimuth;
    private int mAzimuth;
    private SensorManager sensorManager;
    private Sensor mRotationVector, mAccelerometer, mMagnetometer;
    private float[] rMat = new float[9];
    private float[] rOrientation = new float[9];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean isHaveAccelerometer = false, isHaveMagnetometer = false, isHaveRotationVector = false;
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private Location destination;
    private static String latitudeKey = "latitudeKey";
    private static String longitudeKey = "longitudeKey";
    private Location currentLocation = new Location(LocationManager.GPS_PROVIDER);

    private LocationApi locationApi;


    /**
     * Metoda uruchamiana przy starcie aktywności. Przypisuje wartości do wykorzystywanych w klasie zmiennych, a następnie przekierowuje do metody start().
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        locationApi = new LocationApi(this);
        destination = new Location(LocationManager.GPS_PROVIDER);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        imageArrow = (ImageView) findViewById(R.id.img_arrow);
        txtAzimuth = (TextView) findViewById(R.id.txtAzimuth);
        Intent intent = getIntent();
        destination.setLatitude(intent.getDoubleExtra(latitudeKey, 0));
        destination.setLongitude(intent.getDoubleExtra(longitudeKey, 0));
        if (!destination.hasBearing()) {
            txtAzimuth.setText(destination.toString());
        }
        start();
    }

    /**
     * Metoda wywoływana przy każdej zmianie sensora. Pobiera aktualną lokalizację urządzenia oraz azymut w który urządzenie jest skierowane. Azymut zostaje przetworzony na stopnie.
     * Następnie zostany pobrany azymut w którym znajduje się cel i z jego wykorzystaniem zostaje obrócona strzałka w widoku, która ma wskazywać cel. Dodatkowo zostaje pobrana
     * i wyświetlona odległość do celu.
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        currentLocation = locationApi.getLocation(CompassActivity.this);

        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) ((Math.toDegrees(SensorManager.getOrientation(rMat, rOrientation)[0])+360)%360);
        } else if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ){
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
           mLastAccelerometerSet = true;
        } else if( event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        } if(mLastMagnetometerSet && mLastAccelerometerSet) {
           SensorManager.getRotationMatrix(rMat,null, mLastAccelerometer, mLastMagnetometer);
            mAzimuth = (int) ((Math.toDegrees(SensorManager.getOrientation(rMat,rOrientation)[0])+360)%360);
        }

        mAzimuth = Math.round(mAzimuth);
        int locationAzimuth = (int)currentLocation.bearingTo(destination);
        imageArrow.setRotation(mAzimuth + locationAzimuth);
        double kilometers = currentLocation.distanceTo(destination)/1000;
        if(kilometers < 1){
            txtAzimuth.setText( String.format("%.0f", kilometers*1000  ) + " " + getResources().getString(R.string.meters));
        } else {
            txtAzimuth.setText( String.format("%.2f", kilometers  ) + " " +getResources().getString(R.string.kilometers));
        }
    }

    /**
     * Metoda zaimplementowana w celu funkcjonowania sensorów, lecz nie wykorzystywana w tym projekcie.
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Metoda sprawdza z jakiego komponentu korzysta urządzenie i uruchamia akcelerometer i magnetometer lub wektor lokalizacji w zależności od urządzenia. Jeżeli urządzenie nie
     * posiada żadnego z tych podzespołów, zostaje wyświetlony komunikat.
     */
    public void start(){
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)==null){
            if(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null
                    || sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null){
                noSensorAlert();
            } else {
                mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                isHaveAccelerometer = sensorManager.registerListener(this, mAccelerometer, sensorManager.SENSOR_DELAY_UI);
                isHaveMagnetometer = sensorManager.registerListener(this,mMagnetometer,sensorManager.SENSOR_DELAY_UI);
            }
        }
        else {
            mRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            isHaveRotationVector = sensorManager.registerListener(this, mRotationVector, sensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Metoda wyświetlająca na ekranie nowe okno dialogowe z informacją o braku możliwości korzystania z aplikacji. Po naciśnięciu przycisku "Cancel", aplikacja zostanie zamknięta.
     */
    public void noSensorAlert(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage(R.string.error_message)
                .setCancelable(false)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
    }

    /**
     * Metoda wyrejestrowująca poszczególne Listner, w celu optymalizacji działania aplikacji.
     */
    public void stop(){
        if(isHaveMagnetometer && isHaveAccelerometer){
            sensorManager.unregisterListener(this, mAccelerometer);
            sensorManager.unregisterListener(this, mMagnetometer);
        } else if(isHaveRotationVector){
            sensorManager.unregisterListener(this, mRotationVector);
        }
    }

    /**
     * Metoda uruchamiana przed wstrzymaniem aplikacji. Wywołuje metodę stop() z tej klasy oraz metodę stop() z klasy LocationApi
     */
    @Override
    protected void onPause() {
        super.onPause();
        locationApi.stop();
        stop();
    }

    /**
     * Metoda uruchamiana po wznowieniu aplikacji. Wywołuje metodę start().
     */
    @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    /**
     * Metoda wywołująca aktywność GetLocation, reagująca na wciśnięcie przycisku.
     * @param view
     */
    public void stopNavigate(View view) {
        Intent intent = new Intent(this, GetLocationActivity.class);
        startActivity(intent);
    }
}