/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2017 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package firebase.cloudmessaging;

import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.titanium.util.TiConvert;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import java.util.HashMap;
import org.appcelerator.kroll.KrollFunction;
import java.util.Map;
import android.content.Intent;

@Kroll.module(name = "CloudMessaging", id = "firebase.cloudmessaging")
public class CloudMessagingModule extends KrollModule
{

	private static final String LCAT = "FirebaseCloudMessaging";
	private static CloudMessagingModule instance = null;

	public CloudMessagingModule()
	{
		super();
		instance = this;
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		// put module init code that needs to run when the application is created
	}

	// Methods
	@Kroll.method
	public void registerForPushNotifications()
	{
		FirebaseInstanceId.getInstance().getToken();
		parseBootIntent();
	}

	@Kroll.method
	public void subscribeToTopic(String topic)
	{
		FirebaseMessaging.getInstance().subscribeToTopic(topic);
		Log.d(LCAT, "subscribe to " + topic);
	}

	@Kroll.method
	public void unsubscribeFromTopic(String topic)
	{
		FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
		Log.d(LCAT, "unsubscribe from " + topic);
	}

	@Kroll.method
	public void appDidReceiveMessage(KrollDict opt)
	{
		// empty
	}

	@Kroll.method
	public void sendMessage(KrollDict obj)
	{
		FirebaseMessaging fm = FirebaseMessaging.getInstance();

		String fireTo = obj.getString("to");
		String fireMessageId = obj.getString("messageId");
		int ttl = TiConvert.toInt(obj.get("timeToLive"), 0);

		RemoteMessage.Builder rm = new RemoteMessage.Builder(fireTo);
		rm.setMessageId(fireMessageId);
		rm.setTtl(ttl);

		// add custom data
		Map<String, String> data = (HashMap) obj.get("data");
		for (Object o : data.keySet()) {
			rm.addData((String) o, data.get(o));
		}

		if (fireTo != "" && fireMessageId != "") {
			fm.send(rm.build());
		} else {
			Log.e(LCAT, "Please set 'to' and 'messageId'");
		}
	}

	public void onTokenRefresh(String token)
	{
		if (hasListeners("didRefreshRegistrationToken")) {
			KrollDict data = new KrollDict();
			data.put("fcmToken", token);
			fireEvent("didRefreshRegistrationToken", data);
		}
	}

	public void onMessageReceived(HashMap message)
	{
		try {
			if (hasListeners("didReceiveMessage")) {
				KrollDict data = new KrollDict();
				data.put("message", new KrollDict(message));
				fireEvent("didReceiveMessage", data);
			}
		} catch (Exception e) {
			Log.e(LCAT, "Message exception: " + e.getMessage());
		}
	}

	@Kroll.method
	public void createNotificationChannel(KrollDict options)
	{
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return;
		}
		Log.d(LCAT, "createNotificationChannel " + options.toString());
		Context context = Utils.getApplicationContext();
		String sound = (String) options.optString("sound", "default");
		String importance = (String) options.optString("importance", sound.equals("silent") ? "low" : "default");
		String channelId = (String) options.optString("channelId", "default");
		String channelName = (String) options.optString("channelName", channelId);
		int importanceVal = NotificationManager.IMPORTANCE_DEFAULT;
		if (importance.equals("low")) {
			importanceVal = NotificationManager.IMPORTANCE_LOW;
		} else if (importance.equals("high")) {
			importanceVal = NotificationManager.IMPORTANCE_HIGH;
		}

		Uri soundUri = null;
		if (sound.equals("default")) {
			soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		} else if (!sound.equals("silent")) {
			String path = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/"
						  + Utils.getResourceIdentifier("raw", sound);
			Log.d(LCAT, "createNotificationChannel with sound " + sound + " at " + path);
			soundUri = Uri.parse(path);
		}

		NotificationChannel channel = new NotificationChannel(channelId, channelName, importanceVal);
		if (soundUri != null) {
			AudioAttributes audioAttributes = new AudioAttributes.Builder()
												  .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
												  .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
												  .build();
			channel.setSound(soundUri, audioAttributes);
		}
		NotificationManager notificationManager =
			(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(channel);
	}

	@Kroll.getProperty
	public String fcmToken()
	{
		return FirebaseInstanceId.getInstance().getToken();
	}

	@Kroll.setProperty
	public void apnsToken(String str)
	{
		// empty
	}

	public static CloudMessagingModule getInstance()
	{
		if (instance != null)
			return instance;
		else
			return new CloudMessagingModule();
	}

	public void parseBootIntent()
	{
		try {
			Intent intent = TiApplication.getAppRootOrCurrentActivity().getIntent();
			String notification = intent.getStringExtra("fcm_data");
			if (notification != null) {
				HashMap<String, Object> msg = new HashMap<String, Object>();
				msg.put("data", notification);
				onMessageReceived(msg);
				intent.removeExtra("fcm_data");
			} else {
				Log.d(LCAT, "Empty notification in Intent");
			}
		} catch (Exception ex) {
			Log.e(LCAT, "parseBootIntent" + ex);
		}
	}
}
