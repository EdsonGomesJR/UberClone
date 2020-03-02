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
    public static final String user_field = "usr";
    public static final String pwd_field = "pwd";
    public static final int PICK_IMAGE_REQUEST = 9999;

    public static User currentUser;

    public static double base_fare = 2.55; //base uber at newyork

    private static double time_rate = 0.35;
    private static double distance_rate = 1.75;

    public static double formulaPrice(double km, double min) {

        return base_fare + (distance_rate * km) + (time_rate * min);
    }



    public static final String baseURL = "https://maps.googleapis.com";
    public static Location mLastLocation = null;

    public static IGoogleAPI getGoogleAPI() {

        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }

    public static IFCMService getFCMService() {

        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }
}
