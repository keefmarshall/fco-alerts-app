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
	    
	    // Display the fragment as the main content.
	    getFragmentManager().beginTransaction()
	            .replace(android.R.id.content, new CountryPrefs())
	            .commit();
	
	}

   @Override
   protected boolean isValidFragment(String fragmentName)
   {
	   return (fragmentName == "CountryPrefs");
   }
}
