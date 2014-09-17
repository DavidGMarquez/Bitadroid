package ceu.marten.ui;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ceu.marten.bitadroid.R;
import ceu.marten.model.Constants;
import ceu.marten.model.DeviceRecording;
import ceu.marten.model.io.DatabaseHelper;
import ceu.marten.ui.adapters.RecordingsListAdapter;

import com.haarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.haarman.listviewanimations.itemmanipulation.SwipeDismissAdapter;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

/**
 * Loads, from Android's internal Database, and displays a list of the device
 * recordings, previously created in {@link NewRecordingActivity}
 * 
 * @author Carlos Marten
 * 
 */
public class RecordingsActivity extends OrmLiteBaseActivity<DatabaseHelper>
		implements OnDismissCallback{

	// Standard debug constant
	private static final String TAG = RecordingsActivity.class.getName();

	private ListView recordingsLV;
	private String recordingName;
	private RecordingsListAdapter baseAdapter;
	private ArrayList<DeviceRecording> recordings = null;
	private int[] reverseSortedPositions;
	private SharedPreferences.Editor prefEditor = null;
	private LayoutInflater inflater;
	private static final File externalStorageDirectory = Environment.getExternalStorageDirectory();
	
	//DIALOGS
	private AlertDialog confirmationDialog, errorDialog;
	
	/**
	 * Loads recordings from Android's internal Database, and sets up the
	 * recording list view and confirmation dialog
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ly_recordings);
		
		inflater = this.getLayoutInflater();
		if(loadRecordings()){
			setupRecordingListView();
			setupConfirmationDialog();
		}
	}
	
	/**
	 * Loads recordings from Android's internal Database
	 * @return true if loading was successful. false when exception is caught 
	 */
	private boolean loadRecordings() {
		Dao<DeviceRecording, Integer> dao;
		recordings = new ArrayList<DeviceRecording>();
		try {
			dao = getHelper().getRecordingDao();
			QueryBuilder<DeviceRecording, Integer> builder = dao.queryBuilder();
			builder.orderBy(DeviceRecording.DATE_FIELD_NAME, false).limit(100L); 
			recordings = (ArrayList<DeviceRecording>) dao.query(builder.prepare());
		} catch (SQLException e) {
			Log.e(TAG, "Exception loading recordings from android' internal database ", e);
			showErrorDialog(getResources().getString(R.string.ca_error_loading_recordings_message));
			return false;
		}
		return true;
	}

	/**
	 * Initializes recording list view with swipe to dismiss adapter
	 */
	private void setupRecordingListView() {
		// SHORT CLICK LISTENER
		final OnItemClickListener shortPressListener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> AdapterView, View configurationView,int position, long id) {
				recordingName = ((TextView) configurationView.findViewById(R.id.dli_name)).getText().toString();
				File recordingZipFile = new File(externalStorageDirectory + Constants.APP_DIRECTORY + recordingName + Constants.ZIP_FILE_EXTENTION);
				if(fileSizeBiggerThan20Mb(recordingZipFile))
					showRecordingTooBigDialog();
				else
					showSendRecordingOptions(recordingZipFile);
			}
		};
		
		recordingsLV = (ListView) findViewById(R.id.lvSessions);
		recordingsLV.setOnItemClickListener(shortPressListener);
		recordingsLV.setEmptyView(findViewById(R.id.empty_list_recordings));
		
		baseAdapter = new RecordingsListAdapter(this, recordings);
		
		// sets swipe to dismiss adapter from listViewAnimations
		SwipeDismissAdapter swipeAdapter = new SwipeDismissAdapter(baseAdapter, this);
		swipeAdapter.setAbsListView(recordingsLV);
		recordingsLV.setAdapter(baseAdapter);
	}

	/**
	 * Sets up delete confirmation dialog used when user swipes recording away to delete it.
	 * On positive click it deletes the recording from DB and from file system.
	 * On negative click cancels deletion.
	 * Adds custom title and content
	 * Adds positive and negative buttons
	 * Disables default 'close by touching outside dialog' feature
	 */
	private void setupConfirmationDialog() {
		// Initializes a custom title view
		TextView customTitleView = (TextView)inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.ra_confirm_dialog_title);
		customTitleView.setBackgroundColor(getResources().getColor(R.color.waring_dialog));
		
		// Initializes a custom title view
		View contentView = inflater.inflate(R.layout.dialog_confirmation_content, null);
		((TextView)contentView.findViewById(R.id.confirmation_message)).setText(getResources().getString(R.string.ra_confirm_dialog_message));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCustomTitle(customTitleView)
		.setView(contentView)
		.setPositiveButton(getString(R.string.ra_confirm_dialog_positive),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// removes recording form database and its zip file
						removeRecording();
						
						// checks if 'don't ask again' checkbox is checked if so
						// commit the changes and reset the dialog because the view must change
						if(prefEditor != null){
							prefEditor.commit();
							prefEditor = null;
							setupConfirmationDialog();
						}
					}
				});
		builder.setNegativeButton(getString(R.string.ra_confirm_dialog_negative),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// To re-enable swipe to dismiss capabilities
						setupRecordingListView();
						// To reset dialog's checkbox
						setupConfirmationDialog();
					}
				});
	
		confirmationDialog = builder.create();
		confirmationDialog.setCanceledOnTouchOutside(false);
	}

	/**
	 * Removes recording from Android's internal Database and its zip file from the file system. Displays a
	 * removed toast
	 * 
	 * @return true if successfully deleted. False if SQLexception was caught
	 */
	private boolean removeRecording(){ 		
		for (int position : reverseSortedPositions) {
			Dao<DeviceRecording, Integer> dao = null;
			try {
				dao = getHelper().getRecordingDao();
				dao.delete(recordings.get(position));
			} catch (SQLException e) {
				Log.e(TAG, "Exception removing recording from database ", e);
				showErrorDialog(getResources().getString(R.string.ra_error_message_deleting_recording_from_database));
				return false;
			}
			
			// gets selected recording file path
			recordingName = recordings.get(position).getName();
			File file = new File(externalStorageDirectory + Constants.APP_DIRECTORY + recordingName + Constants.ZIP_FILE_EXTENTION);
			
			// Checks if it exists and there was a problem deleting it from the file system
			if(file.exists() && !file.delete()){
				showErrorDialog(getResources().getString(R.string.ra_error_message_deleting_recording_from_filesystem));
			}else{
				baseAdapter.remove(position);
				recordings.remove(position);
			}
			// Tells the media scanner to scan the deleted compressed file, so that
			// it is no longer visible for the user via USB without needing to reboot
			// device because of the MTP protocol
			Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			intent.setData(Uri.fromFile(file));
			sendBroadcast(intent);
		}
		
		displayInfoToast(getString(R.string.ra_recording_removed));
		return true;
	}

	/**
	 * Shows a custom error dialog with its parameter message
	 */
	private void showErrorDialog(String errorMessage) {
		
		// Initializes custom title view
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.ca_error_dialog_title);
		customTitleView.setBackgroundColor(getResources().getColor(R.color.error_dialog));

		// Initializes custom content view
		View contentView = inflater.inflate(R.layout.dialog_confirmation_content, null);
		((TextView) contentView.findViewById(R.id.confirmation_message)).setText(errorMessage);
		
		// dialog builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCustomTitle(customTitleView)
				.setView(contentView)
				.setPositiveButton(getString(R.string.bp_positive_button),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// closes dialog
							}
						});
		errorDialog = builder.create();
		errorDialog.setCanceledOnTouchOutside(false);
		errorDialog.show();
	}
	
	/**
	 * Shows a dialog telling the user that the recording file is bigger than
	 * 20MB and therefore cannot be sent by email
	 */
	private void showRecordingTooBigDialog() {
		
		// Initializes custom title view
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.ra_display_path_dialog_title);
		customTitleView.setBackgroundColor(getResources().getColor(R.color.error_dialog));
		
		// dialog's builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCustomTitle(customTitleView)
				.setMessage(R.string.ra_display_path_dialog_message)
				.setPositiveButton(getString(R.string.bp_positive_button),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// closes dialog
							}
						});
		errorDialog = builder.create();
		errorDialog.setCanceledOnTouchOutside(false);
		errorDialog.show();
	}

	/**
	 * Gets recording file Uri and starts a sendIntent for Android system to
	 * display the chooser based on file type
	 */
	private void showSendRecordingOptions(File recordingZipFile) {
		// gets recording file Uri
		Uri fileUri = Uri.fromFile(recordingZipFile);
		
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("application/zip"); 
		sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
		startActivity(Intent.createChooser(sendIntent, getString(R.string.ra_send_dialog_title)));
	}
	
	/**
	 * Returns true if recording file is bigger than 20 MB. False otherwise
	 */
	private boolean fileSizeBiggerThan20Mb(File recordingZipFile) {
		if((recordingZipFile.length() / 1024d) / 1024d > 20.0d)// (>20MB)?
			return true;
		else
			return false;
	}
	
	/**
	 * creates an info toast showing the message of the parameter it receives
	 * @param messageToDisplay message to be display in the toast
	 */
	private void displayInfoToast(String messageToDisplay) {
		Toast infoToast = new Toast(getApplicationContext());
		View toastView = inflater.inflate(R.layout.toast_info, null);
		infoToast.setView(toastView);
		((TextView) toastView.findViewById(R.id.display_text)).setText(messageToDisplay);
		infoToast.show();
	}


	/**
	 * Callback called when user swipes recording out of the screen
	 */
	@Override
	public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
		// needed to delete recording on confirmation dialog positive button pressed
		this.reverseSortedPositions = reverseSortedPositions;
		
		// getting shared preference to know whether or not to show confirmation dialog
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean dontAskForConfrmation = sharedPref.getBoolean(SettingsActivity.KEY_PREF_CONF_REC, false);
		
		if(!dontAskForConfrmation)
			confirmationDialog.show();
		else
			removeRecording();
	}

	/**
	 * Destroys activity
	 */
	@Override
	public void onBackPressed() {
	    super.onBackPressed();
	    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
	}

	/**
	 * Callback called when 'don't ask again' checkbox of recording dialog is
	 * clicked. If check box change its state to checked, puts the global
	 * recording confirmation preference to true. If its state changed to
	 * unChecked, puts the preference to false
	 */
	public void onDialogCheckBoxClicked(View checkBoxView) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		prefEditor = sharedPref.edit();
		
		if(((CheckBox)checkBoxView).isChecked())
			prefEditor.putBoolean(SettingsActivity.KEY_PREF_CONF_REC, true);
		else{
			prefEditor.putBoolean(SettingsActivity.KEY_PREF_CONF_REC, false);
			// Used to check on recording dialog setup whether the preference has been checked to commit changes
			prefEditor = null;
		}
	}
}
