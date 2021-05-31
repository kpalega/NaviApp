package com.example.compassapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class GetLocationActivity extends AppCompatActivity{
    private Button mNavigateButton;
    private Button mGetLocationButton;
    private Location location;
    private TextView mSavedPoint;
    private List<Float> pointCoordinates;
    private LocationApi locationApi;

    private static String latitudeKey = "latitudeKey";
    private static String longitudeKey = "longitudeKey";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    /**
     * Metoda uruchamiana przy starcie aktywności. Przypisuje wartości do wykorzystywanych w klasie zmiennych.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_location);
        mNavigateButton = findViewById(R.id.start_navigate_button);
        mGetLocationButton = findViewById(R.id.get_location_button);
        mSavedPoint = findViewById(R.id.savedPoint);
        pointCoordinates = new ArrayList<>(2);
        preferences = getSharedPreferences("SHARED_PREFS", MODE_PRIVATE);
        editor = preferences.edit();
        locationApi = new LocationApi(this);
        location = new Location(LocationManager.GPS_PROVIDER);
    }

    /**
     * Metoda uruchamiana przed wstrzymaniem aplikacji. Wywołuje metodę saveData() z tej klasy oraz metodę stop() z klasy LocationApi
     */
    @Override
    protected void onPause() {
        super.onPause();
        locationApi.stop();
        saveData();
    }

    /**
     * Metoda uruchamiana po wznowieniu aplikacji. Wywołuje metodę loadData() oraz updateView().
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        updateView();
    }

    /**
     * Metoda wyświetlająca okno dialogowe zawierające zapytanie przekazane w parametrze. Jeżeli użytkownik potwierdzi akcję zostaną wykonane instrukcje wskazywane przez przekazywaną
     * w parametrze wiadomość. Jeżeli odmówi, okno dialogowe się zamknie, a wybrana akcja zostanie wstrzymana.
     * @param message
     */
    public void alert(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(GetLocationActivity.this);
        builder.setTitle(R.string.alert_title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if( message == getResources().getString(R.string.alert_message )){
                            location = locationApi.getLocation(GetLocationActivity.this);
                            saveCoordinates(location);
                            saveData();
                            updateView();
                        } else if( message == getResources().getString(R.string.alert_delete) ){
                            pointCoordinates.add(0, (float) 0);
                            pointCoordinates.add(0, (float) 0);
                            mSavedPoint.setText(R.string.no_point);
                        }
                    }
                })
                .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                }).show();
    }

    /**
     * Metoda sprawdzająca czy użytkownik udzielił uprawnień aplikacji. Jeżeli tak pobierze lokalizację i ją zapisze. Jeżeli nie wyświetli powiadomienie w formie Toast.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 44 && grantResults.length > 0 && (grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED)){
            location = locationApi.getLocation(this);
            saveCoordinates(location);
        } else {
            Toast.makeText(getApplicationContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Metoda zapisuje pobrane dane do pamięci aplikacji pod podanym kluczem, używając SharedPreferences.
     */
    public  void saveData(){
        editor.putFloat(latitudeKey, pointCoordinates.get(0));
        editor.putFloat(longitudeKey, pointCoordinates.get(1));
        editor.apply();
    }

    /**
     * Metoda pobiera informacje z SharedPreferences o wskazanym kluczu.
     */
    public void loadData(){
        pointCoordinates.add(0, preferences.getFloat(latitudeKey, 0));
        pointCoordinates.add(1, preferences.getFloat(longitudeKey, 0));
    }

    /**
     * Metoda zapisująca długość i szerokość geograficzną do tabeli.
     * @param location
     */
    public void saveCoordinates(Location location){
        if(location != null){
            pointCoordinates.add(0, (float) location.getLatitude());
            pointCoordinates.add(1, (float) location.getLongitude());
        }else{
            pointCoordinates.add(0, (float) 0);
            pointCoordinates.add(1, (float) 0);
        }
    }

    /**
     * Metoda aktaulizująca pole tekstowe dot. pobranego punktu.
     */
    public void updateView(){
        if( pointCoordinates.get(0) != (float) 0 && pointCoordinates.get(1) != (float) 0  ){
            mSavedPoint.setText(getResources().getString(R.string.point) + " " + String.format("%.5f", pointCoordinates.get(0)) + ", " + String.format("%.5f", pointCoordinates.get(1)));
        }
    }

    /**
     * Metoda uruchamia się po naciśnięciu przycisku "Zacznij nawigować!". Sprawdza czy lokalizacja została pobrana. Jeśli tak, uruchamia aktywność Compass, przekazując do niej długość i szerokość geograficzną.
     * @param view
     */

    public void startNavigate(View view) {
        if( pointCoordinates.get(0) != (float) 0 && pointCoordinates.get(1) != (float) 0  ){
                Intent intent = new Intent(this, CompassActivity.class);
                intent.putExtra(latitudeKey, (double) preferences.getFloat(latitudeKey, 0));
                intent.putExtra(longitudeKey, (double) preferences.getFloat(longitudeKey, 0));
                startActivity(intent);
        } else {
            Toast.makeText(GetLocationActivity.this, R.string.start_navigate_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Metoda czyszcząca tablicę ze współrzednych geograficznych po naciśnięciu przycisku "Usuń lokację". Jeżeli nie ma pobranych żadnych danych wyświetli powiadomienie
     * w formie Toast o braku pobranego punktu.
     * @param view
     */
    public void deleteLocation(View view) {
        if( pointCoordinates.get(0) != (float) 0 && pointCoordinates.get(1) != (float) 0  ) {
            alert(getResources().getString(R.string.alert_delete));
        } else {
            Toast.makeText(GetLocationActivity.this, R.string.delete_error, Toast.LENGTH_SHORT ).show();
        }
    }

    /**
     * Metoda uruchamiająca się po naciśnięciu przycisku "Pobierz lokalizację". Jeżeli jest już pobrana lokalizacja, wywoła funkcję alert(). Jeśli nie, zapisze aktualną pozycję.
     * @param view
     */
    public void getCurrentLocation(View view) {
        if( pointCoordinates.get(0) != (float) 0 && pointCoordinates.get(1) != (float) 0  ){
            alert(getResources().getString(R.string.alert_message));
        } else {
            location = locationApi.getLocation(GetLocationActivity.this);
            saveCoordinates(location);
            updateView();
        }
    }
}