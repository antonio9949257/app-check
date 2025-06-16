package com.example.appcheck;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class EstudianteEscaneoFragment extends Fragment {

    public EstudianteEscaneoFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflamos el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_estudiante_escaneo_asistencia, container, false);

        // Configuración del primer botón (diálogo personalizado)
        ImageButton facturacionBtn = view.findViewById(R.id.facturacion);
        facturacionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoPersonalizado();
            }
        });

        // Configuración del segundo botón (AlertDialog estándar)
        ImageButton facturacionBtn2 = view.findViewById(R.id.facturacion2);
        facturacionBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarAlertDialogEstandar();
            }
        });

        return view;
    }

    private void mostrarDialogoPersonalizado() {
        final Dialog dialog = new Dialog(requireActivity());
        dialog.setContentView(R.layout.dialog_custom);
        dialog.setCancelable(true);

        TextView title = dialog.findViewById(R.id.title);
        EditText input = dialog.findViewById(R.id.input);
        Button btnOk = dialog.findViewById(R.id.btn_ok);

        // Personaliza los elementos
        title.setText("Facturación");
        input.setHint("Ingrese número de factura");

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String texto = input.getText().toString();
                // Haz algo con el texto
                dialog.dismiss();
            }
        });

        dialog.show();

        // Ajustar el ancho del diálogo
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            // Opcional: agregar animación
            window.setWindowAnimations(R.style.DialogAnimation);

        }
    }

    private void mostrarAlertDialogEstandar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Confirmación de Facturación")
                .setMessage("¿Está seguro que desea generar la factura?")
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Acción al hacer clic en Aceptar
                        // Ejemplo: generarFactura();
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}