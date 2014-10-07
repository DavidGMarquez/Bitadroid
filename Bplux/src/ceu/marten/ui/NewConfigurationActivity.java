package ceu.marten.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import ceu.marten.bitadroid.R;
import ceu.marten.model.DeviceConfiguration;
import ceu.marten.ui.adapters.ActiveChannelsListAdapter;
import ceu.marten.ui.adapters.DisplayChannelsListAdapter;

/**
 * Class used for the creation of a new device configuration. It receives all
 * the configurations from configurationsActivity to check for duplicate names.
 * If the activity gets a 'position' extra on its intent, it means
 * that it is created to modify a configuration
 * 
 * @author Carlos Marten
 * @see DeviceConfiguration
 */
public class NewConfigurationActivity extends Activity {

	// FREQUENCIES MIN, MAX AND DEFUALT VALUES 
	private static final int RECEPTION_FREQ_MAX = 1000;
	private static final int RECEPTION_FREQ_MIN = 1;
	private static final int DEFAULT_RECEPTION_FREQ = 100;
	private static final int SAMPLING_FREQ_MAX = 100;
	private static final int SAMPLING_FREQ_MIN = 1;
	private static final int DEFAULT_SAMPLING_FREQ = 50;
	private static final int DEFAULT_NUMBER_OF_BITS = 12;
	 private static final TreeSet<Integer> ALLOW_RECEPTION_FREQ = new TreeSet<Integer>(Arrays.asList(1,10,100,1000));
	    	    
	

	// ANDROID' WIDGETS AND ITS LAYOUT INFLATER
	private SeekBar receptionfreqSeekbar, samplingfreqSeekbar;
	private EditText configurationName, macAddress, receptionFreqEditor, samplingFreqEditor;
	private TextView activeChannelsTV, displayChannelsTV;
	private LayoutInflater inflater;

	private String[]  activeChannels = null;
	private boolean[] channelsSelected = null;
	private boolean isUpdatingConfiguration = false;
	
	// CONFIGURATIONS
	private ArrayList<DeviceConfiguration> configurations;
	private DeviceConfiguration newConfiguration, oldConfiguration;

