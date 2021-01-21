package com.chichikolon.xrunning.run.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import com.chichikolon.xrunning.run.content.data.Track;
import com.chichikolon.xrunning.run.content.provider.ContentProviderUtils;

public class AggregatedStatisticsModel extends AndroidViewModel {

    private MutableLiveData<AggregatedStatistics> aggregatedStats;

    public AggregatedStatisticsModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<AggregatedStatistics> getAggregatedStats() {
        if (aggregatedStats == null) {
            aggregatedStats = new MutableLiveData<>();
            loadAggregatedStats();
        }
        return aggregatedStats;
    }

    private void loadAggregatedStats() {
        new Thread(() -> {
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(getApplication().getApplicationContext());
            List<Track> tracks = contentProviderUtils.getTracks();

            AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(tracks);

            aggregatedStats.postValue(aggregatedStatistics);
        }).start();
    }
}
