package ceu.marten.ui.dialogs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ceu.marten.bplux.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.graphics.Color;
import android.widget.TextView;

public class AboutDialog extends Dialog {
	private static final String TAG = AboutDialog.class.getName();
	private static Context mContext = null;

	public AboutDialog(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.dialog_about);
		TextView legalText = (TextView) findViewById(R.id.legal_text);
		legalText.setText(readRawTextFile(R.raw.legal));

		legalText = (TextView) findViewById(R.id.info_text);
		legalText.setText(Html.fromHtml(readRawTextFile(R.raw.info)));
		legalText.setLinkTextColor(Color.BLUE);

		Linkify.addLinks(legalText, Linkify.ALL);
	}

	public static String readRawTextFile(int id) {
		InputStream inputStream = mContext.getResources().openRawResource(id);

		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String line;

		StringBuilder text = new StringBuilder();
		try {
			while ((line = bufferedReader.readLine()) != null) {
				text.append(line);
			}
		} catch (IOException e) {
			Log.e(TAG, "Exception", e);
			return null;
		}
		return text.toString();
	}

}
