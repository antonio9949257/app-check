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

public class BluetoothServiceManager {
    private static final String MATERIA_UUID_BASE = "0000%s-0000-1000-8000-00805F9B34FB";

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;

    private BluetoothLeAdvertiser bleAdvertiser;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGattService bleService;

    private String currentMateriaId;
    private final Map<String, String> codigosGenerados = new HashMap<>();
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
        return bleAdvertiser != null;
    }
    public void startBLEService(String materiaId) {
        this.currentMateriaId = materiaId;

        try {
            // 1) Abre GATT y crea servicio (igual que tenías antes):
            bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback);
            String serviceUuid = String.format(MATERIA_UUID_BASE, materiaId.hashCode() & 0xFFFF);
            ParcelUuid pUuid = new ParcelUuid(UUID.fromString(serviceUuid));
            bleService = new BluetoothGattService(UUID.fromString(serviceUuid),
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);
            // agrega características...
            bluetoothGattServer.addService(bleService);

            // 2) Configurar advertising
            bleAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                    .setConnectable(true)
                    .build();

            // 3) Datos cortos
            String corta = (materiaId != null && materiaId.length() >= 8)
                    ? materiaId.substring(0, 8)
                    : (materiaId != null ? materiaId : "");
            byte[] serviceData = corta.getBytes(StandardCharsets.UTF_8);

            // 4) Advertising principal: solo UUID
            AdvertiseData advData = new AdvertiseData.Builder()
                    .addServiceUuid(pUuid)
                    .build();

            // 5) Scan Response: tu código
            AdvertiseData scanResp = new AdvertiseData.Builder()
                    .addServiceData(pUuid, serviceData)
                    .build();

            // 6) Arranca advertising con ambos
            bleAdvertiser.startAdvertising(settings, advData, scanResp, advertiseCallback);
            eventListener.onServiceStarted();

        } catch (Exception e) {
            Log.e("BLE_SERVICE", "Error starting BLE service", e);
            eventListener.onAdvertisingFailed(e.getMessage());
        }
    }

    public void stopBLEService() {
        try {
            if (bleAdvertiser != null) {
                bleAdvertiser.stopAdvertising(advertiseCallback);
                bleAdvertiser = null;
            }

            if (bluetoothGattServer != null) {
                bluetoothGattServer.close();
                bluetoothGattServer = null;
            }

            eventListener.onServiceStopped();
        } catch (Exception e) {
            Log.e("BLE_SERVICE", "Error stopping BLE service", e);
        }
    }

    public String generateUniqueCode(String matricula) {
        String salt = UUID.randomUUID().toString().substring(0, 8);
        long timestamp = System.currentTimeMillis();
        String raw = matricula + "|" + timestamp + "|" + salt;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 8);
        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            eventListener.onAdvertisingStarted();
        }

        @Override
        public void onStartFailure(int errorCode) {
            String errorMsg = "Error: ";
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
            eventListener.onAdvertisingFailed(errorMsg);
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d("BLE_GATT", "Connection state changed: " + newState);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            if (value == null || value.length == 0) {
                if (bluetoothGattServer != null) {
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_FAILURE, offset, null);
                }
                return;
            }

            try {
                String studentData = new String(value, StandardCharsets.UTF_8);
                String matricula = extractMatricula(studentData);
                String code = generateUniqueCode(matricula);
                codigosGenerados.put(matricula, code);

                if (bluetoothGattServer != null) {
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_SUCCESS, offset, code.getBytes());
                }

                eventListener.onStudentRegistered(studentData);
            } catch (Exception e) {
                Log.e("BLE_GATT", "Error processing data", e);
                if (bluetoothGattServer != null) {
                    bluetoothGattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_FAILURE, offset, null);
                }
            }
        }
    };

    private String extractMatricula(String studentData) {
        try {
            JSONObject json = new JSONObject(studentData);
            return json.getString("matricula");
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public void cleanup() {
        stopBLEService();
        eventListener = null;
    }
}