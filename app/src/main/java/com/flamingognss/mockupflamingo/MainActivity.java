package com.flamingognss.mockupflamingo;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import flamingo.flamingoapi.FlamingoLocation;
import flamingo.flamingoapi.FlamingoLocationCallback;
import flamingo.flamingoapi.FlamingoManager;
import flamingo.flamingoapi.enums.ApplicationType;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, FlamingoLocationCallback {
    private String TAG = "MainActivity";

    /**
     * Flamingo requires access to the INTERNET and FINE LOCATION.
     * Permissions need to be explicitly requested from end user.
     */
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET};
    /**
     * GoogleMap, GoogleLocation
     */
    private GoogleMap mMap;
    private Marker flamingoMarker;
    private MarkerOptions flamingoMarkerOption;

    /**
     * Flamingo
     */
    private LatLng flamingoPosition;
    private boolean isZoomToLayer = false;
    private ImageView iZoomFlamingo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        checkPermissions();

        createWidgets();
        addGoogleMap();

        // implement Flamingo
        FlamingoManager mFlamingoManager = new FlamingoManager(this);
        mFlamingoManager.registerFlamingoService("flamingo_application", "flamingo_password", "flamingo_companyId", ApplicationType.VEHICLE_NAVIGATION, this);
    }

    private void createWidgets() {
        iZoomFlamingo = findViewById(R.id.iZoomFlamingoLocationId);

        iZoomFlamingo.setOnClickListener((View v) -> {
            if (mMap != null && flamingoPosition != null) {
                mMap.setMyLocationEnabled(true);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(flamingoPosition, 18));
            }
        });

        iZoomFlamingo.setAlpha(.2f);
    }

    /**
     * Initialize google location callback and google NMEA callback.
     * <p>
     * The project is created only for demonstration purposes and any details related to
     * GoogleMap implementation cannot be used in other projects.
     */
    private void addGoogleMap() {
        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check the latest version of Google Play Services is installed
        final int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, 1);
            dialog.show();
        } else {
            // Display the map
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setMyLocationEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        addKmlFiles();
        flamingoMarkerOption = new MarkerOptions()
                .title("flamingo")
                .icon(getBitmapFromVector(this, R.drawable.circle_pink))
                .anchor(0.5f, 0.5f);

    }


    /**
     * create personalised marker
     *
     * @param context
     * @param vectorResourceId
     * @return
     */
    private static BitmapDescriptor getBitmapFromVector(@NonNull Context context,
                                                        @DrawableRes int vectorResourceId) {

        Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), vectorResourceId, null);
        if (vectorDrawable == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    protected void checkPermissions() {
        //       isInternetAvailable();
        final List<String> missingPermissions = new ArrayList<>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions.toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
        Log.i(TAG, "missing: " + missingPermissions.toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            for (int index = permissions.length - 1; index >= 0; --index) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Grant " + index + "is not granted");
                    // exit the app if one permission is not granted
                    Toast.makeText(this, "Required permission '" + permissions[index]
                            + "' not granted, exiting", Toast.LENGTH_LONG).show();
                    // finish();
                    return;
                }
            }
            // all permissions were granted
        }
    }


    @Override
    public void registerFlamingoLocationCallback() {

    }

    @Override
    public void unregisterFlamingoLocationCallback() {

    }

    /**
     * Update Flamingo marker on the map when new location data be received
     *
     * @param flamingoLocation:: returns the flamingo positioning solution
     */
    @Override
    public void onFlamingoLocationReceived(FlamingoLocation flamingoLocation) {
        flamingoPosition = new LatLng(flamingoLocation.getLatitude(), flamingoLocation.getLongitude());
        runOnUiThread(() -> {
            if (mMap != null) {
                if (flamingoMarker != null) {
                    flamingoMarker.remove();
                }

                flamingoMarker = mMap.addMarker(flamingoMarkerOption.position(flamingoPosition));
                if (!isZoomToLayer) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(flamingoPosition, 20));
                    iZoomFlamingo.setAlpha(.8f);
                    isZoomToLayer = true;
                }
            }
        });
    }



    /**
     * Ddd reference paths for Application Types: FITNESS, TRACKING, VEHICLE_NAVIGATION and PEDESTRIAN_NAVIGATION.
     * SURVEYING and AUGMENTED_REALITY routes are not linear
     */
    private void addKmlFiles() {
        new ReadKmlFile(R.raw.fitness_path).execute();
        new ReadKmlFile(R.raw.tracking_line).execute();
        new ReadKmlFile(R.raw.vehicle_navigation_path).execute();
        new ReadKmlFile(R.raw.pedestrian_navigation_path).execute();
    }

    private class ReadKmlFile extends AsyncTask {
        private int mFile;
        private KmlLayer layer;


        public ReadKmlFile(int file) {
            mFile = file;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // delete other kml layers if exists
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                layer = new KmlLayer(mMap, mFile, getApplicationContext());
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
            return layer;
        }

        @Override
        protected void onPostExecute(Object s) {
            try {
                // Add KML layer to map
                if (mMap != null) {
                    layer.addLayerToMap();
                }
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
        }
    }

}