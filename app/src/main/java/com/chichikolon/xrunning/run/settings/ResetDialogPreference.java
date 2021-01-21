package com.chichikolon.xrunning.run.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.chichikolon.xrunning.R;import com.chichikolon.xrunning.run.util.PreferencesUtils;

public class ResetDialogPreference extends DialogPreference {

    public ResetDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    interface ResetCallback {
        void onReset();
    }

    public static class ResetPreferenceDialog extends PreferenceDialogFragmentCompat {

        static PreferenceDialogFragmentCompat newInstance(String preferenceKey) {
            ResetPreferenceDialog dialog = new ResetPreferenceDialog();
            final Bundle bundle = new Bundle(1);
            bundle.putString(PreferenceDialogFragmentCompat.ARG_KEY, preferenceKey);
            dialog.setArguments(bundle);

            return dialog;
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                FragmentActivity activity = getActivity();

                PreferencesUtils.resetPreferences(activity, true);
                Toast.makeText(activity, R.string.settings_reset_done, Toast.LENGTH_SHORT).show();

                if (activity instanceof ResetCallback) {
                    ((ResetCallback) activity).onReset();
                }
            }
        }
    }
}
