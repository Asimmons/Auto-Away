package com.stephendiniz.autoaway;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Main extends PreferenceActivity implements OnPreferenceChangeListener
{
	final String SERVICE_PREF	= "serviceCheckBox";
	final String SILENT_PREF	= "silentCheckBox";
	final String MESSAGE_PREF	= "messageEditText";
	final String INFORM_PREF	= "informCheckBox";
	final String DELAY_PREF		= "delayEditText";
	final String LOG_PREF		= "logCheckBox";
	final String REPEAT_PREF	= "repeatCheckBox";
	
	Resources r;
	
	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	
	Preference serviceCheckBox;
	Preference silentCheckBox;
	Preference messageEditText;
	Preference informCheckBox;
	Preference delayEditText;
	Preference logCheckBox;
	Preference repeatCheckBox;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		//Preference Objects
		serviceCheckBox = (Preference)findPreference(SERVICE_PREF);
		serviceCheckBox.setOnPreferenceChangeListener(this);
		
		silentCheckBox = (Preference)findPreference(SILENT_PREF);
		silentCheckBox.setOnPreferenceChangeListener(this);
		
		messageEditText = (Preference)findPreference(MESSAGE_PREF);
		messageEditText.setOnPreferenceChangeListener(this);
		
		informCheckBox = (Preference)findPreference(INFORM_PREF);
		
		delayEditText = (Preference)findPreference(DELAY_PREF);
		delayEditText.setOnPreferenceChangeListener(this);
		
		logCheckBox = (Preference)findPreference(LOG_PREF);
		
		repeatCheckBox = (Preference)findPreference(REPEAT_PREF);
	}
	
	public void onResume()
	{
		super.onResume();
		
		r = getResources();

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		editor = prefs.edit();

		setServiceStatus(prefs.getBoolean(SERVICE_PREF, false));
		setSilentStatus(prefs.getBoolean(SILENT_PREF, true));
		setMessageContent(prefs.getString(MESSAGE_PREF, r.getString(R.string.message_content)));
		setInformStatus(prefs.getBoolean(INFORM_PREF, true));
		setDelayDuration(prefs.getString(DELAY_PREF, "30"));
		setLogStatus(prefs.getBoolean(LOG_PREF, true));
		setRepeatStatus(prefs.getBoolean(REPEAT_PREF, false));

		if(serviceRunning())
			setPreferenceStatus(false);
	}
	
	public void onPause()
	{
		super.onPause();
		
		editor.putBoolean(SERVICE_PREF, serviceRunning());
		editor.putBoolean(SILENT_PREF, getSilentStatus());
		editor.putString(MESSAGE_PREF, getMessageContent());
		editor.putBoolean(INFORM_PREF, getInformStatus());
		editor.putString(DELAY_PREF, Integer.toString(getDelayDuration()));
		editor.putBoolean(LOG_PREF, getLogStatus());
		editor.putBoolean(REPEAT_PREF, getRepeatStatus());
	}
	
	@Override
	public boolean onPreferenceChange(Preference p, Object o)
	{
		if(p.getKey().equals(SERVICE_PREF))
		{
			final Intent awayService = new Intent(this, AwayService.class);
				
			if(prefs.getBoolean(SERVICE_PREF, false))
			{
				setServiceStatus(false);
				setPreferenceStatus(true);
				stopService(awayService);
			}
			
			else
			{
				setServiceStatus(true);
				setPreferenceStatus(false);

				//Set Intent Extras
				awayService.putExtra("extraSilentStatus", getSilentStatus());
				awayService.putExtra("extraMessageContent", getMessageContent());
				awayService.putExtra("extraInformStatus", getInformStatus());
				awayService.putExtra("extraDelayDuration", Integer.toString(getDelayDuration()));
				awayService.putExtra("extraLogStatus", getLogStatus());
				awayService.putExtra("extraRepeatStatus", getRepeatStatus());

				//Start service and terminate activity
				startService(awayService);
				finish();
			}
		}
		else if(p.getKey().equals(MESSAGE_PREF))
			if(getMessageContent().equals(""))
				setMessageContent(r.getString(R.string.message_content));
		else if(p.getKey().equals(DELAY_PREF))
			if(getDelayDuration() < 0)
				setDelayDuration("30");
		
		return true;
	}
	
	private void setPreferenceStatus(boolean status)
	{
		silentCheckBox.setEnabled(status);
		messageEditText.setEnabled(status);
		informCheckBox.setEnabled(status);
		delayEditText.setEnabled(status);
		logCheckBox.setEnabled(status);
		repeatCheckBox.setEnabled(status);
	}
	
	//Getters and Setters for non-final variables
	//Sets private variables AND preference
	public boolean serviceRunning()							{ return prefs.getBoolean(SERVICE_PREF, false);									}
	public void setServiceStatus(boolean serviceRunning)	{ editor.putBoolean(SERVICE_PREF, serviceRunning);	
															  editor.commit();																}

	public boolean getSilentStatus()						{ return prefs.getBoolean(SILENT_PREF, false);									}
	public void setSilentStatus(boolean isSilent)			{ editor.putBoolean(SILENT_PREF, isSilent);	
															  editor.commit();																}
	
	public String getMessageContent() 						{ return prefs.getString(MESSAGE_PREF, r.getString(R.string.message_content));	}
	public void setMessageContent(String messageContent)	{ editor.putString(MESSAGE_PREF, messageContent);
															  editor.commit();																}
	
	public boolean getInformStatus()						{ return prefs.getBoolean(INFORM_PREF, true);									}
	public void setInformStatus(boolean informStatus)		{ editor.putBoolean(INFORM_PREF, informStatus);
															  editor.commit();																}
	
	public int getDelayDuration()							{ return Integer.parseInt(prefs.getString(DELAY_PREF, "30"));					}
	public void setDelayDuration(String delayDuration)		{ editor.putString(DELAY_PREF, delayDuration);
															  editor.commit();																}
	
	public boolean getLogStatus()							{ return prefs.getBoolean(LOG_PREF, true);										}
	public void setLogStatus(boolean logStatus)				{ editor.putBoolean(LOG_PREF, logStatus);
															  editor.commit();																}
	
	public boolean getRepeatStatus()						{ return prefs.getBoolean(REPEAT_PREF, true);									}
	public void setRepeatStatus(boolean repeatStatus)		{ editor.putBoolean(REPEAT_PREF, repeatStatus);
															  editor.commit();																}
}