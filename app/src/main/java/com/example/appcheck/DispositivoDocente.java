package com.example.appcheck;

import android.bluetooth.BluetoothDevice;

public class DispositivoDocente {
    private final BluetoothDevice device;
    private final String nombreMateria;
    private final String idDispositivo;

    public DispositivoDocente(BluetoothDevice device, String nombreMateria) {
        this.device = device;
        this.nombreMateria = nombreMateria;
        this.idDispositivo = device != null ? device.getAddress() : "desconocido";
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getNombreMateria() {
        return nombreMateria;
    }

    public String getIdDispositivo() {
        return idDispositivo;
    }

}