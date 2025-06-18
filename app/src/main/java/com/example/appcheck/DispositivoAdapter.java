package com.example.appcheck;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DispositivoAdapter extends RecyclerView.Adapter<DispositivoAdapter.ViewHolder> {

    public interface OnDispositivoClickListener {
        void onDispositivoClick(DispositivoDocente dispositivo);
    }

    private final List<DispositivoDocente> dispositivos;
    private final OnDispositivoClickListener listener;

    public DispositivoAdapter(List<DispositivoDocente> dispositivos, OnDispositivoClickListener listener) {
        this.dispositivos = dispositivos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dispositivo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DispositivoDocente dispositivo = dispositivos.get(position);
        holder.bind(dispositivo, listener);
    }

    @Override
    public int getItemCount() {
        return dispositivos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;
        private final TextView tvId;
        private final CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreDispositivo);
            tvId = itemView.findViewById(R.id.tvIdDispositivo);
            cardView = (CardView) itemView;
        }

        public void bind(DispositivoDocente dispositivo, OnDispositivoClickListener listener) {
            tvNombre.setText(dispositivo.getNombreMateria());
            tvId.setText("ID: " + dispositivo.getIdDispositivo());

            itemView.setOnClickListener(v -> {
                listener.onDispositivoClick(dispositivo);
                cardView.setCardBackgroundColor(
                        itemView.getContext().getResources().getColor(R.color.blue));
            });
        }
    }
}