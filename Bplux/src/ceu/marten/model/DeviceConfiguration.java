package ceu.marten.model;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;
import ceu.marten.bitadroid.R;
import ceu.marten.model.io.DatabaseHelper;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a Bioplux device configuration Stored in Android's internal 
 * Database with ORMlite annotations 
 * Implements Serializable to transfer instances between activities
 * 
 * If this class is updated. Make sure to update also database version in {@link DatabaseHelper}
 * 
 * @author Carlos Marten
 */

@DatabaseTable(tableName = "DeviceConfigurations")
public class DeviceConfiguration implements Serializable {

	// Used for unique serializable purposes
	private static final long serialVersionUID = -4487071327586521666L;

	private static Context context;
	private static final String SPLIT_PATTERN = "\\*\\.\\*";
	public static final String DATE_FIELD_NAME = "createDate";

	@DatabaseField(generatedId = true)
	private Integer id;

	@DatabaseField(unique = true, canBeNull = true)
	private String name = null;

	@DatabaseField(canBeNull = true)
	private String macAddress = null;

	@DatabaseField(canBeNull = true)
	private String createDate = null;

	@DatabaseField(canBeNull = true)
	private int visualizationFrequency = 0;

	@DatabaseField(canBeNull = true)
	private int samplingFrequency = 0;

	// number of bits can be 8 (default) or 12 [0-255] | [0-4095]
	@DatabaseField(canBeNull = true)
	private int numberOfBits = 8;

	@DatabaseField(dataType = DataType.BYTE_ARRAY)
	private byte[] activeChannels = null;

	@DatabaseField(dataType = DataType.BYTE_ARRAY)
	private byte[] displayChannels = null;

	/**
	 * Empty constructor Needed by the OrmLite to generate object when query invoked
	 */
	public DeviceConfiguration() {}
	
	public DeviceConfiguration(Context _context) {
		DeviceConfiguration.context = _context;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}

	public String getCreateDate() {
		return createDate;
	}

	public void setVisualizationFrequency(int frequency) {
		this.visualizationFrequency = frequency;
	}

	public int getVisualizationFrequency() {
		return visualizationFrequency;
	}

	public void setSamplingFrequency(int samplingFrequency) {
		this.samplingFrequency = samplingFrequency;
	}

	public int getSamplingFrequency() {
		return samplingFrequency;
	}

	public void setNumberOfBits(int numberOfBits) {
		this.numberOfBits = numberOfBits;
	}

	public int getNumberOfBits() {
		return numberOfBits;
	}

	/**
	 * Sets the channels to display transforming String[8] to byte[]
	 * 
	 * @param displayChannelsB
	 */
	public void setDisplayChannels(String[] displayChannels) {
		// transform String[] to StringBuilder and that to Byte[]
		StringBuilder displayChannelsSB = new StringBuilder();
		for (int i = 0; i < displayChannels.length; i++) {
			displayChannelsSB.append(displayChannels[i]);
			if (i != displayChannels.length - 1) {
				// concatenate by this splitter '.'
				displayChannelsSB.append("*.*");
			}
		}
		this.displayChannels = displayChannelsSB.toString().getBytes();
	}

	/**
	 * Get the channels to display
	 * 
	 * @return channels to display as an ArrayList of Integers or 'null' if
	 *         there are none
	 */

	public ArrayList<Integer> getDisplayChannels() {
		if (this.displayChannels == null){
			return null;
		}
		else {
			String displayChannelsConcatenated = new String(this.displayChannels);
			String[] displayChannelsSplitted = displayChannelsConcatenated.split(SPLIT_PATTERN);
			ArrayList<Integer> displayChannels = new ArrayList<Integer>();
			int channelNumber = 1;
			for (String stringDisplayChannel : displayChannelsSplitted) {
				if (stringDisplayChannel.compareTo("null") != 0){
					displayChannels.add(channelNumber);
				}
				channelNumber++;
			}
			return displayChannels;
		}

	}
	
	
	/**
	 * Get the channels to display
	 * 
	 * @return channels to display as an ArrayList of Integers or 'null' if there are none
	 */

