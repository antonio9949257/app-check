package com.example.appcheck;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistrationActivity extends AppCompatActivity {

    private RadioGroup rgRol;
    private RadioButton rbAlumno, rbMaestro;
    private EditText etNombres, etApellidoPaterno, etApellidoMaterno, etCedulaIdentidad;
    private TextView tvMatriculaLabel, tvCelularLabel;
    private EditText etMatricula, etNumeroCelular;
    private LinearLayout llCelular;
    private EditText etCorreoElectronico;
    private Button btnRegistrar;
    private AppDatabase db;
    private String androidId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "usuarios-db").build();

        initViews();

        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void initViews() {
        rgRol = findViewById(R.id.rgRol);
        rbAlumno = findViewById(R.id.rbAlumno);
        rbMaestro = findViewById(R.id.rbMaestro);
        etNombres = findViewById(R.id.etNombres);
        etApellidoPaterno = findViewById(R.id.etApellidoPaterno);
        etApellidoMaterno = findViewById(R.id.etApellidoMaterno);
        etCedulaIdentidad = findViewById(R.id.etCedulaIdentidad);
        tvMatriculaLabel = findViewById(R.id.tvMatriculaLabel);
        tvCelularLabel = findViewById(R.id.tvCelularLabel);
        etMatricula = findViewById(R.id.etMatricula);
        etNumeroCelular = findViewById(R.id.etNumeroCelular);
        llCelular = findViewById(R.id.llCelular);
        etCorreoElectronico = findViewById(R.id.etCorreoElectronico);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        rgRol.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAlumno) {
                tvMatriculaLabel.setVisibility(View.VISIBLE);
                etMatricula.setVisibility(View.VISIBLE);
                tvCelularLabel.setVisibility(View.VISIBLE);
                llCelular.setVisibility(View.VISIBLE);
            } else {
                tvMatriculaLabel.setVisibility(View.GONE);
                etMatricula.setVisibility(View.GONE);
                tvCelularLabel.setVisibility(View.GONE);
                llCelular.setVisibility(View.GONE);
            }
        });

        btnRegistrar.setOnClickListener(v -> {
            if (validateForm()) {
                registerUser();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (etNombres.getText().toString().trim().isEmpty()) {
            etNombres.setError("Ingrese sus nombres");
            isValid = false;
        }

        if (etApellidoPaterno.getText().toString().trim().isEmpty()) {
            etApellidoPaterno.setError("Ingrese su apellido paterno");
            isValid = false;
        }

        if (etCedulaIdentidad.getText().toString().trim().isEmpty()) {
            etCedulaIdentidad.setError("Ingrese su cédula de identidad");
            isValid = false;
        }

        if (etCorreoElectronico.getText().toString().trim().isEmpty()) {
            etCorreoElectronico.setError("Ingrese su correo electrónico");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(etCorreoElectronico.getText().toString()).matches()) {
            etCorreoElectronico.setError("Ingrese un correo electrónico válido");
            isValid = false;
        }

        if (rbAlumno.isChecked()) {
            if (etMatricula.getText().toString().trim().isEmpty()) {
                etMatricula.setError("Ingrese su matrícula");
                isValid = false;
            }

            if (etNumeroCelular.getText().toString().trim().isEmpty()) {
                etNumeroCelular.setError("Ingrese su número de celular");
                isValid = false;
            }
        }

        return isValid;
    }

    private void registerUser() {
        new Thread(() -> {
            try {
                String role = rbAlumno.isChecked() ? "Alumno" : "Maestro";
                String nombres = etNombres.getText().toString().trim();
                String apellidoPaterno = etApellidoPaterno.getText().toString().trim();
                String apellidoMaterno = etApellidoMaterno.getText().toString().trim();
                String cedulaIdentidad = etCedulaIdentidad.getText().toString().trim();
                String correoElectronico = etCorreoElectronico.getText().toString().trim();
                String matricula = rbAlumno.isChecked() ? etMatricula.getText().toString().trim() : "";
                String celular = rbAlumno.isChecked() ? "+591" + etNumeroCelular.getText().toString().trim() : "";
                String fechaRegistro = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());

                Usuario usuario = new Usuario(
                        role, nombres, apellidoPaterno, apellidoMaterno,
                        cedulaIdentidad, matricula, celular,
                        correoElectronico, androidId, fechaRegistro
                );

                db.usuarioDao().insert(usuario);

                runOnUiThread(() -> {
                    Toast.makeText(RegistrationActivity.this,
                            "Registro exitoso!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegistrationActivity.this, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(RegistrationActivity.this,
                                "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void clearForm() {
        etNombres.setText("");
        etApellidoPaterno.setText("");
        etApellidoMaterno.setText("");
        etCedulaIdentidad.setText("");
        etMatricula.setText("");
        etNumeroCelular.setText("");
        etCorreoElectronico.setText("");
        rgRol.check(R.id.rbAlumno);
    }
}