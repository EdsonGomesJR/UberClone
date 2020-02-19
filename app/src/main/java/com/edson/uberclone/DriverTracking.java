package com.edson.uberclone;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.edson.uberclone.Common.Common;
import com.edson.uberclone.Helper.DirectionJSONParser;
import com.edson.uberclone.Model.FCMResponse;
import com.edson.uberclone.Model.Notification;
import com.edson.uberclone.Model.Sender;
import com.edson.uberclone.Model.Token;
import com.edson.uberclone.Remote.IFCMService;
import com.edson.uberclone.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverTracking extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int PLAY_SERVICE_RES_REQUEST = 70001;
    private static int UPDATE_INTERVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;
    double riderLat, riderLng;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private IGoogleAPI mService;
    private Circle riderMarker;
    private Marker driverMarker;
    private Polyline direction;
    private LatLng startPosition, endPosition, currentPosition;
    private String destination;
    Location pickupLocation;
    IFCMService mFCMService;
    GeoFire geoFire;

    String customerId;
    private Button btnStartTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_tracking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (getIntent() != null) {

            riderLat = getIntent().getDoubleExtra("lat", -1.0);
            riderLng = getIntent().getDoubleExtra("lng", -1.0);
            customerId = getIntent().getStringExtra("customerId");
        }

        mService = Common.getGoogleAPI();
        mFCMService = Common.getFCMService();


        setUpLocation();

        btnStartTrip = findViewById(R.id.btnStartTrip);
        btnStartTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnStartTrip.getText().equals("Start Trip")) {

                    pickupLocation = Common.mLastLocation;
                    btnStartTrip.setText("DROP OFF HERE");

                } else if (btnStartTrip.getText().equals("DROP OFF HERE")) {

                    calculateCashFee(pickupLocation, Common.mLastLocation);
                }
            }
        });
    }

    private void calculateCashFee(final Location pickupLocation, Location mLastLocation) {

        //copy from getDirection and make a few changes

        String requestApi = null;

        try {

            requestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + pickupLocation.getLatitude() + "," + pickupLocation.getLongitude() + "&" +
                    "destination=" + mLastLocation.getLatitude() + "," + mLastLocation.getLongitude() + "&" +
                    "key=" + getResources().getString(R.string.google_direction_api);


            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {

                            try {

                                //extract JSON


                                //Root Object
                                JSONObject jsonObject = new JSONObject(response.body());

                                JSONArray routes = jsonObject.getJSONArray("routes");
                                JSONObject object = routes.getJSONObject(0);

                                JSONArray legs = object.getJSONArray("legs");
                                JSONObject legsObject = legs.getJSONObject(0);

                                //Get distance
                                JSONObject distance = legsObject.getJSONObject("distance");
                                String distance_text = distance.getString("text");
                                //only get number from string to parse it
                                Double distance_value = Double.parseDouble(distance_text.replaceAll("[^0-9\\\\.]+", ""));

                                //get time
                                JSONObject timeObject = legsObject.getJSONObject("duration");
                                String time_text = timeObject.getString("text");
                                //only get number from string to parse it
                                Double time_value = Double.parseDouble(time_text.replaceAll("[^0-9\\\\.]+", ""));

                                sendDropOffNotification(customerId);

                                //create new activity
                                Intent intent = new Intent(DriverTracking.this, TripDetail.class);
                                intent.putExtra("start_address", legsObject.getString("start_address"));
                                intent.putExtra("end_address", legsObject.getString("end_address"));
                                intent.putExtra("time", String.valueOf(time_value));
                                intent.putExtra("distance", String.valueOf(distance_value));
                                intent.putExtra("total", Common.formulaPrice(distance_value, time_value));
                                intent.putExtra("location_start", String.format("%s,%s", pickupLocation.getLatitude(), pickupLocation.getLongitude()));
                                intent.putExtra("location_end", String.format("%s,%s", Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {

                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {

                            Toast.makeText(DriverTracking.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void setUpLocation() {


        buildLocationRequest();
        buildLocationCallBack();
        displayLocation();
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Common.mLastLocation = location;

                }
                displayLocation();
            }
        };
    }


    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                )) {

            return;

        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        Common.mLastLocation = location;

                        if (Common.mLastLocation != null) {


                            final double latitude = Common.mLastLocation.getLatitude();
                            final double longitude = Common.mLastLocation.getLongitude();

                            if (driverMarker != null)
                                driverMarker.remove();
                            driverMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))
                                    .title("You")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17.0f));
                            if (direction != null)
                                direction.remove(); // remove old direction
                            getDireciton();
                        } else {

                            Log.d("ERROR", "cannot get your location: ");
                        }
                    }
                });


    }

    private void getDireciton() {

        currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());

        String requestApi = null;

        try {

            requestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination=" + riderLat + "," + riderLng + "&" +
                    "key=" + getResources().getString(R.string.google_direction_api);
            Log.d("Eds", requestApi);//print url for debug

            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {

                            try {

                                new ParserTask().execute(response.body().toString());


                            } catch (Exception e) {

                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {

                            Toast.makeText(DriverTracking.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void buildLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            boolean isSuccess = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_style_map)
            );

            if (!isSuccess) {
                Log.e("Error", "Failed to load map");
            }
        } catch (Resources.NotFoundException ex) {
            ex.printStackTrace();
        }

        riderMarker = mMap.addCircle(new CircleOptions()
                .center(new LatLng(riderLat, riderLng))
                .radius(50) //radius is 50m
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f));

        //create Geo fencing with radius 50

        geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(Common.driver_tbl));
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(riderLat, riderLng), 0.05f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                //we willl ned rider ID to send notification
                //so we'll pass it from previous activiy (CustommerCall)
                sendArrivedNotification(customerId);
                btnStartTrip.setEnabled(true);

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });


        buildLocationRequest();
        buildLocationCallBack();
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());

    }

    private void sendArrivedNotification(String customerId) {

        Token token = new Token(customerId);
        //we will send this notification with title "Arrived" and body this string
        Notification notification = new Notification("Arrived", String.format(
                "The driver %s has arrived at your location",
                Common.currentUser.getName()));
        Log.d("user", "sendArrivedNotification: " + Common.currentUser.getName());

        Sender sender = new Sender(token.getToken(), notification);

        mFCMService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success != 1) {

                    Toast.makeText(DriverTracking.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    private void sendDropOffNotification(String customerId) {
        //notificação quando abacar a viagem
        Token token = new Token(customerId);
        //we will send this notification with title "Arrived" and body this string
        Notification notification = new Notification("DropOff", customerId);
        Log.d("user", "sendArrivedNotification: " + Common.currentUser.getName());

        Sender sender = new Sender(token.getToken(), notification);

        mFCMService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success != 1) {

                    Toast.makeText(DriverTracking.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        ProgressDialog mDialog = new ProgressDialog(DriverTracking.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.setMessage("Please wait....");
            mDialog.show();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {

                jObject = new JSONObject(strings[0]);
                DirectionJSONParser parser = new DirectionJSONParser();

                routes = parser.parse(jObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return routes;

        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();

            ArrayList points = null;
            PolylineOptions polylineOptions = null;

            for (int i = 0; i < lists.size(); i++) {

                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = lists.get(i);

                for (int j = 0; j < path.size(); j++) {

                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);

                }
                polylineOptions.addAll(points);
                polylineOptions.width(10);
                polylineOptions.color(Color.RED);
                polylineOptions.geodesic(true);
            }

            direction = mMap.addPolyline(polylineOptions);
        }
    }
}
