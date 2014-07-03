
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



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
	private static String sendMessage(HttpURLConnection connection, String message) throws IOException, InterruptedException {
		
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

	private static String newGame(String pid, String color) throws IOException, InterruptedException {
		//String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"ruleSet\":\"anythingGoes\", \"gameType\":\"Africa\"}";
		String jsonPayload = String.format("{\"messageType\":\"newGame\", \"pid\":\"%s\", \"color\":\"%s\", \"ruleSet\":\"anythingGoes\", \"gameType\":\"Africa\"}", pid, color);
		log.info("jsonPayload {}", jsonPayload);
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);
        assertTrue(responseMessage.startsWith("{\"gid\":\""));
        String gid = responseMessage.substring(8, responseMessage.length() - 3);
        return gid;
	}
	
	private static void joinGame(String gid, String pid, String color) throws IOException, InterruptedException {

		String jsonPayload = String.format("{\"messageType\":\"joinGame\", \"gid\":\"%s\", \"pid\":\"%s\",\"color\":\"$s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;
		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void startGame(String gid, String pid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"startGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void buildTrack(String gid, String pid, String mileposts) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"buildTrack\", \"gid\":\"%s\", \"pid\":\"%s\", \"mileposts\":%s}", gid, pid, mileposts);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void upgradeTrain(String gid, String pid, String upgradeType) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"upgradeTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"upgradeType\":%s}", gid, pid, upgradeType);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void startTrain(String gid, String pid, String city) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"startTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"city\":%s}", gid, pid, city);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void moveTrain(String gid, String pid, String mileposts) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"moveTrain\", \"gid\":\"%s\", \"pid\":\"%s\", \"mileposts\":%s}", gid, pid, mileposts);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void pickupDeliverLoad(String messageType, String gid, String pid, String city, String load) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"%s\", \"gid\":\"%s\", \"pid\":\"%s\", \"city\":\"%s\", \"load\":\"%s\"}", messageType, gid, pid, city, load);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
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
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void endTurn(String gid, String pid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"endTurn\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	private static void endGame(String gid, String pid) throws IOException, InterruptedException {
		String jsonPayload = String.format("{\"messageType\":\"endGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		log.info("payload {}", jsonPayload);;

		HttpURLConnection connection = (HttpURLConnection) new URL(serverURL).openConnection();
        String responseMessage = sendMessage(connection, jsonPayload);
        assertEquals(connection.getResponseCode(), 200);

        if (responseMessage != null && responseMessage.length() == 0)
        	log.info("Got response message {}", responseMessage);
	}
	
	// Send a newGame request to the server check the result
	@Test
	public void testNewGame() throws Exception {
		Thread serverThread = startServer();
		
		String responseMessage = newGame("Adam", "black");

        log.info("Got response message {}", responseMessage);
        HttpTrainServer.stopServer();
        serverThread.join();
    }

	// Send a newGame request to the server check the result
	@Test
	public void testJoinGame() throws Exception {
		Thread serverThread = startServer();
		
        String gid = newGame("Adam", "red");
        joinGame(gid, "Sandra", "green");
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
    }

	// Send a newGame request to the server check the result
	@Test
	public void testStartGame() throws Exception {
		Thread serverThread = startServer();
		
        String gid = newGame("Adam", "red");
        joinGame(gid, "Sandra", "green");
        joinGame(gid, "Sandy", "red");
        joinGame(gid, "Robin", "purple");
        startGame(gid, "Adam");
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
    }

	// Send a buildTrack request to the server check the result
	@Test
	public void testBuildTrack() throws Exception {
		Thread serverThread = startServer();
		
        String gid = newGame("Adam", "red");
        joinGame(gid, "Sandra", "green");
        joinGame(gid, "Sandy", "red");
        joinGame(gid, "Robin", "purple");
        startGame(gid, "Adam");
        
        String mileposts = "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1},{\"x\":2,\"y\":2}]";
        buildTrack(gid, "Sandy", mileposts);
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
    }


	// Send a upgradeTrain request to the server check the result
	@Test
	public void testUpgradeTrain() throws Exception {
		Thread serverThread = startServer();
		
        String gid = newGame("Adam", "red");
        joinGame(gid, "Sandra", "green");
        joinGame(gid, "Sandy", "red");
        joinGame(gid, "Robin", "purple");
        startGame(gid, "Adam");
        
        upgradeTrain(gid, "Robin", "Speed");
        
        String mileposts = "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1},{\"x\":2,\"y\":2}]";
        buildTrack(gid, "Sandy", mileposts);
        upgradeTrain(gid, "Sandy", "Capacity");
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
    }

	// Send a buildTrack request to the server check the result
	@Test
	public void testMoveTrain() throws Exception {
		Thread serverThread = startServer();
		
        String gid = newGame("Adam", "red");
        joinGame(gid, "Sandra", "green");
        joinGame(gid, "Sandy", "red");
        joinGame(gid, "Robin", "purple");
        startGame(gid, "Adam");
        
        String mileposts = "[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1},{\"x\":2,\"y\":2}]";
        buildTrack(gid, "Sandy", mileposts);
        
        startTrain(gid, "Sandy", "Dakar");
        moveTrain(gid, "Sandy", mileposts);
        pickupLoad(gid, "Sandy", "Abidjan", "turnips");
        moveTrain(gid, "Sandy", mileposts);
        deliverLoad(gid, "Sandy", "Port Harcourt", "turnips");
        dumpLoad(gid, "Sandy", "Port Harcourt", "iron");
        endTurn(gid, "Sandy");
        endGame(gid, "Sandra");
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
    }





}
