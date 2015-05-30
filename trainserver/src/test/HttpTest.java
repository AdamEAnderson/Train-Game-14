package test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

//import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import train.HttpTrainServer;


public class HttpTest {

	static final String serverURL = "http://127.0.0.1:8080/";
	protected static Logger log = LoggerFactory.getLogger(HttpTest.class);
	
	public Thread startServer()
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
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			
		}
		return t;
	}
	
	public static void connectToServer(HttpURLConnection connection) throws IOException, InterruptedException
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
	
	public static String getResponse(HttpURLConnection connection) throws IOException {
        // read the output from the server
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
   
        String line = null;
        while ((line = reader.readLine()) != null)
          stringBuilder.append(line + "\n");

        return stringBuilder.toString();
	}
	
	// Send a simple POST request to the server with the message as content and return the result code
	public static String sendMessage(HttpURLConnection connection, String message, boolean isPost) throws IOException, InterruptedException {
		
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        if (isPost)
        	connection.setDoOutput(true);
        connectToServer(connection);

        if (message != null && message.length() > 0)
        	connection.getOutputStream().write(message.getBytes());
        // give it 15 seconds to respond
        connection.setReadTimeout(15*1000);

        return getResponse(connection);
    }

	// Send a simple POST request to the server with the message as content and return the result code
	public static String sendPostMessage(HttpURLConnection connection, String message) throws IOException, InterruptedException {
		return sendMessage(connection, message, true);
    }

	public static String status(String gid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"status\",\"gid\":\"%s\"}", gid);
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
        assertTrue(responseMessage.startsWith("{\"gid\":\"" + gid + "\""));
        return responseMessage;
	}
	
	public static String list(String listType, String expectedGid) throws IOException, InterruptedException {
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
        assertTrue(responseMessage.startsWith("{\"gidNames\":{"));
        String gid = responseMessage.substring(14, 22);
        assertEquals(expectedGid, gid);
        return gid;
	}
	
	public static String newGame(String pid, String color) throws IOException, InterruptedException {
		String name = "TestGame";
		String jsonPayload = String.format("{\"messageType\":\"newGame\", \"pid\":\"%s\", \"color\":\"%s\", \"gameType\":\"africa\", \"name\":\"%s\"}", pid, color, name);
		log.info("jsonPayload {}", jsonPayload);
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        log.info("Got response message: {}", responseMessage);
        assertEquals(connection.getResponseCode(), 200);
        assertTrue(responseMessage.startsWith("{\"gid\":\""));
        String gid = responseMessage.substring(8, 16);
        return gid;
	}
	
	public static void joinGame(String gid, String pid, String color) throws IOException, InterruptedException {

		String jsonPayload = String.format("{\"messageType\":\"joinGame\", \"gid\":\"%s\", \"pid\":\"%s\",\"color\":\"%s\"}", gid, pid, color);
		log.info("payload {}", jsonPayload);;
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void startGame(String gid, String pid, boolean ready) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"startGame\", \"gid\":\"%s\", \"pid\":\"%s\", \"ready\":\"%s\"}", gid, pid, ready);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void buildTrack(String gid, String pid, String mileposts) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"buildTrack\", \"gid\":\"%s\", \"pid\":\"%s\", \"mileposts\":%s}", gid, pid, mileposts);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void upgradeTrain(String gid, String pid, String upgradeType) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"upgradeTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"upgradeType\":%s}", gid, pid, upgradeType);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void placeTrain(String gid, String pid, int train, String where) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"placeTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"train\":%s, \"where\":%s}", gid, pid, train, where);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void moveTrain(String gid, String pid, String rid, int train, String mileposts) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"moveTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"rid\":\"%s\", \"train\":%s, \"mileposts\":%s}", gid, pid, rid, train, mileposts);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void pickupDeliverLoad(String messageType, String gid, String pid, int train, String load) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"%s\", \"gid\":\"%s\", \"pid\":\"%s\", \"train\":%s, \"load\":\"%s\"}", messageType, gid, pid, train, load);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void pickupLoad(String gid, String pid, int train, String load) throws IOException, InterruptedException {
		pickupDeliverLoad("pickupLoad", gid, pid, train, load);
	}
	
/*	private static void deliverLoad(String gid, String pid, int train, String load) throws IOException, InterruptedException {
		pickupDeliverLoad("deliverLoad", gid, pid, train, load);
	} */
	
	public static void dumpLoad(String gid, String pid, int train,  String load) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"dumpLoad\", \"gid\":\"%s\", \"pid\":\"%s\", \"train\":%s, \"load\":\"%s\"}", gid, pid, train, load);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void endTurn(String gid, String pid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"endTurn\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void endGame(String gid, String pid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"endGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	public static void startRecording(String name) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"startRecording\", \"file\":\"%s\"}", name);
		log.info("jsonPayload {}", jsonPayload);
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);
	}

	public static void endRecording() throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"endRecording\"}");
		log.info("jsonPayload {}", jsonPayload);
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);
	}
	
	public static void playRecording(String name) throws  IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"playRecording\", \"file\":\"%s\"}", name);
		log.info("jsonPayload {}", jsonPayload);
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);
	}

	public String getActivePlayer(String gid) throws Exception {
        String playerName = status(gid);
        String pidSig = "pid\":\"";
        int nameLocation = playerName.indexOf(pidSig);
        playerName = playerName.substring(nameLocation + pidSig.length());	// chop off start
        return playerName.substring(0, playerName.indexOf("\""));
	}
	
	public Integer getIntegerValue(String gid, String key) throws Exception {
        String statusString = status(gid);
        String sig = String.format("%s\":\"", key);
        int nameLocation = statusString.indexOf(sig);
        statusString = statusString.substring(nameLocation + sig.length());	// chop off start
        return Integer.parseInt(statusString.substring(0, statusString.indexOf(",")));
	}
	
}
