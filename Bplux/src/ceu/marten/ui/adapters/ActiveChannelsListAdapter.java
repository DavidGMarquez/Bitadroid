package ceu.marten.ui.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import ceu.marten.bitadroid.R;

public class ActiveChannelsListAdapter extends ArrayAdapter<String> implements OnClickListener, OnItemSelectedListener {

	private final Context context;
	private List<String> strings;
	private String[] activeSensors = null;
	private boolean[] checkedStatus;

	public ActiveChannelsListAdapter(Context context, List<String> channelNumbers, String[] activeChannels) {
		super(context, 0, 0, channelNumbers);
		this.context = context;
		this.strings = channelNumbers;
		this.activeSensors = new String[channelNumbers.size()];
		this.checkedStatus = new boolean[channelNumbers.size()];

		if (activeChannels != null) {
			for (int i = 0; i < activeChannels.length; i++) {
				if (activeChannels[i].compareTo("null") != 0) {
					checkedStatus[i] = true;
					activeSensors[i] = activeChannels[i];
				}
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewGroup rowView = (ViewGroup) convertView;
		if (rowView == null) {
			rowView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.li_active_channels, parent, false);
		}
		
		Spinner spinner = (Spinner) rowView.findViewById(R.id.li_ac_sensors);
		CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.li_ac_checkbox);
		checkBox.setTag(R.id.TAG_POSITION, position);
		checkBox.setTag(R.id.TAG_SPINNER, spinner);
		checkBox.setOnClickListener(this);
		
		if (checkedStatus[position]) {
			checkBox.setChecked(true);
		} else {
			checkBox.setChecked(false);
		}

		spinner.setTag(position);
		spinner.setOnItemSelectedListener(this);

		if (activeSensors[position] != null && activeSensors[position].compareTo("null") != 0) {
			for (int counter = 0; counter < spinner.getCount(); counter++) {
				if (spinner.getItemAtPosition(counter).toString().compareTo(activeSensors[position]) == 0){
					spinner.setSelection(counter);				
				}
			}
		}
		
		TextView channelNumber = (TextView) rowView.findViewById(R.id.li_ac_channelNumber);
		String currentItem = strings.get(position);
		channelNumber.setText(currentItem);

		return rowView;
	}

	@Override
	public void onClick(View view) {
		CheckBox currentCheckbox = (CheckBox)view;
		if (currentCheckbox.isChecked()) {
			int position = (Integer) currentCheckbox.getTag(R.id.TAG_POSITION);
			checkedStatus[position] = true;
			Spinner sp = (Spinner) currentCheckbox.getTag(R.id.TAG_SPINNER);
			activeSensors[position] = sp.getSelectedItem().toString();

		} else {
			int position = (Integer) currentCheckbox.getTag(R.id.TAG_POSITION);
			checkedStatus[position] = false;
			activeSensors[position] = null;
		}
	}

	public String[] getChecked() {
		return activeSensors;
	}

	@Override
	public void onItemSelected(AdapterView<?> spinner, View currentText, int pos, long id) {
		int position = (Integer) spinner.getTag();
		// TextView t = (TextView) currentText;
		// t.setTextSize(Float.parseFloat(context.getResources().getString(R.string.active_channel_dialog_spinner_text)));
		if (checkedStatus[position]) {
			activeSensors[position] = spinner.getSelectedItem().toString();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> spinner) {

	}

}
