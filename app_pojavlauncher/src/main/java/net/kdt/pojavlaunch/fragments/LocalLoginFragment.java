package net.kdt.pojavlaunch.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import net.kdt.pojavlaunch.ui.vojo.VojoActivity;

/** Retired inherited login route. Credentials are collected only by Microsoft's browser flow. */
@Deprecated
public class LocalLoginFragment extends Fragment {
    public static final String TAG = "LOCAL_LOGIN_FRAGMENT";
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        Button button = new Button(requireContext());
        button.setText("Open Vojo Accounts");
        button.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
        button.setOnClickListener(v -> startActivity(new Intent(requireContext(), VojoActivity.class)
            .putExtra("vojo.openAccounts", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        return button;
    }
    public boolean canGoBack() { return false; }
    public void goBack() { }
}
