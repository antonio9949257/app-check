package com.example.appcheck;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DocenteRegistroFragment extends Fragment implements BluetoothServiceManager.BluetoothEventListener {

    // Views
    private Spinner spinnerMaterias;
    private TextView tvEstadoBLE;
    private TextView tvDebugConsole;
    private Button btnIniciarServicio;
    private RecyclerView rvEstudiantes;
    private ProgressBar progressBar;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    // Adapters
    private EstudianteAdapter estudianteAdapter;

    // Data
    private List<Materia> materias = new ArrayList<>();
    private List<Estudiante> estudiantesRegistrados = new ArrayList<>();
    private String currentMateriaId;

    // Services
    private BluetoothServiceManager bluetoothService;
    private MateriaViewModel materiaViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_docente_registro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initViews(view);

        // Setup RecyclerView
        setupRecyclerView();

        // Initialize Bluetooth service
        bluetoothService = new BluetoothServiceManager(requireContext(), this);

        // Load materias from database
        loadMaterias();

        // Test messages (debug only)
        if (BuildConfig.DEBUG) {
            uiHandler.postDelayed(() -> {
                addDebugMessage("[SISTEMA] Monitor BLE inicializado\n");
                addDebugMessage("[PRUEBA] Conexión simulada: 11:22:33:44:55:66\n");
                addDebugMessage("[PRUEBA] Datos recibidos: {\"nombres\":\"Juan\",\"matricula\":\"TEST001\"}\n");
            }, 1000);
        }
    }

    private void initViews(View view) {
        spinnerMaterias = view.findViewById(R.id.spinnerMaterias);
        tvEstadoBLE = view.findViewById(R.id.tvEstadoBLE);
        btnIniciarServicio = view.findViewById(R.id.btnIniciarServicio);
        rvEstudiantes = view.findViewById(R.id.rvEstudiantes);
        progressBar = view.findViewById(R.id.progressBar);
        tvDebugConsole = view.findViewById(R.id.tvDebugConsole);

        // Configure debug console
        tvDebugConsole.setMovementMethod(new ScrollingMovementMethod());
        clearDebugConsole();

        btnIniciarServicio.setOnClickListener(v -> toggleBLEService());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(false);
        layoutManager.setStackFromEnd(false);
        rvEstudiantes.setLayoutManager(layoutManager);

        rvEstudiantes.addItemDecoration(new DividerItemDecoration(
                requireContext(), DividerItemDecoration.VERTICAL));

        estudianteAdapter = new EstudianteAdapter(requireContext(), estudiantesRegistrados);
        rvEstudiantes.setAdapter(estudianteAdapter);

        rvEstudiantes.setItemAnimator(new DefaultItemAnimator());
    }

    private void loadMaterias() {
        materiaViewModel = new ViewModelProvider(this).get(MateriaViewModel.class);

        materiaViewModel.getAllMaterias().observe(getViewLifecycleOwner(), materiasFromDb -> {
            if (materiasFromDb != null && !materiasFromDb.isEmpty()) {
                materias.clear();
                materias.addAll(materiasFromDb);

                ArrayAdapter<Materia> adapter = new ArrayAdapter<Materia>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        materias) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        view.setText(materias.get(position).getNombre());
                        return view;
                    }

                    @Override
                    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        view.setText(materias.get(position).getNombre() + " (" + materias.get(position).getSigla() + ")");
                        return view;
                    }
                };

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerMaterias.setAdapter(adapter);

                spinnerMaterias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        currentMateriaId = materias.get(position).getSigla();
                        addDebugMessage("[SISTEMA] Materia seleccionada: " + currentMateriaId + "\n");
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        currentMateriaId = null;
                    }
                });
            } else {
                Toast.makeText(requireContext(), "No hay materias registradas", Toast.LENGTH_SHORT).show();
                addDebugMessage("[ERROR] No hay materias disponibles\n");
            }
        });
    }

    private void toggleBLEService() {
        if (currentMateriaId == null) {
            Toast.makeText(getContext(), "Selecciona una materia primero", Toast.LENGTH_SHORT).show();
            addDebugMessage("[ERROR] No se seleccionó materia\n");
            return;
        }

        if (bluetoothService.isRunning()) {
            bluetoothService.stopBLEService();
            addDebugMessage("[SISTEMA] Servicio BLE detenido\n");
        } else {
            showProgress(true);
            bluetoothService.startBLEService(currentMateriaId);
            addDebugMessage("[SISTEMA] Iniciando servicio BLE...\n");
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnIniciarServicio.setEnabled(!show);
    }

    // BluetoothEventListener implementation
    @Override
    public void onDataReceived(String deviceAddress, String data) {
        uiHandler.post(() -> {
            try {
                String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                String formattedMessage = String.format(Locale.US, "[%s] %s: %s\n",
                        timestamp,
                        deviceAddress,
                        data);

                addDebugMessage(formattedMessage);
                Log.d("BLE_DEBUG_UI", formattedMessage.trim());

            } catch (Exception e) {
                Log.e("BLE_UI_ERROR", "Error al mostrar datos", e);
                addDebugMessage("[ERROR] " + e.getMessage() + "\n");
            }
        });
    }

    @Override
    public void onStudentRegistered(String studentData) {
        uiHandler.post(() -> {
            try {
                JSONObject json = new JSONObject(studentData);
                Estudiante estudiante = new Estudiante(
                        json.getString("nombres"),
                        json.getString("apellidoPaterno"),
                        json.getString("apellidoMaterno"),
                        json.getString("matricula"),
                        json.getString("cedulaIdentidad"),
                        json.optString("celular", ""),
                        json.optString("correo", "")
                );

                // Verificar si el estudiante ya está registrado
                boolean existe = false;
                for (Estudiante e : estudiantesRegistrados) {
                    if (e.getMatricula().equals(estudiante.getMatricula())) {
                        existe = true;
                        break;
                    }
                }

                if (!existe) {
                    estudiantesRegistrados.add(0, estudiante);
                    estudianteAdapter.notifyItemInserted(0);
                    rvEstudiantes.smoothScrollToPosition(0);

                    addDebugMessage("[REGISTRO] Nuevo estudiante: " + estudiante.getNombreCompleto() + "\n");
                    Toast.makeText(getContext(), "Nuevo registro: " + estudiante.getNombreCompleto(), Toast.LENGTH_SHORT).show();
                } else {
                    addDebugMessage("[ADVERTENCIA] Estudiante ya registrado: " + estudiante.getMatricula() + "\n");
                    Toast.makeText(getContext(), "Estudiante ya registrado", Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                Log.e("BLE_ERROR", "Error al parsear JSON: " + e.getMessage(), e);
                addDebugMessage("[ERROR] Formato de datos inválido\n");
                Toast.makeText(getContext(), "Error en formato de datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAdvertisingStarted() {
        uiHandler.post(() -> {
            showProgress(false);
            tvEstadoBLE.setText("Servicio BLE: ACTIVO - " + currentMateriaId);
            tvEstadoBLE.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
            btnIniciarServicio.setText("Detener Servicio");
            addDebugMessage("[SISTEMA] Servicio BLE activo\n");
        });
    }

    @Override
    public void onAdvertisingFailed(String error) {
        uiHandler.post(() -> {
            showProgress(false);
            tvEstadoBLE.setText("Error BLE: " + error);
            tvEstadoBLE.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
            btnIniciarServicio.setText("Iniciar Servicio");
            addDebugMessage("[ERROR] Fallo en servicio BLE: " + error + "\n");
        });
    }

    @Override
    public void onServiceStarted() {
        addDebugMessage("[SISTEMA] Servicio BLE iniciado\n");
    }

    @Override
    public void onServiceStopped() {
        uiHandler.post(() -> {
            showProgress(false);
            tvEstadoBLE.setText("Servicio BLE: INACTIVO");
            tvEstadoBLE.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
            btnIniciarServicio.setText("Iniciar Servicio");
            addDebugMessage("[SISTEMA] Servicio BLE detenido\n");
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.cleanup();
        }
        uiHandler.removeCallbacksAndMessages(null);
    }

    private void addDebugMessage(String message) {
        if (tvDebugConsole != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Limitar a 100 líneas para evitar sobrecarga
                String currentText = tvDebugConsole.getText().toString();
                String[] lines = currentText.split("\n");
                if (lines.length >= 100) {
                    currentText = currentText.substring(currentText.indexOf('\n') + 1);
                }

                tvDebugConsole.setText(currentText + message);

                // Auto-scroll
                final Layout layout = tvDebugConsole.getLayout();
                if (layout != null) {
                    int scrollAmount = layout.getLineTop(tvDebugConsole.getLineCount()) - tvDebugConsole.getHeight();
                    if (scrollAmount > 0) {
                        tvDebugConsole.scrollTo(0, scrollAmount);
                    }
                }
            });
        }
    }

    private void clearDebugConsole() {
        if (tvDebugConsole != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvDebugConsole.setText("");
            });
        }
    }

    // Estudiante Adapter
    private static class EstudianteAdapter extends RecyclerView.Adapter<EstudianteAdapter.ViewHolder> {
        private final List<Estudiante> estudiantes;
        private final LayoutInflater inflater;

        EstudianteAdapter(Context context, List<Estudiante> estudiantes) {
            this.inflater = LayoutInflater.from(context);
            this.estudiantes = estudiantes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_estudiante, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Estudiante estudiante = estudiantes.get(position);

            holder.tvNombre.setText(estudiante.getNombreCompleto());
            holder.tvMatricula.setText(String.format("Matrícula: %s", estudiante.getMatricula()));
            holder.tvCedula.setText(String.format("Cédula: %s", estudiante.getCedulaIdentidad()));

            if (!TextUtils.isEmpty(estudiante.getCelular())) {
                holder.tvCelular.setText(String.format("Celular: %s", estudiante.getCelular()));
                holder.tvCelular.setVisibility(View.VISIBLE);
            } else {
                holder.tvCelular.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(estudiante.getCorreo())) {
                holder.tvCorreo.setText(String.format("Correo: %s", estudiante.getCorreo()));
                holder.tvCorreo.setVisibility(View.VISIBLE);
            } else {
                holder.tvCorreo.setVisibility(View.GONE);
            }

            holder.tvHora.setText(DateFormat.format("HH:mm", new Date()));
        }

        @Override
        public int getItemCount() {
            return estudiantes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvNombre, tvMatricula, tvCedula, tvCelular, tvCorreo, tvHora;

            ViewHolder(View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tvNombre);
                tvMatricula = itemView.findViewById(R.id.tvMatricula);
                tvCedula = itemView.findViewById(R.id.tvCedula);
                tvCelular = itemView.findViewById(R.id.tvCelular);
                tvCorreo = itemView.findViewById(R.id.tvCorreo);
                tvHora = itemView.findViewById(R.id.tvHora);
            }
        }
    }
}