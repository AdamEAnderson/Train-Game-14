
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpTest {

	static final String serverURL = "http://127.0.0.1:8080/";
	private static Logger log = LoggerFactory.getLogger(HttpTest.class);
	
	private Thread startServer()
	{
		Thread t = new Thread(new Runnable() {
	         public void run()
	         {
	        	 try {
	        		 HttpTrainServer.startServer();
	        	 } catch (Exception e) {
	        		 
	        	 }
	         }
		});
		t.start();
		return t;
	}
	
	private void connectToServer(HttpURLConnection connection) throws IOException, InterruptedException
	{
        // give it 15 seconds to respond
        connection.setReadTimeout(15*1000);
        
        // wait for the server to come up
        for (int tryCount = 50; tryCount > 0; --tryCount) {
	        try {
	        	connection.connect();
	        	return;
	        } catch (IOException e) {
	        	System.out.print("Connecting...");
	        	if (tryCount <= 0)
	        		throw e;
	        	Thread.sleep(1000);
	        }
        }
        
		
	}
	
	// Send a very simple GET request to the server and check the result
	@Test
	public void testGet() throws Exception {
		Thread serverThread = startServer();
		
        HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        connection.setRequestMethod("GET");
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);

        // give it 15 seconds to respond
        connection.setReadTimeout(15*1000);
        
        connectToServer(connection);
        
        // read the output from the server
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
   
        String line = null;
        while ((line = reader.readLine()) != null)
        {
          stringBuilder.append(line + "\n");
        }
        System.out.println(stringBuilder.toString());

        int code = connection.getResponseCode();
        System.out.println("Got response code " + code);

        HttpTrainServer.stopServer();
        serverThread.join();
    }

	// Send a simple POST request to the server with some JSON as content and check the result
	@Test
	public void testPost() throws Exception {
		Thread serverThread = startServer();
		
		String jsonPayload = "{\"foo\":\"bar\"}";

        HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        connection.setRequestMethod("GET");
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setDoOutput(true);
        connectToServer(connection);
        connection.getOutputStream().write(jsonPayload.getBytes());
        // give it 15 seconds to respond
        connection.setReadTimeout(15*1000);
        
        
        // read the output from the server
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
   
        String line = null;
        while ((line = reader.readLine()) != null)
          stringBuilder.append(line + "\n");
        System.out.println(stringBuilder.toString());

        int code = connection.getResponseCode();
        System.out.println("Got response code " + code);

        HttpTrainServer.stopServer();
        serverThread.join();
    }

	// Send a simple POST request to the server with the message as content and return the result code
	private String sendMessage(HttpURLConnection connection, String message) throws Exception {
		
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setDoOutput(true);
        connectToServer(connection);

        connection.getOutputStream().write(message.getBytes());
        // give it 15 seconds to respond
        connection.setReadTimeout(15*1000);
        
        
        // read the output from the server
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
   
        String line = null;
        while ((line = reader.readLine()) != null)
          stringBuilder.append(line + "\n");

        return stringBuilder.toString();
    }

	// Send a newGame request to the server check the result
	@Test
	public void testNewGame() throws Exception {
		Thread serverThread = startServer();
		
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"ruleSet\":\"anythingGoes\", \"gameType\":\"Africa\"}";

        HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        log.info("Got response message {}", responseMessage);
        HttpTrainServer.stopServer();
        serverThread.join();
    }

	// Send a newGame request to the server check the result
	@Test
	public void testJoinGame() throws Exception {
		Thread serverThread = startServer();
		
		// newGame
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"ruleSet\":\"anythingGoes\", \"gameType\":\"Africa\"}";
        HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String gid = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        //join game
		jsonPayload = "{\"messageType\":\"joinGame\", \"gid\":\"";
		jsonPayload += gid.substring(8, gid.length() - 3);
		jsonPayload += "\", \"pid\":\"Sandra\", \"color\":\"red\"}";
		log.info("payload {}", jsonPayload);;

        connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        log.info("Got response message {}", responseMessage);
        HttpTrainServer.stopServer();
        serverThread.join();
    }


}
