package com.stephendiniz.autoaway;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

public class AwayService extends Service
{
	private boolean silentStatus;
	private String messageContent;
	private boolean informStatus;
	private int delayDuration;
	private boolean logStatus;
	private boolean repeatStatus;
	private String returnAddress;
	private int notifyCount;
	
	final int NOTIFICATION_ID = 1;
	
	private List<String> addresses = new ArrayList<String>();
	
	private Timer timer = new Timer();
	private Bundle infoBundle;

	Resources r;
	
	AudioManager aManager;
	
	private BroadcastReceiver smsReceiver;
	
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);
		
		setNotifyCount(0);
		createNotification(0);
		
		infoBundle = intent.getExtras();
		setSilentStatus(infoBundle.getBoolean("extraSilentStatus"));
		setMessageContent(infoBundle.getString("extraMessageContent"));
		setInformStatus(infoBundle.getBoolean("extraInformStatus"));
		setDelayDuration(infoBundle.getString("extraDelayDuration"));
		setLogStatus(infoBundle.getBoolean("extraLogStatus"));
		setRepeatStatus(infoBundle.getBoolean("extraRepeatStatus"));
		
		aManager = (AudioManager)getBaseContext().getSystemService(Context.AUDIO_SERVICE);
		
		if(getSilentStatus())
			aManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		
		smsReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				Bundle bundle = intent.getExtras();
				SmsMessage[] msgs = null;

				if(null != bundle)
				{
					setReturnAddress(null);
					Object[] pdus = (Object[]) bundle.get("pdus");
					msgs = new SmsMessage[pdus.length];

					for (int i = 0; i < msgs.length; i++)
					{
						msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
						setReturnAddress(msgs[i].getOriginatingAddress());
					}
					
					repeatCheck();
				}
			}
		};
		
		registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
	}

	public void onDestroy()
	{
		super.onDestroy();
		
		destroyNotification();
		
		//Return back to Normal Ringer state (if it was changed)
		if(getSilentStatus())
			aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		
		//Release Timer
		timer.cancel();
		timer.purge();
		
		unregisterReceiver(smsReceiver);
	}

	public void setDelay()
	{
		timer.schedule(new TimerTask()
		{
			public void run()
			{
				sendSms();
			}
		},(long)(1000*getDelayDuration()));
	}
	
	public void repeatCheck()
	{
		if(getRepeatStatus() || !(addresses.contains(getReturnAddress())))
			setDelay();

		if(!addresses.contains(getReturnAddress()))
			addresses.add(getReturnAddress());
	}
	
	private void createNotification(int number)
	{
		NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification;
		
		r = getResources();
		
		if(getNotifyCount() > 0)
		{
			//Destory old Notification
			nManager.cancel(NOTIFICATION_ID);

			notification = new Notification(R.drawable.notification_icon, r.getString(R.string.notification_ticker_text_2) + " " + hyphenate(getReturnAddress()), System.currentTimeMillis());
			notification.setLatestEventInfo(this, r.getString(R.string.notification_title) + " (" + getNotifyCount() + ")", r.getString(R.string.notification_content), PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0));
		}
		else
		{
			notification = new Notification(R.drawable.notification_icon, r.getString(R.string.notification_ticker_text), System.currentTimeMillis());
			notification.setLatestEventInfo(this, r.getString(R.string.notification_title), r.getString(R.string.notification_content), PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0));
		}
		
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		nManager.notify(NOTIFICATION_ID, notification);
		setNotifyCount(getNotifyCount() + 1);
	}
	
	private void destroyNotification()
	{
		NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nManager.cancel(NOTIFICATION_ID);
	}
	
	public String hyphenate(String number)
	{
		if (number.length() == 10)
			return number.substring(0,3) + "-" + number.substring(3,6) + "-" + number.substring(6,10);
		
		//Not 10 digits long - Unable to hyphenate
		return number;
	}
	
	public void sendSms()
	{
		SmsManager manager = SmsManager.getDefault();
		int length = getMessageContent().length();

		createNotification(getNotifyCount());
		
		if (length > 160)
		{
			ArrayList<String> messagelist = manager.divideMessage(getMessageContent());

			manager.sendMultipartTextMessage(getReturnAddress(), null, messagelist, null, null);
		}
		else
			manager.sendTextMessage(getReturnAddress(), null, getMessageContent(), null, null);
	}

	@Override
	public IBinder onBind(Intent i) { return null; }
	
	//Getters and Setters for non-final variables
	//Sets private variables AND preference
	public boolean getSilentStatus()						{ return silentStatus;												}
	public void setSilentStatus(boolean silentStatus)		{ this.silentStatus = silentStatus;									}
	
	public String getMessageContent() 						{ if(getInformStatus()) { return "[Auto-Away]: " + messageContent;	}
															  return messageContent;											}
	public void setMessageContent(String messageContent)	{ this.messageContent = messageContent;								}

	public boolean getInformStatus()						{ return informStatus;												}
	public void setInformStatus(boolean informStatus)		{ this.informStatus = informStatus;									}
	
	public int getDelayDuration()							{ return delayDuration;												}
	public void setDelayDuration(String delayDuration)		{ this.delayDuration = Integer.parseInt(delayDuration);				}
	
	public String getReturnAddress()						{ return returnAddress;												}
	public void setReturnAddress(String returnAddress)		{ this.returnAddress = returnAddress;								}
	
	public boolean getLogStatus()							{ return logStatus;													}
	public void setLogStatus(boolean logStatus)				{ this.logStatus = logStatus;										}
	
	public boolean getRepeatStatus()						{ return repeatStatus;												}
	public void setRepeatStatus(boolean repeatStatus)		{ this.repeatStatus = repeatStatus;									}
	
	public int getNotifyCount()								{ return notifyCount;												}
	public void setNotifyCount(int notifyCount)				{ this.notifyCount = notifyCount;									}
}