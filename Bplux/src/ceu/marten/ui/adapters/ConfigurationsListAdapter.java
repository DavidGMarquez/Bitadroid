package ceu.marten.ui.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ceu.marten.bitadroid.R;
import ceu.marten.model.DeviceConfiguration;

import com.haarman.listviewanimations.ArrayAdapter;

public class ConfigurationsListAdapter extends ArrayAdapter<DeviceConfiguration> {

	private final Context context;

	public ConfigurationsListAdapter(Context context,
			ArrayList<DeviceConfiguration> loadedDevices) {
		super(loadedDevices);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewGroup rowView = (ViewGroup) convertView;
		if (rowView == null) {
			rowView = (ViewGroup) LayoutInflater.from(context).inflate(
					R.layout.li_configuration, parent, false);
		}

		TextView name = (TextView) rowView.findViewById(R.id.dli_name);
		TextView receptionFreq = (TextView) rowView.findViewById(R.id.dli_reception_freq);
		TextView samplingFreq = (TextView) rowView.findViewById(R.id.dli_sampling_freq);
		TextView mac = (TextView) rowView.findViewById(R.id.dli_mac);
		TextView bits = (TextView) rowView.findViewById(R.id.dli_nbits);
		TextView date = (TextView) rowView.findViewById(R.id.dli_date);
		TextView activeChannels = (TextView) rowView.findViewById(R.id.dli_active_channels);
		TextView channelsToDisplay = (TextView) rowView.findViewById(R.id.dli_channels_to_display);

		DeviceConfiguration configuration = getItem(position);

		name.setText(configuration.getName());
		receptionFreq.setText(String.valueOf(configuration.getVisualizationFrequency()) + " Hz");
		samplingFreq.setText(String.valueOf(configuration.getSamplingFrequency()) + " Hz");
		mac.setText(configuration.getMacAddress());
		bits.setText(String.valueOf(configuration.getNumberOfBits()) + " bits");
		date.setText(configuration.getCreateDate());
		activeChannels.setText(context.getString(R.string.cl_active) + " " + configuration.getActiveChannels().toString());
		channelsToDisplay.setText(context.getString(R.string.cl_display)
				+ " " + configuration.getDisplayChannels().toString());
		
		return rowView;
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).hashCode();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

}