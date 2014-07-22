package ceu.marten.ui.dialogs;

import ceu.marten.bitadroid.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;


public class HelpDialog extends Dialog {

	public HelpDialog(Context context) {
		super(context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.dialog_help);
	}

}
