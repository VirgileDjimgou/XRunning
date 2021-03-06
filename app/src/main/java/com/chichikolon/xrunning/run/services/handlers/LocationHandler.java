package com.chichikolon.xrunning.run.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.chichikolon.xrunning.R;
import com.chichikolon.xrunning.run.content.data.TrackPoint;
import com.chichikolon.xrunning.run.util.LocationUtils;
import com.chichikolon.xrunning.run.util.PreferencesUtils;
import com.chichikolon.xrunning.run.util.TrackPointUtils;
import com.chichikolon.xrunning.run.util.UnitConversions;

class LocationHandler implements HandlerServer.Handler, LocationListener, GpsStatus.GpsStatusListener {

    private final String TAG = LocationHandler.class.getSimpleName();

    private LocationManager locationManager;
    private final HandlerServer handlerServer;
    private GpsStatus gpsStatus;
    private LocationListenerPolicy locationListenerPolicy;
    private long currentRecordingInterval;
    private int recordingGpsAccuracy;
    private TrackPoint lastValidTrackPoint;

    public LocationHandler(HandlerServer handlerServer) {
        this.handlerServer = handlerServer;
    }

    @Override
    public void onStart(Context context) {
        gpsStatus = new GpsStatus(context, this, PreferencesUtils.getMinRecordingInterval(context));
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        registerLocationListener();
    }

    @Override
    public void onStop(Context context) {
        unregisterLocationListener();
        locationManager = null;
        if (gpsStatus != null) {
            gpsStatus.stop();
            gpsStatus = null;
        }
    }

    @Override
    public void onSharedPreferenceChanged(Context context, SharedPreferences preferences, String key) {
        if (PreferencesUtils.isKey(context, R.string.min_recording_interval_key, key)) {
            int minRecordingInterval = PreferencesUtils.getMinRecordingInterval(context);
            if (minRecordingInterval == PreferencesUtils.getMinRecordingIntervalAdaptBatteryLife(context)) {
                // Choose battery life over moving time accuracy.
                locationListenerPolicy = new AdaptiveLocationListenerPolicy(30 * UnitConversions.ONE_SECOND_MS, 5 * UnitConversions.ONE_MINUTE_MS, 5);
            } else if (minRecordingInterval == PreferencesUtils.getMinRecordingIntervalAdaptAccuracy(context)) {
                // Get all the updates.
                locationListenerPolicy = new AdaptiveLocationListenerPolicy(UnitConversions.ONE_SECOND_MS, 30 * UnitConversions.ONE_SECOND_MS, 0);
            } else {
                locationListenerPolicy = new AbsoluteLocationListenerPolicy(minRecordingInterval * UnitConversions.ONE_SECOND_MS);
            }

            if (locationManager != null) {
                registerLocationListener();
            }
        }
        if (PreferencesUtils.isKey(context, R.string.recording_gps_accuracy_key, key)) {
            recordingGpsAccuracy = PreferencesUtils.getRecordingGPSAccuracy(context);
        }
        if (PreferencesUtils.isKey(context, R.string.min_recording_interval_key, key)) {
            if (gpsStatus != null) {
                gpsStatus.onMinRecordingIntervalChanged(PreferencesUtils.getMinRecordingInterval(context));
            }
        }
        if (PreferencesUtils.isKey(context, R.string.recording_distance_interval_key, key)) {
            if (gpsStatus != null) {
                gpsStatus.onRecordingDistanceChanged(PreferencesUtils.getRecordingDistanceInterval(context));
            }
        }
    }

    /**
     * Checks if location is valid and builds a track point that will be send through HandlerServer.
     *
     * @param location {@link Location} object.
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (gpsStatus != null) {
            gpsStatus.onLocationChanged(location);
        }

        if (!LocationUtils.isValidLocation(location)) {
            Log.w(TAG, "Ignore newTrackPoint. location is invalid.");
            return;
        }

        TrackPoint trackPoint = new TrackPoint(location);

        if (!TrackPointUtils.fulfillsAccuracy(trackPoint, recordingGpsAccuracy)) {
            Log.d(TAG, "Ignore newTrackPoint. Poor accuracy.");
            return;
        }

        long idleTime = 0L;
        if (TrackPointUtils.after(trackPoint, lastValidTrackPoint)) {
            idleTime = trackPoint.getTime() - lastValidTrackPoint.getTime();
        }

        locationListenerPolicy.updateIdleTime(idleTime);
        if (currentRecordingInterval != locationListenerPolicy.getDesiredPollingInterval()) {
            registerLocationListener();
        }

        lastValidTrackPoint = trackPoint;
        handlerServer.sendTrackPoint(trackPoint, recordingGpsAccuracy);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        if (gpsStatus != null) {
            gpsStatus.onGpsEnabled();
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (gpsStatus != null) {
            gpsStatus.onGpsDisabled();
        }
    }

    private void registerLocationListener() {
        if (locationManager == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        try {
            long interval = locationListenerPolicy.getDesiredPollingInterval();
            currentRecordingInterval = interval;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, locationListenerPolicy.getMinDistance_m(), this);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not register location listener; permissions not granted.", e);
        }
    }

    private void unregisterLocationListener() {
        if (locationManager == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        locationManager.removeUpdates(this);
        locationManager = null;
    }

    /**
     * Called from {@link GpsStatus} to inform that GPS status has changed from prevStatus to currentStatus.
     *
     * @param prevStatus    previous {@link GpsStatusValue}.
     * @param currentStatus current {@link GpsStatusValue}.
     */
    @Override
    public void onGpsStatusChanged(GpsStatusValue prevStatus, GpsStatusValue currentStatus) {
        handlerServer.sendGpsStatus(currentStatus);
    }

    public GpsStatusValue getGpsStatus() {
        return gpsStatus != null ? gpsStatus.getGpsStatus() : GpsStatusValue.GPS_NONE;
    }
}
