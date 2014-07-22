package ceu.marten.ui.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import ceu.marten.bitadroid.R;

public class DisplayChannelsListAdapter extends ArrayAdapter<String>
		implements OnClickListener {

	private final Context context;
	private boolean[] channelsToDisplay;
	private ArrayList<String> channels;
	private ArrayList<String> sensors;
	private boolean[] checkedStatus;

	//CONSTRUCTOR 
	public DisplayChannelsListAdapter(Context context,
			ArrayList<String> channels, ArrayList<String> sensors, ArrayList<Integer> channelsToDisplayChecked) {
		super(context, 0, 0, channels);
		this.context = context;
		this.channels = channels;
		this.sensors = sensors;
		this.checkedStatus = new boolean[channels.size()];
		this.channelsToDisplay = new boolean[channels.size()];
		
		//INIT ALL CHANNELSTODISPLAY VARIABLE TO FALSE
		for (int i = 0; i < channelsToDisplay.length; i++)
			channelsToDisplay[i] = false;
		
		//CHECK ALL THE CHECKBOXES THAT WERE CHECKED BEFORE
		if(channelsToDisplayChecked!=null){
			for(int i=0;i < channelsToDisplayChecked.size();i++){
				for(int j=0; j< channels.size();j++)
					if(channels.get(j).contains(String.valueOf(channelsToDisplayChecked.get(i)))){
						checkedStatus[j]=true;
						channelsToDisplay[j]=true;
					}
			}
		}
		
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		//INFLATE ROW VIEW XML
		ViewGroup rowView = (ViewGroup) convertView;
		if (rowView == null) {
			rowView = (ViewGroup) LayoutInflater.from(context).inflate(
					R.layout.li_channels_to_display, parent, false);
		}

		// GET VIEWS
		TextView channelNumber = (TextView) rowView
				.findViewById(R.id.li_ctd_channelNumber);
		TextView sensor = (TextView) rowView.findViewById(R.id.li_ctd_sensor);
		CheckBox cb = (CheckBox) rowView.findViewById(R.id.li_ctd_checkbox);

		// SETUP CHECK BOX
		cb.setTag(R.id.TAG_POSITION, position);
		cb.setOnClickListener(this);
		if(checkedStatus[position] == true) {
			 cb.setChecked(true);
			 } else {
				 cb.setChecked(false);
			}
		// SETUP CHANNEL NUMBER
		channelNumber.setText(channels.get(position));

		// SETUP SENSOR
		sensor.setText(sensors.get(position));

		return rowView;
	}

	@Override
	public void onClick(View view) {
		CheckBox checkbox = (CheckBox)view;
		if (checkbox.isChecked()) {
			int position = (Integer) checkbox.getTag(R.id.TAG_POSITION);
			checkedStatus[position]=true;
			channelsToDisplay[position] = true;

		} else {
			int position = (Integer) checkbox.getTag(R.id.TAG_POSITION);
			checkedStatus[position] = false;
			channelsToDisplay[position] = false;
		}
	}

	public boolean[] getChecked() {
		return channelsToDisplay;
	}
}
