package com.chichikolon.xrunning.run.content.provider;

import android.database.Cursor;
import android.util.Log;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.chichikolon.xrunning.run.content.data.Track;
import com.chichikolon.xrunning.run.content.data.TrackPoint;

/**
 * A lightweight wrapper around the original {@link Cursor} with a method to clean up.
 */
public class TrackPointIterator implements Iterator<TrackPoint>, AutoCloseable {

    private static final String TAG = TrackPointIterator.class.getSimpleName();

    private final ContentProviderUtils contentProviderUtils;
    private final Track.Id trackId;
    private final boolean descending;
    private final CachedTrackPointsIndexes indexes;
    private long lastTrackPointId = -1L;
    private Cursor cursor;


    public TrackPointIterator(ContentProviderUtils contentProviderUtils, Track.Id trackId, long startTrackPointId, boolean descending) {
        this.contentProviderUtils = contentProviderUtils;
        this.trackId = trackId;
        this.descending = descending;

        cursor = getCursor(startTrackPointId);
        indexes = cursor != null ? new CachedTrackPointsIndexes(cursor)
                : null;
    }

    /**
     * Gets the track point cursor.
     *
     * @param trackPointId the starting track point id
     */
    private Cursor getCursor(long trackPointId) {
        return contentProviderUtils.getTrackPointCursor(trackId, trackPointId, contentProviderUtils.getDefaultCursorBatchSize(), descending);
    }

    /**
     * Advances the cursor to the next batch. Returns true if successful.
     */
    private boolean advanceCursorToNextBatch() {
        long trackPointId = lastTrackPointId == -1L ? -1L : lastTrackPointId + (descending ? -1 : 1);
        Log.d(TAG, "Advancing track point id: " + trackPointId);
        cursor.close();
        cursor = getCursor(trackPointId);
        return cursor != null;
    }

    public long getTrackPointId() {
        return lastTrackPointId;
    }

    @Override
    public boolean hasNext() {
        if (cursor == null) {
            return false;
        }
        if (cursor.isAfterLast()) {
            return false;
        }
        if (cursor.isLast()) {
            if (cursor.getCount() != contentProviderUtils.getDefaultCursorBatchSize()) {
                return false;
            }
            return advanceCursorToNextBatch() && !cursor.isAfterLast();
        }
        return true;
    }

    @Override
    public TrackPoint next() {
        if (cursor == null) {
            throw new NoSuchElementException();
        }
        if (!cursor.moveToNext()) {
            if (!advanceCursorToNextBatch() || !cursor.moveToNext()) {
                throw new NoSuchElementException();
            }
        }
        lastTrackPointId = cursor.getLong(indexes.idIndex);
        return ContentProviderUtils.fillTrackPoint(cursor, indexes);
    }

    @Override
    public void close() {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}