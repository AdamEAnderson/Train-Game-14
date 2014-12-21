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

        if (message != null && message.length() > 0)
        	connection.getOutputStream().write(message.getBytes());
        // give it 15 seconds to respond
        connection.setReadTimeout(15*1000);

        return getResponse(connection);
    }

	// Send a simple POST request to the server with the message as content and return the result code
	private static String sendPostMessage(HttpURLConnection connection, String message) throws IOException, InterruptedException {
		return sendMessage(connection, message, true);
    }

	private static String status(String gid) throws IOException, InterruptedException {
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
        assertTrue(responseMessage.startsWith("{\"gidNames\":{"));
        String gid = responseMessage.substring(14, 22);
        assertEquals(expectedGid, gid);
        return gid;
	}
	
	private static String newGame(String pid, String color) throws IOException, InterruptedException {
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
	
	private static void joinGame(String gid, String pid, String color) throws IOException, InterruptedException {

		String jsonPayload = String.format("{\"messageType\":\"joinGame\", \"gid\":\"%s\", \"pid\":\"%s\",\"color\":\"%s\"}", gid, pid, color);
		log.info("payload {}", jsonPayload);;
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void startGame(String gid, String pid, boolean ready) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"startGame\", \"gid\":\"%s\", \"pid\":\"%s\", \"ready\":\"%s\"}", gid, pid, ready);
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
	
	private static void placeTrain(String gid, String pid, int train, String where) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"placeTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"train\":%s, \"where\":%s}", gid, pid, train, where);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void moveTrain(String gid, String pid, int train, String mileposts) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"moveTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"train\":%s, \"mileposts\":%s}", gid, pid, train, mileposts);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void pickupDeliverLoad(String messageType, String gid, String pid, int train, String load) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"%s\", \"gid\":\"%s\", \"pid\":\"%s\", \"train\":%s, \"load\":\"%s\"}", messageType, gid, pid, train, load);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendPostMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void pickupLoad(String gid, String pid, int train, String load) throws IOException, InterruptedException {
		pickupDeliverLoad("pickupLoad", gid, pid, train, load);
	}
	
/*	private static void deliverLoad(String gid, String pid, int train, String load) throws IOException, InterruptedException {
		pickupDeliverLoad("deliverLoad", gid, pid, train, load);
	} */
	
	private static void dumpLoad(String gid, String pid, int train,  String load) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"dumpLoad\", \"gid\":\"%s\", \"pid\":\"%s\", \"train\":%s, \"load\":\"%s\"}", gid, pid, train, load);
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
	

	private String getActivePlayer(String gid) throws Exception {
        String playerName = status(gid);
        playerName = playerName.substring(30);	// chop off start
        return playerName.substring(0, playerName.indexOf("\""));
	}
	
	// Send a stream of requests to the server check the results
	//@Test
	public void testTrain() throws Exception {
		Thread serverThread = startServer();
		
        String gid = newGame("Huey", "red");
        list("joinable", gid);	// the new game should appear in the list of joinable games
        joinGame(gid, "Louie", "green");
        startGame(gid, "Huey", true);
        startGame(gid, "Louie", true);
        String currentPlayer = getActivePlayer(gid);
        
        String mileposts = "[{\"x\":34,\"y\":58},{\"x\":33,\"y\":58},{\"x\":32,\"y\":58},{\"x\":31,\"y\":59}]";
        buildTrack(gid, currentPlayer, mileposts);
        
        // skip past building turns
        endTurn(gid, currentPlayer);
        upgradeTrain(gid, getActivePlayer(gid), "Speed");
        endTurn(gid, getActivePlayer(gid));
        endTurn(gid, getActivePlayer(gid));
        endTurn(gid, getActivePlayer(gid));
        endTurn(gid, getActivePlayer(gid));
        endTurn(gid, getActivePlayer(gid));
                
        placeTrain(gid, currentPlayer, 0, "{\"x\":34, \"y\":58}");

        String moveMileposts = "[ {\"x\":33,\"y\":58},{\"x\":32,\"y\":58},{\"x\":31,\"y\":59}]";
        pickupLoad(gid, currentPlayer, 0, "Diamonds");
        pickupLoad(gid, currentPlayer, 0, "Arms");
        moveTrain(gid, currentPlayer, 0, moveMileposts);
        // deliverLoad(gid, currentPlayer, "turnips");
        dumpLoad(gid, currentPlayer, 0, "Arms");
        endTurn(gid, currentPlayer);
        endGame(gid, "Huey");
        endGame(gid, "Louie");
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
    }





}
