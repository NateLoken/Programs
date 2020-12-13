package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

// import java.io.BufferedReader;
// import java.io.InputStream;
// import java.io.InputStreamReader;
// import java.io.OutputStream;
import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

	private Socket socket;
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	private int errorCode;
	String userDirectory = System.getProperty("user.dir");
	private String mimeType;
	private boolean favSet;

	/**
	* Constructor: must have a valid open socket
	**/
	public WebWorker(Socket s)
	{
	   socket = s;
	}

	public String getMimeType() 
	{
		return this.mimeType;
	}
	
	public void setMimeType(String type) 
	{
		this.mimeType = type;
	}
	
	public int getErrorCode() 
	{
		return this.errorCode;
	}
	
	public void setErrorCode(int num) 
	{
		this.errorCode = num;
	}
	
	// Easy way to format dates
	public String getDate() 
	{
		String dateToString;
		Date date = new Date();
		DateFormat dateF = DateFormat.getDateTimeInstance();
		dateF.setTimeZone(TimeZone.getTimeZone("MST"));
		
		dateToString = dateF.format(date);
		return dateToString;
	}
	
	public boolean getFavicon() 
	{
		return this.favSet;
	}
	
	public void setFavicon(boolean set) 
	{
		this.favSet = set;
	}

	/**
	* Worker thread starting point. Each worker handles just one HTTP 
	* request and then returns, which destroys the thread. This method
	* assumes that whoever created the worker created it with a valid
	* open socket object.
	**/
	public void run()
	{
	    System.err.println("Handling connection...");
	    try 
	    {
	        InputStream  is = socket.getInputStream();
	        OutputStream os = socket.getOutputStream();
	        String contentFile = readHTTPRequest(is);
	        
	        // If nothing went wrong...then do this
	        if(getErrorCode() == 200) 
	        {
	        	// Checking what type of file is being processed
				if(contentFile.contains(".html"))
				{
					setMimeType("text/html");
				}
				
				else if(contentFile.contains(".gif"))
				{
					setMimeType("image/gif");
				}
				
				else if(contentFile.contains(".jpeg") || contentFile.contains(".jpg"))
				{
					setMimeType("image/jpeg");
				}
				
				else if(contentFile.contains(".png"))
				{
					setMimeType("image/png");
				}
				
				else if(contentFile.contains(".ico")) 
				{
					setFavicon(true);
					setMimeType("image/x-icon");
				}
				
				else
				{
					setMimeType("text/html");
				}
			} 
	        
	        else 
	        {
	        	mimeType = "text/html";
	        }
	        
	        writeHTTPHeader(os, getMimeType(), contentFile);
	        writeContent(os, getMimeType(), contentFile);
	        os.flush();
	        socket.close();
	        
	    }
	    
	    catch (Exception e) 
	    {
	        System.err.println("Output error: " + e);
	    }
	    
	    System.err.println("Done handling connection.");
	    return;
	}

	/**
	* Read the HTTP request header.
	**/
	private String readHTTPRequest(InputStream is)
	{
	    String line;
	    String path = "";
	    BufferedReader r = new BufferedReader(new InputStreamReader(is));
	   
	    while (true) 
	    {
	        try 
	        {
	            while (!r.ready()) 
	            {
	            	Thread.sleep(1);
	            }
	            
	            line = r.readLine();

	            if(line.contains("GET ")) 
	            {
	                path = line.substring(4);
	                
	                for(int i = 0; i < path.length(); i++)
	                {
	                    if(path.charAt(i) == ' ')
	                    {
	                        path = path.substring(0, i);
	                    }
	                }
	            }
	            
	            System.err.println("Request line: (" + line + ")");
	            
	            if (line.length() == 0)
	            {
	                break;
	            }
	        } 
	        
	        catch (Exception e) 
	        {
	            System.err.println("Request error: "+e);
	            break;
	        }
		}
		
		if(path.equals("/"))
		{
			path = "/" + DEFAULT_FILE;
		}
	    
		File file = new File(userDirectory+path);
		
		if(file.exists())
		{
			setErrorCode(200);
		}
		// File doesnt exist so send out classic 404 error
		else
		{
			setErrorCode(404);
		}
		
	    return path;
	}

	/**
	* Write the HTTP header lines to the client network connection.
	* @param os is the OutputStream object to write to
	* @param contentType is the string MIME content type (e.g. "text/html")
	**/
	private void writeHTTPHeader(OutputStream os, String contentType, String contentPath) throws Exception
	{
	    String path = userDirectory + contentPath;
	    
		try 
		{
			FileReader contentFile = new FileReader(path);
	        os.write("HTTP/1.1 200 OK\n".getBytes());
	        System.out.println("Content Collected: " + path + " successfully!");
	    }
		
	    catch(FileNotFoundException fnfe) 
		{
	        os.write("HTTP/1.1 404 ERROR\n".getBytes());
	    }
	    
	    os.write("Date: ".getBytes());
	    os.write(getDate().getBytes());
	    os.write("\n".getBytes());
	    os.write("Server: Nates's Server\n".getBytes());
	    os.write("Connection: close\n".getBytes());
	    os.write("Content-Type: ".getBytes());
	    os.write(contentType.getBytes());
	    os.write("\n\n".getBytes());
	    return;
	}

	/**x  
	* Write the data content to the client network connection. This MUST
	* be done after the HTTP header has been written out.
	* @param os is the OutputStream object to write to
	**/
	private void writeContent(OutputStream os, String contentType, String contentPath) throws Exception
	{
		final String dateTag = "<cs371date>";
		final String serverTag = "<cs371server>";
		String content = "";
		String path = userDirectory + contentPath;
		
		if(contentType.contains("text/html")) 
		{
	        try 
	        {
	            File fileName = new File(path);
	            BufferedReader inBuffer = new BufferedReader(new FileReader(fileName));
	            
	            while((content = inBuffer.readLine()) != null) 
	            {
	            	// Replace that tag with date and server tag as required in p1
	                if(content.contains(dateTag))
	                {
						os.write(content.replace(dateTag, getDate()).getBytes());
	                }
	                
	                if(content.contains(serverTag))
	                {
						os.write(content.replace(serverTag, "My Server 2.0").getBytes());
	                }
	                
	                os.write(content.getBytes());
	                os.write( "\n".getBytes());
	            }
	        }
	        
	        catch(FileNotFoundException fnfe) 
	        {
	            System.err.println("ERROR: File " + path + " does not exist!");
	            write404Content(os, path);
	        }
	    }
		
		// if the file contains an image then send to IOstream
		else if(contentType.contains("image")) 
		{
			try
			{
	            File file = new File(path);
	            int fileLength = (int) file.length();
	            FileInputStream inputStream = new FileInputStream(file);
	            
	            byte allBytes[] = new byte[fileLength];
	            
	            inputStream.read(allBytes);
	            os.write(allBytes);
			} 
			
			catch (FileNotFoundException fnfe) 
			{
	            System.err.println("ERROR: Image not found at: " + path);
			}
		}
		
		else 
		{
			write404Content(os,path);
		}
	}

	private void write404Content(OutputStream os, String path) throws Exception
	{
		String content = "";
		System.err.println("ERROR: File does not exist");
		os.write("HTTP/1.1 404: Not Found".getBytes());
		File file = new File(FILE_NOT_FOUND);
		BufferedReader inBuffer = new BufferedReader(new FileReader(file));
		while((content = inBuffer.readLine()) != null){
		   os.write(content.getBytes());
		   os.write("\n".getBytes());
		}
		inBuffer.close();
	
	}

} // end class
