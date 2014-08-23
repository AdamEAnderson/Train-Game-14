package train;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import com.google.gson.Gson;

/** Translates incoming http GET and PUT into calls on TrainGame interface */
public class HttpTrainServerHandler extends SimpleChannelInboundHandler<Object> implements Runnable {

	private HttpRequest request;
	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();
	private boolean isPost = false;
	
	private BlockingQueue<TrainMessage> messageQueue = new ArrayBlockingQueue<TrainMessage>(1000);
	private boolean isStopped = false;		// when true, stop handling new messages
	private Thread messageHandler;

	private final static String NEW_GAME = "newGame";
	private final static String JOIN_GAME = "joinGame";
	private final static String RESUME_GAME = "resumeGame";
	private final static String START_GAME = "startGame";
	private final static String BUILD_TRACK = "buildTrack";
	private final static String TEST_BUILD_TRACK = "testBuildTrack";
	private final static String UPGRADE_TRAIN = "upgradeTrain";
	private final static String PLACE_TRAIN = "placeTrain";
	private final static String PICKUP_LOAD = "pickupLoad";
	private final static String DELIVER_LOAD = "deliverLoad";
	private final static String DUMP_LOAD = "dumpLoad";
	private final static String TEST_MOVE_TRAIN = "testMoveTrain";
	private final static String MOVE_TRAIN = "moveTrain";
	private final static String TURN_IN_CARDS = "turnInCards";
	private final static String END_TURN = "endTurn";
	private final static String END_GAME = "endGame";
	private final static String RESIGN_GAME = "resignGame";
	
	private final static String LIST = "list";
	private final static String STATUS = "status";
	private final static String LIST_COLORS = "listColors";
	
	private static Logger log = LoggerFactory.getLogger(HttpTrainServerHandler.class);
	
	private static class TrainMessage {
		ChannelHandlerContext ctx;
		HttpObject httpMessage;
		String jsonMessage;
		
		public TrainMessage(ChannelHandlerContext ctx, Object httpMessage, String jsonMessage) {
			this.ctx = ctx;
			ReferenceCountUtil.retain(httpMessage);
			this.httpMessage = (HttpObject)httpMessage;
			this.jsonMessage = jsonMessage;
		}
	}

	public HttpTrainServerHandler() {
		// start the message loop
		messageHandler = new Thread(this);
		messageHandler.start();
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		String requestText = null;
		if (msg instanceof HttpContent) {
			HttpContent httpContent = (HttpContent) msg;
			ByteBuf content = httpContent.content();
			if (content.isReadable()) 
				requestText = content.toString(CharsetUtil.UTF_8);
		}
		buf.setLength(0);
		if (msg instanceof HttpRequest) {
			HttpRequest request = this.request = (HttpRequest) msg;

			if (is100ContinueExpected(request))
				send100Continue(ctx);

			if (request.getMethod() == HttpMethod.GET)
			{
				//log.info("Incoming GET: {}", request.getUri());
				isPost = false;
				// Incoming is either a request for games to join, games to resume, 
				// or status on a particular game
				String query = request.getUri().substring(request.getUri().indexOf("?") + 1);	// get the query part
				query = query.replaceAll("%22", "\""); // quick and dirty url decode
				messageQueue.add(new TrainMessage(ctx, msg, query));
				return;
			}
			else if (request.getMethod() == HttpMethod.POST) 
				isPost = true;
		} 
		if (isPost && requestText != null) {
			log.info("requestText: {}", requestText);
			messageQueue.add(new TrainMessage(ctx, msg, requestText));
		}
	}

	public void stop() {
		isStopped = true;
		try {
			messageHandler.join();
			TrainServer.stop();
		} catch (InterruptedException e) {
            Thread.currentThread().interrupt();
		}		
	}
	
	public void run() {
		while (!isStopped) {
			try {
				TrainMessage message = (TrainMessage) messageQueue.take();
				handleMessage(message);
				ReferenceCountUtil.release(message.httpMessage);
			} catch (InterruptedException e) {
	             Thread.currentThread().interrupt();
			}
		}
	}
	
