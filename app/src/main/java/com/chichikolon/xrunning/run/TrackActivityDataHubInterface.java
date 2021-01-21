package com.chichikolon.xrunning.run;

import com.chichikolon.xrunning.run.content.TrackDataHub;

/**
 * Interface for communication between activities that use {@link com.chichikolon.xrunning.run.content.TrackDataHub} and their fragments that need thi data hub.
 */
public interface TrackActivityDataHubInterface {
    TrackDataHub getTrackDataHub();
}
