package pl.gregoryiwanek.altimeter.app.details;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.util.List;

import pl.gregoryiwanek.altimeter.app.BasicActivity;
import pl.gregoryiwanek.altimeter.app.R;
import pl.gregoryiwanek.altimeter.app.data.database.SessionDataSource;
import pl.gregoryiwanek.altimeter.app.data.database.SessionRepository;
import pl.gregoryiwanek.altimeter.app.data.database.local.LocalDataSource;
import pl.gregoryiwanek.altimeter.app.utils.databaseexporter.DatabaseExporter;
import pl.gregoryiwanek.altimeter.app.utils.formatconventer.FormatAndValueConverter;

/**
 * Main activity of Details section.
 */
public class DetailsActivity extends BasicActivity {

    private DetailsFragment mDetailsFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        super.initiateUI();
        readPreferences();
        setDetailsFragment();
        setPresenter();
    }

    private void setDetailsFragment() {
        mDetailsFragment =
                (DetailsFragment) getSupportFragmentManager().findFragmentById(R.id.contentFrame);

        if (mDetailsFragment == null) {
            mDetailsFragment = DetailsFragment.newInstance();
            addFragmentToActivityOnStart(
                    getSupportFragmentManager(), mDetailsFragment, R.id.contentFrame);
        }
    }

    @SuppressWarnings("UnusedAssignment")
    private void setPresenter() {
        String id = getIntent().getStringExtra("sessionId");
        DetailsPresenter detailsPresenter = new DetailsPresenter(id,
                SessionRepository.getInstance(LocalDataSource.newInstance(this)),
                mDetailsFragment);
    }

    private void readPreferences() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String units = sharedPref.getString("pref_set_units", "KILOMETERS");
        FormatAndValueConverter.setUnitsFormat(units);
    }
}
