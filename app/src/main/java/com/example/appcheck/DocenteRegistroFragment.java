package com.example.appcheck;
//    Docentes puedan:
//    Seleccionar una materia de la BD
//    Publicar un servicio BLE con el nombre de la materia
//    Generar códigos únicos para estudiantes
//    Ver confirmaciones de registro
//    Estudiantes puedan:
//    Conectarse al servicio BLE
//    Recibir su código único (de forma segura)
//    Confirmar registro
//    import android.bluetooth.BluetoothAdapter;

import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DocenteRegistroFragment extends Fragment implements BluetoothServiceManager.BluetoothEventListener {

    // Views
    private Spinner spinnerMaterias;
    private TextView tvEstadoBLE;
    private Button btnIniciarServicio;
    private RecyclerView rvEstudiantes;
    private ProgressBar progressBar;

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
    }

    private void initViews(View view) {
        spinnerMaterias = view.findViewById(R.id.spinnerMaterias);
        tvEstadoBLE = view.findViewById(R.id.tvEstadoBLE);
        btnIniciarServicio = view.findViewById(R.id.btnIniciarServicio);
        rvEstudiantes = view.findViewById(R.id.rvEstudiantes);
        progressBar = view.findViewById(R.id.progressBar);

        btnIniciarServicio.setOnClickListener(v -> toggleBLEService());
    }

    private void setupRecyclerView() {
        estudianteAdapter = new EstudianteAdapter(estudiantesRegistrados);
        rvEstudiantes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEstudiantes.setAdapter(estudianteAdapter);
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
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        currentMateriaId = null;
                    }
                });
            } else {
                Toast.makeText(requireContext(), "No hay materias registradas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleBLEService() {
        if (currentMateriaId == null) {
            Toast.makeText(getContext(), "Selecciona una materia primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothService.isRunning()) {
            bluetoothService.stopBLEService();
        } else {
            showProgress(true);
            bluetoothService.startBLEService(currentMateriaId);
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnIniciarServicio.setEnabled(!show);
    }

    // BluetoothEventListener implementation
    @Override
    public void onStudentRegistered(String studentData) {
        requireActivity().runOnUiThread(() -> {
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

                estudiantesRegistrados.add(estudiante);
                estudianteAdapter.notifyItemInserted(estudiantesRegistrados.size() - 1);

                Toast.makeText(getContext(),
                        "Nuevo registro: " + estudiante.getNombres(),
                        Toast.LENGTH_SHORT).show();

            } catch (JSONException e) {
                Toast.makeText(getContext(), "Error procesando datos del estudiante", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAdvertisingStarted() {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            tvEstadoBLE.setText("Servicio BLE: ACTIVO - " + currentMateriaId);
            tvEstadoBLE.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
            btnIniciarServicio.setText("Detener Servicio");
        });
    }

    @Override
    public void onAdvertisingFailed(String error) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            tvEstadoBLE.setText("Error BLE: " + error);
            tvEstadoBLE.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
            btnIniciarServicio.setText("Iniciar Servicio");
        });
    }

    @Override
    public void onServiceStarted() {
        // Puedes añadir lógica adicional aquí si es necesario
    }

    @Override
    public void onServiceStopped() {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            tvEstadoBLE.setText("Servicio BLE: INACTIVO");
            tvEstadoBLE.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
            btnIniciarServicio.setText("Iniciar Servicio");
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.cleanup();
        }
    }

    // Estudiante Adapter
    private static class EstudianteAdapter extends RecyclerView.Adapter<EstudianteAdapter.ViewHolder> {
        private final List<Estudiante> estudiantes;

        EstudianteAdapter(List<Estudiante> estudiantes) {
            this.estudiantes = estudiantes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_estudiante, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Estudiante estudiante = estudiantes.get(position);
            holder.tvNombre.setText(estudiante.getNombreCompleto());
            holder.tvMatricula.setText(estudiante.getMatricula());
            holder.tvHora.setText(android.text.format.DateFormat.format("HH:mm", new java.util.Date()));
        }

        @Override
        public int getItemCount() {
            return estudiantes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvMatricula, tvHora;

            ViewHolder(View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tvNombre);
                tvMatricula = itemView.findViewById(R.id.tvMatricula);
                tvHora = itemView.findViewById(R.id.tvHora);
            }
        }
    }
}