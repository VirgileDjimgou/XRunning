package com.chichikolon.xrunning.run.services;

import de.dennisguse.opentracks.services.handlers.GpsStatusValue;

/**
 * Interface all activities have to implements to receive information from the service.
 */
public interface TrackRecordingServiceCallback {
    void onGpsStatusChange(GpsStatusValue newStatus);
}
