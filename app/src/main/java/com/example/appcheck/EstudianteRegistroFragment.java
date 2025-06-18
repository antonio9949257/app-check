package com.example.appcheck;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EstudianteRegistroFragment extends Fragment implements EstudianteBLEManager.EstudianteBLEListener, DispositivoAdapter.OnDispositivoClickListener {

    private static final String TAG = "EstudianteRegistro";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private EstudianteBLEManager bleManager;
    private TextView tvEstadoBLE;
    private TextView tvInfoEstudiante;
    private Button btnIniciarEscaneo;
    private Button btnRegistrarse;
    private Button btnCancelar;
    private RecyclerView rvDispositivos;
    private DispositivoAdapter dispositivoAdapter;
    private List<DispositivoDocente> dispositivos = new ArrayList<>();
    private DispositivoDocente dispositivoSeleccionado;

    private String androidId;
    private AppDatabase db;
    private Usuario usuario;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicialización similar a LoginActivity
        db = Room.databaseBuilder(requireContext(),
                AppDatabase.class, "app_database").build();

        androidId = Settings.Secure.getString(requireContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // Cargar usuario al crear el fragmento
        cargarUsuario();
    }

    private void cargarUsuario() {
        new Thread(() -> {
            usuario = db.usuarioDao().getUsuarioPorAndroidId(androidId);

            requireActivity().runOnUiThread(() -> {
                if (usuario == null) {
                    Toast.makeText(getContext(), "Usuario no registrado", Toast.LENGTH_LONG).show();
                    requireActivity().onBackPressed();
                } else {
                    // Configurar UI con los datos del usuario
                    configurarDatosEstudiante();
                }
            });
        }).start();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_estudiante_registro, container, false);

        // Inicializar vistas
        tvEstadoBLE = view.findViewById(R.id.tvEstadoBLE);
        tvInfoEstudiante = view.findViewById(R.id.tvInfoEstudiante);
        btnIniciarEscaneo = view.findViewById(R.id.btnIniciarEscaneo);
        btnRegistrarse = view.findViewById(R.id.btnRegistrarse2);
        btnCancelar = view.findViewById(R.id.btnCancelar);
        rvDispositivos = view.findViewById(R.id.rvDispositivos);

        // Configurar RecyclerView
        dispositivoAdapter = new DispositivoAdapter(dispositivos, this);
        rvDispositivos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDispositivos.setAdapter(dispositivoAdapter);
        rvDispositivos.setVisibility(View.GONE);

        // Configurar listeners
        btnIniciarEscaneo.setOnClickListener(v -> verificarPermisosYEscaneo());
        btnRegistrarse.setOnClickListener(v -> registrarAsistencia());
        btnCancelar.setOnClickListener(v -> requireActivity().stopLockTask());

        return view;
    }

    private void configurarDatosEstudiante() {
        if (usuario == null) {
            Log.e(TAG, "Usuario es null en configurarDatosEstudiante");
            Toast.makeText(getContext(), "Error: Datos de usuario no disponibles", Toast.LENGTH_LONG).show();
            requireActivity().onBackPressed();
            return;
        }

        // Inicializar BLE Manager con datos del usuario
        bleManager = new EstudianteBLEManager(requireContext(), this);
        bleManager.setDatosEstudiante(
                usuario.getNombres(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                usuario.getMatricula(),
                usuario.getCedulaIdentidad(),
                usuario.getCelular() != null ? usuario.getCelular() : "",
                usuario.getCorreoElectronico() != null ? usuario.getCorreoElectronico() : "",
                usuario.getRol()
        );

        // Mostrar información del estudiante
        String nombreCompleto = usuario.getNombres() + " " +
                usuario.getApellidoPaterno() + " " +
                (usuario.getApellidoMaterno() != null ? usuario.getApellidoMaterno() : "");

        tvInfoEstudiante.setText(String.format("Estudiante: %s\nRol: %s\nMatrícula: %s",
                nombreCompleto.trim(), usuario.getRol(), usuario.getMatricula()));
    }

    private void verificarPermisosYEscaneo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }

        // Verificar estado Bluetooth
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        iniciarEscaneo();
    }

    private void iniciarEscaneo() {
        Log.d(TAG, "Iniciando escaneo BLE");
        tvEstadoBLE.setText("Escaneando dispositivos...");
        btnIniciarEscaneo.setEnabled(false);
        btnRegistrarse.setEnabled(false);
        dispositivoSeleccionado = null;

        // Ocultar RecyclerView y limpiar lista
        rvDispositivos.setVisibility(View.GONE);
        dispositivos.clear();
        dispositivoAdapter.notifyDataSetChanged();

        if (bleManager != null) {
            bleManager.startScanning();
        } else {
            Log.e(TAG, "BLE Manager no inicializado");
            tvEstadoBLE.setText("Error: BLE no inicializado");
            btnIniciarEscaneo.setEnabled(true);
        }
    }

    private void registrarAsistencia() {
        if (dispositivoSeleccionado == null) {
            Toast.makeText(getContext(), "Selecciona un docente primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bleManager == null) {
            Toast.makeText(getContext(), "Error: BLE no inicializado", Toast.LENGTH_SHORT).show();
            return;
        }

        tvEstadoBLE.setText("Registrando asistencia...");
        btnRegistrarse.setEnabled(false);
        bleManager.connectToDevice(dispositivoSeleccionado);
    }

    @Override
    public void onDeviceFound(DispositivoDocente dispositivo) {
        requireActivity().runOnUiThread(() -> {
            // Verificar si el dispositivo ya está en la lista
            for (DispositivoDocente d : dispositivos) {
                if (d.getIdDispositivo().equals(dispositivo.getIdDispositivo())) {
                    return; // Ya existe, no hacer nada
                }
            }

            dispositivos.add(dispositivo);
            dispositivoAdapter.notifyItemInserted(dispositivos.size() - 1);

            // Mostrar RecyclerView cuando se encuentra el primer dispositivo
            if (dispositivos.size() == 1) {
                rvDispositivos.setVisibility(View.VISIBLE);
            }

            tvEstadoBLE.setText(dispositivos.size() + " dispositivos encontrados");
        });
    }

    @Override
    public void onScanStarted() {
        requireActivity().runOnUiThread(() -> {
            tvEstadoBLE.setText("Escaneando dispositivos...");
        });
    }

    @Override
    public void onScanStopped() {
        requireActivity().runOnUiThread(() -> {
            btnIniciarEscaneo.setEnabled(true);

            if (dispositivos.isEmpty()) {
                rvDispositivos.setVisibility(View.GONE);
                tvEstadoBLE.setText("No se encontraron dispositivos");
            } else {
                tvEstadoBLE.setText("Escaneo completado. " + dispositivos.size() + " dispositivos encontrados");
                btnRegistrarse.setEnabled(dispositivoSeleccionado != null);
            }
        });
    }

    @Override
    public void onConnectionSuccess(String codigoConfirmacion) {
        requireActivity().runOnUiThread(() -> {
            tvEstadoBLE.setText("¡Asistencia registrada! Código: " + codigoConfirmacion);
            Toast.makeText(getContext(), "Registro exitoso", Toast.LENGTH_SHORT).show();
            btnRegistrarse.setEnabled(false);
        });
    }

    @Override
    public void onConnectionFailed(String error) {
        requireActivity().runOnUiThread(() -> {
            tvEstadoBLE.setText("Error: " + error);
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            btnRegistrarse.setEnabled(true);
        });
    }

    @Override
    public void onStatusChanged(String mensaje) {
        requireActivity().runOnUiThread(() -> {
            tvEstadoBLE.setText(mensaje);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                iniciarEscaneo();
            } else {
                Toast.makeText(getContext(), "Se requieren todos los permisos para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == getActivity().RESULT_OK) {
                iniciarEscaneo();
            } else {
                Toast.makeText(getContext(), "Bluetooth debe estar activado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bleManager != null) {
            bleManager.cleanup();
        }
    }

    @Override
    public void onDispositivoClick(DispositivoDocente dispositivo) {
        dispositivoSeleccionado = dispositivo;
        btnRegistrarse.setEnabled(true);
        tvEstadoBLE.setText("Seleccionado: " + dispositivo.getNombreMateria());
    }
}