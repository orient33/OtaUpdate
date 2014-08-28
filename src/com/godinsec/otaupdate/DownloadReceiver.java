package com.godinsec.otaupdate;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.godinsec.otaupdate.Util.UpdateInfo;

public class DownloadReceiver extends BroadcastReceiver {
	private static final String TAG = "DownloadReceiver]]";
	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			long cache_id = Util.getDownloadId(context);
			long downloadId = intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			if (cache_id != downloadId) {
				Util.loge(TAG, "download id = " + downloadId);
				return;
			}
			DownloadManager dm = ((DownloadManager) context.getSystemService("download"));
			Query query = new Query();
			query.setFilterById(cache_id);
			Cursor cur = dm.query(query);
			if (cur != null && cur.moveToFirst()) {
				int columnIndex = cur.getColumnIndex(DownloadManager.COLUMN_STATUS);
				if (DownloadManager.STATUS_SUCCESSFUL == cur.getInt(columnIndex)) {
					String fileUri = cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
					String filename = cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME));
					Util.logd(TAG, "download, local uri = " + fileUri +",, local filename = "+ filename);
					showUpdateInstall(context, filename, Util.getUpdateInfoCache(context));
					//wipe cache
					Util.putDownloadInfo(context, 0, null);
				}
				cur.close();
			}
		}
	
	}
	private void showUpdateInstall(Context context, String file, UpdateInfo info) {
		Intent intent = new Intent(context, InstallUpdate.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("update_file", file);
		intent.putExtra("update_info", info);
		context.startActivity(intent);
	}
}
