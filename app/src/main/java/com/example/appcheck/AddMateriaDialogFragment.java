package com.example.appcheck;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class AddMateriaDialogFragment extends DialogFragment {
    public interface AddMateriaDialogListener {
        void onMateriaAdded(Materia materia);
    }

    private AddMateriaDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (AddMateriaDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement AddMateriaDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_materia, null);

        EditText etNombre = view.findViewById(R.id.etNombre);
        EditText etSigla = view.findViewById(R.id.etSigla);
        EditText etInstitucion = view.findViewById(R.id.etInstitucion);

        builder.setView(view)
                .setTitle("Agregar nueva materia")
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String nombre = etNombre.getText().toString();
                    String sigla = etSigla.getText().toString();
                    String institucion = etInstitucion.getText().toString();

                    Materia nuevaMateria = new Materia(nombre, sigla, institucion);
                    listener.onMateriaAdded(nuevaMateria);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dismiss();
                });

        return builder.create();
    }
}