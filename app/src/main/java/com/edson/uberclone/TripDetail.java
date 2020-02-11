package com.edson.uberclone;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.edson.uberclone.Common.Common;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.DayOfWeek;

import java.util.Calendar;
import java.util.Locale;

public class TripDetail extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView txtDate, txtFee, txtBaseFare, txtTime, txtDistance, txtEstimatedPayout, txtFrom, txtTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //init view

        txtBaseFare = findViewById(R.id.txtBaseFare);
        txtDate = findViewById(R.id.txtDate);
        txtDistance = findViewById(R.id.txtDistance);
        txtEstimatedPayout = findViewById(R.id.txtEstimatedPayout);
        txtFee = findViewById(R.id.txtFee);
        txtFrom = findViewById(R.id.txtFrom);
        txtTo = findViewById(R.id.txtTo);
        txtTime = findViewById(R.id.txtTime);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        settingInformation();
    }

    private void settingInformation() {

        if (getIntent() != null) {

            //Set text
            Calendar calendar = Calendar.getInstance();
            String date = String.format("%s, %d/%d", convertToDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK)),
                    calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH + 1));

            txtDate.setText(date);

            txtFee.setText(String.format("$ %.2f", getIntent().getDoubleExtra("total", 0.0)));
            txtEstimatedPayout.setText(String.format("$ %.2f", getIntent().getDoubleExtra("total", 0.0)));
            txtBaseFare.setText(String.format("$ %.2f", Common.base_fare));
            txtTime.setText(String.format("%s min", getIntent().getStringExtra("time")));
            txtDistance.setText(String.format("%s km", getIntent().getStringExtra("distance")));
            txtFrom.setText(getIntent().getStringExtra("start_address"));
            txtTo.setText(getIntent().getStringExtra("end_address"));

            //add marker
            String[] location_end = getIntent().getStringExtra("location_end").split(",");
            LatLng dropOff = new LatLng(Double.parseDouble(location_end[0]), Double.parseDouble(location_end[1]));

            mMap.addMarker(new MarkerOptions().position(dropOff)
                    .title("Drop Off Here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dropOff, 12.0f));


        }
    }

    private String convertToDayOfWeek(int day) {

        switch (day) {

            case Calendar.SUNDAY:
                return "DOMINGO";

            case Calendar.MONDAY:
                return "SEGUNDA-FEIRA";
            case Calendar.TUESDAY:
                return "TERÇA-FEIRA";
            case Calendar.WEDNESDAY:
                return "QUARTA-FEIRA";
            case Calendar.THURSDAY:
                return "QUINTA-FEIRA";
            case Calendar.FRIDAY:
                return "SEXTA-FEIRA";
            case Calendar.SATURDAY:
                return "SÁBADO";

            default:
                return "UNK";

        }
    }
}
