/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.chichikolon.xrunning.run.services.sensors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import com.chichikolon.xrunning.run.content.sensor.SensorData;
import com.chichikolon.xrunning.run.content.sensor.SensorDataCycling;
import com.chichikolon.xrunning.run.content.sensor.SensorDataCyclingPower;
import com.chichikolon.xrunning.run.content.sensor.SensorDataHeartRate;
import com.chichikolon.xrunning.run.util.BluetoothUtils;

/**
 * Manages connection to a Bluetooth LE sensor and subscribes for onChange-notifications.
 * Also parses the transferred data into {@link SensorDataObserver}.
 */
public abstract class BluetoothConnectionManager {

    private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

    private final SensorDataObserver observer;

    private final UUID serviceUUUID;
    private final UUID measurementUUID;
    private BluetoothGatt bluetoothGatt;

    private final BluetoothGattCallback connectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING:
                    Log.d(TAG, "Connecting to sensor: " + gatt.getDevice());
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "Connected to sensor: " + gatt.getDevice());

                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.d(TAG, "Disconnecting from sensor: " + gatt.getDevice());
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "Disconnected from sensor: " + gatt.getDevice());
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(serviceUUUID);
            if (service == null) {
                Log.e(TAG, "Could not get service for address=" + gatt.getDevice().getAddress() + " serviceUUID=" + serviceUUUID);
                return;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(measurementUUID);
            if (characteristic == null) {
                Log.e(TAG, "Could not get BluetoothCharacteristic for address=" + gatt.getDevice().getAddress() + " serviceUUID=" + serviceUUUID + " characteristicUUID=" + measurementUUID);
                return;
            }
            gatt.setCharacteristicNotification(characteristic, true);

            // Register for updates.
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BluetoothUtils.CLIENT_CHARACTERISTIC_CONFIG_UUID);
            if (descriptor == null) {
                Log.e(TAG, "CLIENT_CHARACTERISTIC_CONFIG_UUID characteristic not available; cannot request notifications for changed data.");
                return;
            }

            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            String sensorName = gatt.getDevice().getName();
            Log.d(TAG, "Received data from " + sensorName);

            SensorData sensorData = parsePayload(sensorName, gatt.getDevice().getAddress(), characteristic);
            if (sensorData != null) {
                observer.onChanged(sensorData);
            }
        }
    };

    BluetoothConnectionManager(UUID serviceUUUID, UUID measurementUUID, SensorDataObserver observer) {
        this.serviceUUUID = serviceUUUID;
        this.measurementUUID = measurementUUID;
        this.observer = observer;
    }

    synchronized void connect(Context context, @NonNull BluetoothDevice device) {
        if (bluetoothGatt != null) {
            Log.w(TAG, "Already connected; ignoring.");
        }

        Log.d(TAG, "Connecting to: " + device);

        bluetoothGatt = device.connectGatt(context, true, connectCallback);
        SensorData sensorData = createEmptySensorData(bluetoothGatt.getDevice().getAddress());
        observer.onChanged(sensorData);
    }

    synchronized void disconnect() {
        if (bluetoothGatt == null) {
            Log.w(TAG, "Cannot disconnect if not connected.");
            return;
        }
        bluetoothGatt.close();
        observer.onDisconnecting(createEmptySensorData(bluetoothGatt.getDevice().getAddress()));

        bluetoothGatt = null;
    }

    synchronized boolean isSameBluetoothDevice(String address) {
        if (bluetoothGatt == null) {
            return false;
        }

        return address.equals(bluetoothGatt.getDevice().getAddress());
    }

    protected abstract SensorData createEmptySensorData(String address);

    /**
     * @return null if data could not be parsed.
     */
    protected abstract SensorData parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic);

    public static class HeartRate extends BluetoothConnectionManager {

        HeartRate(@NonNull SensorDataObserver observer) {
            super(BluetoothUtils.HEART_RATE_SERVICE_UUID, BluetoothUtils.HEART_RATE_MEASUREMENT_CHAR_UUID, observer);
        }

        @Override
        protected SensorData createEmptySensorData(String address) {
            return new SensorDataHeartRate(address);
        }

        @Override
        protected SensorDataHeartRate parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
            Integer heartRate = BluetoothUtils.parseHeartRate(characteristic);

            return heartRate != null ? new SensorDataHeartRate(address, sensorName, heartRate) : null;
        }
    }

    public static class CyclingCadence extends BluetoothConnectionManager {

        CyclingCadence(SensorDataObserver observer) {
            super(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, BluetoothUtils.CYCLING_SPEED_CADENCE_MEASUREMENT_CHAR_UUID, observer);
        }

        @Override
        protected SensorData createEmptySensorData(String address) {
            return new SensorDataCycling.Cadence(address);
        }

        @Override
        protected SensorDataCycling.Cadence parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
            SensorDataCycling.CadenceAndSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
            if (cadenceAndSpeed == null) {
                return null;
            }

            if (cadenceAndSpeed.getCadence() != null) {
                return cadenceAndSpeed.getCadence();
            }

            //Workaround for Wahoo CADENCE: this sensor reports speed (instead of cadence)
            if (cadenceAndSpeed.getSpeed() != null) {
                return new SensorDataCycling.Cadence(cadenceAndSpeed.getSpeed());
            }

            return null;
        }
    }

    public static class CyclingSpeed extends BluetoothConnectionManager {

        CyclingSpeed(SensorDataObserver observer) {
            super(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, BluetoothUtils.CYCLING_SPEED_CADENCE_MEASUREMENT_CHAR_UUID, observer);
        }

        @Override
        protected SensorData createEmptySensorData(String address) {
            return new SensorDataCycling.Speed(address);
        }

        @Override
        protected SensorDataCycling.Speed parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
            SensorDataCycling.CadenceAndSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
            if (cadenceAndSpeed != null) {
                return cadenceAndSpeed.getSpeed();
            }
            return null;
        }
    }

    public static class CyclingPower extends BluetoothConnectionManager {

        CyclingPower(@NonNull SensorDataObserver observer) {
            super(BluetoothUtils.CYCLING_POWER_UUID, BluetoothUtils.CYCLING_POWER_MEASUREMENT_CHAR_UUID, observer);
        }

        @Override
        protected SensorData createEmptySensorData(String address) {
            return new SensorDataCyclingPower(address);
        }

        @Override
        protected SensorDataCyclingPower parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
            Integer cyclingPower = BluetoothUtils.parseCyclingPower(characteristic);

            return cyclingPower != null ? new SensorDataCyclingPower(address, sensorName, cyclingPower) : null;
        }
    }

    interface SensorDataObserver {

        void onChanged(SensorData sensorData);

        void onDisconnecting(SensorData sensorData);
    }
}
