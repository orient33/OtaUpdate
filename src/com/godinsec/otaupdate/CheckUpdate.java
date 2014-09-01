package com.godinsec.otaupdate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.godinsec.otaupdate.Util.DeviceInfo;
import com.godinsec.otaupdate.Util.UpdateInfo;
import com.godinsec.otaupdate.XmlParserHelper.NetError;

public class CheckUpdate extends Activity {

	private static final String TAG ="CheckUpdate";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_update);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		Button mmCheckButton;
		Activity mmActivity;
		public PlaceholderFragment() {
		}

		@Override
		public void onActivityCreated(Bundle b){
			super.onActivityCreated(b);
			mmActivity= getActivity();
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_check_update,
					container, false);
			mmCheckButton = (Button) rootView
					.findViewById(R.id.bt_check_update);
			mmCheckButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					checkUpdate();
				}
			});
			return rootView;
		}

		private void checkUpdate() {
			final Activity act = mmActivity;
			new AsyncTask<Void, NetError, UpdateInfo>() {
				ProgressDialog dialog;
				boolean failed = false;
				@Override
				protected void onPreExecute(){
					dialog = new ProgressDialog(act);
					dialog.setMessage(act.getString(R.string.checking_update));
					dialog.setCancelable(false);
					dialog.show();
				}
				
				@Override
				protected UpdateInfo doInBackground(Void... v) {
					NetError  error = new NetError();
					ConnectivityManager cm =(ConnectivityManager)act.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo ni = cm.getActiveNetworkInfo();
					if (ni != null) {
						DeviceInfo device = XmlParserHelper.getDeviceInfo(error);
						List<UpdateInfo> updates = new ArrayList<UpdateInfo>();
						if (device != null) 
							updates = XmlParserHelper.getUpdateInfos(device.url, Util.SYS_VERSION, error);
						Util.logd(TAG, "updates list size is " + updates.size());
						if (updates.size() > 0) {
							return updates.get(0);
						}
					} else {
						error.code = -1;
						error.msg = act.getString(R.string.no_network);
					}
					if(error.code != 0)
						publishProgress(error);
					return null;
				}
				
				@Override
				protected void onProgressUpdate(NetError... nes){
					failed = true;
					String title = act.getString(R.string.notice),
							ok = act.getString(R.string.ok);
					AlertDialog.Builder b = new AlertDialog.Builder(act);
					b.setTitle(title).setMessage(nes[0].msg + ". (" + nes[0].code + ")")
							.setPositiveButton(ok, null);
					b.show();
				}
				
				@Override
				protected void onPostExecute(UpdateInfo update){
					dialog.dismiss();
					dialog = null;
					if (!failed) {
						showUpdateInfo(update);
					}
				}
			}.execute();
		}
		
		void showUpdateInfo(final UpdateInfo ui){
			String title = mmActivity.getString(R.string.app_name);
			String msg = mmActivity.getString(R.string.has_no_update);
			String buttonMsg = mmActivity.getString(R.string.ok);
			if(ui != null){
				msg = ui.toString();
				buttonMsg=mmActivity.getString(R.string.download);
			}
			AlertDialog.Builder b = new AlertDialog.Builder(mmActivity);
			b.setTitle(title).setMessage(msg)
					.setPositiveButton(buttonMsg,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									if (ui != null)
										startDownload(ui);
								}
							}
					);
			b.show();
		}
		
		void startDownload(UpdateInfo ui) {
	        DownloadManager dm=((DownloadManager)mmActivity.getSystemService("download"));
	        Uri uri = Uri.parse(ui.url);
	        Request dwreq = new DownloadManager.Request(uri);
	        dwreq.setTitle(getString(R.string.download_title));
	        dwreq.setDescription(getString(R.string.download_description));
	        // hide API , only build from android source. not for 3th app.
	        dwreq.setDestinationToSystemCache();
	        dwreq.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
	        
	        long id = dm.enqueue(dwreq);
	        Util.putDownloadInfo(mmActivity, id, ui);
		}
	}

}
