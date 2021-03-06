package com.chichikolon.xrunning.run.services;

import androidx.annotation.VisibleForTesting;

import com.chichikolon.xrunning.run.content.data.Marker;
import com.chichikolon.xrunning.run.content.data.Track;
import com.chichikolon.xrunning.run.content.data.TrackPoint;
import com.chichikolon.xrunning.run.content.sensor.SensorDataSet;
import com.chichikolon.xrunning.run.services.handlers.GpsStatusValue;
import com.chichikolon.xrunning.run.services.sensors.BluetoothRemoteSensorManager;

/**
 * TODO: There is a bug in Android that leaks Binder instances. This bug is
 * especially visible if we have a non-static class, as there is no way to
 * nullify reference to the outer class (the service). A workaround is to use
 * a static class and explicitly clear service and detach it from the
 * underlying Binder. With this approach, we minimize the leak to 24 bytes per
 * each service instance. For more details, see the following bug:
 * http://code.google.com/p/android/issues/detail?id=6426.
 */
class TrackRecordingServiceBinder extends android.os.Binder implements TrackRecordingServiceInterface {
    private TrackRecordingService trackRecordingService;

    TrackRecordingServiceBinder(TrackRecordingService trackRecordingService) {
        this.trackRecordingService = trackRecordingService;
    }

    @Override
    public void addListener(TrackRecordingServiceCallback listener) {
        trackRecordingService.addListener(listener);
    }

    @Override
    public GpsStatusValue getGpsStatus() {
        return trackRecordingService.getGpsStatus();
    }

    @Override
    public void startGps() {
        trackRecordingService.tryStartGps();
    }

    public void stopGps() {
        trackRecordingService.stopGps(true);
    }

    @Override
    public Track.Id startNewTrack() {
        return trackRecordingService.startNewTrack();
    }

    @Override
    public void resumeTrack(Track.Id trackId) {
        trackRecordingService.resumeTrack(trackId);
    }

    @Override
    public void pauseCurrentTrack() {
        trackRecordingService.pauseCurrentTrack();
    }

    @Override
    public void resumeCurrentTrack() {
        trackRecordingService.resumeCurrentTrack();
    }

    @Override
    public void endCurrentTrack() {
        trackRecordingService.endCurrentTrack();
    }

    @Override
    public boolean isRecording() {
        return trackRecordingService.isRecording();
    }

    @Override
    public boolean isPaused() {
        return trackRecordingService.isPaused();
    }

    @Override
    public Track.Id getRecordingTrackId() {
        return trackRecordingService.getRecordingTrackId();
    }

    @Override
    public long getTotalTime() {
        return trackRecordingService.getTotalTime();
    }

    @Override
    public Marker.Id insertMarker(String name, String category, String description, String photoUrl) {
        return trackRecordingService.insertMarker(name, category, description, photoUrl);
    }

    @Override
    public SensorDataSet getSensorData() {
        return trackRecordingService.getSensorDataSet();
    }

    @Override
    public Float getElevationGain_m() {
        return trackRecordingService.getElevationGain_m();
    }

    @Override
    public Float getElevationLoss_m() {
        return trackRecordingService.getElevationLoss_m();
    }

    @VisibleForTesting
    @Override
    public void setRemoteSensorManager(BluetoothRemoteSensorManager remoteSensorManager) {
        trackRecordingService.setRemoteSensorManager(remoteSensorManager);
    }

    /**
     * Detaches from the track recording service. Clears the reference to the
     * outer class to minimize the leak.
     */
    void detachFromService() {
        trackRecordingService = null;
    }

    @VisibleForTesting
    @Override
    public void newTrackPoint(TrackPoint trackPoint, int recordingGpsAccuracy) {
        trackRecordingService.newTrackPoint(trackPoint, recordingGpsAccuracy);
    }
}
