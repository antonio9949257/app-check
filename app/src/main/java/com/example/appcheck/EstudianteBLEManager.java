package com.example.appcheck;

import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class EstudianteBLEManager {
    private static final String TAG = "EstudianteBLEManager";
    private static final long SCAN_PERIOD = 15000; // 15 segundos
    private static final UUID SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("0000A001-0000-1000-8000-00805F9B34FB");
    private static final UUID READ_CHARACTERISTIC_UUID = UUID.fromString("0000A002-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bleScanner;
    private final EstudianteBLEListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BluetoothGatt bluetoothGatt;
    private String datosEstudiante;
    private ScanCallback scanCallback;

    public interface EstudianteBLEListener {
        void onDeviceFound(DispositivoDocente dispositivo);
        void onScanStarted();
        void onScanStopped();
        void onConnectionSuccess(String codigoConfirmacion);
        void onConnectionFailed(String error);
        void onStatusChanged(String mensaje);
    }

    public EstudianteBLEManager(Context context, EstudianteBLEListener listener) {
        this.context = context;
        this.listener = listener;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bleScanner = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeScanner() : null;

        if (bleScanner == null) {
            Log.e(TAG, "No se pudo obtener BluetoothLeScanner");
        }
    }

    public void setDatosEstudiante(String nombres, String apellidoPaterno, String apellidoMaterno,
                                   String matricula, String cedulaIdentidad,
                                   String celular, String correo, String rol) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("nombres", nombres);
            jsonData.put("apellidoPaterno", apellidoPaterno);
            jsonData.put("apellidoMaterno", apellidoMaterno);
            jsonData.put("matricula", matricula);
            jsonData.put("cedulaIdentidad", cedulaIdentidad);
            jsonData.put("celular", celular);
            jsonData.put("correo", correo);
            jsonData.put("rol", rol); // Campo adicional para el rol

            this.datosEstudiante = jsonData.toString();
            Log.d(TAG, "Datos estudiante configurados: " + this.datosEstudiante);
        } catch (JSONException e) {
            Log.e(TAG, "Error formateando datos del estudiante", e);
            this.datosEstudiante = null;
            handler.post(() -> {
                listener.onConnectionFailed("Error en formato de datos");
                listener.onStatusChanged("Error en datos del estudiante");
            });
        }
    }

    public void startScanning() {
        if (bleScanner == null) {
            Log.e(TAG, "BluetoothLeScanner no disponible");
            handler.post(() -> {
                listener.onConnectionFailed("Bluetooth no disponible");
                listener.onStatusChanged("Bluetooth no disponible");
            });
            return;
        }

        // Detener cualquier escaneo previo
        stopScanning();

        // Notificar que el escaneo comienza
        handler.post(() -> {
            listener.onScanStarted();
            listener.onStatusChanged("Buscando dispositivos...");
        });

        // Configurar callback de escaneo
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                processScanResult(result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "Error en escaneo BLE: " + errorCode);
                handler.post(() -> {
                    listener.onConnectionFailed("Error en escaneo: " + errorCode);
                    listener.onScanStopped();
                });
            }
        };

        // Configurar filtro para el servicio específico
        try {
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                    .build();

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            Log.d(TAG, "Iniciando escaneo BLE...");
            bleScanner.startScan(List.of(filter), settings, scanCallback);

            // Programar parada del escaneo
            handler.postDelayed(this::stopScanning, SCAN_PERIOD);
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar escaneo BLE", e);
            handler.post(() -> {
                listener.onConnectionFailed("Error al iniciar escaneo");
                listener.onScanStopped();
            });
        }
    }

    private void processScanResult(ScanResult result) {
        if (result == null || result.getDevice() == null) return;

        BluetoothDevice device = result.getDevice();
        String deviceName = device.getName() != null ? device.getName() : "Dispositivo desconocido";
        String deviceAddress = device.getAddress();
        String nombreMateria = extraerNombreMateria(result);

        Log.d(TAG, "Dispositivo encontrado: " + deviceName + " (" + deviceAddress + ")");

        DispositivoDocente dispositivo = new DispositivoDocente(device, nombreMateria);

        handler.post(() -> {
            listener.onDeviceFound(dispositivo);
            listener.onStatusChanged("Dispositivo encontrado: " + deviceName);
        });
    }

    private String extraerNombreMateria(ScanResult result) {
        try {
            if (result.getScanRecord() != null && result.getScanRecord().getServiceData() != null) {
                byte[] serviceData = result.getScanRecord().getServiceData(new ParcelUuid(SERVICE_UUID));
                if (serviceData != null && serviceData.length > 0) {
                    return new String(serviceData, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al extraer nombre de materia", e);
        }
        return "Docente";
    }

    public void stopScanning() {
        if (bleScanner != null && scanCallback != null) {
            try {
                Log.d(TAG, "Deteniendo escaneo BLE");
                bleScanner.stopScan(scanCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error al detener escaneo BLE", e);
            } finally {
                scanCallback = null;
                handler.post(() -> {
                    listener.onScanStopped();
                    listener.onStatusChanged("Escaneo detenido");
                });
            }
        }
    }

    public void connectToDevice(DispositivoDocente dispositivo) {
        if (dispositivo == null || dispositivo.getDevice() == null) {
            handler.post(() -> {
                listener.onConnectionFailed("Dispositivo no válido");
                listener.onStatusChanged("Error: Dispositivo no válido");
            });
            return;
        }

        handler.post(() -> {
            listener.onStatusChanged("Conectando...");
        });

        // Cerrar conexión anterior si existe
        closeGatt();

        try {
            bluetoothGatt = dispositivo.getDevice().connectGatt(context, false, gattCallback);
            Log.d(TAG, "Intentando conectar a: " + dispositivo.getDevice().getName());
        } catch (Exception e) {
            Log.e(TAG, "Error al conectar", e);
            handler.post(() -> {
                listener.onConnectionFailed("Error al conectar");
                listener.onStatusChanged("Error de conexión");
            });
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange: " + newState + ", status: " + status);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                handler.post(() -> {
                    listener.onStatusChanged("Conectado, descubriendo servicios...");
                });
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handler.post(() -> {
                    listener.onConnectionFailed("Desconectado");
                    listener.onStatusChanged("Desconectado");
                });
                closeGatt();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    handler.post(() -> {
                        listener.onStatusChanged("Enviando datos...");
                    });
                    enviarDatosEstudiante(service);
                } else {
                    handler.post(() -> {
                        listener.onConnectionFailed("Servicio no encontrado");
                        listener.onStatusChanged("Error: Servicio no encontrado");
                    });
                    closeGatt();
                }
            } else {
                handler.post(() -> {
                    listener.onConnectionFailed("Error descubriendo servicios");
                    listener.onStatusChanged("Error descubriendo servicios");
                });
                closeGatt();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic readCharacteristic =
                        gatt.getService(SERVICE_UUID).getCharacteristic(READ_CHARACTERISTIC_UUID);

                if (readCharacteristic != null) {
                    gatt.readCharacteristic(readCharacteristic);
                } else {
                    handler.post(() -> {
                        listener.onConnectionFailed("Característica de lectura no encontrada");
                        listener.onStatusChanged("Error: Característica no encontrada");
                    });
                    closeGatt();
                }
            } else {
                handler.post(() -> {
                    listener.onConnectionFailed("Error enviando datos");
                    listener.onStatusChanged("Error enviando datos");
                });
                closeGatt();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getValue() != null) {
                String codigo = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                handler.post(() -> {
                    listener.onConnectionSuccess(codigo);
                    listener.onStatusChanged("Registro exitoso");
                });
            } else {
                handler.post(() -> {
                    listener.onConnectionFailed("Error leyendo confirmación");
                    listener.onStatusChanged("Error leyendo confirmación");
                });
            }
            closeGatt();
        }
    };

    private void enviarDatosEstudiante(BluetoothGattService service) {
        if (service == null || datosEstudiante == null) {
            handler.post(() -> {
                listener.onConnectionFailed("Datos no disponibles");
                listener.onStatusChanged("Error: Datos no disponibles");
            });
            closeGatt();
            return;
        }

        BluetoothGattCharacteristic writeCharacteristic =
                service.getCharacteristic(WRITE_CHARACTERISTIC_UUID);

        if (writeCharacteristic != null) {
            writeCharacteristic.setValue(datosEstudiante.getBytes(StandardCharsets.UTF_8));
            if (!bluetoothGatt.writeCharacteristic(writeCharacteristic)) {
                handler.post(() -> {
                    listener.onConnectionFailed("Error al escribir característica");
                    listener.onStatusChanged("Error al escribir datos");
                });
                closeGatt();
            }
        } else {
            handler.post(() -> {
                listener.onConnectionFailed("Característica de escritura no encontrada");
                listener.onStatusChanged("Error: Característica no encontrada");
            });
            closeGatt();
        }
    }

    private void closeGatt() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception e) {
                Log.e(TAG, "Error al cerrar conexión GATT", e);
            } finally {
                bluetoothGatt = null;
            }
        }
    }

    public void cleanup() {
        stopScanning();
        closeGatt();
        handler.removeCallbacksAndMessages(null);
    }
}