package com.example.appcheck;

import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BluetoothServiceManager {
    private static final String TAG = "BLE_SERVICE";

    // UUIDs constantes
    private static final UUID SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("0000A001-0000-1000-8000-00805F9B34FB");
    private static final UUID READ_CHARACTERISTIC_UUID = UUID.fromString("0000A002-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;

    private BluetoothLeAdvertiser bleAdvertiser;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGattService bleService;

    private String currentMateriaId;
    private final Map<String, String> codigosGenerados = new HashMap<>();

    // NUEVO: Mapa para asociar dispositivos con sus códigos
    private final Map<BluetoothDevice, String> deviceCodes = new ConcurrentHashMap<>();
    private final Map<BluetoothDevice, String> deviceStudentData = new ConcurrentHashMap<>();

    private BluetoothEventListener eventListener;

    public interface BluetoothEventListener {
        void onStudentRegistered(String studentData);
        void onAdvertisingStarted();
        void onAdvertisingFailed(String error);
        void onServiceStarted();
        void onServiceStopped();
    }

    public BluetoothServiceManager(Context context, BluetoothEventListener listener) {
        this.context = context;
        this.eventListener = listener;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public boolean isRunning() {
        return bleAdvertiser != null && bluetoothGattServer != null;
    }

    public void startBLEService(String materiaId) {
        this.currentMateriaId = materiaId;

        Log.d(TAG, "Iniciando servicio BLE para materia: " + materiaId);

        try {
            // 1. Inicializar GATT Server
            bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback);
            if (bluetoothGattServer == null) {
                throw new Exception("No se pudo crear GATT Server");
            }

            // 2. Crear servicio BLE con UUID fijo
            bleService = new BluetoothGattService(SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            // 3. Agregar características
            // Característica de escritura (para recibir datos del estudiante)
            BluetoothGattCharacteristic writeChar = new BluetoothGattCharacteristic(
                    WRITE_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);

            // Característica de lectura (para enviar código de confirmación)
            BluetoothGattCharacteristic readChar = new BluetoothGattCharacteristic(
                    READ_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);

            bleService.addCharacteristic(writeChar);
            bleService.addCharacteristic(readChar);

            // 4. Agregar servicio al GATT Server
            if (!bluetoothGattServer.addService(bleService)) {
                throw new Exception("No se pudo agregar el servicio BLE");
            }

            // 5. Configurar advertising
            bleAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (bleAdvertiser == null) {
                throw new Exception("Advertising BLE no soportado");
            }

            // Configuración del advertising
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                    .setConnectable(true)
                    .setTimeout(0) // Advertising indefinido
                    .build();

            // Datos cortos de la materia (para scan response)
            String corta = (materiaId != null && materiaId.length() >= 8)
                    ? materiaId.substring(0, 8) : (materiaId != null ? materiaId : "DOCENTE");
            byte[] serviceData = corta.getBytes(StandardCharsets.UTF_8);

            // Datos principales del advertising (solo UUID)
            AdvertiseData advData = new AdvertiseData.Builder()
                    .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                    .setIncludeDeviceName(false)
                    .build();

            // Datos adicionales en scan response (nombre de materia)
            AdvertiseData scanResp = new AdvertiseData.Builder()
                    .addServiceData(new ParcelUuid(SERVICE_UUID), serviceData)
                    .setIncludeDeviceName(true)
                    .build();

            // 6. Iniciar advertising
            bleAdvertiser.startAdvertising(settings, advData, scanResp, advertiseCallback);
            Log.d(TAG, "Advertising iniciado para: " + materiaId);

            if (eventListener != null) {
                eventListener.onServiceStarted();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error starting BLE service", e);
            cleanup();
            if (eventListener != null) {
                eventListener.onAdvertisingFailed(e.getMessage());
            }
        }
    }

    public void stopBLEService() {
        Log.d(TAG, "Deteniendo servicio BLE");

        try {
            if (bleAdvertiser != null) {
                bleAdvertiser.stopAdvertising(advertiseCallback);
                bleAdvertiser = null;
            }

            if (bluetoothGattServer != null) {
                bluetoothGattServer.close();
                bluetoothGattServer = null;
            }

            // Limpiar mapas
            deviceCodes.clear();
            deviceStudentData.clear();
            codigosGenerados.clear();

            if (eventListener != null) {
                eventListener.onServiceStopped();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error stopping BLE service", e);
        }
    }

    public String generateUniqueCode(String matricula, BluetoothDevice device) {
        try {
            String salt = UUID.randomUUID().toString().substring(0, 8);
            long timestamp = System.currentTimeMillis();
            String deviceId = device.getAddress();
            String raw = matricula + "|" + currentMateriaId + "|" + timestamp + "|" + salt + "|" + deviceId;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            String code = bytesToHex(hash).substring(0, 12).toUpperCase();

            Log.d(TAG, "Código generado para " + matricula + ": " + code);
            return code;

        } catch (Exception e) {
            Log.e(TAG, "Error generando código único", e);
            return UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        }
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "Advertising iniciado exitosamente");
            if (eventListener != null) {
                eventListener.onAdvertisingStarted();
            }
        }

        @Override
        public void onStartFailure(int errorCode) {
            String errorMsg = "Error BLE: ";
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errorMsg += "Datos demasiado grandes";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errorMsg += "Demasiados anuncios activos";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    errorMsg += "Ya está activo";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    errorMsg += "Error interno";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errorMsg += "No soportado";
                    break;
                default:
                    errorMsg += "Código desconocido: " + errorCode;
            }
            Log.e(TAG, errorMsg);

            if (eventListener != null) {
                eventListener.onAdvertisingFailed(errorMsg);
            }
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            String deviceInfo = device.getAddress() + " (" + (device.getName() != null ? device.getName() : "Sin nombre") + ")";

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Dispositivo conectado: " + deviceInfo);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Dispositivo desconectado: " + deviceInfo);
                // Limpiar datos del dispositivo al desconectarse
                deviceCodes.remove(device);
                deviceStudentData.remove(device);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {

            Log.d(TAG, "onCharacteristicWriteRequest de: " + device.getAddress());

            // Verificar que es la característica de escritura correcta
            if (!characteristic.getUuid().equals(WRITE_CHARACTERISTIC_UUID)) {
                Log.w(TAG, "Escritura en característica incorrecta: " + characteristic.getUuid());
                if (bluetoothGattServer != null && responseNeeded) {
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_FAILURE, offset, null);
                }
                return;
            }

            if (value == null || value.length == 0) {
                Log.w(TAG, "Datos vacíos recibidos");
                if (bluetoothGattServer != null && responseNeeded) {
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_FAILURE, offset, null);
                }
                return;
            }

            try {
                String studentData = new String(value, StandardCharsets.UTF_8);
                Log.d(TAG, "Datos del estudiante recibidos: " + studentData);

                // Extraer matrícula para generar código
                String matricula = extractMatricula(studentData);
                if (matricula == null || matricula.isEmpty()) {
                    Log.w(TAG, "No se pudo extraer matrícula de los datos");
                    if (bluetoothGattServer != null && responseNeeded) {
                        bluetoothGattServer.sendResponse(device, requestId,
                                BluetoothGatt.GATT_FAILURE, offset, null);
                    }
                    return;
                }

                // Generar código único para este dispositivo/estudiante
                String code = generateUniqueCode(matricula, device);

                // Almacenar datos asociados al dispositivo
                deviceCodes.put(device, code);
                deviceStudentData.put(device, studentData);
                codigosGenerados.put(matricula, code);

                // Enviar respuesta exitosa
                if (bluetoothGattServer != null && responseNeeded) {
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_SUCCESS, offset, null);
                }

                // Notificar a la UI sobre el nuevo registro
                if (eventListener != null) {
                    eventListener.onStudentRegistered(studentData);
                }

                Log.d(TAG, "Estudiante registrado exitosamente: " + matricula + " -> " + code);

            } catch (Exception e) {
                Log.e(TAG, "Error procesando datos del estudiante", e);
                if (bluetoothGattServer != null && responseNeeded) {
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_FAILURE, offset, null);
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattCharacteristic characteristic) {

            Log.d(TAG, "onCharacteristicReadRequest de: " + device.getAddress());

            if (characteristic.getUuid().equals(READ_CHARACTERISTIC_UUID)) {
                // CORREGIDO: Enviar el código específico para este dispositivo
                String response = deviceCodes.get(device);

                if (response != null) {
                    Log.d(TAG, "Enviando código de confirmación: " + response);
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_SUCCESS, offset,
                            response.getBytes(StandardCharsets.UTF_8));

                    // Opcional: remover el código después de enviarlo (uncomment si se desea)
                    // deviceCodes.remove(device);
                } else {
                    Log.w(TAG, "No hay código disponible para el dispositivo: " + device.getAddress());
                    String errorResponse = "NO_CODE";
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_SUCCESS, offset,
                            errorResponse.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                Log.w(TAG, "Lectura de característica incorrecta: " + characteristic.getUuid());
                bluetoothGattServer.sendResponse(device, requestId,
                        BluetoothGatt.GATT_FAILURE, offset, null);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Servicio BLE agregado exitosamente");
            } else {
                Log.e(TAG, "Error agregando servicio BLE: " + status);
            }
        }
    };

    private String extractMatricula(String studentData) {
        try {
            JSONObject json = new JSONObject(studentData);
            return json.optString("matricula", null);
        } catch (Exception e) {
            Log.e(TAG, "Error extrayendo matrícula", e);
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // Método para obtener códigos generados (útil para debugging)
    public Map<String, String> getCodigosGenerados() {
        return new HashMap<>(codigosGenerados);
    }

    // Método para obtener el número de dispositivos conectados
    public int getConnectedDevicesCount() {
        return deviceCodes.size();
    }

    public void cleanup() {
        stopBLEService();
        eventListener = null;
    }
}