package pl.grzegorziwanek.altimeter.app.model.location;

import android.location.Location;
import android.support.annotation.Nullable;

import pl.grzegorziwanek.altimeter.app.model.Session;

/**
 * Created by Grzegorz Iwanek on 20.12.2016.
 */
public interface CallbackResponse {

    interface LocationChangedCallback {

        void onNewLocationFound(Location location);
    }

    interface AddressFetchedCallback {

        void onAddressFound(String address);
    }

    interface ElevationFetchedCallback {

        void onElevationFound(Double elevation);
    }

    interface FullInfoCallback {

        void onFullInfoAcquired(Session session);
    }

    void stopListenForLocations();

    void startListenForLocations(@Nullable FullInfoCallback callback);

    void clearSessionData();
}
