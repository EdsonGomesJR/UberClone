package com.edson.uberclone.ui.updateinfo;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.edson.uberclone.Common.Common;
import com.edson.uberclone.MainActivity;
import com.edson.uberclone.Model.User;
import com.edson.uberclone.NavHomeDrawer;
import com.edson.uberclone.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dmax.dialog.SpotsDialog;

import static android.app.Activity.RESULT_OK;


public class UpdateInfoFragment extends Fragment {

    //Firebase Storage
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    public UpdateInfoFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //init firebase storage
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("CHANGE INFORMATION");
        alertDialog.setMessage("Please fill all information");

        inflater = getActivity().getLayoutInflater();


        View layout_change_info = inflater.inflate(R.layout.layout_update_information, null);

        final MaterialEditText edtName = layout_change_info.findViewById(R.id.edtName);
        final MaterialEditText edtPhone = layout_change_info.findViewById(R.id.edtPhone);
        ImageView image_upload = layout_change_info.findViewById(R.id.image_upload);

        image_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        alertDialog.setView(layout_change_info);

        alertDialog.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                final AlertDialog waitingDialog = new SpotsDialog(getActivity());
                waitingDialog.show();

                String name = edtName.getText().toString();
                String phone = edtPhone.getText().toString();

                Map<String, Object> updateInfo = new HashMap<>();
                if (!TextUtils.isEmpty(name)) {
                    updateInfo.put("name", name);
                }
                if (!TextUtils.isEmpty(phone)) {
                    updateInfo.put("phone", phone);
                }


                final DatabaseReference driverInformations = FirebaseDatabase.getInstance()
                        .getReference(Common.user_driver_tbl);
                driverInformations.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .updateChildren(updateInfo)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                    /**
                                     * passa as informações de alteração de nome e telefone do driver
                                     * para a variavel Common.currentUser, assim atualiza os dados no nav header quando o driver
                                     * clicar em update não será necessario relogar no app, como é feito na aula
                                     */
                                    driverInformations
                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    Common.currentUser = dataSnapshot.getValue(User.class);
                                                    startActivity(new Intent(getActivity(), NavHomeDrawer.class));
                                                    getActivity().finish();
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                    Toast.makeText(getActivity(), "Information Updated !", Toast.LENGTH_SHORT).show();

                                } else {
                                    Toast.makeText(getActivity(), "Information Update Error", Toast.LENGTH_SHORT).show();
                                }
                                waitingDialog.dismiss();
                            }
                        });
            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

            }
        });

        alertDialog.show();

        return null;
    }

    private void chooseImage() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), Common.PICK_IMAGE_REQUEST);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {


            Uri saveUri = data.getData();
            if (saveUri != null) {

                final ProgressDialog mDialog = new ProgressDialog(getActivity());
                mDialog.setMessage("Uploading...");
                mDialog.show();

                String imageName = UUID.randomUUID().toString(); //random name image upload
                final StorageReference imageFolder = storageReference.child("images/" + imageName);
                imageFolder.putFile(saveUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                mDialog.dismiss();
                                Toast.makeText(getActivity(), "Uploaded !", Toast.LENGTH_SHORT).show();
                                imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        //update this url avatar property of User
                                        //First u need add avatar property on user model
                                        Map<String, Object> avatarUpdate = new HashMap<>();
                                        avatarUpdate.put("avatarUrl", uri.toString());

                                        DatabaseReference driverInformations = FirebaseDatabase.getInstance()
                                                .getReference(Common.user_driver_tbl);
                                        driverInformations.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .updateChildren(avatarUpdate)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(getActivity(), "Uploaded !", Toast.LENGTH_SHORT).show();

                                                        } else {
                                                            Toast.makeText(getActivity(), "Upload Error", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    }
                                });
                            }
                        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = ((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                        mDialog.setMessage("Uploaded    " + progress + "%");
                    }
                });
            }

        }
    }
}
