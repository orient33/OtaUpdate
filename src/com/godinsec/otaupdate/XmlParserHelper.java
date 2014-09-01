package com.godinsec.otaupdate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.text.TextUtils;
import android.util.Xml;

import com.godinsec.otaupdate.Util.DeviceInfo;
import com.godinsec.otaupdate.Util.UpdateInfo;

public class XmlParserHelper {
	private static final String TAG ="[XmlParserHelper]";
	public static final class NetError{
		public String msg;
		public int code;
		public NetError(){
			msg = "";
			code = 0;
		}
	}
	/**
	 *  获取本设备的更新地址 url<br>
	 *  注意 需从 非UI线程调用
	 * */
	public static final DeviceInfo getDeviceInfo(NetError ne){
		InputStream is = null;
		try{
			is = requestURL(Util.URL, ne); 
			if(is == null)
				return null;
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(is, "UTF-8");
			int type;
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (type != XmlPullParser.START_TAG)
					continue;
				String tag = parser.getName().trim();
				if (!Util.DEVICE_TAG.equals(tag))
					continue;
				String name = parser.getAttributeValue(null, Util.DEVICE_TAG_NAME);
				if (TextUtils.isEmpty(name))
					continue;
				String url = parser.getAttributeValue(null, Util.DEVICE_TAG_URL);
				Util.logd(TAG, "find Device: " + name + ", url=" + url);
				if(Util.DEVICE_NAME.equalsIgnoreCase(name.trim()))
					return new DeviceInfo(name, url);
			}
		} catch (XmlPullParserException e) {
			Util.loge(TAG, "getDeviceInfo()  " + e);
			ne.code = 10;
			ne.msg = e.getMessage();
		} catch (IOException e) {
			Util.loge(TAG, "getDeviceInfo()  " + e);
			ne.code = 11;
			ne.msg = e.getMessage();
		} finally {
			Util.closeQuiet(is);
		}
		return null;
	}

	/** 请求url, 返回其InputStream*/
	private static final InputStream requestURL(String url, NetError ne) {
		InputStream is = null;
		HttpGet httpGet = new HttpGet(url);
		HttpClient client = new DefaultHttpClient();
		HttpResponse hRes = null;
		int resCode = -1;
		try {
			Util.logd(TAG, "requestURL()  url = " + url);
			Util.logd(TAG, "set time out  6 s !!!");
			client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 6*1000);
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 6*1000);
			hRes = client.execute(httpGet);
			resCode = hRes.getStatusLine().getStatusCode();
			if (resCode != 200) {
				Util.loge(TAG, "requestURL() resCode = " + resCode);
				ne.code = resCode;
				ne.msg = "http response error. " + resCode;
				return null;
			}
			is = hRes.getEntity().getContent();
		} catch (IOException e) {
			Util.loge(TAG, "" + e);
			ne.code = 1;
			ne.msg = e.getMessage();
		}
		return is;
	}
	
	/**
	 *  从地址url中得到所有升级描述，并筛选出适合本设备的升级信息, 即版本基于version_from.<br>
	 *  注意 需从 非UI线程调用 
	 * */
	public static final List<UpdateInfo> getUpdateInfos(String url, String version_from, NetError ne){
		List<UpdateInfo> updates = new ArrayList<UpdateInfo>();
		if(!url.startsWith("http://"))
			url = Util.BASE_URL + url;
		
		InputStream is = requestURL(url, ne);
		if(is != null){
			XmlPullParser parser = Xml.newPullParser();
			try{
				parser.setInput(is, "utf-8");
				int type;
				String index = "", vf = "", vt = "", des = "", u = "", size = "", md5 = "";
				while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
					if (type == XmlPullParser.START_TAG) {
						String tag = parser.getName();
//						Util.logd(TAG, "start  tag : " + tag);
						if (Util.UPDATE_TAG_INDEX.equals(tag)) {
							index = parser.nextText().trim();
						} else if (Util.UPDATE_TAG_VF.equals(tag)) {
							vf = parser.nextText().trim();
						} else if (Util.UPDATE_TAG_VT.equals(tag)) {
							vt = parser.nextText().trim();
						} else if (Util.UPDATE_TAG_DES.equals(tag)) {
							des = parser.nextText().trim();
						} else if (Util.UPDATE_TAG_URL.equals(tag)) {
							u = parser.nextText().trim();
						} else if (Util.UPDATE_TAG_SIZE.equals(tag)) {
							size = parser.nextText().trim();
						} else if (Util.UPDATE_TAG_MD5.equals(tag)) {
							md5 = parser.nextText().trim();
							// 这是最后一个字段  故这里开始组装解析结果
							UpdateInfo ui = new UpdateInfo(index, vf, vt, des, u, size, md5);
							if (ui.version_from.equalsIgnoreCase(version_from)) {
								updates.add(ui);
								Util.logd(TAG, "find match version_from. ");
							}
							Util.logd(TAG, "UpdateInfo:" + ui);
						} else {
							; 		//	Util.logd(TAG, "not handle tag <> : " + tag);
						}
					}
				}
			} catch (XmlPullParserException e) {
				Util.loge(TAG, "writeUpdateInfoList() " + e);
				ne.code = 20;
				ne.msg = e.getMessage();
			} catch (IOException e) {
				Util.loge(TAG, "writeUpdateInfoList() " + e);
				ne.code = 21;
				ne.msg = e.getMessage();
			} finally {
				Util.closeQuiet(is);
			}
		
		}
		return updates;
	}
	
}