	/**
	 * Initializes variables and widgets of the activity. The saveInstanceState
	 * bundle has always all the configurations. And if the activity is created
	 * to modify a configuration it also has the position of the configuration
	 * the user wish to update
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ly_new_configuration);

		inflater = this.getLayoutInflater();
		configurations = new ArrayList<DeviceConfiguration>();
		configurations = (ArrayList<DeviceConfiguration>) getIntent()
				.getExtras().getSerializable(ConfigurationsActivity.KEY_CONFIGURATIONS);

		// CREATES AND SETS DEFAULT VALUES FOR THE NEW CONFIGURATION
		newConfiguration = new DeviceConfiguration(this);
		newConfiguration.setVisualizationFrequency(DEFAULT_RECEPTION_FREQ);
		newConfiguration.setSamplingFrequency(DEFAULT_SAMPLING_FREQ);
		newConfiguration.setNumberOfBits(DEFAULT_NUMBER_OF_BITS);
		
		
		// GETS ALL THE VIEWS
		receptionfreqSeekbar = (SeekBar) findViewById(R.id.nc_reception_seekbar);
		samplingfreqSeekbar = (SeekBar) findViewById(R.id.nc_sampling_seekbar);
		receptionFreqEditor = (EditText) findViewById(R.id.nc_reception_freq_view);
		samplingFreqEditor = (EditText) findViewById(R.id.nc_sampling_freq_view);
		configurationName = (EditText) findViewById(R.id.dev_name);
		macAddress = (EditText) findViewById(R.id.nc_mac_address);
		activeChannelsTV = (TextView) findViewById(R.id.nc_txt_active_channels);
		displayChannelsTV = (TextView) findViewById(R.id.nc_txt_channels_to_show);
		

		// INIT SEEKBARS AND EDITORS TO DEFAULT STATE
		receptionfreqSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean changedByUser) {
						if (changedByUser) {
							int valueToChangeRounded=ALLOW_RECEPTION_FREQ.floor(progress+1)-RECEPTION_FREQ_MIN;
							Log.i("TRYANDROID", "samplingprogress "+		progress+" floor:"+valueToChangeRounded);
							
							receptionFreqEditor.setText(String.valueOf(valueToChangeRounded + RECEPTION_FREQ_MIN));
							newConfiguration.setVisualizationFrequency(valueToChangeRounded + RECEPTION_FREQ_MIN);
						}
					}
					// needed for the listener
					public void onStartTrackingTouch(SeekBar seekBar) {}
					public void onStopTrackingTouch(SeekBar seekBar) {}
				});
		
		receptionFreqEditor.setOnEditorActionListener(new OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView receptionFreqEditorTextView,int actionId, KeyEvent event) {
						boolean handled = false;

						if (actionId == EditorInfo.IME_ACTION_DONE) {
							String frequencyString = receptionFreqEditorTextView.getText().toString();
							if (frequencyString.compareTo("") == 0)
								frequencyString = "0";
							int newFrequency = Integer.parseInt(frequencyString);

							setReceptionFrequency(newFrequency);
							closeKeyboardAndClearFocus();
							handled = true;
						}
						return handled;
					}
					
					private void setReceptionFrequency(int newFrequency) {
						// accepted frequency
						if (newFrequency >= RECEPTION_FREQ_MIN && newFrequency <= RECEPTION_FREQ_MAX) {
							receptionfreqSeekbar.setProgress((newFrequency - RECEPTION_FREQ_MIN));
							newConfiguration.setVisualizationFrequency(newFrequency);
						// frequency introduced is too big
						} else if (newFrequency > RECEPTION_FREQ_MAX) {
							receptionfreqSeekbar.setProgress(RECEPTION_FREQ_MAX);
							receptionFreqEditor.setText(String.valueOf(RECEPTION_FREQ_MAX));
							newConfiguration.setVisualizationFrequency(RECEPTION_FREQ_MAX);
							displayErrorToast(getString(R.string.nc_error_max_frequency) + " "+ RECEPTION_FREQ_MAX + "Hz");
						// frequency introduced is too small
						} else {
							receptionfreqSeekbar.setProgress(0);
							receptionFreqEditor.setText(String.valueOf(RECEPTION_FREQ_MIN));
							newConfiguration.setVisualizationFrequency(RECEPTION_FREQ_MIN);
							displayErrorToast(getString(R.string.nc_error_min_frequency) + " "+ RECEPTION_FREQ_MIN + " Hz");
						}
					}

					private void closeKeyboardAndClearFocus() {
						receptionFreqEditor.clearFocus();
						InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

					}
				});

		samplingfreqSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean changedByUser) {
						if (changedByUser) {
							samplingFreqEditor.setText(String.valueOf(progress + SAMPLING_FREQ_MIN));
							newConfiguration.setSamplingFrequency(progress + SAMPLING_FREQ_MIN);
						}
					}
					// needed for the listener
					public void onStartTrackingTouch(SeekBar seekBar) {}
					public void onStopTrackingTouch(SeekBar seekBar)  {}
				});

		samplingFreqEditor.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView currentView, int actionId, KeyEvent event) {
				boolean handled = false;

				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String frequencyString = currentView.getText().toString();
					if (frequencyString.compareTo("") == 0)
						frequencyString = "0";
					int newFrequency = Integer.parseInt(frequencyString);

					setFrequency(newFrequency);
					closeKeyboardAndClearFocus();
					handled = true;
				}
				return handled;
			}

			private void setFrequency(int newFrequency) {
				// accepted frequency
				if (newFrequency >= SAMPLING_FREQ_MIN && newFrequency <= SAMPLING_FREQ_MAX) {
					samplingfreqSeekbar.setProgress((newFrequency - SAMPLING_FREQ_MIN));
					newConfiguration.setSamplingFrequency(newFrequency);
				// frequency introduced is too big
				} else if (newFrequency > SAMPLING_FREQ_MAX) {
					samplingfreqSeekbar.setProgress(SAMPLING_FREQ_MAX);
					samplingFreqEditor.setText(String.valueOf(SAMPLING_FREQ_MAX));
					newConfiguration.setSamplingFrequency(SAMPLING_FREQ_MAX);
					displayErrorToast(getString(R.string.nc_error_max_frequency)+ " " + SAMPLING_FREQ_MAX + "Hz");
				// frequency introduced is too small
				} else {
					samplingfreqSeekbar.setProgress(0);
					samplingFreqEditor.setText(String.valueOf(SAMPLING_FREQ_MIN));
					newConfiguration.setSamplingFrequency(SAMPLING_FREQ_MIN);
					displayErrorToast(getString(R.string.nc_error_min_frequency)+ " " + SAMPLING_FREQ_MIN + " Hz");
				}
			}

			private void closeKeyboardAndClearFocus() {
				samplingFreqEditor.clearFocus();
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});
		
		
		/* ********* UPDATE CONFIGURATION CODE ********* */
		if(getIntent().getExtras().containsKey(ConfigurationsActivity.KEY_CONFIGURATION_POSITION)) 
			isUpdatingConfiguration = true;
		
