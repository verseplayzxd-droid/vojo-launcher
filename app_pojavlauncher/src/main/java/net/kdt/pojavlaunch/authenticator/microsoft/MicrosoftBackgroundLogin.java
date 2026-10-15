package net.kdt.pojavlaunch.authenticator.microsoft;

import androidx.annotation.Nullable;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.listener.DoneListener;
import net.kdt.pojavlaunch.authenticator.listener.ErrorListener;
import net.kdt.pojavlaunch.authenticator.listener.ProgressListener;

/** Compatibility boundary for retired inherited authentication callers. */
@Deprecated
public class MicrosoftBackgroundLogin {
    public MicrosoftBackgroundLogin(boolean isRefresh, String codeOrRefreshToken) {
        // Do not retain a credential passed by an inherited caller.
    }
    public void performLogin(@Nullable ProgressListener progress,
                             @Nullable DoneListener done,
                             @Nullable ErrorListener error) {
        if (error != null) Tools.runOnUiThread(() -> error.onLoginError(
            new IllegalStateException("The legacy sign-in flow is disabled. Use Vojo's Accounts screen.")));
    }
}
