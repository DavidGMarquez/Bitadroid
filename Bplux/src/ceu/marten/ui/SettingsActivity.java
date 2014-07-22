package ceu.marten.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import ceu.marten.bitadroid.R;
import ceu.marten.model.Constants;

public class SettingsActivity extends PreferenceActivity{

	public static final String KEY_PREF_CONF_CONFIG = "ask_confirmation_configuration";
	public static final String KEY_PREF_CONF_REC = "ask_confirmation_recording";
	public static final String KEY_PREF_ZOOM_VALUE = "zoom";
	public static final String KEY_DRAW_IN_BACKGROUND = "draw_in_background";

	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();

		switch (extras.getInt(Constants.KEY_SETTINGS_TYPE)) {
		case 1:
			addPreferencesFromResource(R.xml.main_preference);
			addPreferencesFromResource(R.xml.recording_preference);
			setTitle(getString(R.string.global_settings_label));
			break;
		case 2:
			addPreferencesFromResource(R.xml.recording_preference);
			getPreferenceScreen().findPreference(KEY_DRAW_IN_BACKGROUND).setEnabled(extras.getBoolean(Constants.KEY_SETTINGS_DRAW_STATE));
			setTitle(getString(R.string.recording_settings_label));
			break;
		default:
			break;
		}
	}
	
}