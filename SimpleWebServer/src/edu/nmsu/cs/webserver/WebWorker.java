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

	/**
	* Constructor: must have a valid open socket
	**/
	public WebWorker(Socket s)
	{
	   socket = s;
	}

	public void setCode(int num){
	   this.errorCode = num;
	}

	public int getCode(){
	   return this.errorCode;
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
	   try {
	      InputStream  is = socket.getInputStream();
	      OutputStream os = socket.getOutputStream();
	      String fileContent = readHTTPRequest(is);
	      writeHTTPHeader(os,"text/html", fileContent);
	      writeContent(os, fileContent);
	      os.flush();
	      socket.close();
	   } catch (Exception e) {
	      System.err.println("Output error: "+e);
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
	   while (true) {
	      try {
	         while (!r.ready()) Thread.sleep(1);
	         line = r.readLine();
	         if(line.contains("GET ")){
	            path = line.substring(4);
	            for(int i = 0; i < path.length(); i++){
	               if(path.charAt(i) == ' ')
	                  path = path.substring(0, i);
	            }
	         }
	         System.err.println("Request line: ("+line+")");
	         if (line.length()==0) break;
	      } catch (Exception e) {
	         System.err.println("Request error: "+e);
	         break;
	      }
	   }
	   if(path.equals("/"))
	   	path = "/" + DEFAULT_FILE;
	   File file = new File(userDirectory+path);
	   if(file.exists()){
	      setCode(200);
	   }
	   else{
	      setCode(404);
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
	   Date d = new Date();
	   DateFormat df = DateFormat.getDateTimeInstance();
	   df.setTimeZone(TimeZone.getTimeZone("GMT"));
	   if(getCode() == 200){
	      os.write("HTTP/1.1 200 OK\n".getBytes());
	      System.out.println("Content Collected: " + path + " successfully");
	   }
	   else{
	      os.write("HTTP/1.1 404 ERROR\n".getBytes());
	   }
	   os.write("Date: ".getBytes());
	   os.write((df.format(d)).getBytes());
	   os.write("\n".getBytes());
	   os.write("Server: Nate's Server\n".getBytes());
	   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
	   //os.write("Content-Length: 438\n".getBytes()); 
	   os.write("Connection: close\n".getBytes());
	   os.write("Content-Type: ".getBytes());
	   os.write(contentType.getBytes());
	   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
	   return;
	}

	/**x  
	* Write the data content to the client network connection. This MUST
	* be done after the HTTP header has been written out.
	* @param os is the OutputStream object to write to
	**/
	private void writeContent(OutputStream os, String contentPath) throws Exception
	{
	   final String date = "<cs371date>";
	   final String server = "<cs371server>";
	   String content = "";
	   String path = userDirectory + contentPath;
	   Date d = new Date();
	   String newDate;
	   DateFormat df = DateFormat.getDateTimeInstance();
	   df.setTimeZone(TimeZone.getTimeZone("GMT"));
	   newDate = df.format(d);
	   try{  
	      File fileName = new File(path);
	      BufferedReader inBuffer = new BufferedReader(new FileReader(fileName));
	      while((content = inBuffer.readLine()) != null){
	         if(content.contains(date)){
				os.write(content.replace(date,newDate).getBytes());
	         }
	         if(content.contains(server)){
				os.write(content.replace(server, "My Server 1.0").getBytes());
			 }
	         os.write("\n".getBytes());
	      }
	      inBuffer.close();
	  
	   }
	   catch(Exception e){
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
	}

} // end class
