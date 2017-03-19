package pl.grzegorziwanek.altimeter.app.data.statistics;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

import pl.grzegorziwanek.altimeter.app.R;
import pl.grzegorziwanek.altimeter.app.utils.Constants;
import rx.Observable;
import rx.functions.Func0;

/**
 * Created by Grzegorz Iwanek on 19.03.2017.
 */
class StatisticsTaskRx {
    private static StatisticsTaskRx INSTANCE = null;
    private Context mContext;

    private StatisticsTaskRx(Context context) {
        mContext = context;
    }

    public static StatisticsTaskRx getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new StatisticsTaskRx(context);
        }
        return INSTANCE;
    }

    Observable<Map<String,String>> getStatisticsObservable() {
        return Observable.defer(new Func0<Observable<Map<String, String>>>() {
            @Override
            public Observable<Map<String, String>> call() {
                return Observable.just(getStatistics());
            }
        });
    }

    private Map<String,String> getStatistics() {
        SharedPreferences preferences = loadSharedPref();
        return getStatisticsMap(preferences);
    }

    private SharedPreferences loadSharedPref() {
        return PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    private Map<String,String> getStatisticsMap(SharedPreferences preferences) {
        Map<String,String> map = new HashMap<>();
        String[] statisticsNames = mContext.getResources().getStringArray(R.array.statistics_names);
        for (String name : statisticsNames) {
            String statistic = preferences.getString(name, Constants.DEFAULT_TEXT);
            map.put(name, statistic);
        }
        return map;
    }

    Observable<Boolean> getResetObservable() {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(isResetSuccess());
            }
        });
    }

    private Boolean isResetSuccess() {
        try {
            SharedPreferences preferences = loadSharedPref();
            resetStatistics(preferences);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void resetStatistics(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        String[] statisticsNames = mContext.getResources().getStringArray(R.array.statistics_names);
        for (String name: statisticsNames) {
            editor.putString(name, Constants.DEFAULT_TEXT);
        }
        editor.apply();
    }
}
