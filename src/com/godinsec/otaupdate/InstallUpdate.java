package com.godinsec.otaupdate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.RecoverySystem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.godinsec.otaupdate.Util.UpdateInfo;

public class InstallUpdate extends Activity implements View.OnClickListener {
	private static final String TAG = "[InstallUpdate]";
	private File mUpdateFile;
	private UpdateInfo mUpdateInfo;
	private TextView mText;
	private String mUpdateDescription;
	private boolean mSaveFile;
	private int mClicked;
	private NotificationManager mNotificationManager;
	private static final String NOTIFICATION_TAG = "update_tag";
	private static final int NOTIFICATION_ID = 102;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_update_install);
	    findViewById(R.id.abandon).setOnClickListener(this);
	    findViewById(R.id.install).setOnClickListener(this);
	    mText = (TextView)findViewById(R.id.update_info);
	    mSaveFile = true;
        String sd_path = Environment.getExternalStorageDirectory().getPath();
        Intent intent = getIntent();
        //获取 UpdateInfo 
        if (intent.getStringExtra("update_file") != null) {
            String file_path = getIntent().getStringExtra("update_file");
            mUpdateInfo = (UpdateInfo) getIntent().getParcelableExtra(
                    "update_info");
            mUpdateFile = new File(file_path.substring(file_path.indexOf(sd_path)));
            Util.putStringToSP(this, "file_pa", file_path.substring(file_path.indexOf(sd_path)));
        } else { 			// come from Notification 
            mUpdateInfo = Util.getUpdateInfoCache(this);
            mUpdateFile = new File(Util.getStringFromSP(this, "file_pa"));
        }
        
        //根据 UpdateInfo 提取信息 组装提示等
        String update_size = "";
        int size_kb = Integer.parseInt(mUpdateInfo.size) / 1024;
        if (size_kb < 1024)
            update_size = size_kb + " KB";
        else
            update_size = size_kb / 1024 + " MB";
        mUpdateDescription = getString(R.string.version) + mUpdateInfo.version_to
                + " ( " + update_size + " )\n"
                + getString(R.string.description) + mUpdateInfo.description + "\n";
        mText.setText(mUpdateDescription);
        Util.logd(TAG, "sd_path = " + sd_path);
        mNotificationManager = (NotificationManager) getSystemService("notification");
	}

	@Override
	public void onStart(){
	    super.onStart();
	    mClicked = 0;
	}
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.abandon:
		    mSaveFile = false;
		    mClicked = 1;
		    mNotificationManager.cancel(NOTIFICATION_TAG,NOTIFICATION_ID);
			break;
		case R.id.install:
		    mSaveFile = true;
		    mClicked = 3;
			if (((RadioButton) findViewById(R.id.install_now)).isChecked()) {
				String md5 = getMD5(mUpdateFile);
				if (mUpdateInfo.md5.equalsIgnoreCase(md5)) {
					try {
						Util.logd(TAG, "path = " + mUpdateFile.getPath());
						RecoverySystem.installPackage(this, mUpdateFile);
					} catch (IOException e) {
						Toast.makeText(this, "" + e, Toast.LENGTH_LONG).show();
						Util.loge(TAG, " " + e);
					}
				} else {
					Util.loge(TAG, "md5 error! Right is " + mUpdateInfo.md5);
					mNotificationManager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID);
					mSaveFile = false;
					Toast.makeText(this, getString(R.string.check_file_failed),
							Toast.LENGTH_LONG).show();
				}

			} else {
                updateLater();
            }
            break;
		}
		this.finish();
	}

    @Override
    public void onStop() {
        super.onStop();
        if (!mSaveFile && mUpdateFile != null && mUpdateFile.exists()) {
            mUpdateFile.delete();
        }
        if (mClicked == 0) {
            updateLater();
        } 
    }

    private void updateLater() {
        long when = System.currentTimeMillis();
        CharSequence contentTitle = getText(R.string.app_name);
        CharSequence contentText = getText(R.string.update_later_msg);
        
        Intent intent= new Intent(this,InstallUpdate.class);
        PendingIntent cIntent=PendingIntent.getActivity(this, 0, intent, 0);
        Notification.Builder builder=new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
        .setTicker(getString(R.string.update_later_msg))
        .setWhen(when+100)
        .setContentIntent(cIntent)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setAutoCancel(false)
        .setOngoing(true);
        mNotificationManager.notify(NOTIFICATION_TAG,NOTIFICATION_ID ,builder.build());
        Util.putDownloadInfo(this, 0, mUpdateInfo);
	}
    
	private static String getMD5(File file) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		try {
			FileInputStream in = new FileInputStream(file);
			FileChannel ch = in.getChannel();
			MessageDigest md = MessageDigest.getInstance("MD5");
			MappedByteBuffer bb = ch.map(FileChannel.MapMode.READ_ONLY, 0,
					file.length());
			md.update(bb);
			byte updateBytes[] = md.digest();
			int len = updateBytes.length;
			char myChar[] = new char[len * 2];
			int k = 0;
			for (int i = 0; i < len; i++) {
				byte b = updateBytes[i];
				myChar[k++] = hexDigits[b >>> 4 & 0x0f];
				myChar[k++] = hexDigits[b & 0x0f];
			}
			in.close();
			ch.close();
			String md5 = new String(myChar).toUpperCase(Locale.ENGLISH);
			Util.logd(TAG, "MD5 of download = " + md5);
			return md5;
		} catch (IOException e) {
			Util.loge(TAG, e + " ");
		} catch (NoSuchAlgorithmException e) {
			Util.loge(TAG, e + " ");
		}
		return "error.";
	}
}
