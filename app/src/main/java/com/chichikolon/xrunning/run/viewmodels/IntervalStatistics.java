package com.chichikolon.xrunning.run.viewmodels;

import java.util.ArrayList;
import java.util.List;

import com.chichikolon.xrunning.run.content.data.TrackPoint;
import com.chichikolon.xrunning.run.util.LocationUtils;
import com.chichikolon.xrunning.run.util.UnitConversions;

public class IntervalStatistics {
    private final List<Interval> intervalList = new ArrayList<>();
    private final float distanceInterval_m;

    /**
     * @param trackPoints        the list of TrackPoint.
     * @param distanceInterval_m the meters of every interval.
     */
    public IntervalStatistics(List<TrackPoint> trackPoints, float distanceInterval_m) {
        intervalList.clear();
        this.distanceInterval_m = distanceInterval_m;

        if (trackPoints == null || trackPoints.size() == 0) {
            return;
        }

        Interval interval = new Interval();
        interval.gain_m += trackPoints.get(0).hasElevationGain() ? trackPoints.get(0).getElevationGain() : 0;
        interval.loss_m += trackPoints.get(0).hasElevationLoss() ? trackPoints.get(0).getElevationLoss() : 0;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint prevTrackPoint = trackPoints.get(i - 1);
            TrackPoint trackPoint = trackPoints.get(i);

            if (LocationUtils.isValidLocation(trackPoint.getLocation()) && LocationUtils.isValidLocation(prevTrackPoint.getLocation())) {
                interval.distance_m += prevTrackPoint.distanceTo(trackPoint);
                interval.time_ms += trackPoint.getTime() - prevTrackPoint.getTime();
                interval.gain_m += trackPoint.hasElevationGain() ? trackPoint.getElevationGain() : 0;
                interval.loss_m += trackPoint.hasElevationLoss() ? trackPoint.getElevationLoss() : 0;

                if (interval.distance_m >= distanceInterval_m) {
                    float adjustFactor = distanceInterval_m / interval.distance_m;
                    Interval adjustedInterval = new Interval(interval);
                    adjustedInterval.adjust(adjustFactor);

                    intervalList.add(adjustedInterval);

                    interval = new Interval(interval.distance_m - adjustedInterval.distance_m, interval.time_ms - adjustedInterval.time_ms);
                }
            }
        }

        if (interval.distance_m > 1f) {
            intervalList.add(interval);
        }
    }


    public List<Interval> getIntervalList() {
        return intervalList;
    }

    /**
     * Return the last completed interval.
     * An interval is complete if its distance is equal to distanceInterval_m.
     *
     * @return the interval object or null if any interval is completed.
     */
    public Interval getLastInterval() {
        if (intervalList.size() == 1 && intervalList.get(0).getDistance_m() < distanceInterval_m) {
            return null;
        }

        for (int i = intervalList.size() - 1; i >= 0; i--) {
            if (intervalList.get(i).getDistance_m() >= distanceInterval_m) {
                return this.intervalList.get(i);
            }
        }

        return null;
    }

    public static class Interval {
        private float distance_m = 0f;
        private float time_ms = 0f;
        private float gain_m = 0f;
        private float loss_m = 0f;

        public Interval() {}

        public Interval(float distance_m, float time_ms) {
            this.distance_m = distance_m;
            this.time_ms = time_ms;
            this.gain_m = 0f;
            this.loss_m = 0f;
        }

        public Interval(Interval i) {
            distance_m = i.distance_m;
            time_ms = i.time_ms;
            gain_m = i.gain_m;
            loss_m = i.loss_m;
        }

        public float getDistance_m() {
            return distance_m;
        }

        public void adjust(float adjustFactor) {
            distance_m *= adjustFactor;
            time_ms *= adjustFactor;
        }

        /**
         * @return speed of the interval in m/s.
         */
        public float getSpeed_ms() {
            if (distance_m == 0f) {
                return 0f;
            }
            return distance_m / (float) (time_ms * UnitConversions.MS_TO_S);
        }

        public float getGain_m() {
            return gain_m;
        }

        public float getLoss_m() {
            return loss_m;
        }
    }
}
