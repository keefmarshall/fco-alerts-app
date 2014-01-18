package uk.co.eleusis.android.fcoalerts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

public class CountryPrefs extends PreferenceFragment
{
	private final static String TAG = "PreferenceActivity";
	
	private List<String> countries;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.preferences);

        setCountries();
    }

	private void setCountries()
	{
    	AsyncTask<String, Integer, List<Map<String, Object>>> requestTask = 
    			new AsyncTask<String, Integer, List<Map<String, Object>>>()
    	{
    		@Override
    		protected List<Map<String, Object>> doInBackground(String... params) 
    		{
    	        ServerComms comms = new ServerComms();
    			String content = "";
    			try 
    			{
    				content = comms.getRequest(params[0]);
    			}
    			catch (IOException e) 
    			{
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    			return comms.parseJsonList(content);
    		}
    		
    	    protected void onPostExecute(List<Map<String, Object>> countries)
    	    {
    	    	List<String> countryList = new ArrayList<String>();
    	    	for (Map<String, Object> country : countries)
    	    	{
    	    		countryList.add((String)country.get("name"));
    	    	}
    	    	
    	    	CountryPrefs.this.countries = countryList;
    	        PreferenceScreen root = CountryPrefs.this.getPreferenceScreen();
    	        MultiSelectListPreference list =
    	        		(MultiSelectListPreference) root.findPreference("countries");
    	        list.setEntries(countryList.toArray(new String[]{}));
    	        list.setEntryValues(countryList.toArray(new String[]{}));
    	        list.setEnabled(true);
    	        
    	        Log.i(TAG, "Got country list with " + countryList.size() + " entries.");
    	        Log.i(TAG, "First entry is " + countryList.get(0));
    	    }

    	};
    	
    	requestTask.execute("http://fcoalerts.herokuapp.com/countries.json");

	}

}
