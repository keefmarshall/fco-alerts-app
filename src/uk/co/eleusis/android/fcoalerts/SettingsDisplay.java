package uk.co.eleusis.android.fcoalerts;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

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
	    
	    addBackButtonToActionBar();
	    
	    // Display the fragment as the main content.
	    getFragmentManager().beginTransaction()
	            .replace(android.R.id.content, new CountryPrefs())
	            .commit();
	
   	}

   	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void addBackButtonToActionBar()
   	{
   		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
   		{
	        ActionBar actionBar = getActionBar();
	        actionBar.setDisplayHomeAsUpEnabled(true);
	        actionBar.setHomeButtonEnabled(true);
   		}
   	}
   	
   	@Override
   	public void onPause()
   	{
   		super.onPause();
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_left);
   	}
   	
   	@Override
   	public boolean onOptionsItemSelected(MenuItem menuItem)
   	{
   		if (menuItem.getItemId() == android.R.id.home)
   		{
	   		onBackPressed();
	   	    return true;
   		}
   		else
   		{
   			return super.onOptionsItemSelected(menuItem);
   		}
   	}
   	
   	@Override
   	protected boolean isValidFragment(String fragmentName)
   	{
   		return (fragmentName == "CountryPrefs");
   	}
}
