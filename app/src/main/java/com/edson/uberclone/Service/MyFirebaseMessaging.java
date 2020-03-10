package com.edson.uberclone.Service;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.edson.uberclone.CustommerCall;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessaging extends FirebaseMessagingService {


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData() != null) {

            Map<String, String> data = new HashMap<>();
            String customer = data.get("customer");
            String lat = data.get("lat");
            String lng = data.get("lng");
            //because i will send the firebase message with contain lat and lng from Rider app
            //so i need convert message to LatLng


            Intent intent = new Intent(getBaseContext(), CustommerCall.class);
            intent.putExtra("lat", lat);
            intent.putExtra("lng", lng);
            intent.putExtra("customer", customer);


            startActivity(intent);
        }

    }
}