	public String getDisplayChannelsWithSensors() {
		if (this.displayChannels == null){
			return null;
		}
		else {
			String displayChannelsConcatenated = new String(this.displayChannels);
			String[] displayChannelsSplitted = displayChannelsConcatenated.split(SPLIT_PATTERN);
			StringBuilder displayChannelsSB = new StringBuilder();
			int channelNumber = 1;
			for (String s : displayChannelsSplitted) {
				if (s.compareTo("null") != 0){
					displayChannelsSB.append(context
							.getString(R.string.nc_dialog_channel) + " " + channelNumber + " "
							+ context.getString(R.string.nc_dialog_with_sensor) + " " + s + "\n");
				}
				channelNumber++;
			}
			return displayChannelsSB.toString();
		}
	}
	

	/**
	 * 
	 * @return number of channels to display as a natural number [0-8]
	 */
	public int getDisplayChannelsNumber() {
		int numberOfChannelsToDisplay = 0;
		String entire = new String(this.displayChannels);
		String[] channelsToDisplay = entire.split(SPLIT_PATTERN);
		for (String s : channelsToDisplay) {
			if (s.compareTo("null")!=0){
				numberOfChannelsToDisplay++;
			}
		}
		return numberOfChannelsToDisplay;
	}

	/**
	 * Sets the active channels for the configuration Transforms String[] to
	 * Byte[] to save on internal DB
	 */
	public void setActiveChannels(String[] activeChannelsStr) {
		StringBuilder activeChannelsSB = new StringBuilder();
		for (int i = 0; i < activeChannelsStr.length; i++) {
			activeChannelsSB.append(activeChannelsStr[i]);
			if (i != activeChannelsStr.length - 1) {
				activeChannelsSB.append("*.*");
			}
		}
		this.activeChannels = activeChannelsSB.toString().getBytes();
	}

	/**
	 * Gets the active sensors of the configuration with null fill
	 * 
	 * @return the active channels or null if there are none
	 */
	public String[] getActiveSensors() {
		if (this.activeChannels != null) {
			String activeChannelsConcatenated = new String(this.activeChannels);
			return activeChannelsConcatenated.split(SPLIT_PATTERN);
		} else{
			return null;
		}
	}

	/**
	 * Gets the active channels of the configuration
	 * 
	 * @return the active channels or null if there are none
	 */
	public ArrayList<Integer> getActiveChannels() {
		ArrayList<Integer> activeChannels = new ArrayList<Integer>();
		String[] activeChannelsStr;
		if (this.activeChannels != null) {
			// returns active channels concatenated by '.' and with 'null' fill
			String activeChannelsConcatenated = new String(this.activeChannels);
			activeChannelsStr = activeChannelsConcatenated.split(SPLIT_PATTERN);
			for (int i = 0; i < activeChannelsStr.length; i++) {
				if (activeChannelsStr[i].compareToIgnoreCase("null") != 0){
					activeChannels.add(i + 1);
				}
			}
			return activeChannels;
		} else{
			return null;
		}
	}
	

	/**
	 * Gets the active channels as an integer [0-255] for bioplux API
	 * 
	 * @return active channels integer or 0 if there are none activated
	 */
	public int getActiveChannelsAsInteger() {
		int activeChannelsInteger = 0;
		String[] activeChannelsStr;
		if (this.activeChannels != null) {
			String activeChannelsConcatenated = new String(this.activeChannels);
			activeChannelsStr = activeChannelsConcatenated.split(SPLIT_PATTERN);
			for (int i = 0; i < activeChannelsStr.length; i++) {
				if (activeChannelsStr[i].compareToIgnoreCase("null") != 0){
					activeChannelsInteger += Math.pow(2, i);
				}
			}
			return activeChannelsInteger;
		} else{
			return 0;
		}
	}

	/**
	 * Gets the number of channels activated [1-8]
	 * 
	 * @return number of channels activated or 0 if there are none
	 */
	public int getActiveChannelsNumber() {
		int activeChannelsNumber = 0;
		if (this.activeChannels != null) {
			String activeChannelsConcatenated = new String(this.activeChannels);
			String[] activeChannelsStr = activeChannelsConcatenated
					.split(SPLIT_PATTERN);
			for (int i = 0; i < activeChannelsStr.length; i++) {
				if (activeChannelsStr[i].compareToIgnoreCase("null") != 0){
					activeChannelsNumber++;
				}
			}
			return activeChannelsNumber;
		} else{
			return 0;
		}
	}
}
