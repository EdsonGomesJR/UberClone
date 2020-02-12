package com.edson.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.edson.uberclone.Common.Common;
import com.edson.uberclone.Model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    DatabaseReference users;
    private Button btnRegistrar, btnLogar;
    private RelativeLayout rootLayout;
    private FirebaseAuth auth;
    private FirebaseDatabase db;

    private TextView txt_forgot_pwd;


    //adiciona o Calligraphy  ao contexto base
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //config o calligraphy para alterar a font do texto *OBS tem que estar antes do setContentView
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_main);

        //init Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference(Common.user_driver_tbl);


        inicializarWidgets();


    }

    //evento de clique
    public void irParaTelaDeCadastro(View view) {

        showCadastroDialog();

    }

    public void irParaTelaDeLogin(View view) {

        showLoginDialog();

    }

    private void showLoginDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("LOGIN");
        dialog.setMessage("Por favor use seu e-mail para Logar");

        LayoutInflater inflater = LayoutInflater.from(this);
        final View login_layout = inflater.inflate(R.layout.layout_login, null);

        final MaterialEditText edtMail = login_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtSenha = login_layout.findViewById(R.id.edtPassword);

        dialog.setView(login_layout);

        //set button
        dialog.setPositiveButton("LOGAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                //setar o botão de login como desabilitado enquanto  está carregando
                btnLogar.setEnabled(false);



                //checar validação
                if (TextUtils.isEmpty(edtMail.getText().toString())) {

                    Snackbar.make(rootLayout, "Por favor digite um endereço de email",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edtSenha.getText().toString())) {

                    Snackbar.make(rootLayout, "Por favor digite uma senha ",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (edtSenha.length() < 6) {

                    Snackbar.make(rootLayout, "Por favor digite uma senha com 6 digitos ou mais",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }

                final SpotsDialog carregandoDialog = new SpotsDialog(MainActivity.this);
                carregandoDialog.show();
                //login
                auth.signInWithEmailAndPassword(edtMail.getText().toString(), edtSenha.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                carregandoDialog.dismiss();
                                FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl)
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                Common.currentUser = dataSnapshot.getValue(User.class);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                startActivity(new Intent(MainActivity.this, Welcome.class));
                                finish();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        carregandoDialog.dismiss();

                        Snackbar.make(rootLayout, "Login Falhou: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();
//ativando o botão de logar
                        btnLogar.setEnabled(true);
                    }
                });

            }
        });

        dialog.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        dialog.show();
    }


    private void showCadastroDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("CADASTRO");
        dialog.setMessage("Por favor use seu e-mail para cadastrar");

        LayoutInflater inflater = LayoutInflater.from(this);
        View register_layout = inflater.inflate(R.layout.layout_register, null);

        final MaterialEditText edtMail = register_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtSenha = register_layout.findViewById(R.id.edtPassword);
        final MaterialEditText edtNome = register_layout.findViewById(R.id.edtNome);
        final MaterialEditText edtTelefone = register_layout.findViewById(R.id.edtPhone);

        dialog.setView(register_layout);

        //set button
        dialog.setPositiveButton("CADASTRO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                //checar validação

                if (TextUtils.isEmpty(edtMail.getText().toString())) {

                    Snackbar.make(rootLayout, "Por favor digite um endereço de email",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edtSenha.getText().toString())) {

                    Snackbar.make(rootLayout, "Por favor digite uma senha",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (edtSenha.length() < 6) {

                    Snackbar.make(rootLayout, "Por favor digite uma senha com 6 digitos ou mais",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edtNome.getText().toString())) {

                    Snackbar.make(rootLayout, "Por favor digite um nome",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edtTelefone.getText().toString())) {

                    Snackbar.make(rootLayout, "Por favor digite um telefone",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }


                auth.createUserWithEmailAndPassword(edtMail.getText().toString(), edtSenha.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {

                                //salvar usuario no db
                                User user = new User();
                                user.setEmail(edtMail.getText().toString());
                                user.setPassword(edtSenha.getText().toString());
                                user.setName(edtNome.getText().toString());
                                user.setPhone(edtTelefone.getText().toString());

                                //usando uid do current user do firebase como  chave
                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Snackbar.make(rootLayout, "Cadastrado com Sucesso!"
                                                        , Snackbar.LENGTH_SHORT).show();

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(rootLayout, "Falha ao cadastrar:   " + e.getMessage()
                                                , Snackbar.LENGTH_SHORT).show();

                                    }
                                });

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(rootLayout, "Falha ao criar usuario:   " + e.getMessage()
                                , Snackbar.LENGTH_SHORT).show();
                    }
                });

            }
        });

        dialog.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void inicializarWidgets() {

        btnLogar = findViewById(R.id.btnSignIn);
        btnRegistrar = findViewById(R.id.btnRegister);
        rootLayout = findViewById(R.id.rootLayout);
        txt_forgot_pwd = findViewById(R.id.txt_forgot_pwd);
        txt_forgot_pwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showDialogForgotPwd();
                return false;
            }
        });
    }

    private void showDialogForgotPwd() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("FORGOT PASSWORD");
        alertDialog.setMessage("Please enter your e-mail address");

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View forgot_pwd_layout = inflater.inflate(R.layout.layout_forgot_pwd, null);

        final MaterialEditText edtEmail = forgot_pwd_layout.findViewById(R.id.edtEmail);
        alertDialog.setView(forgot_pwd_layout);

        //set button
        alertDialog.setPositiveButton("RESET", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                final SpotsDialog waitingDialog = new SpotsDialog(MainActivity.this);
                waitingDialog.show();

                auth.sendPasswordResetEmail(edtEmail.getText().toString().trim())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.dismiss();
                                waitingDialog.dismiss();
                                Snackbar.make(rootLayout, "Reset password link has been sent to your email", Snackbar.LENGTH_LONG).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        dialog.dismiss();
                        waitingDialog.dismiss();
                        Snackbar.make(rootLayout, "" + e.getMessage(), Snackbar.LENGTH_LONG).show();

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
    }
}
