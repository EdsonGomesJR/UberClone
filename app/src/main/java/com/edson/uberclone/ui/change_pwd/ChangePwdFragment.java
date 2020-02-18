package com.edson.uberclone.ui.change_pwd;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.edson.uberclone.Common.Common;
import com.edson.uberclone.NavHomeDrawer;
import com.edson.uberclone.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ChangePwdFragment extends Fragment {

    Button btnApplyChanges;




    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_change_pwd, container, false);


        final MaterialEditText edtPassword = root.findViewById(R.id.edtPassword);
        final MaterialEditText edtNewPassword = root.findViewById(R.id.edtNewPassword);
        final MaterialEditText edtRepeatPassword = root.findViewById(R.id.edtRepeatPassword);
        btnApplyChanges = root.findViewById(R.id.btn_apply_changes);

        btnApplyChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final SpotsDialog waitingDialog = new SpotsDialog(getContext());
//
                waitingDialog.show();

                if (edtNewPassword.getText().toString().equals(edtRepeatPassword.getText().toString())) {

                    String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                    //get auth credencials from the user for re-authentication
                    //example with only email
                    AuthCredential credential = EmailAuthProvider.getCredential(email, edtPassword.getText().toString());
                    FirebaseAuth.getInstance().getCurrentUser()
                            .reauthenticate(credential)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        FirebaseAuth.getInstance().getCurrentUser()
                                                .updatePassword(edtRepeatPassword.getText().toString())
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {

                                                        if (task.isSuccessful()) {
                                                            //update driver information passowrd collumn
                                                            Map<String, Object> password = new HashMap<>();

                                                            password.put("password", edtRepeatPassword.getText().toString());

                                                            DatabaseReference driverInformation = FirebaseDatabase.getInstance()
                                                                    .getReference(Common.user_driver_tbl);

                                                            driverInformation.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                    .updateChildren(password)
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()) {
                                                                                waitingDialog.dismiss();
                                                                                Toast.makeText(getActivity(), "Password has changed", Toast.LENGTH_SHORT).show();
                                                                                Intent intent = new Intent(getActivity(), NavHomeDrawer.class);
                                                                                startActivity(intent);
                                                                                getActivity().finish();

                                                                            } else {

                                                                                waitingDialog.dismiss();
                                                                                Toast.makeText(getActivity(), "Password has changed, but not updated to Database", Toast.LENGTH_SHORT).show();
                                                                            }

                                                                        }
                                                                    });

                                                        } else {
                                                            waitingDialog.dismiss();

                                                            Toast.makeText(getActivity(), "Password doesn't change", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });

                                    } else {

                                        waitingDialog.dismiss();
                                        Toast.makeText(getActivity(), "Wrong old password", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });


                } else {

                    waitingDialog.dismiss();
                    Toast.makeText(getActivity(), "Password doesn't match!", Toast.LENGTH_SHORT).show();
                }


            }
        });


        return root;

    }
}