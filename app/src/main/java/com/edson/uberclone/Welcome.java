package com.edson.uberclone;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edson.uberclone.Model.Token;
import com.google.android.libraries.places.api.Places;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.edson.uberclone.Common.Common;
import com.edson.uberclone.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.text.Line;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;

import com.google.android.libraries.places.widget.AutocompleteSupportFragment;

import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Welcome extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;

    //play services

    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 70001;
    private static int UPDATE_INTERVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;
    DatabaseReference drivers;
    GeoFire geoFire;
    Marker mCurrent;
    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;
    private LocationRequest mLocationRequest;


    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;


    //car animation
    private List<LatLng> polyLineList;
    private Marker carMarker;
    private float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPosition, endPosition, currentPosition;
    private int index, next;
    //private Button btnGo;

    private String destination;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, greyPolyline;

    private PlaceAutocompleteFragment places;

    //Presence System
    DatabaseReference onlineRef, currentUserRef;

    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
            if (index < polyLineList.size() - 1) {

                index++;
                next = index + 1;
            }

            if (index < polyLineList.size() - 1) {

                startPosition = polyLineList.get(index);
                endPosition = polyLineList.get(next);
            }

            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());

            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {

                    v = valueAnimator.getAnimatedFraction();
                    lng = v * endPosition.longitude + (1 - v) * startPosition.longitude;
                    lat = v * endPosition.latitude + (1 - v) * startPosition.latitude;
                    LatLng newPos = new LatLng(lat, lng);
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f, 0.5f);
                    carMarker.setRotation(getBearing(startPosition, newPos));//rolamento
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(newPos)
                                    .zoom(15.5f)
                                    .build()
                    ));
                }
            });
            valueAnimator.start();
            handler.postDelayed(this, 3000);
        }
    };
    private IGoogleAPI mService;

    private float getBearing(LatLng startPosition, LatLng endPosition) {
        double lat = Math.abs(startPosition.latitude - endPosition.latitude);
        double lng = Math.abs(startPosition.longitude - endPosition.longitude);

        if (startPosition.latitude < endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));

        else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);

        else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float) ((Math.toDegrees(Math.atan(lng / lat))) + 180);

        else if (startPosition.latitude < endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);


        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_direction_api));
        PlacesClient placesClient = Places.createClient(this);

        //Presence System
        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");
        currentUserRef = FirebaseDatabase.getInstance().getReference(Common.driver_tbl)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //We will remove value from Driver tbl when driver is disconnected
                currentUserRef.onDisconnect().removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //init view
        location_switch = findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {

                if (isOnline) {

                    FirebaseDatabase.getInstance().goOnline(); // set connected when switch is on

                    buildLocationCallBack();
                    buildLocationRequest();
                    fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "Você está Online!", Snackbar.LENGTH_SHORT)
                            .show();

                } else {


                    if (handler != null) {
                        handler.removeCallbacks(drawPathRunnable);
                    }
                    FirebaseDatabase.getInstance().goOffline();//set disconnected  when switch is off
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                    mMap.clear();
                    Snackbar.make(mapFragment.getView(), "Você está OFFLINE!", Snackbar.LENGTH_SHORT)
                            .show();
                }

            }
        });

        polyLineList = new ArrayList<>();
        // btnGo = findViewById(R.id.btnGo);
        //  edtPlace = findViewById(R.id.place_autocomplete_fragment);

        //places api

        if (!Places.isInitialized()) {

            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_direction_api));
        }

        //initialize the AutocompleteSupportFragment

        AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS));

        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                if (location_switch.isChecked()) {
                    destination = place.getAddress().toString();
                    destination = destination.replace(" ", "+");

                    getDirection();

                } else {

                    Toast.makeText(Welcome.this, "Please change your status to ONLINE", Toast.LENGTH_SHORT).show();
                    mMap.clear();
                }

            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(Welcome.this, "" + status.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        /** places = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
         places.setOnPlaceSelectedListener(new PlaceSelectionListener() {
        @Override public void onPlaceSelected(Place place) {

        if(location_switch.isChecked())
        {
        destination = place.getAddress().toString();
        destination = destination.replace(" ","+");

        getDirection();

        }
        else
        {

        Toast.makeText(Welcome.this, "Please change your status to ONLINE", Toast.LENGTH_SHORT).show();

        }

        }

        @Override public void onError(Status status) {
        Toast.makeText(Welcome.this, "" + status.toString(), Toast.LENGTH_SHORT).show();

        }
        });*/

        /** btnGo.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
        //
        //destination = edtPlace.getText().toString();
        destination = destination.replace(" ", "+"); //replace space with + for fecth data
        Log.d("Eds", destination);

        getDirection();
        }
        });
         */

        //Geo fire
        drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        geoFire = new GeoFire(drivers);

        setUpLocation();

        mService = Common.getGoogleAPI();

        updateFirebaseToken();
    }

    private void updateFirebaseToken() {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        final DatabaseReference tokens = db.getReference(Common.token_tbl);

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                Token token = new Token(instanceIdResult.getToken());
                tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(token);
            }
        });

    }

    private void getDirection() {

        currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());

        String requestApi = null;

        try {

            requestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination=" + destination + "&" +
                    "key=" + getResources().getString(R.string.google_direction_api);
            Log.d("Eds", requestApi);//print url for debug

            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {

                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");

                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyline = poly.getString("points");
                                    polyLineList = decodePoly(polyline);
                                }

                                //adjusting bounds
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (LatLng latLng : polyLineList)
                                    builder.include(latLng);
                                LatLngBounds bounds = builder.build();
                                CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                                mMap.animateCamera(mCameraUpdate);

                                polylineOptions = new PolylineOptions();
                                polylineOptions.color(Color.GRAY);
                                polylineOptions.width(5);
                                polylineOptions.startCap(new SquareCap());
                                polylineOptions.endCap(new SquareCap());
                                polylineOptions.jointType(JointType.ROUND);
                                polylineOptions.addAll(polyLineList);
                                greyPolyline = mMap.addPolyline(polylineOptions);

                                blackPolylineOptions = new PolylineOptions();
                                blackPolylineOptions.color(Color.BLACK);
                                blackPolylineOptions.width(5);
                                blackPolylineOptions.startCap(new SquareCap());
                                blackPolylineOptions.endCap(new SquareCap());
                                blackPolylineOptions.jointType(JointType.ROUND);
                                blackPolyline = mMap.addPolyline(blackPolylineOptions);

                                mMap.addMarker(new MarkerOptions()
                                        .position(polyLineList.get(polyLineList.size() - 1))
                                        .title("Pickup Location"));

                                //animation

                                ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0, 100);
                                polyLineAnimator.setDuration(2000);
                                polyLineAnimator.setInterpolator(new LinearInterpolator());
                                polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        List<LatLng> points = greyPolyline.getPoints();
                                        int percentValue = (int) valueAnimator.getAnimatedValue();
                                        int size = points.size();
                                        int newPoints = (int) (size * (percentValue / 100.0f));
                                        List<LatLng> p = points.subList(0, newPoints);
                                        blackPolyline.setPoints(p);
                                    }
                                });

                                polyLineAnimator.start();

                                carMarker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                        .flat(true)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                                handler = new Handler();
                                index = -1;
                                next = 1;
                                handler.postDelayed(drawPathRunnable, 3000);


                            } catch (JSONException e) {

                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {

                        }
                    });

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {


            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    buildLocationCallBack();
                    buildLocationRequest();
                    if (location_switch.isChecked())
                        displayLocation();
                }

        }


    }


    private void setUpLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                )) {
            //request Runtime permission
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_REQUEST_CODE);
        } else {

            buildLocationRequest();
            buildLocationCallBack();
            if (location_switch.isChecked())
                displayLocation();
        }
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

    private void buildLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
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

                            if (location_switch.isChecked()) {
                                final double latitude = Common.mLastLocation.getLatitude();
                                final double longitude = Common.mLastLocation.getLongitude();

                                //update to firebase
                                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                                    @Override
                                    public void onComplete(String key, DatabaseError error) {
                                        //add marker
                                        if (mCurrent != null)
                                            mCurrent.remove(); //remove o marcador que ja esta
                                        mCurrent = mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(latitude, longitude))
                                                .title("Você"));


                                        //mover a camera para essa posição
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));


                                    }
                                });
                            }

                        } else {

                            Log.d("ERROR", "cannot get your location: ");
                        }
                    }
                });


    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = mCurrent.getRotation();
        final long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                float rot = t * i + (1 - t) * startRotation;
                mCurrent.setRotation(-rot > 180 ? rot / 2 : rot);

                if (t < 1.0) {

                    handler.postDelayed(this, 16);
                }
            }
        });


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        buildLocationRequest();
        buildLocationCallBack();
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());


    }


}
