package ceu.marten.ui;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import ceu.marten.bitadroid.R;
import ceu.marten.model.Constants;
import ceu.marten.model.DeviceConfiguration;
import ceu.marten.model.DeviceRecording;
import ceu.marten.model.io.DataManager;
import ceu.marten.model.io.DatabaseHelper;
import ceu.marten.services.BiopluxService;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.jjoe64.graphview.GraphView.GraphViewData;

/**
 * Used to record a session based on a configuration and display the
 * corresponding channels or if only one is to be displayed it shows the
 * configuration' details. Connects to a Bioplux service
 * 
 * @author Carlos Marten
 * 
 */
public class NewRecordingActivity extends OrmLiteBaseActivity<DatabaseHelper> implements android.widget.PopupMenu.OnMenuItemClickListener, OnSharedPreferenceChangeListener {

	private static final String TAG = NewRecordingActivity.class.getName();
	
	// Keys used for communication with activity
	public static final String KEY_DURATION = "duration";
	public static final String KEY_RECORDING_NAME = "recordingName";
	public static final String KEY_CONFIGURATION = "configSelected";
	
	// key for recovery. Used when android kills activity
	public static final String KEY_CHRONOMETER_BASE = "chronometerBase";
	
	// 10 seconds
	private  final int maxDataCount = 10000; 

	// Android's widgets
	private  TextView uiRecordingName, uiConfigurationName, uiNumberOfBits,
			uiReceptionFrequency, uiSamplingFrequency, uiActiveChannels,
			uiMacAddress;
	private Button uiMainbutton;
	private Chronometer chronometer;

	// DIALOGS
	private  AlertDialog connectionErrorDialog;
	private  ProgressDialog savingDialog;
	
	// AUX VARIABLES
	private Context classContext = this;
	private Bundle extras;
	private LayoutInflater inflater;
	
	private DeviceConfiguration recordingConfiguration;
	private DeviceRecording recording;
	
	private Graph[] graphs;
	private int[] displayChannelPosition;
	private int currentZoomValue = 0;
	private String duration = null; 
	private SharedPreferences sharedPref = null;
	
	private boolean isServiceBounded = false;
	private boolean recordingOverride = false;
	private boolean savingDialogMessageChanged = false;
	private boolean closeRecordingActivity = false;
	private boolean drawState = true; //true -> Enable | false -> Disable
	private boolean goToEnd = true;
	
	// ERROR VARIABLES
	private int bpErrorCode   = 0;
	private boolean serviceError = false;
	private boolean connectionError = false;
	
	// MESSENGERS USED TO COMMUNICATE ACTIVITY AND SERVICE
	private Messenger serviceMessenger = null;
	private final Messenger activityMessenger = new Messenger(new IncomingHandler());

	/**
	 * Listener for new started touches on graphs vertical labels
	 */
	private OnTouchListener graphTouchListener = new OnTouchListener()
    {
        @Override
        public boolean onTouch(View v, MotionEvent event){
        	// get masked (not specific to a pointer) action
    		int maskedAction = event.getActionMasked();

    		switch (maskedAction) {
    			case MotionEvent.ACTION_DOWN:
    				if(goToEnd){
    					goToEnd = false;
    				}
    				else{
    					goToEnd = true;
    				}
    			break;
    		}
        	return true;
        }
   };
	
	/**
	 * Handler that receives messages from the service. It receives frames data,
	 * error messages and a saved message if service stops correctly
	 * 
	 */

