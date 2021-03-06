package pl.gregoryiwanek.altimeter.app.details;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.Map;

import pl.gregoryiwanek.altimeter.app.data.database.SessionDataSource;
import pl.gregoryiwanek.altimeter.app.data.database.SessionRepository;
import pl.gregoryiwanek.altimeter.app.utils.databaseexporter.DatabaseExporter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Presenter class of Details section.
 */
class DetailsPresenter implements DetailsContract.Presenter {

    private SessionDataSource.DetailsSessionCallback callbackDetails;
    private final DetailsFragment mDetailsView;
    private final SessionRepository mSessionRepository;
    private final String sessionId;
    DetailsPresenter(String id,
                     @NonNull SessionRepository sessionRepository,
                     @NonNull DetailsFragment detailsFragment) {
        sessionId = id;
        mSessionRepository = sessionRepository;
        mDetailsView = checkNotNull(detailsFragment);
        mDetailsView.setPresenter(this);
        setCallbacks();
    }

    private void setCallbacks() {
        callbackDetails = new SessionDataSource.DetailsSessionCallback() {
            @Override
            public void onDetailsLoaded(Bundle args) {
                mDetailsView.setTitleTextView(args.getString("title"));
                mDetailsView.setDescriptionTextView(args.getString("description"));
                mDetailsView.setIdTextView(args.getString("id"));
                mDetailsView.setNumPointsTextView(args.getString("numOfPoints"));
                mDetailsView.setTimeStartTextView(args.getString("timeStart"));
                mDetailsView.setTimeEndTextView(args.getString("timeEnd"));
                mDetailsView.setDistanceTextView(args.getString("distance"));
            }

            @Override
            public void onChangesSaved() {
                mDetailsView.showChangesSaved();
            }

            @Override
            public void onExportDataLoaded(List<String[]> args) {
                for (String[] arrayArg : args) {
                    for (String str : arrayArg) {
                        System.out.println("array string is: " + str);
                    }
                }
            }
        };
    }

    @Override
    public void start() {
        Context context = mDetailsView.getContext();
        mSessionRepository.getDetails(sessionId, callbackDetails, context);
    }

    @Override
    public void saveTextChanges() {
        mDetailsView.sendChanges();
    }

    @Override
    public void saveChangesInRepository(Map<String, String> changes) {
        mSessionRepository.updateDetailsChanges(callbackDetails, changes);
    }

    @Override
    public void exportSessionDataAsFile() {
        Context context = mDetailsView.getContext();
        DatabaseExporter exporter = DatabaseExporter.getInstance();
        exporter.exportSessionDataAsFile(
                mSessionRepository.getDataToExportAsFile(sessionId, callbackDetails, context)
        );
    }
}
