package com.edson.uberclone.Service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.edson.uberclone.Common.Common;
import com.edson.uberclone.Model.Token;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseIdService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d("New_TOKEN", s);
        updateTokenToServer(s); //When have refresh token, we need update to our Realtime Database

    }

    private void updateTokenToServer(String s) {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(s);
        if (FirebaseAuth.getInstance().getCurrentUser() != null)//if already login, must update Token
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .setValue(token);
    }
}
