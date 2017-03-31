package pl.grzegorziwanek.altimeter.app.about;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import pl.grzegorziwanek.altimeter.app.R;

public class AboutFragmentAuthor extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_author, container, false);

        ButterKnife.bind(this, view);
        return view;
    }
}