	@SuppressLint("HandlerLeak")
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BiopluxService.MSG_DATA: {
				appendDataToGraphs(
						msg.getData().getDouble(BiopluxService.KEY_X_VALUE),
						msg.getData().getShortArray(BiopluxService.KEY_FRAME_DATA));
				break;
			}
			case BiopluxService.MSG_CONNECTION_ERROR: {
				serviceError = true;
				savingDialog.dismiss();
				displayConnectionErrorDialog(msg.arg1);
				break;
			}
			case DataManager.MSG_PERCENTAGE: {
				if (!savingDialogMessageChanged && msg.arg2 == DataManager.STATE_COMPRESSING_FILE) {
					savingDialog.setMessage(getString(R.string.nr_saving_dialog_compressing_message));
					savingDialogMessageChanged = true;
				}
				savingDialog.setProgress(msg.arg1);

				break;
			}
			case BiopluxService.MSG_SAVED: {
				savingDialog.dismiss();
				saveRecordingOnInternalDB();
				if (closeRecordingActivity) {
					closeRecordingActivity = false;
					finish();
					overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
				}
				displayInfoToast(getString(R.string.nr_info_rec_saved));
				break;
			}
			default: {
				super.handleMessage(msg);
			}
			}
		}
	}

	/**
	 * Bind connection used to bind and unbind with service
	 * onServiceConnected() called when the connection with the service has been established,
	 * giving us the object we can use to interact with the service. We are
	 * communicating with the service using a Messenger, so here we get a
	 * client-side representation of that from the raw IBinder object.
	 */
	private ServiceConnection bindConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceMessenger = new Messenger(service);
			isServiceBounded = true;
			Message msg = Message.obtain(null, BiopluxService.MSG_REGISTER_CLIENT);
			msg.replyTo = activityMessenger;
			try {
				serviceMessenger.send(msg);
			} catch (RemoteException e) {
				Log.e(TAG, "service conection failed", e);
				displayConnectionErrorDialog(10); // 10 -> fatal error
			}
		}
		
		/**
		 *  This is called when the connection with the service has been
		 *  unexpectedly disconnected -- that is, its process crashed.
		 */
		public void onServiceDisconnected(ComponentName className) {
			serviceMessenger = null;
			isServiceBounded = false;
			Log.i(TAG, "service disconnected");
		}
	};

	/**
	 * Appends x and y values received from service to all active graphs. The
	 * graph always moves to the last value added
	 */
	 void appendDataToGraphs(double xValue, short[] data) {
		if(!serviceError){
			for (int i = 0; i < graphs.length; i++) {
				graphs[i].getSerie().appendData(
						new GraphViewData(xValue,
								data[displayChannelPosition[i]]), goToEnd, maxDataCount);
			}
		}
	}

	/**
	 * Sends recording duration to the service by message when recording is
	 * stopped
	 */
	private void sendRecordingDuration() {
		if (isServiceBounded && serviceMessenger != null) {
			Message msg = Message.obtain(null, BiopluxService.MSG_RECORDING_DURATION, 0, 0);
			Bundle extras = new Bundle();
			extras.putString(KEY_DURATION, duration); 
			msg.setData(extras);
			msg.replyTo = activityMessenger;
			try {
				serviceMessenger.send(msg);
			} catch (RemoteException e) {
				Log.e(TAG, "Error sending duration to service", e);
				displayConnectionErrorDialog(10); // 10 -> fatal error
			}
		}else{Log.e(TAG, "Error sending duration to service");}
	}
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ly_new_recording);
		Log.i(TAG, "onCreate()");

		// GETTING EXTRA INFO FROM INTENT
		extras = getIntent().getExtras();
		recordingConfiguration = (DeviceConfiguration) extras.getSerializable(ConfigurationsActivity.KEY_CONFIGURATION);
		recording = new DeviceRecording();
		recording.setName(extras.getString(ConfigurationsActivity.KEY_RECORDING_NAME).toString());
		

		// INIT GLOBAL VARIABLES
		savingDialog = new ProgressDialog(classContext);
		savingDialog.setTitle(getString(R.string.nr_saving_dialog_title));
		savingDialog.setMessage(getString(R.string.nr_saving_dialog_adding_header_message)); 
		savingDialog.setCancelable(false);
		savingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		savingDialog.setProgress(0); //starts with 0%
		savingDialog.setMax(100); //100%
		
		inflater = this.getLayoutInflater();
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		currentZoomValue = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_ZOOM_VALUE, "150"));
		graphs = new Graph[recordingConfiguration.getDisplayChannelsNumber()];
		

		// Used to update zoom values
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    settings.registerOnSharedPreferenceChangeListener(this);
		
		// calculates the display channel position of frame received
		displayChannelPosition = new int[recordingConfiguration.getDisplayChannels().size()];
		int displayIterator = 0;
		for(int i=0; i < recordingConfiguration.getActiveChannels().size(); i++){
			if(recordingConfiguration.getActiveChannels().get(i) == recordingConfiguration.getDisplayChannels().get(displayIterator)){
				displayChannelPosition[displayIterator] = i;
				if(displayIterator < (recordingConfiguration.getDisplayChannels().size()-1))
				displayIterator++;
			}
		}
		
		// INIT ANDROID' WIDGETS
		uiRecordingName = (TextView) findViewById(R.id.nr_txt_recordingName);
		uiRecordingName.setText(recording.getName());
		uiMainbutton = (Button) findViewById(R.id.nr_bttn_StartPause);
		chronometer = new Chronometer(classContext);
		
		initActivityContentLayout();
		
		// SETUP DIALOG
		setupConnectionErrorDialog();
	}
	
	
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i(TAG, "onRestoreInstanceState");
		if (isServiceRunning()) {
			chronometer.setBase(savedInstanceState.getLong(KEY_CHRONOMETER_BASE));
			chronometer.start();
			uiMainbutton.setText(getString(R.string.nr_button_stop));
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		// If service is running re-bind to it to send recording duration
		if (isServiceRunning()) {
			bindToService();
		}
		super.onResume();
		Log.i(TAG, "onResume()");
	}

	private void initActivityContentLayout() {
		
		LayoutParams graphParams, detailParameters;
		View graphsView = findViewById(R.id.nr_graphs);
		
		// Initializes layout parameters
		graphParams = new LayoutParams(LayoutParams.MATCH_PARENT,
				Integer.parseInt((getResources()
						.getString(R.string.graph_height))));
		detailParameters = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);

		
			for (int i = 0; i < recordingConfiguration
					.getDisplayChannelsNumber(); i++) {
				graphs[i] = new Graph(this,
						getString(R.string.nc_dialog_channel)
								+ " "
								+ recordingConfiguration.getDisplayChannels()
										.get(i).toString());
				LinearLayout graph = (LinearLayout) inflater.inflate(
						R.layout.in_ly_graph, null);
				graphs[i].getGraphView().setOnTouchListener(graphTouchListener);
				((ViewGroup) graph).addView(graphs[i].getGraphView());
				((ViewGroup) graphsView).addView(graph, graphParams);
			}
		

		// If just one channel is being displayed, show configuration details
		if (recordingConfiguration.getDisplayChannelsNumber() == 1) {
			View details = inflater.inflate(R.layout.in_ly_graph_details, null);
			((ViewGroup) graphsView).addView(details, detailParameters);
			
			// get views
			uiConfigurationName = (TextView) findViewById(R.id.nr_txt_configName);
			uiNumberOfBits = (TextView) findViewById(R.id.nr_txt_config_nbits);
			uiReceptionFrequency = (TextView) findViewById(R.id.nr_reception_freq);
			uiSamplingFrequency = (TextView) findViewById(R.id.nr_sampling_freq);
			uiActiveChannels = (TextView) findViewById(R.id.nr_txt_channels_active);
			uiMacAddress = (TextView) findViewById(R.id.nr_txt_mac);

			// fill them
			uiConfigurationName.setText(recordingConfiguration.getName());
			uiReceptionFrequency.setText(String.valueOf(recordingConfiguration
					.getVisualizationFrequency()) + " Hz");
			uiSamplingFrequency.setText(String.valueOf(recordingConfiguration
					.getSamplingFrequency()) + " Hz");
			uiNumberOfBits.setText(String.valueOf(recordingConfiguration
					.getNumberOfBits()) + " bits");
			uiMacAddress.setText(recordingConfiguration.getMacAddress());
			uiActiveChannels.setText(recordingConfiguration.getActiveChannels()
					.toString());
		}
	}
	
	/**
	 * called when the back button is pressed and the recording is still
	 * running. On positive click, Stops and saves the recording, finishes
	 * activity so that parent gets focus
	 */
	private void showBackDialog() {
		// Sets a custom title view
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.nr_back_dialog_title);
		customTitleView.setBackgroundColor(getResources().getColor(R.color.waring_dialog));
		
		// dialog builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCustomTitle(customTitleView)
				.setView(inflater.inflate(R.layout.dialog_newrecording_backbutton_content, null))
				.setPositiveButton(
						getString(R.string.nr_back_dialog_positive_button),
						new DialogInterface.OnClickListener() {
							// stops, saves and finishes recording
							public void onClick(DialogInterface dialog, int id) {
								stopRecording();
								closeRecordingActivity = true;
								// dialog gets closed
							}
						});
		builder.setNegativeButton(
				getString(R.string.nc_dialog_negative_button),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// dialog gets closed
					}
				});

		AlertDialog backDialog = builder.create();
		backDialog.show();

	}

	/**
	 * Creates and shows a bluetooth error dialog if mac address is other than
	 * 'test' and the bluetooth adapter is turned off. On positive click it
	 * sends the user to android' settings for the user to turn bluetooth on
	 * easily
	 */
	private void showBluetoothDialog() {
		// Initializes custom title
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.nr_bluetooth_dialog_title);
		customTitleView.setBackgroundColor(getResources().getColor(R.color.error_dialog));
		
		// dialogs builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCustomTitle(customTitleView)
				.setMessage(getResources().getString(R.string.nr_bluetooth_dialog_message))
				.setPositiveButton(getString(R.string.nr_bluetooth_dialog_positive_button),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent intentBluetooth = new Intent();
						intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
						classContext.startActivity(intentBluetooth);
					}
				});
		builder.setNegativeButton(
				getString(R.string.nc_dialog_negative_button),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// dialog gets closed
					}
				});

		// creates and shows bluetooth dialog
		(builder.create()).show();
	}
	
	/**
	 * Sets up a connection error dialog with custom title. This is used to add
	 * custom message '.setMessage()' and display different possible connection
	 * errors it '.show()'
	 * 
	 */
	private void setupConnectionErrorDialog() {
		
		// Initializes custom title
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.nr_bluetooth_dialog_title);
		customTitleView.setBackgroundColor(getResources().getColor(R.color.error_dialog));
		
		// builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCustomTitle(customTitleView)
			   .setPositiveButton(
				getString(R.string.bp_positive_button),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						savingDialog.dismiss();
						closeRecordingActivity = true;
						
					}
				});
		connectionErrorDialog = builder.create();
		connectionErrorDialog.setCancelable(false);
		connectionErrorDialog.setCanceledOnTouchOutside(false);
		
	}

	/**
	 * Called when 'start recording' button is twice pressed On positive click,
	 * the current recording is removed and graph variables and views are reset.
	 * The overwrite recording starts right away
	 * 
	 */
	private void showOverwriteDialog() {
		
		// initializes custom title view
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.nr_overwrite_dialog_title);
		customTitleView.setBackgroundColor(getResources().getColor(R.color.waring_dialog));
		
		// dialog' builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCustomTitle(customTitleView)
				.setMessage(R.string.nr_overwrite_dialog_message)
				.setPositiveButton(
						getString(R.string.nr_overwrite_dialog_positive_button),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// deletes current recording from Android's internal Database
								try {
									Dao<DeviceRecording, Integer> dao = getHelper().getRecordingDao();
									dao.delete(recording);
								} catch (SQLException e) {
									Log.e(TAG, "saving recording exception", e);
								}
								
								// Reset activity variables
								recordingOverride = false;
								serviceError = false;
								closeRecordingActivity = false;
								savingDialogMessageChanged = false;
								goToEnd = true;
								
								// Reset activity content
								View graphsView = findViewById(R.id.nr_graphs);
								((ViewGroup) graphsView).removeAllViews();
								initActivityContentLayout();
								savingDialog.setMessage(getString(R.string.nr_saving_dialog_adding_header_message));
								savingDialog.setProgress(0);
								
								startRecording();
							}
						});
		builder.setNegativeButton(
				getString(R.string.nc_dialog_negative_button),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// dialog gets closed
					}
				});

		(builder.create()).show();
	}

	/**
	 * If recording is running, shows save and quit confirmation dialog. If
	 * service is stopped. destroys activity
	 */
	@Override
	public void onBackPressed() {
		if (isServiceRunning())
			showBackDialog();
		else {
			super.onBackPressed();
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		}
	}

	/**
	 * Stops and saves the recording in database and data as zip file
	 */
	private void stopRecording(){
		savingDialog.show();
		stopChronometer();
		sendRecordingDuration();
		unbindFromService();
		stopService(new Intent(NewRecordingActivity.this, BiopluxService.class));
		uiMainbutton.setText(getString(R.string.nr_button_start));
		drawState = true;
	}
	
	/**
	 * Starts the recording if mac address is 'test' and recording is not
	 * running OR if bluetooth is supported by the device, bluetooth is enabled,
	 * mac is other than 'test' and recording is not running. Returns always
	 * false for the main thread to be stopped and thus be available for the
	 * progress dialog  spinning circle when we test the connection
	 */
	private boolean startRecording() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		final ProgressDialog progress;
		if(recordingConfiguration.getMacAddress().compareTo("test")!= 0){ // 'test' is used to launch device emulator
			if (mBluetoothAdapter == null) {
				displayInfoToast(getString(R.string.nr_bluetooth_not_supported));
				return false;
			}
			if (!mBluetoothAdapter.isEnabled()){
				showBluetoothDialog();
				return false;
			}
		}
		
		progress = ProgressDialog.show(this,getResources().getString(R.string.nr_progress_dialog_title),getResources().getString(R.string.nr_progress_dialog_message), true);
		Thread connectionThread = 
				new Thread(new Runnable() {
			@Override
			public void run() {
				
				//Revisar		BitalinoAndroidDevice connectionTest = new BitalinoAndroidDevice(recordingConfiguration.getMacAddress()); 
					//Revisar
					//Revisar					connectionTest.Close();
				
				
				runOnUiThread(new Runnable(){
				    public void run(){
				    	progress.dismiss();
						if(connectionError){
							displayConnectionErrorDialog(bpErrorCode);
						}else{
							Intent intent = new Intent(classContext, BiopluxService.class);
							intent.putExtra(KEY_RECORDING_NAME, recording.getName());
							intent.putExtra(KEY_CONFIGURATION, recordingConfiguration);
							startService(intent);
							bindToService();
							startChronometer();
							uiMainbutton.setText(getString(R.string.nr_button_stop));
							displayInfoToast(getString(R.string.nr_info_started));
							drawState = false;
						}
				    }
				});
			}
		});
		
		if(recordingConfiguration.getMacAddress().compareTo("test")==0 && !isServiceRunning() && !recordingOverride)
			connectionThread.start();
		else if(mBluetoothAdapter.isEnabled() && !isServiceRunning() && !recordingOverride) {
			connectionThread.start();
		}
		return false;
	}
	
	/**
	 * Displays an error dialog with corresponding message based on the
	 * errorCode it receives. If code is unknown it displays FATAL ERROR message
	 */
	private void displayConnectionErrorDialog(int errorCode) {
		// Initializes custom title
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setBackgroundColor(getResources().getColor(R.color.error_dialog));
		switch(errorCode){
		case 1:
			connectionErrorDialog.setMessage(getResources().getString(R.string.bp_address_incorrect));
			break;
		case 2:
			connectionErrorDialog.setMessage(getResources().getString(R.string.bp_adapter_not_found));
			break;
		case 3:
			connectionErrorDialog.setMessage(getResources().getString(R.string.bp_device_not_found));
			break;
		case 4:
			connectionErrorDialog.setMessage(getResources().getString(R.string.bp_contacting_device));
			break;
		case 5:
			connectionErrorDialog.setMessage(getResources().getString(R.string.bp_port_could_not_be_opened));
			break;
		case 6:
			customTitleView.setText(R.string.nr_storage_error_dialog_title);
			connectionErrorDialog.setCustomTitle(customTitleView);
			connectionErrorDialog.setMessage(getResources().getString(R.string.bp_error_writing_a_frame));
			break;
		case 7:
			customTitleView.setText(R.string.nr_storage_error_dialog_title);
			connectionErrorDialog.setCustomTitle(customTitleView);
			connectionErrorDialog.setMessage(getResources().getString(R.string.bp_error_saving_recording));
			break;
		default:
			connectionErrorDialog.setMessage("FATAL ERROR");
			break;
		}
		connectionErrorDialog.show();
	}

	/**
	 * Displays a custom view information toast with the message it receives as
	 * parameter
	 */
	private void displayInfoToast(String messageToDisplay) {
		Toast infoToast = new Toast(classContext);
		View toastView = inflater.inflate(R.layout.toast_info, null);
		infoToast.setView(toastView);
		((TextView) toastView.findViewById(R.id.display_text)).setText(messageToDisplay);
		infoToast.show();
	}
	
	/**
	 * Starts Android' chronometer widget to display the recordings duration
	 */
	private void startChronometer() {
		chronometer.setBase(SystemClock.elapsedRealtime());
		chronometer.start();
	}

	/**
	 * Stops the chronometer and calculates the duration of the recording
	 */
	private void stopChronometer() {
		chronometer.stop();
		long elapsedMiliseconds = SystemClock.elapsedRealtime()
				- chronometer.getBase();
		duration = String.format("%02d:%02d:%02d",
				(int) ((elapsedMiliseconds / (1000 * 60 * 60)) % 24), 	// hours
				(int) ((elapsedMiliseconds / (1000 * 60)) % 60),	  	// minutes
				(int) (elapsedMiliseconds / 1000) % 60);				// seconds
	}

	/**
	 * Saves the recording on Android's internal Database with ORMLite
	 */
	public void saveRecordingOnInternalDB() {
		DateFormat dateFormat = DateFormat.getDateTimeInstance();
		Date date = new Date();

		recording.setConfiguration(recordingConfiguration);
		recording.setSavedDate(dateFormat.format(date));
		recording.setDuration(duration);
		try {
			Dao<DeviceRecording, Integer> dao = getHelper().getRecordingDao();
			dao.create(recording);
		} catch (SQLException e) {
			Log.e(TAG, "saving recording exception", e);
			displayConnectionErrorDialog(10); // 10 -> fatal error
		}
	}

	/**
	 * Gets all the processes that are running on the OS and checks whether the
	 * bioplux service is running. Returns true if it is running and false
	 * otherwise
	 */
	private boolean isServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (BiopluxService.class.getName().equals(service.service.getClassName()) && service.restarting == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Attaches connection with the service and passes the recording name and
	 * the correspondent configuration to it on its intent
	 */
	void bindToService() {
		Intent intent = new Intent(classContext, BiopluxService.class);
		bindService(intent, bindConnection, 0);
	}

	/**
	 * Detach our existing connection with the service
	 */
	void unbindFromService() {
		if (isServiceBounded) {
			unbindService(bindConnection);
			isServiceBounded = false;
		}
	}
	
	/**
	 * Main button of activity. Starts, overwrites and stops recording depending
	 * of whether the recording was never started, was started or was started
	 * and stopped
	 */
	public void onMainButtonClicked(View view) {
		// Starts recording
		if (!isServiceRunning() && !recordingOverride) {
			startRecording();
		// Overwrites recording
		} else if (!isServiceRunning() && recordingOverride) {
			showOverwriteDialog();
		// Stops recording
		} else if (isServiceRunning()) {
			recordingOverride = true;
			stopRecording();
		}
	}
	
	/************************  EVENTS **********************/
	
	public void onClikedMenuItems(View v) {
	    PopupMenu popup = new PopupMenu(this, v);
	    popup.setOnMenuItemClickListener(this);
	    MenuInflater inflater = popup.getMenuInflater();
	    inflater.inflate(R.menu.new_recording_menu, popup.getMenu());
	    popup.show();
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.nr_settings:
	        	Intent recordingSettingsIntent = new Intent(this, SettingsActivity.class);
	        	recordingSettingsIntent.putExtra(Constants.KEY_SETTINGS_TYPE, 2);
	        	recordingSettingsIntent.putExtra(Constants.KEY_SETTINGS_DRAW_STATE, drawState);
	        	startActivity(recordingSettingsIntent);
	            return true;
	        case R.id.nr_restore_zoom:
	        	long startValue = 0;
	        	for (int i = 0; i < graphs.length; i++){
	        		if(graphs[i].getxValue() - Long.valueOf(getResources().getString(
							R.string.graph_viewport_size)) < Long.valueOf(getResources().getString(
							R.string.graph_viewport_size)))
	        			startValue = graphs[i].getxValue();
	        		else
	        			startValue = graphs[i].getxValue() - Long.valueOf(getResources().getString(
								R.string.graph_viewport_size));
	        			
	        		graphs[i].getGraphView().setViewPort(startValue, Long.valueOf(getResources().getString(
							R.string.graph_viewport_size)));
	        		graphs[i].getGraphView().redrawAll();
	        	}
	    			
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.compareTo(SettingsActivity.KEY_PREF_ZOOM_VALUE) == 0)
			currentZoomValue = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_ZOOM_VALUE, "150"));
	}

	/**
	 * Widens the graphs' view port
	 */
	public void zoomIn(View view){
		for (int i = 0; i < graphs.length; i++)
			graphs[i].getGraphView().zoomIn(currentZoomValue); 
	}
	
	/**
	 * Shortens the graphs' view port
	 */
	public void zoomOut(View view){
		double zoomOutValue = 66.6;
		if(currentZoomValue == 200)
			zoomOutValue = 50;
		else if(currentZoomValue == 300)
			zoomOutValue = 33.3;
		else if(currentZoomValue == 400)
			zoomOutValue = 25;
		for (int i = 0; i < graphs.length; i++)
			graphs[i].getGraphView().zoomOut(zoomOutValue); 
	}
	
	
	@Override
	protected void onPause() {
		try {
			unbindFromService();
		} catch (Throwable t) {
			Log.e(TAG, "failed to unbind from service when activity is destroyed", t);
			displayConnectionErrorDialog(10); // 10 -> fatal error
		}
		super.onPause();
		Log.i(TAG, "onPause()");
	}
	

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSavedInstance");
		outState.putLong(KEY_CHRONOMETER_BASE, chronometer.getBase());
		super.onSaveInstanceState(outState);
	}

	/**
	 * Destroys activity
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy()");
	}
}
