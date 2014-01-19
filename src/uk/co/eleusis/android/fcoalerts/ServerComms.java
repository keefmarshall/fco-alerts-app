package uk.co.eleusis.android.fcoalerts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Methods for communicating with the fco-alerts-server online via HTTP
 * 
 * @author keithm
 *
 */
public class ServerComms 
{
	public String getRequest(String url) throws ClientProtocolException, IOException
	{
		HttpClient client = new DefaultHttpClient();
		HttpGet method = new HttpGet(url);
		HttpResponse response = client.execute(method);
		HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder content = new StringBuilder();
        while((line = reader.readLine()) != null)
        {
        	content.append(line);
        	content.append("\n");
        }
     
        reader.close();
        return content.toString();
	}
	
	public List<Map<String, Object>> parseJsonList(String jsonList)
	{
		Gson gson = new Gson();
		Type myType = new TypeToken<List<Map<String, Object>>>() {}.getType();
		return gson.fromJson(jsonList, myType);
	}
	
	public String postRequest(String url, Map<String, String> params) throws IOException
	{
		HttpClient client = new DefaultHttpClient();
		HttpPost method = new HttpPost(url);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		for (String key : params.keySet())
		{
			nameValuePairs.add(new BasicNameValuePair(key, params.get(key)));
		}
		
		method.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		
		HttpResponse response = client.execute(method);
		BufferedReader reader = 
				new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String line = "";
        StringBuilder content = new StringBuilder();
		while ((line = reader.readLine()) != null) 
		{
        	content.append(line);
        	content.append("\n");	
		}
		reader.close();
		return content.toString();
	}
}
