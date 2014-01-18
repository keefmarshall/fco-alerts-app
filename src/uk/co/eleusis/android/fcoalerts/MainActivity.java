package uk.co.eleusis.android.fcoalerts;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/**
 * MainActivity for FCO alerts - this is mostly a copy of DemoActivity from
 * the Google code example, but adjusted for my needs.
 * 
 * A lot of it is concerned with the GCM registration process - this registers the
 * current device with Google's Cloud Messaging system. Our application server at 
 * the other end posts messages to GCM, with a device-specific key so that GCM 
 * knows where to post them to.
 * 
 * @author keithm
 *
 */
public class MainActivity extends ListActivity 
{

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    private static final String SENDER_ID = SecretKeys.GOOGLE_PROJECT_NUMBER;

    /**
     * Tag used on log messages.
     */
    private static final String TAG = "FCOAlerts";

    private Context context;
    private String regid;
    
    private GCMRegistration gcmreg;
    
    private SimpleAdapter alertListAdapter;
    private List<Map<String, Object>> alerts;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        gcmreg = new GCMRegistration(this, SENDER_ID);
        
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (gcmreg.checkPlayServices()) 
        {
        	gcmreg.init();
            regid = gcmreg.getRegistrationId(context);

            if (regid.isEmpty()) 
            {
                gcmreg.registerInBackground(context);
            }
        }
        else 
        {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }
    
    @Override
    protected void onStart() 
    {
        super.onStart();
        
        // Fetch the latest feeds and show them on the home page
        // TODO: get this URL from somewhere saner :)
        fetchLatestAlerts();
    }

    private void fetchLatestAlerts()
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
    		
    	    protected void onPostExecute(List<Map<String, Object>> alerts)
    	    {
    	    	for (Map<String, Object> alert : alerts)
    	    	{
    	    		alert.put("description", Html.fromHtml((String)alert.get("description")));
    	    	}
    	    	
    	    	MainActivity.this.alerts = alerts;
    	    	drawAlerts();
    	    }

    	};
    	
    	requestTask.execute("http://fcoalerts.herokuapp.com/latest");
    }
    
    private void drawAlerts()
    {
    	// need to strip HTML from descriptions - this is hacky, probably should build
    	// my own list adapter..
    	
    	alertListAdapter = new SimpleAdapter(
                this, alerts, R.layout.alert_item, 
                new String[] { "title", "description" },
                new int[] { R.id.title, R.id.description });
    	setListAdapter(alertListAdapter);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
    	Map<String, Object> alert = alerts.get(position);
    	Uri uri = Uri.parse((String)alert.get("link"));
    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    	startActivity(intent);
    }
    
    @Override
    protected void onResume() 
    {
        super.onResume();
        // Check device for Play Services APK.
        gcmreg.checkPlayServices();
    }

    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    // MENU SETTINGS
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        // Handle item selection
        switch (item.getItemId()) 
        {
        case R.id.action_settings:
            startSettingsDisplay();
            return true;
            
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void startSettingsDisplay()
    {
        Intent settingsIntent = new Intent().setClass(this, SettingsDisplay.class);
        startActivity(settingsIntent);
    }
    

}
