package com.example.cameraqr;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class ScanActivity extends AppCompatActivity {
    SurfaceView surfaceView;
    TextView txtBarcodeValue;//added to display barcode value
    TextView txtDistance; // Added for displaying distance
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private static final int REQUEST_LOCATION_PERMISSION = 202;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private FusedLocationProviderClient fusedLocationClient;


    //----------onCreate---------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //-----back button to go back to main activity --------
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        txtBarcodeValue = findViewById(R.id.txtBarcodeValue);
        surfaceView = findViewById(R.id.surfaceView);
        txtDistance = findViewById(R.id.txtDistance);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }
        };
        initialiseDetectorsAndSources();
    }
// ---------setting up bar code reader and source----------
    private void initialiseDetectorsAndSources() {
        //Toast.makeText(getApplicationContext(), "Barcode scanner started", Toast.LENGTH_SHORT).show();

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            ScanActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        //if (surfaceView.getHolder() != null) { // Check for null
                            cameraSource.start(surfaceView.getHolder());
                       // }
                    }
                    else {
                        ActivityCompat.requestPermissions(ScanActivity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        //-------------detection logic and fetch value---------------
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    final String qrCodeValue = barcodes.valueAt(0).displayValue;

                    txtBarcodeValue.post(new Runnable() {
                        @Override
                        public void run() {
                            txtBarcodeValue.setText(qrCodeValue);
                            extractAndDisplayDistance(qrCodeValue);
                        }
                    });
                }
            }

        //-------extract geoLocation from Qrcode and calculate distance-----
            //----if QRcode is link then open the link in map option----------
            private void extractAndDisplayDistance(String qrCodeValue) {
                if (qrCodeValue.startsWith("geo:")) {
                    String geoData = qrCodeValue.substring(4);
                    String[] latLng = geoData.split(","); // make two string of array with long and lattitude
                    if (latLng.length == 2) {
                        double qrLatitude = Double.parseDouble(latLng[0].trim());
                        double qrLongitude = Double.parseDouble(latLng[1].trim());

                        Location lastKnownLocation = getLastKnownLocation();//function loads the last known location

                        if (lastKnownLocation != null) {
                            double deviceLatitude = lastKnownLocation.getLatitude();
                            double deviceLongitude = lastKnownLocation.getLongitude();

                            float[] results = new float[1];
                            Location.distanceBetween(
                                    deviceLatitude, deviceLongitude,
                                    qrLatitude, qrLongitude,
                                    results
                            );
                            double distanceInKm = results[0] / 1000.0;

                            txtDistance.setText(String.format("Distance Between QRcode location and Current Location: %.2f km", distanceInKm));
                            txtBarcodeValue.setText("QRcode location: " + qrCodeValue);

                        } else {
                            txtDistance.setText("Scan again to check Distance from current location");
                            txtBarcodeValue.setText("QR location Captured: " + qrCodeValue);

                        }
                    }
                }else if (qrCodeValue.startsWith("http:") || qrCodeValue.startsWith("https:")) {
                    // Barcode is a link
                    txtDistance.setText("Click to open link");
                    setLinkClickListener(qrCodeValue);

                }
            }
            //---------helper method to get the link-------------------
            private void setLinkClickListener(final String link) {
                txtDistance.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openLinkInBrowser(link);
                    }
                });
            }
            //----------parse the link data and start new intent-------
            private void openLinkInBrowser(String link) {
                Uri uri = Uri.parse(link);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }

            //----------getting the last known location of the device -------


            private Location getLastKnownLocation() {
                if (ActivityCompat.checkSelfPermission(
                        ScanActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(
                        ScanActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED)
                    {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                0,
                                0,
                                locationListener
                    );
                    //return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                } else {
                    ActivityCompat.requestPermissions(ScanActivity.this, new
                            String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                        }
                return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

        });

    }

    // ----backButton to go back to main_activity-------------------
    @Override
    public void onBackPressed() {
        // Handle the back button press here
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onPause() {
        super.onPause();
        cameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialiseDetectorsAndSources();
    }
}
