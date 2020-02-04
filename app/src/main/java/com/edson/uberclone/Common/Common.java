package com.edson.uberclone.Common;

import android.location.Location;

import com.edson.uberclone.Model.User;
import com.edson.uberclone.Remote.FCMClient;
import com.edson.uberclone.Remote.IFCMService;
import com.edson.uberclone.Remote.IGoogleAPI;
import com.edson.uberclone.Remote.RetrofitClient;

public class Common {


    public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String user_rider_tbl = "RiderInformation";
    public static final String pickup_request_tbl = "PickupRequest";
    public static final String token_tbl = "Tokens";
    public static final String fcmURL = "https://fcm.googleapis.com";

    public static User currentUser;


    public static final String baseURL = "https://maps.googleapis.com";
    public static Location mLastLocation = null;

    public static IGoogleAPI getGoogleAPI() {

        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }

    public static IFCMService getFCMService() {

        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }
}