	private void handleMessage(TrainMessage message) {
		String requestType = parseMessageType(message.jsonMessage);
		try {
			switch (requestType) {
				case NEW_GAME:
					buf.append(TrainServer.newGame(message.jsonMessage));
					log.debug("newGame buf {}", buf);
					break;
				case JOIN_GAME:
					buf.append(TrainServer.joinGame(message.jsonMessage));
					log.debug("joinGame buf {}", buf);
					break;
				case RESUME_GAME:
					buf.append(TrainServer.resumeGame(message.jsonMessage));
					log.debug("resumeGame buf {}", buf);
					break;
				case START_GAME:
					TrainServer.startGame(message.jsonMessage);
					break;
				case TEST_BUILD_TRACK:
					TrainServer.testBuildTrack(message.jsonMessage);
					break;
				case BUILD_TRACK:
					TrainServer.buildTrack(message.jsonMessage);
					break;
				case UPGRADE_TRAIN:
					TrainServer.upgradeTrain(message.jsonMessage);
					break;
				case PLACE_TRAIN:
					TrainServer.placeTrain(message.jsonMessage);
					break;
				case TEST_MOVE_TRAIN:
					TrainServer.testMoveTrain(message.jsonMessage);
					break;
				case MOVE_TRAIN:
					TrainServer.moveTrain(message.jsonMessage);
					break;
				case PICKUP_LOAD:
					TrainServer.pickupLoad(message.jsonMessage);
					break;
				case DELIVER_LOAD:
					TrainServer.deliverLoad(message.jsonMessage);
					break;
				case DUMP_LOAD:
					TrainServer.dumpLoad(message.jsonMessage);
					break;
				case TURN_IN_CARDS:
					TrainServer.turnInCards(message.jsonMessage);
					break;
				case END_TURN:
					TrainServer.endTurn(message.jsonMessage);
					break;
				case END_GAME:
					TrainServer.endGame(message.jsonMessage);
					break;
				case RESIGN_GAME:
					TrainServer.resignGame(message.jsonMessage);
					break;
				case LIST:
					buf.append(TrainServer.list(message.jsonMessage));
					break;
				case STATUS:
					buf.append(TrainServer.status(message.jsonMessage));
					break;
				case LIST_COLORS:
					buf.append(TrainServer.listColors(message.jsonMessage));
					break;
				default:
					throw new GameException(GameException.INVALID_MESSAGE_TYPE);
			}
			if (!writeResponse(message.httpMessage, message.ctx)) {
				// If keep-alive is off, close the connection once the
				// content is fully written.
				message.ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
						ChannelFutureListener.CLOSE);
			}
		} catch (GameException e) {
			String errorString = e.getMessage();
			log.error("Game exception {}", errorString);
			Gson gson = new Gson();
			String jsonError = gson.toJson(errorString);
            FullHttpResponse result = new DefaultFullHttpResponse(HTTP_1_1, 
            	BAD_REQUEST, Unpooled.copiedBuffer(jsonError, CharsetUtil.UTF_8));
            result.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
            result.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            sendHttpResponse(message.ctx, result);
		}
		message.ctx.flush();
	}
	
	private static int findNthExprInString(String s, String expr, int n)
	{
		int index = -1;
		int findCount = 0;
		while (findCount < n)
		{
			index = s.indexOf(expr, index + 1);
			if (index == -1)
				break;	// expr not found
			++findCount;
		}
		return index;
	}
	
	// Custom parsing of the messageType, so we can dispatch
	private static String parseMessageType(String requestText) {
		// String extends from the 3rd to the fourth 4th quote
		int startIndex = findNthExprInString(requestText, "\"", 3);
		if (startIndex < 0 || startIndex + 1 >= requestText.length())
			return "badMessage";
		int endIndex = findNthExprInString(requestText, "\"", 4);
		if (endIndex < 0 || endIndex >= requestText.length())
			return "badMessage";
		return requestText.substring(startIndex + 1, endIndex);
	}

	private boolean writeResponse(HttpObject currentObj,
			ChannelHandlerContext ctx) {

		// If there's no explicit response, send an OK on success
		if (currentObj.getDecoderResult().isSuccess() && 
				buf.toString().length() == 0)
			buf.append("{\"status\":\"OK\"}");

		// Build the response object.
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
			currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
			Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

		response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
		response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			
		//log.info("Sending response length {}", response.content().readableBytes());
		//log.info("Sending response text", buf.toString());
		return sendHttpResponse(ctx, response);
	}

    private boolean sendHttpResponse(ChannelHandlerContext ctx, 
    		FullHttpResponse response) {
        // Generate an error page if response getStatus code is not OK (200).

        // Send the response and close the connection if necessary.
		boolean keepAlive = isKeepAlive(request);
		if (keepAlive) {
			// Add 'Content-Length' header only for a keep-alive connection.
			response.headers().set(CONTENT_LENGTH,
					response.content().readableBytes());
			// Add keep alive header as per:
			// -
			// http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
			response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}

		// Write the response.
		ctx.write(response);
		return keepAlive;
    }

	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				CONTINUE);
		ctx.write(response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
