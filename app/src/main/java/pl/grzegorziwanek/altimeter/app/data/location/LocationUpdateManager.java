package pl.grzegorziwanek.altimeter.app.data.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import pl.grzegorziwanek.altimeter.app.data.Session;
import pl.grzegorziwanek.altimeter.app.data.StaticHandler;
import pl.grzegorziwanek.altimeter.app.data.location.managers.BarometerManager;
import pl.grzegorziwanek.altimeter.app.data.location.managers.GpsManager;
import pl.grzegorziwanek.altimeter.app.data.location.managers.NetworkManager;
import pl.grzegorziwanek.altimeter.app.data.location.managers.models.BarometerAltitudeModel;
import pl.grzegorziwanek.altimeter.app.data.location.managers.models.CombinedLocationModel;
import pl.grzegorziwanek.altimeter.app.data.location.managers.models.GpsAltitudeModel;
import pl.grzegorziwanek.altimeter.app.data.location.managers.models.NetworkAltitudeModel;
import pl.grzegorziwanek.altimeter.app.data.location.services.elevation.BarometerListener;
import pl.grzegorziwanek.altimeter.app.data.location.services.elevation.GpsLocationListener;
import pl.grzegorziwanek.altimeter.app.data.location.services.elevation.NetworkTaskRx;
import pl.grzegorziwanek.altimeter.app.data.location.services.helpers.AddressService;
import pl.grzegorziwanek.altimeter.app.data.location.services.helpers.airporttask.AirportsTaskRx;
import pl.grzegorziwanek.altimeter.app.data.location.services.helpers.airporttask.AirportsWithDataTaskRx;
import pl.grzegorziwanek.altimeter.app.data.location.services.helpers.airporttask.xmlparser.XmlAirportValues;
import pl.grzegorziwanek.altimeter.app.utils.Constants;
import pl.grzegorziwanek.altimeter.app.utils.FormatAndValueConverter;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ... on 01.02.2017.
 */

public class LocationUpdateManager implements LocationResponse {
    private static LocationUpdateManager INSTANCE = null;
    private static FullInfoCallback callbackFullInfo;
    private LocationChangedCallback callbackInitiation;
    private AddressFoundCallback callbackAddress;
    private GpsElevationCallback callbackGps;
    private AddressResultReceiver mResultReceiver;
    private LocationResponse mGpsLocationListener;
    private BarometerListener mBarometerListener;
    private Context mContext;
    private StaticHandler handler;
    private Runnable mBarometerRunnable;
    private Runnable mNetworkRunnable;
    private Runnable mDataCombinedRunnable;
    private static Session mSession = null;
    private Subscription mBarometerSubscription = null;

    private LocationUpdateManager(@NonNull Context context) {
        setCallbacks();
        setVariables(context);
        setManagers();
        setRunnable();

    }

