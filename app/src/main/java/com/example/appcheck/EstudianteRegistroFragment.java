package com.example.appcheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class EstudianteRegistroFragment extends Fragment {

    public EstudianteRegistroFragment() {
        // Constructor público vacío requerido
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflamos el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_docente_historial, container, false);

        // Aquí puedes configurar cualquier lógica adicional para tu fragmento
        // Por ejemplo, listeners de botones, etc.

        return view;
    }
}