		if(isUpdatingConfiguration){
			oldConfiguration = configurations.get(getIntent().getExtras().getInt(ConfigurationsActivity.KEY_CONFIGURATION_POSITION)); 
			//FILL WIDGETS FIELDS WITH CONFIGURATION TO EDIT DETAILS
			configurationName.setText(oldConfiguration.getName());
			macAddress.setText(oldConfiguration.getMacAddress());
			receptionfreqSeekbar.setProgress(oldConfiguration.getVisualizationFrequency()- RECEPTION_FREQ_MIN);
			receptionFreqEditor.setText(String.valueOf(oldConfiguration.getVisualizationFrequency()));
			samplingfreqSeekbar.setProgress(oldConfiguration.getSamplingFrequency());
			samplingFreqEditor.setText(String.valueOf(oldConfiguration.getSamplingFrequency()));
			activeChannelsTV.setVisibility(View.VISIBLE);
			activeChannelsTV.setText(getString(R.string.nc_channels_to_activate)+" "+oldConfiguration.getActiveChannels());
			displayChannelsTV.setVisibility(View.VISIBLE);
			displayChannelsTV.setText(oldConfiguration.getDisplayChannelsWithSensors());
			
			// MODIFY VARIABLES FOR VALIDATION PURPOSES
			activeChannels = oldConfiguration.getActiveSensors();
			boolean[] boolArray = {true};
			channelsSelected = boolArray;
			configurations.remove(configurations.get(getIntent().getExtras().getInt(ConfigurationsActivity.KEY_CONFIGURATION_POSITION))); 
			newConfiguration = oldConfiguration;
		}
	}

	/**
	 * When back button pressed, it means that the user wish to cancel the
	 * configuration, so it displays an info toast letting the user know that it
	 * was canceled
	 * 
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
		displayInfoToast(getString(R.string.nc_info_canceled));
	}

	/**
	 * Sets up and shows the active channels picker dialog
	 * 
	 */
	private void showActiveChannelsDialog() {

		// get channel numbers from values, converts them to list and pass as an attribute to the adapter
		String[] rawChannelNumbers = getResources().getStringArray(R.array.channels);
		List<String> channelNumbersAL = Arrays.asList(rawChannelNumbers);
		final ActiveChannelsListAdapter activeChannelsListAdapter = new ActiveChannelsListAdapter(
				this, channelNumbersAL, newConfiguration.getActiveSensors());

		// custom title for dialog
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.nc_dialog_title_channels_to_activate);

		// ACTIVE CHANNELS BUILDER
		AlertDialog.Builder activeChannelsBuilder;
		activeChannelsBuilder = new AlertDialog.Builder(this)
				.setCustomTitle(customTitleView).setView(getLayoutInflater().inflate(R.layout.dialog_channels_listview, null))
				.setPositiveButton(getString(R.string.nc_dialog_positive_button),
						new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// reset display channels because active channels have changed. Leaves error message if there was one
							if(displayChannelsTV.getError() == null){
								displayChannelsTV.setText("");
								displayChannelsTV.setVisibility(View.GONE);
								channelsSelected = null;
							}
							// get active channels from adapter and sets it to the new configuration
							activeChannels = activeChannelsListAdapter.getChecked();
							newConfiguration.setActiveChannels(activeChannels);
							newConfiguration.setDisplayChannels(activeChannels);
						
							if (noChannelsActivated(activeChannels)){
								if(activeChannelsTV.getError() == null){
									activeChannelsTV.setText("");
									activeChannelsTV.setVisibility(View.GONE);
								}
									
							}else{
								activeChannelsTV.setVisibility(View.VISIBLE);
								activeChannelsTV.setError(null);
								activeChannelsTV.setTextColor(getResources().getColor(R.color.blue));
								activeChannelsTV.setText(getString(R.string.nc_channels_to_activate));
								activeChannelsTV.append("  " + newConfiguration.getActiveChannels().toString());
							}
						}
				})
		.setNegativeButton(getString(R.string.nc_dialog_negative_button),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//dismisses the dialog
					}
				});

		// creates and shows dialog before setting the list view so that the views are inflated
		AlertDialog activeChannelsDialog = activeChannelsBuilder.create();
		activeChannelsDialog.show();

		ListView activeChannelsListView = (ListView) activeChannelsDialog.findViewById(R.id.lv_channelsSelection);
		activeChannelsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		activeChannelsListView.setItemsCanFocus(false);
		activeChannelsListView.setAdapter(activeChannelsListAdapter);
	}

	/**
	 * Creates and shows a dialog for picking which of the active channels the
	 * user wants to show on his android device. At least one is required
	 */
	private void showDisplayChannels() {
	
		final ArrayList<String> channels = new ArrayList<String>(); //example {channel 1, channel 5}
		final ArrayList<String> sensors = new ArrayList<String>();  //example {blood pressure, temperature}
		
		// FILL THE CHANNELS AND SENSORS ARRAYS FOR THE LIST ADAPTER
		String[] activeSensors = newConfiguration.getActiveSensors();
		for (int i = 0; i < activeSensors.length; i++) {
			if (activeSensors[i].compareTo("null") != 0) {
				channels.add(getString(R.string.nc_dialog_channel) + " "+ (i + 1));
				sensors.add(activeSensors[i]);
			}
		}
	
		final DisplayChannelsListAdapter displayChannelsListAdapter = new DisplayChannelsListAdapter(this, channels, sensors, newConfiguration.getDisplayChannels());
		AlertDialog displayChannelsDialog;
	
		TextView customTitleView = (TextView) inflater.inflate(R.layout.dialog_custom_title, null);
		customTitleView.setText(R.string.nc_dialog_title_channels_to_display);
	
		// BUILDER
		AlertDialog.Builder displayChannelsBuilder = new AlertDialog.Builder(this);
		displayChannelsBuilder.setCustomTitle(customTitleView).setView(getLayoutInflater().inflate(R.layout.dialog_channels_listview, null))
				.setPositiveButton(getString(R.string.nc_dialog_positive_button), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							channelsSelected = displayChannelsListAdapter.getChecked();
							String[] displayChannels = new String[8];
	
							if (numberOfChannelsSelected(channelsSelected) == 0){
								displayChannelsTV.setText("");
								displayChannelsTV.setVisibility(View.GONE);
							}
								
							else{
								displayChannelsTV.setVisibility(View.VISIBLE);
								displayChannelsTV.setError(null);
								displayChannelsTV.setTextColor(getResources().getColor(R.color.blue));
								convertBooleanToString(displayChannels, channelsSelected);
								newConfiguration.setDisplayChannels(displayChannels);
								displayChannelsTV.setText(newConfiguration.getDisplayChannelsWithSensors());
							}
						}
					private void convertBooleanToString(String[] channelsToDisplayArray, boolean[] channelsSelected) {
						for (int i = 0; i < channelsSelected.length; i++) {
							if (channelsSelected[i]) {
								int in = Character.getNumericValue((channels.get(i).toString().charAt(channels.get(i).toString().length() - 1)) - 1);
								channelsToDisplayArray[in] = sensors.get(i);
							}
						}
					}
				});
		displayChannelsBuilder.setNegativeButton(getString(R.string.nc_dialog_negative_button), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//dismisses the dialog
					}
				});
	
		// CREATE DIALOG
		displayChannelsDialog = displayChannelsBuilder.create();
		displayChannelsDialog.show();
	
		// LIST VIEW CONFIGURATION
		ListView channelsToDisplayListView;
		channelsToDisplayListView = (ListView) displayChannelsDialog.findViewById(R.id.lv_channelsSelection);
		channelsToDisplayListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		channelsToDisplayListView.setItemsCanFocus(false);
		channelsToDisplayListView.setAdapter(displayChannelsListAdapter);
	
	}

	/**
	 * Returns true if <b>no</b> channels are activated and false if there is at least one activated
	 */
	private boolean noChannelsActivated(String[] channelsActivated) {
		int counter = 0;
		for (int i = 0; i < channelsActivated.length; i++) {
			if (channelsActivated[i] != null)
				counter++;
		}
		if (counter == 0){
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Returns 0 if there are no channels activated
	 * @param channelsSelected boolean[]
	 * @return number of channels selected 
	 */
	private int numberOfChannelsSelected(boolean[] channelsSelected) {
		int counter = 0;
		for (int i = 0; i < channelsSelected.length; i++) {
			if (channelsSelected[i]) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 * Creates and displays a custom error toast with custom message that it receives
	 */
	private void displayErrorToast(String messageToDisplay) {
		Toast errorToast = new Toast(getApplicationContext());

		View toastView = inflater.inflate(R.layout.toast_error, null);
		errorToast.setView(toastView);
		((TextView) toastView.findViewById(R.id.display_text)).setText(messageToDisplay);

		errorToast.show();
	}

	/**
	 * Creates and displays a custom information toast with custom message that
	 * it receives
	 */
	private void displayInfoToast(String messageToDisplay) {
		Toast infoToast = new Toast(getApplicationContext());

		View toastView = inflater.inflate(R.layout.toast_info, null);
		infoToast.setView(toastView);
		((TextView) toastView.findViewById(R.id.display_text)).setText(messageToDisplay);

		infoToast.show();
	}

	/**
	 * Validates all the configuration fields. 
	 * Returns false if an error is encountered and true otherwise
	 */
	private boolean validateFields() {
		boolean validated = true;
	
		// VALIDATE NAME FIELD ALREADY EXISTS 
			for (DeviceConfiguration c : configurations) {
				if (c.getName().compareTo(configurationName.getText().toString()) == 0) {
					configurationName.setError(getString(R.string.nc_error_message_name_duplicate));
					configurationName.requestFocus();
					validated = false;
				}
			}
		
		// VALIDATE NAME FIELD IS NOT NULL
		if (configurationName.getText().toString() == null || configurationName.getText().toString().compareTo("") == 0) {
			configurationName.setError(getString(R.string.nc_error_message_name_null));
			configurationName.requestFocus();
			validated = false;
		}
	
		// VALIDATE MAC FIELD
		String macSyntax = "^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$";
		if (macAddress.getText().toString() == null || macAddress.getText().toString().compareTo("") == 0 
													|| !macAddress.getText().toString().matches(macSyntax)
													&& macAddress.getText().toString().compareTo("test") != 0) {
			macAddress.setError(getString(R.string.nc_error_message_mac));
			if (validated){
				macAddress.requestFocus();
			}
			validated = false;
		}
		
		// VALIDATE RECEPTION FREQUENCY
		if (receptionFreqEditor.getText().toString() == null || 
				receptionFreqEditor.getText().toString().compareTo("") == 0 
				|| Integer.parseInt(receptionFreqEditor.getText().toString()) < RECEPTION_FREQ_MIN
				|| Integer.parseInt(receptionFreqEditor.getText().toString()) > RECEPTION_FREQ_MAX) {
			receptionFreqEditor.setError("invalid frequency");
			if (validated){
				receptionFreqEditor.requestFocus();
			}
			validated = false;
		}
		
		// VALIDATE SAMPLING FREQUENCY
		if (samplingFreqEditor.getText().toString() == null || 
				receptionFreqEditor.getText().toString().compareTo("") == 0 
				|| Integer.parseInt(samplingFreqEditor.getText().toString()) < SAMPLING_FREQ_MIN
				|| Integer.parseInt(samplingFreqEditor.getText().toString()) > SAMPLING_FREQ_MAX) {
			samplingFreqEditor.setError("invalid frequency");
			if (validated){
				samplingFreqEditor.requestFocus();
			}
			validated = false;
		}
			
		// VALIDATE ACTIVE CHANNELS
		if (activeChannels == null || noChannelsActivated(activeChannels)) {
			activeChannelsTV.setError("");
			activeChannelsTV.setTextColor(Color.RED);
			activeChannelsTV.setVisibility(View.VISIBLE);
			activeChannelsTV.setText(getString(R.string.nc_error_message_active_channels)+ "  ");
			validated = false;
		}
	
		// VALIDATE CHANNELS TO DISPLAY
		if (channelsSelected == null || numberOfChannelsSelected(channelsSelected) == 0) {
			displayChannelsTV.setError("");
			displayChannelsTV.setTextColor(Color.RED);
			displayChannelsTV.setVisibility(View.VISIBLE);
			displayChannelsTV.setText(getString(R.string.nc_error_message_channels_to_display)+ "  ");
			validated = false;
		}
		return validated;
	}
	
	/*************************** BUTTON EVENTS ****************************/

	/**
	 * Validates, submits and finishes the activity returning focus to its
	 * parent activity {@link ConfigurationsActivity}
	 */
	public void onClickedSubmit(View submitButtonView) {
		
		newConfiguration.setName(configurationName.getText().toString());
		newConfiguration.setMacAddress(macAddress.getText().toString());
		newConfiguration.setVisualizationFrequency(Integer.parseInt(receptionFreqEditor.getText().toString()));
		newConfiguration.setSamplingFrequency(Integer.parseInt(samplingFreqEditor.getText().toString()));

		if(!isUpdatingConfiguration){
			DateFormat dateFormat = DateFormat.getDateTimeInstance();
			Date date = new Date();
			newConfiguration.setCreateDate(dateFormat.format(date));
		}
		
		if (validateFields()) {
			Intent returnIntent = new Intent();
			returnIntent.putExtra(ConfigurationsActivity.KEY_CONFIGURATIONS, newConfiguration);
		
			if(!isUpdatingConfiguration)
				displayInfoToast(getString(R.string.nc_info_created));
			else{
				returnIntent.putExtra(ConfigurationsActivity.KEY_OLD_CONFIGURATION, oldConfiguration);
				displayInfoToast(getString(R.string.nc_info_modified));
			}
			setResult(RESULT_OK, returnIntent);
			finish();
			overridePendingTransition(R.anim.slide_in_top,R.anim.slide_out_bottom);
		}
	}

	/**
	 * Cancels the creation/modification of new {@link DeviceConfiguration}.
	 * Displays an info toast to inform the client that its configuration has
	 * been canceled
	 */
	public void onClickedCancel(View cancelButtonView) {
		finish();
		overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
		displayInfoToast(getString(R.string.nc_info_canceled));
	}

	/**
	 * Shows active channels dialog calling
	 * {@link NewConfigurationActivity#showActiveChannelsDialog()}
	 */
	public void onClickedActiveChannelsButton(View activeChannelsButton) {
		showActiveChannelsDialog();
	}

	/**
	 * Shows display channels picker dialog or displays an error toast if active
	 * channels have not yet been introduced
	 */
	public void onClickedDisplayChannelsButton(View displayChannelsButton) {
		// CHECKS IF CHANNELS TO ACTIVATE ARE ALREADY FILLED
		if(newConfiguration.getActiveSensors() != null && newConfiguration.getActiveChannelsNumber() != 0){
			showDisplayChannels();
		}
		else{
			displayErrorToast(getString(R.string.nc_error_button_channels_to_display));
		}
	}
}
