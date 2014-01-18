package uk.co.eleusis.android.fcoalerts;

import java.util.List;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Settings - list of countries etc
 * 
 * @author keithm
 *
 */
public class SettingsDisplay extends PreferenceActivity 
{
	   @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	    }

	    @Override
	    public void onBuildHeaders(List<Header> target) {       
	        loadHeadersFromResource(R.xml.preference_headers, target);
	    }

}
