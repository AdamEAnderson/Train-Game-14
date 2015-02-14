package player;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RequestQueue {
	public BlockingQueue<String> requestQueue;
	public final int QUEUE_SIZE = 1000;
	
	RequestQueue() {
		requestQueue = new ArrayBlockingQueue<String>(QUEUE_SIZE);
	}
	
	RequestQueue(int queueSize) {
		requestQueue = new ArrayBlockingQueue<String>(queueSize);
	}
};
