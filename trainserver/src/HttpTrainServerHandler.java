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
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HttpTrainServerHandler extends SimpleChannelInboundHandler<Object> {

	private HttpRequest request;
	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();
	private boolean isPost = false;

	private final static String NEW_GAME = "newGame";
	private final static String JOIN_GAME = "joinGame";
	private final static String START_GAME = "startGame";
	private final static String BUILD_TRACK = "buildTrack";
	private final static String UPGRADE_TRAIN = "upgradeTrain";
	private final static String START_TRAIN = "startTrain";
	private final static String PICKUP_LOAD = "pickupLoad";
	private final static String DELIVER_LOAD = "deliverLoad";
	private final static String DUMP_LOAD = "dumpLoad";
	private final static String MOVE_TRAIN = "moveTrain";
	private final static String END_TURN = "endTurn";
	private final static String END_GAME = "endGame";
	
	private static Logger log = LoggerFactory.getLogger(HttpTrainServerHandler.class);

	private static RandomString gameNamer = new RandomString(8); // use for
																	// generating
																	// (semi)unique
																	// gameIds

	Map<String, Game> games = new HashMap<String, Game>(); // games currently in progress;

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpRequest) {
			buf.setLength(0);
			HttpRequest request = this.request = (HttpRequest) msg;

			if (is100ContinueExpected(request))
				send100Continue(ctx);

			if (request.getMethod() == HttpMethod.GET)
			{
				isPost = false;
				buf.append(status());
				if (!writeResponse(request, ctx)) {
					// If keep-alive is off, close the connection once the
					// content is fully written.
					ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
							ChannelFutureListener.CLOSE);
				}
				return;
			}
			else if (request.getMethod() == HttpMethod.POST) 
				isPost = true;
		} 
		if (isPost) {
			if (msg instanceof HttpContent) {
				HttpContent httpContent = (HttpContent) msg;

				ByteBuf content = httpContent.content();
				if (content.isReadable()) {
					String requestText = content.toString(CharsetUtil.UTF_8);
					String requestType = parseMessageType(requestText);
					try {
						switch (requestType) {
						case NEW_GAME:
							String strResponse = newGame(requestText);
							buf.append(strResponse);
							log.info("newGame buf {}", buf);
							break;
						case JOIN_GAME:
							joinGame(requestText);
							break;
						case START_GAME:
							startGame(requestText);
							break;
						case BUILD_TRACK:
							buildTrack(requestText);
							break;
						case UPGRADE_TRAIN:
							upgradeTrain(requestText);
							break;
						case START_TRAIN:
							startTrain(requestText);
							break;
						case MOVE_TRAIN:
							moveTrain(requestText);
							break;
						case PICKUP_LOAD:
							pickupLoad(requestText);
							break;
						case DELIVER_LOAD:
							deliverLoad(requestText);
							break;
						case DUMP_LOAD:
							dumpLoad(requestText);
							break;
						case END_TURN:
							endTurn(requestText);
							break;
						case END_GAME:
							endGame(requestText);
							break;
						default:
							throw new GameException(GameException.INVALID_MESSAGE_TYPE);
						}
					} catch (GameException e) {
						String errorString = e.getMessage();
						Gson gson = new Gson();
						String jsonError = gson.toJson(errorString);
			            FullHttpResponse result = new DefaultFullHttpResponse(HTTP_1_1, 
			            	BAD_REQUEST, Unpooled.copiedBuffer(jsonError, CharsetUtil.UTF_8));
			            sendHttpResponse(ctx, result);

						buf.append(jsonError);
					}
				}

				if (msg instanceof LastHttpContent) {

					LastHttpContent trailer = (LastHttpContent) msg;
					if (!writeResponse(trailer, ctx)) {
						// If keep-alive is off, close the connection once the
						// content is fully written.
						ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
								ChannelFutureListener.CLOSE);
					}
				}
			}
		}
	}

	private String status() {
		return null;
	}

	class NewGameData {
		//public String messageType;
		public String pid; // host playerId
		public String color; // color for track building
		public String ruleSet; // name for rules of the game
		public String gameType; // which game (Africa, Eurasia, etc.)
		
		NewGameData() {}
	}
	
	class NewGameResponse {
		public String gid;
		NewGameResponse() {}
	}
	
	private String newGame(String requestText) throws GameException {			
		String gameId = null;
		Gson gson = new GsonBuilder().create();
		NewGameData data = gson.fromJson(requestText, NewGameData.class);
		Game game = new Game();
		gameId = gameNamer.nextString();
		games.put(gameId, game);
		game.newGame(data.pid, data.color, data.ruleSet, data.gameType);
		NewGameResponse response = new NewGameResponse();
		response.gid = gameId;
		return gson.toJson(response);
	}

	class JoinGameData {
		public String gid;
		public String pid;
		public String color;
		}
	
	private void joinGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		JoinGameData data = gson.fromJson(requestText, JoinGameData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.joinGame(data.pid, data.color);

	}

	class StartGameData {
		public String gid;
		public String pid;
	}

	private void startGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StartGameData data = gson.fromJson(requestText, StartGameData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.startGame(data.pid);
	}

	class BuildTrackData {
		public String gid;
		public String pid;
		public MilePostId[] mileposts;
	}

	private void buildTrack(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		BuildTrackData data = gson.fromJson(requestText, BuildTrackData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.buildTrack(data.pid, data.mileposts);
	}

	class UpgradeTrainData {
		public String gid;
		public String pid;
		public String upgradeType;
	}

	private void upgradeTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		UpgradeTrainData data = gson.fromJson(requestText,
				UpgradeTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (data.upgradeType == "Capacity" && data.upgradeType == "Speed")
			throw new GameException(GameException.INVALID_UPGRADE);
		game.upgradeTrain(data.pid,
				data.upgradeType == "Capacity" ? UpgradeType.CAPACITY
						: UpgradeType.SPEED);
	}

	class StartTrainData {
		public String gid;
		public String pid;
		public String city;
	}

	private void startTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StartTrainData data = gson.fromJson(requestText, StartTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.startTrain(data.pid, data.city);
	}

	class MoveTrainData {
		public String gid;
		public String pid;
		public MilePostId[] mileposts;
	}

	private void moveTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		MoveTrainData data = gson.fromJson(requestText, MoveTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.moveTrain(data.pid, data.mileposts);
	}

	class PickupLoadData {
		public String gid;
		public String pid;
		public String city;
		public String load;
	}

	private void pickupLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		PickupLoadData data = gson.fromJson(requestText, PickupLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.pickupLoad(data.pid, data.city, data.load);
	}

	class DeliverLoadData {
		public String gid;
		public String pid;
		public String city;
		public String load;
	}

	private void deliverLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		DeliverLoadData data = gson
				.fromJson(requestText, DeliverLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.deliverLoad(data.pid, data.city, data.load);
	}

	class DumpLoadData {
		public String gid;
		public String pid;
		public String load;
	}

	private void dumpLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		DumpLoadData data = gson.fromJson(requestText, DumpLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.dumpLoad(data.pid, data.load);
	}

	class EndTurnData {
		public String gid;
		public String pid;
	}

	private void endTurn(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndTurnData data = gson.fromJson(requestText, EndTurnData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endTurn(data.pid);
	}

	class EndGame {
		public String gid;
		public String pid;
	}

	private void endGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndGame data = gson.fromJson(requestText, EndGame.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endGame(data.pid);
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
		int endIndex = findNthExprInString(requestText, "\"", 4);
		return requestText.substring(startIndex + 1, endIndex);
	}

	private boolean writeResponse(HttpObject currentObj,
			ChannelHandlerContext ctx) {
		// Decide whether to close the connection or not.
		boolean keepAlive = isKeepAlive(request);
		// Build the response object.
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
				Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

		response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");

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

    private void sendHttpResponse(ChannelHandlerContext ctx, 
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
