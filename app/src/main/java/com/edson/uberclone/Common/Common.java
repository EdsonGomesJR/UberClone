package com.edson.uberclone.Common;

import com.edson.uberclone.Remote.IGoogleAPI;
import com.edson.uberclone.Remote.RetrofitClient;

public class Common {

    public static final String baseURL = "https://maps.googleapis.com";

    public static IGoogleAPI getGoogleAPI() {

        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
