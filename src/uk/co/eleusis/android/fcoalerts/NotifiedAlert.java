package uk.co.eleusis.android.fcoalerts;

/**
 * This represents the content we get delivered from the server via
 * Google Cloud Messages. It's a deliberately cut-down version of the
 * full alert, with just the information we need.
 * 
 * @author keithm
 *
 */
public class NotifiedAlert 
{
	private String messageId;
	private String title;
	private String description;
	private String link;

	public NotifiedAlert()
	{
		
	}
	
	public NotifiedAlert(String messageId, String title, String description, String link) 
	{
		this.messageId = messageId;
		this.title = title;
		this.description = description;
		this.link = link;
	}

	
	public String getTitle() 
	{
		return title;
	}
	
	public void setTitle(String title) 
	{
		this.title = title;
	}
	
	public String getDescription() 
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public String getLink() 
	{
		return link;
	}
	
	public void setLink(String link) 
	{
		this.link = link;
	}

	public String getMessageId() 
	{
		return messageId;
	}

	public void setMessageId(String messageId) 
	{
		this.messageId = messageId;
	}
}
