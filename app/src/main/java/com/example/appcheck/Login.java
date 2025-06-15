package com.example.appcheck;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

public class Login extends AppCompatActivity {
    private Button btnRegister, btnLogin;
    private TextView tvEstadoRegistro, tvRegistrate, tvInstrucciones;
    private String androidId = "";
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        tvEstadoRegistro = findViewById(R.id.tvEstadoRegistro);
        tvRegistrate = findViewById(R.id.tvRegistrate);
        tvInstrucciones = findViewById(R.id.tvInstrucciones);

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "usuarios-db").build();

        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, RegistrationActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            checkUserRegistration();
        });

        checkInitialState();
    }

    private void checkInitialState() {
        new Thread(() -> {
            Usuario usuario = db.usuarioDao().getUsuarioPorAndroidId(androidId);
            runOnUiThread(() -> {
                if (usuario != null) {
                    tvEstadoRegistro.setVisibility(View.VISIBLE);
                    tvEstadoRegistro.setText(String.format("Bienvenido: %s\nRol: %s\nID: %s",
                            usuario.getNombres() + " " + usuario.getApellidoPaterno(),
                            usuario.getRol(),
                            androidId.substring(0, 6) + "..."));

                    tvRegistrate.setVisibility(View.GONE);
                    tvInstrucciones.setVisibility(View.GONE);

                    toggleLoginViews(true);
                } else {
                    tvEstadoRegistro.setVisibility(View.GONE);
                    tvRegistrate.setVisibility(View.VISIBLE);
                    tvInstrucciones.setVisibility(View.VISIBLE);

                    toggleLoginViews(false);
                }
            });
        }).start();
    }

    private void toggleLoginViews(boolean isRegistered) {
        if (isRegistered) {
            btnRegister.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
        } else {
            btnRegister.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
        }
    }

    private void checkUserRegistration() {
        new Thread(() -> {
            Usuario usuario = db.usuarioDao().getUsuarioPorAndroidId(androidId);
            runOnUiThread(() -> {
                if (usuario != null) {
                    if (usuario.getRol().equals("Alumno")) {
                        startActivity(new Intent(Login.this, MainActivityStudent.class));
                    } else if (usuario.getRol().equals("Maestro")) {
                        startActivity(new Intent(Login.this, MainActivityMaster.class));
                    }
                    finish();
                } else {
                    Toast.makeText(Login.this,
                            "No se encontró registro para este dispositivo",
                            Toast.LENGTH_SHORT).show();
                    toggleLoginViews(false);
                }
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInitialState();
    }
}