package test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import train.HttpTrainServer;


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
	
	private static void connectToServer(HttpURLConnection connection) throws IOException, InterruptedException
	{
        // give it 15 seconds to respond
        connection.setReadTimeout(1500*1000);
        
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
	
	private static String getResponse(HttpURLConnection connection) throws IOException {
        // read the output from the server
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
   
        String line = null;
        while ((line = reader.readLine()) != null)
          stringBuilder.append(line + "\n");

        return stringBuilder.toString();
	}
	
	// Send a simple POST request to the server with the message as content and return the result code
	private static String sendMessage(HttpURLConnection connection, String message, boolean isPost) throws IOException, InterruptedException {
		
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        if (isPost)
        	connection.setDoOutput(true);
        connectToServer(connection);

        if (message != null || message.length() > 0)
        	connection.getOutputStream().write(message.getBytes());
        // give it 15 seconds to respond
        connection.setReadTimeout(15*1000);

        return getResponse(connection);
    }

	// Send a simple POST request to the server with the message as content and return the result code
	private static String sendGetMessage(HttpURLConnection connection, String message) throws IOException, InterruptedException {
		return sendMessage(connection, message, false);
    }

	// Send a simple POST request to the server with the message as content and return the result code
	private static String sendPostMessage(HttpURLConnection connection, String message) throws IOException, InterruptedException {
		return sendMessage(connection, message, true);
    }

	private static String list(String listType, String expectedGid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"list\",\"listType\":\"%s\"}", listType);
		jsonPayload = jsonPayload.replace("\"", "%22");	// url encode double quotes
		log.info("jsonPayload {}", jsonPayload);

		String url = serverURL + "?" + jsonPayload;	// form query string
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        
        int code = connection.getResponseCode();
        System.out.println("Got response code " + code);

        String responseMessage = getResponse(connection);
        log.info("Got response message: {}", responseMessage);
        assertEquals(connection.getResponseCode(), 200);
        assertTrue(responseMessage.startsWith("{\"gids\":["));
        String gid = responseMessage.substring(10, 18);
        assertEquals(expectedGid, gid);
        return gid;
	}
	
	private static String newGame(String pid, String color) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"newGame\", \"pid\":\"%s\", \"color\":\"%s\", \"gameType\":\"africa\"}", pid, color);
		log.info("jsonPayload {}", jsonPayload);
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        log.info("Got response message: {}", responseMessage);
        assertEquals(connection.getResponseCode(), 200);
        assertTrue(responseMessage.startsWith("{\"gid\":\""));
        String gid = responseMessage.substring(8, 16);
        return gid;
	}
	
	private static void joinGame(String gid, String pid, String color) throws IOException, InterruptedException {

		String jsonPayload = String.format("{\"messageType\":\"joinGame\", \"gid\":\"%s\", \"pid\":\"%s\",\"color\":\"%s\"}", gid, pid, color);
		log.info("payload {}", jsonPayload);;
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void startGame(String gid, String pid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"startGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void buildTrack(String gid, String pid, String mileposts) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"buildTrack\", \"gid\":\"%s\", \"pid\":\"%s\", \"mileposts\":%s}", gid, pid, mileposts);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void upgradeTrain(String gid, String pid, String upgradeType) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"upgradeTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"upgradeType\":%s}", gid, pid, upgradeType);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void startTrain(String gid, String pid, String where) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"startTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"where\":%s}", gid, pid, where);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void moveTrain(String gid, String pid, String mileposts) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"moveTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"mileposts\":%s}", gid, pid, mileposts);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void pickupDeliverLoad(String messageType, String gid, String pid, String city, String load) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"%s\", \"gid\":\"%s\", \"pid\":\"%s\", \"city\":\"%s\", \"load\":\"%s\"}", messageType, gid, pid, city, load);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void pickupLoad(String gid, String pid, String city, String load) throws IOException, InterruptedException {
		pickupDeliverLoad("pickupLoad", gid, pid, city, load);
	}
	
	private static void deliverLoad(String gid, String pid, String city, String load) throws IOException, InterruptedException {
		pickupDeliverLoad("deliverLoad", gid, pid, city, load);
	}
	
	private static void dumpLoad(String gid, String pid, String city, String load) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"dumpLoad\", \"gid\":\"%s\", \"pid\":\"%s\", \"city\":\"%s\", \"load\":\"%s\"}", gid, pid, city, load);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void endTurn(String gid, String pid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"endTurn\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void endGame(String gid, String pid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"endGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	

	// Send a stream of requests to the server check the results
	@Test
	public void testTrain() throws Exception {
		Thread serverThread = startServer();
		
        String gid = newGame("Adam", "red");
        list("joinable", gid);	// the new game should appear in the list of joinable games
        joinGame(gid, "Sandra", "green");
        joinGame(gid, "Sandy", "aqua");
        joinGame(gid, "Robin", "purple");
        startGame(gid, "Adam");
     /*   All of the following requires getting status messages 
      * so we can use the correct player, etc.
        String mileposts = "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1},{\"x\":2,\"y\":2}]";
        buildTrack(gid, "Sandy", mileposts);
        
        startTrain(gid, "Sandy", "{\"x\":1, \"y\":1}");
        moveTrain(gid, "Sandy", mileposts);
        pickupLoad(gid, "Sandy", "Abidjan", "turnips");
        moveTrain(gid, "Sandy", mileposts);
        deliverLoad(gid, "Sandy", "Port Harcourt", "turnips");
        dumpLoad(gid, "Sandy", "Port Harcourt", "iron");
        endTurn(gid, "Sandy");
        upgradeTrain(gid, "Adam", "Speed");
        endGame(gid, "Sandra");
        */
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
    }





}
