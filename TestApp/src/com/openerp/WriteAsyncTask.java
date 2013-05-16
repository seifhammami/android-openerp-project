package com.openerp;

import java.util.HashMap;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import com.example.testapp.R;

public class WriteAsyncTask extends
		AsyncTask<HashMap<String, Object>, String, Boolean> {
	private Activity activity;
	public ProgressDialog dialog;
	private OpenErpConnect oc;
	HashMap<String, String> context;

	public WriteAsyncTask(Activity act) {
		this.activity = act;
		this.context = new HashMap<String, String>();

	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		dialog = new ProgressDialog(activity);
		dialog.setMessage(activity.getString(R.string.sConnecting) + "...");
		dialog.setIndeterminate(false);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.show();
	}

	private void loadConnection() {
		this.oc = OpenErpHolder.oc;
	}

	/*
	 * Writes record 
	 * values[0] -> Original values 
	 * values[1] -> Modified values
	 * values[2] -> Fields Types -> Necessary?
	 */
	@Override
	protected Boolean doInBackground(HashMap<String, Object>... values) {
		Boolean writeOk = false;
		loadConnection();
		
		if(values[0].containsKey("id")){
			Object oid = values[0].get("id");
			Long lid = Long.valueOf(oid.toString());
			writeOk = oc.write(OpenErpHolder.modelName, new Long[] {lid},values[1], null);
		}
		return writeOk;

	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {

		} else {

		}
		if (dialog.isShowing()) {
			dialog.dismiss();
		}
	}

}
