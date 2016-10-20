package com.applozic.mobicomkit.uiwidgets.conversation.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicommons.commons.core.utils.PermissionsUtils;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.applozic.mobicomkit.uiwidgets.instruction.ApplozicPermissions;


public class MobicomLocationActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, ActivityCompat.OnRequestPermissionsResultCallback {

    SupportMapFragment mapFragment;
    LatLng position;
    RelativeLayout sendLocation;
    private LinearLayout layout;
    public Snackbar snackbar;
    Location mCurrentLocation;
    protected GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    public static final int LOCATION_SERVICE_ENABLE = 1001;
    protected static final long UPDATE_INTERVAL = 5;
    protected static final long FASTEST_INTERVAL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applozic_location);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_map_screen);
        toolbar.setTitle("Send Location");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sendLocation = (RelativeLayout) findViewById(R.id.sendLocation);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        onNewIntent(getIntent());

        processLocation();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        position = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        googleMap.addMarker(new MarkerOptions().position(position).title(""));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 20));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(17), 2000, null);

        sendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("test", "in map");
                Intent intent = new Intent();
                intent.putExtra("latitude", mCurrentLocation.getLatitude());
                intent.putExtra("longitude", mCurrentLocation.getLongitude());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        new ConversationUIService(this).onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SERVICE_ENABLE) {
            if (((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                googleApiClient.connect();
            } else {
                Toast.makeText(MobicomLocationActivity.this, R.string.unable_to_fetch_location, Toast.LENGTH_LONG).show();
            }
            return;
        }
    }

    public void processingLocation() {
        if (!((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.location_services_disabled_title)
                    .setMessage(R.string.location_services_disabled_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.location_service_settings, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, LOCATION_SERVICE_ENABLE);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Toast.makeText(MobicomLocationActivity.this, R.string.location_sending_cancelled, Toast.LENGTH_LONG).show();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            googleApiClient.disconnect();
            googleApiClient.connect();
        }
    }

    public void processLocation() {
      //  if (Utils.hasMarshmallow()) {
          //  new ApplozicPermissions(MobicomLocationActivity.this, layout).checkRuntimePermissionForLocation();
       // } else {
            processingLocation();
       // }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(((Object) this).getClass().getSimpleName(),
                "onConnectionSuspended() called.");

    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (mCurrentLocation == null) {
                Toast.makeText(this, R.string.waiting_for_current_location, Toast.LENGTH_SHORT).show();
                locationRequest = new LocationRequest();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(UPDATE_INTERVAL);
                locationRequest.setFastestInterval(FASTEST_INTERVAL);
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            }

            if (mCurrentLocation != null)
                mapFragment.getMapAsync(this);

        } catch (Exception e) {
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            if (location != null) {
                mCurrentLocation = location;
            }
        } catch (Exception e) {
        }
    }

    public void showSnackBar(int resId) {
        snackbar = Snackbar.make(layout, resId,
                Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

}
