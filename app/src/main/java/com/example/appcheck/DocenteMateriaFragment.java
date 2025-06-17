package com.example.appcheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DocenteMateriaFragment extends Fragment implements AddMateriaDialogFragment.AddMateriaDialogListener {

    private MateriaAdapter materiaAdapter;
    private RecyclerView rvMaterias;
    private View tvEmptyState;
    private AppDatabase db;
    private MateriaViewModel materiaViewModel;

    public DocenteMateriaFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_docente_materia, container, false);

        // Inicializar vistas
        rvMaterias = view.findViewById(R.id.rvMaterias);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        MaterialButton btnAddMateria = view.findViewById(R.id.btnAddMateria);

        // Configurar RecyclerView
        materiaAdapter = new MateriaAdapter(new ArrayList<>());
        rvMaterias.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMaterias.setAdapter(materiaAdapter);

        // Configurar base de datos Room
        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();

        // Configurar ViewModel
        materiaViewModel = new ViewModelProvider(this).get(MateriaViewModel.class);
        materiaViewModel.init(db.materiaDao());
        materiaViewModel.getAllMaterias().observe(getViewLifecycleOwner(), new Observer<List<Materia>>() {
            @Override
            public void onChanged(List<Materia> materias) {
                materiaAdapter.setMaterias(materias);
                updateEmptyState(materias.size());
            }
        });

        // Configurar botón para agregar nueva materia
        btnAddMateria.setOnClickListener(v -> {
            AddMateriaDialogFragment dialog = new AddMateriaDialogFragment();
            dialog.show(getParentFragmentManager(), "AddMateriaDialogFragment");
        });

        return view;
    }

    @Override
    public void onMateriaAdded(Materia materia) {
        // Insertar la nueva materia en la base de datos
        new Thread(() -> {
            db.materiaDao().insert(materia);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Materia agregada", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void updateEmptyState(int itemCount) {
        if (itemCount == 0) {
            rvMaterias.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvMaterias.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    // ViewModel para Materia
    public static class MateriaViewModel extends androidx.lifecycle.ViewModel {
        private MateriaDao materiaDao;
        private LiveData<List<Materia>> allMaterias;

        public void init(MateriaDao materiaDao) {
            this.materiaDao = materiaDao;
            this.allMaterias = (LiveData<List<Materia>>) materiaDao.getAllMaterias();
        }

        public LiveData<List<Materia>> getAllMaterias() {
            return allMaterias;
        }
    }
    public void addNewMateria(Materia materia) {
        new Thread(() -> {
            // Insertar en la base de datos
            db.materiaDao().insert(materia);

            // Actualizar UI en el hilo principal
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Materia agregada", Toast.LENGTH_SHORT).show();
                // Actualizar tu RecyclerView aquí
            });
        }).start();
    }

    // Adapter para el RecyclerView
    public static class MateriaAdapter extends RecyclerView.Adapter<MateriaAdapter.MateriaViewHolder> {
        private List<Materia> materias;

        public MateriaAdapter(List<Materia> materias) {
            this.materias = materias;
        }

        public void setMaterias(List<Materia> materias) {
            this.materias = materias;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MateriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new MateriaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MateriaViewHolder holder, int position) {
            Materia materia = materias.get(position);
            holder.textView.setText(materia.getNombre() + " (" + materia.getSigla() + ")");
        }

        @Override
        public int getItemCount() {
            return materias.size();
        }

        static class MateriaViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView textView;

            public MateriaViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}