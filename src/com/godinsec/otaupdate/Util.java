package com.godinsec.otaupdate;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Util {
	public static final String BASE_URL = "http://10.0.0.58/";
	public static final String URL = BASE_URL + "devices.xml";

	public static final String DEVICE_TAG = "device";
	public static final String DEVICE_TAG_NAME = "name";
	public static final String DEVICE_TAG_URL = "url";

	public static final String UPDATE_TAG = "update";
	public static final String UPDATE_TAG_INDEX = "index";
	public static final String UPDATE_TAG_VF = "version_from";
	public static final String UPDATE_TAG_VT = "version_to";
	public static final String UPDATE_TAG_DES = "description";
	public static final String UPDATE_TAG_URL = "url";
	public static final String UPDATE_TAG_SIZE = "size";
	public static final String UPDATE_TAG_MD5 = "md5";

	public static final String DEVICE_NAME = android.os.Build.MODEL;
	public static final String SYS_VERSION = android.os.Build.DISPLAY;

	private static final String TAG = "Util";
	private static final String PREFERENCE_NAME = "update_config";
	private static final String CONFIG_DOWNLOAD_ID = "download_id";
	private static final String CONFIG_UPDATE_INFO = "update_info";

	private Util() {
	}

	/** 一个设备的name 以及升级的版本信息所在地址url*/
	static class DeviceInfo{
		public final String name, url;
		public DeviceInfo(String n, String u){
			name = n;
			url = u;
		}
	}
	
	/** 设备的一次升级描述信息 */
	static class UpdateInfo  implements Parcelable {
		public String index, version_from, version_to, description, url, size, md5;
		private UpdateInfo(){}
		private UpdateInfo(Parcel in){
			index = in.readString();
			version_from = in.readString();
			version_to = in.readString();
			description = in.readString();
			url = in.readString();
			size = in.readString();
			md5 = in.readString();
		}
		public UpdateInfo(String i, String vf, String vt, String d, String u,
				String s, String m) {
			index = i;			// 
			version_from = vf;	// 升级的基础版本
			version_to = vt;  	// 升级的目标版本
			description = d;	// 描述, 如 说明此次升级的更新内容
			if (!u.contains("://"))	/* 若不含 "://" 则视为相对url */
				url = Util.BASE_URL + u;
			else
				url = u; 		// 升级文件的更新包地址
			size = s;			// 升级文件的大小  单位 byte
			md5 = m;			// 升级文件的MD5值
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(index + ";");
			builder.append(version_from + ";");
			builder.append(version_to + ";");
			builder.append(description + ";");
			builder.append(url + ";");
			builder.append(size + ";");
			builder.append(md5 + ";");
			return builder.toString();
		}

		public static final UpdateInfo createFromString(String s) {
			UpdateInfo info = new UpdateInfo();
			String[] values = s.split(";");
			int i = 0;
			try {
				info.index = values[i++];
				info.version_from = values[i++];
				info.version_to = values[i++];
				info.description = values[i++];
				info.url = values[i++];
				info.size = values[i++];
				info.md5 = values[i++];
			} catch (Exception e) {
				Util.loge(TAG, " UpdateInfo.createFromString() " + e);
				return null;
			}
			return info;
		}
		
		public static final Parcelable.Creator<UpdateInfo> CREATOR = new Parcelable.Creator<UpdateInfo>(){

			@Override
			public UpdateInfo createFromParcel(Parcel in) {
				return new UpdateInfo(in);
			}

			@Override
			public UpdateInfo[] newArray(int size) {
				return new UpdateInfo[size];
			}
			
		};
		@Override
		public int describeContents() {
			return 0;
		}
		@Override
		public void writeToParcel(Parcel out, int flag) {
			out.writeString(index);
			out.writeString(version_from);
			out.writeString(version_to);
			out.writeString(description);
			out.writeString(url);
			out.writeString(size);
			out.writeString(md5);
		}
	}
	
	public static final void logd(String tag, String s){
		Log.d(tag, s);
	}
	public static final void loge(String tag, String s){
		Log.e(tag, s);
	}
	
	public static final void closeQuiet(InputStream is){
		if (is == null)
			return;
		try {
			is.close();
		} catch (IOException e) {
		}
	}

    public static final String getStringFromSP(Context con,String key){
        SharedPreferences pref=con.getSharedPreferences(PREFERENCE_NAME, 0);
        return pref.getString(key, " ");
    }
    public static final void putStringToSP(Context con,String key, String value){
        SharedPreferences pref=con.getSharedPreferences(PREFERENCE_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public static final UpdateInfo getUpdateInfoCache(Context context){
    	SharedPreferences pref= context.getSharedPreferences(PREFERENCE_NAME,0);
    	return UpdateInfo.createFromString(pref.getString(CONFIG_UPDATE_INFO, ""));
    }
    
    public static final long getDownloadId(Context context){
    	SharedPreferences pref= context.getSharedPreferences(PREFERENCE_NAME,0);
    	return pref.getLong(CONFIG_DOWNLOAD_ID, 0);
    }
    public static void putDownloadInfo(Context context, long id, UpdateInfo info){
    	SharedPreferences pref= context.getSharedPreferences(PREFERENCE_NAME,0);
    	SharedPreferences.Editor editor = pref.edit();
    	editor.putLong(CONFIG_DOWNLOAD_ID, id);
    	editor.putString(CONFIG_UPDATE_INFO, info == null ? "" : info.toString());
    	editor.commit();
    }
}
