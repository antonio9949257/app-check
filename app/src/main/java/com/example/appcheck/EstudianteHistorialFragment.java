package com.example.appcheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class EstudianteHistorialFragment extends Fragment {
    public EstudianteHistorialFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflamos el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_estudiante_historial, container, false);

        // Aquí puedes configurar cualquier lógica adicional para tu fragmento
        // Por ejemplo, listeners de botones, etc.

        return view;
    }
}
