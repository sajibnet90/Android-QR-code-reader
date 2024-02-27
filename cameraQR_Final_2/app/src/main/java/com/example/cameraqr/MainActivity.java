package com.example.cameraqr;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button scanButton;
    private Button checkLocationButton;
    private TextView txtCurrentLocation;

    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ------Initialize FusedLocationProviderClient-------
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
    }

    private void initViews() {
        scanButton = findViewById(R.id.scanButton);
        checkLocationButton = findViewById(R.id.btnCheckLocation);
        txtCurrentLocation = findViewById(R.id.txtCurrentLocation);

        scanButton.setOnClickListener(this);
        checkLocationButton.setOnClickListener(this);
    }

    // -------- method to update current location---------------
    private void updateCurrentLocation() {
        // -----Check for location permission------
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Get last known location.
                        //if (location == null) {
                        //    txtCurrentLocation.setText("Location not available");
                        //} else {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        String currentLocation = "Latitude: " + latitude + "\nLongitude: " + longitude;
                        txtCurrentLocation.setText(currentLocation);//show to TextView
                        //}
                    });

        }else{
            // -----Request location permission if not granted------
            String[] permissions = {
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };
            ActivityCompat.requestPermissions(this, permissions, 42);
            txtCurrentLocation.setText("Click again to see location");
        }
    }


    @Override
        public void onClick(View v) {
            if (v.getId() == R.id.scanButton) {
                //------start ScanActivity--------
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
            } else if (v.getId() == R.id.btnCheckLocation) {
                // -----Call the method to update and display current location---
                updateCurrentLocation();
            }
        }

    }
