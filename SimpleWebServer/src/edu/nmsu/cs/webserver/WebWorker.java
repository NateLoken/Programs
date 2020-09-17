package edu.nmsu.cs.webserver;
/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;
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

public String getDate() 
{
	String dateToString;
	Date date = new Date();
	DateFormat dateF = DateFormat.getDateTimeInstance();
	dateF.setTimeZone(TimeZone.getTimeZone("MST"));
	
	dateToString = dateF.format(date);
	return dateToString;
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
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

 private void writeContent(OutputStream os, String contentPath) throws Exception
 {
    String content = "";
    String path = userDirectory + contentPath;
    try{  
       File fileName = new File(path);
       BufferedReader inBuffer = new BufferedReader(new FileReader(fileName));
       while((content = inBuffer.readLine()) != null){
          if(content.contains("<cs371date>")){
             content.replace("<cs371date>", getDate());
          }
          if(content.contains("<cs371server")){
             content.replace("<cs371server>", "My Server 1.0");
          } 
          os.write(content.getBytes());
          os.write("\n".getBytes());
       }
       inBuffer.close();
      
    }
    catch(Exception e){
       System.err.println("ERROR: File does not exist");
       os.write("HTTP/1.1 404: Not Found".getBytes());
       File file = new File("404.html");
       BufferedReader inBuffer = new BufferedReader(new FileReader(file));
       while((content = inBuffer.readLine()) != null){
          os.write(content.getBytes());
          os.write("\n".getBytes());
       }
       inBuffer.close();
    }
 }

} // end class
