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
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.godinsec.otaupdate.Util.DeviceInfo;
import com.godinsec.otaupdate.Util.UpdateInfo;

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
			new AsyncTask<Void, Void, UpdateInfo>() {
				ProgressDialog dialog;
				@Override
				protected void onPreExecute(){
					dialog = new ProgressDialog(mmActivity);
					dialog.setMessage("checking update...");
					dialog.setCancelable(false);
					dialog.show();
				}
				
				@Override
				protected UpdateInfo doInBackground(Void... arg0) {
					DeviceInfo device = XmlParserHelper.getDeviceInfo();
					List<UpdateInfo> updates =new ArrayList<UpdateInfo>();
					if(device != null){
						updates = XmlParserHelper.getUpdateInfos(device.url, Util.SYS_VERSION);
					} else {
						Util.loge(TAG, " get device info failed !!");
					}
					Util.logd(TAG, "updates list size is " + updates.size());
					if (updates.size() > 0) {
						return updates.get(0);
					}
					return null;
				}
				
				@Override
				protected void onPostExecute(UpdateInfo update){
					dialog.dismiss();
					dialog = null;
					showUpdateInfo(update);
				}
			}.execute();
		}
		
		void showUpdateInfo(final UpdateInfo ui){
			String msg="has no update", buttonMsg =" OK ";
			if(ui != null){
				msg = ui.toString();
				buttonMsg="Download";
			}
			AlertDialog.Builder b = new AlertDialog.Builder(mmActivity);
			b.setTitle("update").setMessage(msg)
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
//			Toast.makeText(mmActivity, "start download! \n" + ui.url, Toast.LENGTH_LONG).show();
			File f = new File(Environment.getExternalStorageDirectory().getPath()+"/update.zip");
	        if (f.exists()) {
	            f.delete();
	        }
	        DownloadManager dm=((DownloadManager)mmActivity.getSystemService("download"));
	        Uri uri = Uri.parse(ui.url);
	        Environment.getExternalStoragePublicDirectory(
	                Environment.DIRECTORY_DOWNLOADS).mkdirs();
	        Request dwreq = new DownloadManager.Request(uri);
	        dwreq.setTitle(getString(R.string.download_title));
	        dwreq.setDescription(getString(R.string.download_description));

	        dwreq.setDestinationInExternalPublicDir(
	                Environment.DIRECTORY_DOWNLOADS, 	//recovery下不能访问
	                "../update.zip");	// download/../update.zip    maybe should check if file exist .
	        
	        dwreq.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
	        
	        long id = dm.enqueue(dwreq);
	        Util.putDownloadInfo(mmActivity, id, ui);
		}
	}

}
