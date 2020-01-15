package com.edson.uberclone.Common;

import com.edson.uberclone.Remote.IGoogleAPI;
import com.edson.uberclone.Remote.RetrofitClient;

public class Common {

    public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String user_rider_tbl = "RiderInformation";
    public static final String pickup_request_tbl = "PickupRequest";


    public static final String baseURL = "https://maps.googleapis.com";

    public static IGoogleAPI getGoogleAPI() {

        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
