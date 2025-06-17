// DispositivoDocente.java
package com.example.appcheck;

import android.bluetooth.BluetoothDevice;

public class DispositivoDocente {
    private BluetoothDevice device;
    private String nombreMateria;
    private String idDispositivo;

    public DispositivoDocente(BluetoothDevice device, String nombreMateria) {
        this.device = device;
        this.nombreMateria = nombreMateria;
        this.idDispositivo = device.getAddress();
    }

    // Getters
    public BluetoothDevice getDevice() { return device; }
    public String getNombreMateria() { return nombreMateria; }
    public String getIdDispositivo() { return idDispositivo; }
}