    public static LocationUpdateManager getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LocationUpdateManager(context);
        } else {
            INSTANCE = new LocationUpdateManager(context);
        }
        return INSTANCE;
    }

    private void setCallbacks() {
        callbackInitiation = new LocationChangedCallback() {
            @Override
            public void onInitialLocationIdentified(Location location) {
                if (isAirportUpdateRequired(location.getTime())) {
                    updateAirportInfo(location);
                }
                if (GpsManager.isGpsEnabled() && location.getAltitude() != 0) {
                    GpsAltitudeModel.setAltitude(location.getAltitude());
                }
                mSession.setCurrLocation(location);
            }
        };

        callbackAddress = new AddressFoundCallback() {
            @Override
            public void onAddressFound(String address) {
                mSession.setAddress(address);
            }
        };

        callbackGps = new GpsElevationCallback() {
            @Override
            public void onGpsLocationFound(Location location) {
                if (GpsManager.isGpsEnabled()) {
                    saveSessionsLocation(location);
                    appendLocationToList();
                    fetchAddressService(location);

                    if (location.getAltitude() != 0) {
                        saveNonZeroGpsAltitude(location);
                    }
                    mSession.appendGraphPoint(mSession.getCurrentLocation().getTime(), CombinedLocationModel.getCombinedAltitude());
                }
            }
        };
    }

    private void setVariables(Context context) {
        mSession = new Session("","");
        mContext = context;
        mGpsLocationListener = GpsLocationListener.getInstance(context, callbackInitiation, callbackGps);
        mBarometerListener = BarometerListener.getInstance(context);
        mResultReceiver = new AddressResultReceiver(new Handler());
        handler = new StaticHandler();
    }

    private void setManagers() {
        GpsManager.setGpsEnabled(false);
        NetworkManager.setNetworkEnabled(false);
        BarometerManager.setBarometerEnabled(false);
        SessionUpdateModel.readAirportUpdateLocation(mContext);
        SessionUpdateModel.readAirportPressure(mContext);
    }

    private void setRunnable() {
        mBarometerRunnable = new Runnable() {
            @Override
            public void run() {
                if (BarometerManager.isBarometerEnabled()) {
                    mBarometerListener.registerListener();
                }
            }
        };

        mNetworkRunnable = new Runnable() {
            @Override
            public void run() {
                if (NetworkManager.isNetworkEnabled()) {
                    fetchCurrentElevationRx(mSession.getCurrentLocation());
                }
            }
        };

        mDataCombinedRunnable = new Runnable() {
            @Override
            public void run() {
                // TODO: 23.02.2017 set case when GPS is enabled, and other disabled (app crash)
                // TODO: 23.02.2017 set case when GPS is disabled and network is enabled (call of "identify position" every 30sec)
                // TODO: 23.02.2017 set case when only Barometer is enabled (only altitude will be shown)
                mSession.getLocationList().get(mSession.getLocationList().size()-1).setAltitude(CombinedLocationModel.getCombinedAltitude());
                CombinedLocationModel.setUpdateTime(System.currentTimeMillis());
                mSession.appendGraphPoint(CombinedLocationModel.getUpdateTime(), CombinedLocationModel.getCombinedAltitude());
                callbackFullInfo.onFullInfoAcquired(mSession);
                handler.postDelayed(mDataCombinedRunnable, 20000);
            }
        };
    }

    private void fetchBarometerAltitudeRx() {
        if (mBarometerSubscription == null) {
            mBarometerSubscription = mBarometerListener.getPressureAltitudePublishSubject()
                    .observeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Double>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Double barAltitude) {
                            BarometerAltitudeModel.setAltitude(barAltitude);
                            mBarometerListener.unregisterListener();
                            barAltitude = FormatAndValueConverter.roundValue(barAltitude);
                            String altitude = String.valueOf(barAltitude);
                            callbackFullInfo.onBarometerInfoAcquired(altitude);
                            CombinedLocationModel.updateCombinedAltitude();
                            handler.postDelayed(mBarometerRunnable, 20000);
                        }
                    });
        }
    }

    private void fetchCurrentElevationRx(Location location) {
        NetworkTaskRx taskRx = new NetworkTaskRx();
        taskRx.setLocationsStr(location);
        taskRx.getElevationObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Double>() {
                    @Override
                    public void onCompleted() {
                        this.unsubscribe();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Double elevation) {
                        //round elevation value (to set precision to meters)
                        elevation = (double) Math.round(elevation);
                        appendLocationToList();
                        setTextViewStrings();

                        NetworkAltitudeModel.setAltitude(elevation);
                        NetworkAltitudeModel.setMeasureTime(System.currentTimeMillis());

                        elevation = FormatAndValueConverter.roundValue(elevation);
                        String alt = String.valueOf(elevation);
                        callbackFullInfo.onNetworkInfoAcquired(alt);

                        CombinedLocationModel.updateCombinedAltitude();
                        setCurrentElevation(CombinedLocationModel.getCombinedAltitude());
                        handler.postDelayed(mNetworkRunnable, Constants.NETWORK_INTERVAL_VALUE);

                        if (!GpsManager.isGpsEnabled()) {
                            identifyCurrentLocation();
                        }
                    }
                });
    }

    private void fetchAddressService(Location location) {
        Intent intent = new Intent(mContext, AddressService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA, location);
        mContext.startService(intent);
    }

    private void fetchNearestAirportsRx(Location location) {
        String airportRadialDistance = FormatAndValueConverter.setRadialDistanceString(
                location.getLatitude(), location.getLongitude());

        AirportsTaskRx taskRx = new AirportsTaskRx();
        taskRx.setRadialDistanceStr(airportRadialDistance);
        taskRx.getNearestAirportsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<XmlAirportValues>>() {
                    @Override
                    public void onCompleted() {
                        this.unsubscribe();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(List<XmlAirportValues> values) {
                        BarometerManager.setAirportsList(values);
                        if (!BarometerManager.isAirportsListEmpty()) {
                            fetchAirportsPressureRx();
                        }
                    }
                });
    }

    private void fetchAirportsPressureRx() {
        String airportsSymbols =
                FormatAndValueConverter.getAirportsSymbolString(BarometerManager.getAirportsList());

        AirportsWithDataTaskRx taskRx = new AirportsWithDataTaskRx();
        taskRx.setStationsString(airportsSymbols);
        taskRx.getAirportsWithDataObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<XmlAirportValues>>() {
                    @Override
                    public void onCompleted() {
                        this.unsubscribe();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(List<XmlAirportValues> xmlAirportValues) {
                        BarometerManager.setAirportsList(xmlAirportValues);
                        assignAirportPressure();
                        BarometerManager.resetList();
                    }
                });
    }

    private void saveSessionsLocation(Location location) {
        if (mSession.getCurrentLocation() != null) {
            mSession.setLastLocation(mSession.getCurrentLocation());
        }
        mSession.setCurrLocation(location);
    }

    public void unsubscribeOnDestroy() {
        mBarometerSubscription.unsubscribe();
        mBarometerSubscription = null;
        resetAllData();
    }

    private void setCurrentElevation(Double elevation) {
        mSession.setCurrentElevation(elevation);
        mSession.getCurrentLocation().setAltitude(elevation);
    }

    private void appendLocationToList() {
        mSession.appendLocationPoint(mSession.getCurrentLocation());
    }

    private void setTextViewStrings() {
        // TODO: 01.03.2017 kick that out to model
        mSession.setCurrentElevation(FormatAndValueConverter.roundValue(CombinedLocationModel.getCombinedAltitude()));
        mSession.setElevationOnList(FormatAndValueConverter.roundValue(CombinedLocationModel.getCombinedAltitude()));
        SessionUpdateModel.setGeoCoordinateStr(mSession);
        SessionUpdateModel.setSessionsDistance(mSession);
        SessionUpdateModel.setSessionsHeight(mSession);
    }

    private void saveNonZeroGpsAltitude(Location location) {
        GpsAltitudeModel.setAltitude(location.getAltitude());
        Double gpsAlt = FormatAndValueConverter.roundValue(location.getAltitude());
        callbackFullInfo.onGpsInfoAcquired(String.valueOf(gpsAlt));
        CombinedLocationModel.updateCombinedAltitude();
    }

    private void assignAirportPressure() {
        float pressure = FormatAndValueConverter.fetchAirportPressure(BarometerManager.getAirportsList(),
                BarometerManager.getUpdateLatitude(), BarometerManager.getUpdateLongitude());
        BarometerManager.setClosestAirportPressure(pressure);
        SessionUpdateModel.saveAirportPressure(mContext);
    }

    @Override
    public void startListenForLocations(@Nullable FullInfoCallback callback) {
        callbackFullInfo = callback;
        SessionUpdateModel.updateDistanceUnits(mContext);

        if (GpsManager.isGpsEnabled()) {
            mGpsLocationListener.startListenForLocations(null);
        }

        if (NetworkManager.isNetworkEnabled()) {
            fetchCurrentElevationRx(mSession.getCurrentLocation());
            NetworkManager.setMeasureTime(mSession.getCurrentLocation().getTime());
        }

        if (BarometerManager.isBarometerEnabled()) {
            if (!mBarometerListener.isBarometerListenerRegistered()) {
                fetchBarometerAltitudeRx();
                mBarometerListener.registerListener();
            }
        }

        handler.postDelayed(mDataCombinedRunnable, 10000);
    }

    @Override
    public void identifyCurrentLocation() {
        mGpsLocationListener.identifyCurrentLocation();
    }

    @Override
    public void stopListenForLocations(boolean isLocked) {
        mGpsLocationListener.stopListenForLocations(isLocked);
        mBarometerListener.unregisterListener();
        resetHandlers();
        if (isLocked) {
            lockSession();
        }
    }

    public void setManagerState(Class<?> manager, boolean isEnabled) {
        if (manager == GpsManager.class) {
            GpsManager.setGpsEnabled(isEnabled);
        } else if (manager == NetworkManager.class) {
            NetworkManager.setNetworkEnabled(isEnabled);
        } else if (manager == BarometerManager.class){
            BarometerManager.setBarometerEnabled(isEnabled);
        }
    }

    private void lockSession() {
        mSession.setLocked(true);
    }

    @Override
    public void resetAllData() {
        mSession.clearData();
        callbackFullInfo.onFullInfoAcquired(mSession);
        resetElevationSourceTextView();
        resetHandlers();
        resetListeners();

        identifyCurrentLocation();
    }

    private void resetElevationSourceTextView() {
        callbackFullInfo.onGpsInfoAcquired(Constants.DEFAULT_TEXT);
        callbackFullInfo.onNetworkInfoAcquired(Constants.DEFAULT_TEXT);
        callbackFullInfo.onBarometerInfoAcquired(Constants.DEFAULT_TEXT);
    }

    private void resetHandlers() {
        handler.removeCallbacks(mBarometerRunnable);
        handler.removeCallbacks(mNetworkRunnable);
        handler.removeCallbacks(mDataCombinedRunnable);
    }

    private void resetListeners() {
        GpsManager.resetData();
        NetworkManager.resetData();
        BarometerManager.resetData();
    }

    private void updateAirportInfo(Location location) {
        BarometerManager.setAirportMeasureTime(location.getTime());
        SessionUpdateModel.saveAirportUpdateLocation(location, mContext);
        fetchNearestAirportsRx(location);
    }

    private boolean isAirportUpdateRequired(double currentTime) {
        return (currentTime - BarometerManager.getAirportMeasureTime()) > Constants.HALF_HOUR;
    }

    public Session getSession() {
        return mSession;
    }

    @SuppressLint("ParcelCreator")
    private class AddressResultReceiver extends ResultReceiver {

        String mAddressOutput;

        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            callbackAddress.onAddressFound(mAddressOutput);
        }
    }
}


