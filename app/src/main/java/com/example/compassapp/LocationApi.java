package com.example.compassapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class LocationApi extends Activity {
    private LocationManager locationManager;
    private Location location;
    private LocationListener locationListener;

    /**
     * Metoda wywołująca aktywność GetLocation, reagująca na wciśnięcie przycisku.
     * @param context
     */
    LocationApi(Context context){
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

            }
        };
        location = new Location(LocationManager.GPS_PROVIDER);
    }

    /**
     * Metoda pobierająca aktualną lokalizację. Po sprawdzeniu czy użytkownik udzielił uprawnień, LocationManager rejestruje LocationListner, który nasłuchuje zmnian w lokalizacji.
     * Następnie zostaje pobrana aktualna lokalizacja i jeżeli pobrana lokacja nie jest pusta, rezultat zostaje udostępniony klasie wywołującej metodę.
     * @param context
     * @return
     */

    public Location getLocation(Context context) {
        Activity activity = (Activity) context;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener );
            if (locationManager != null){
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null){
                    return location;
                }
            }

        } else {
            ActivityCompat.requestPermissions(activity, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 44);
        }
        return location;
    }

    /**
     * Metoda wyrejestrowująca LocationListner, w celu optymalizacji działania aplikacji.
     */
    public void stop(){
        locationManager.removeUpdates(locationListener);
    }

}
