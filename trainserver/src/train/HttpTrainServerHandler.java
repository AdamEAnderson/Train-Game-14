package train;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import com.google.gson.Gson;

/** Translates incoming http GET and PUT into calls on TrainGame interface */
public class HttpTrainServerHandler extends SimpleChannelInboundHandler<Object> {

	private HttpRequest request;
	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();
	private boolean isPost = false;

	private final static String NEW_GAME = "newGame";
	private final static String JOIN_GAME = "joinGame";
	private final static String RESUME_GAME = "resumeGame";
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
	
	private final static String LIST = "list";
	private final static String STATUS = "status";
	
	private static Logger log = LoggerFactory.getLogger(HttpTrainServerHandler.class);

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
				try {
					String query = request.getUri().substring(request.getUri().indexOf("?") + 1);	// get the query part
					query = query.replaceAll("%22", "\""); // quick and dirty url decode
					String requestType = parseMessageType(query);
					switch (requestType) {
						case LIST:
							buf.append(TrainServer.list(query));
							break;
						case STATUS:
							buf.append(TrainServer.status(query));
							break;
						default:
							throw new GameException(GameException.INVALID_MESSAGE_TYPE);
					}
					if (!writeResponse(request, ctx)) {
						// If keep-alive is off, close the connection once the
						// content is fully written.
						ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
								ChannelFutureListener.CLOSE);
					}
				} catch (GameException e) {
					String errorString = e.getMessage();
					log.error("Game exception {}", errorString);
					Gson gson = new Gson();
					String jsonError = gson.toJson(errorString);
		            FullHttpResponse result = new DefaultFullHttpResponse(HTTP_1_1, 
		            	BAD_REQUEST, Unpooled.copiedBuffer(jsonError, CharsetUtil.UTF_8));
		            sendHttpResponse(ctx, result);

					buf.append(jsonError);
				}
				return;
			}
			else if (request.getMethod() == HttpMethod.POST) 
				isPost = true;
		} 
		if (isPost) {
			if (requestText != null) {
				log.info("requestText: {}", requestText);
				String requestType = parseMessageType(requestText);
				try {
					switch (requestType) {
					case NEW_GAME:
						buf.append(TrainServer.newGame(requestText));
						log.info("newGame buf {}", buf);
						break;
					case JOIN_GAME:
						buf.append(TrainServer.joinGame(requestText));
						log.info("joinGame buf {}", buf);
						break;
					case RESUME_GAME:
						buf.append(TrainServer.resumeGame(requestText));
						log.info("resumeGame buf {}", buf);
						break;
					case START_GAME:
						TrainServer.startGame(requestText);
						break;
					case BUILD_TRACK:
						TrainServer.buildTrack(requestText);
						break;
					case UPGRADE_TRAIN:
						TrainServer.upgradeTrain(requestText);
						break;
					case START_TRAIN:
						TrainServer.startTrain(requestText);
						break;
					case MOVE_TRAIN:
						TrainServer.moveTrain(requestText);
						break;
					case PICKUP_LOAD:
						TrainServer.pickupLoad(requestText);
						break;
					case DELIVER_LOAD:
						TrainServer.deliverLoad(requestText);
						break;
					case DUMP_LOAD:
						TrainServer.dumpLoad(requestText);
						break;
					case END_TURN:
						TrainServer.endTurn(requestText);
						break;
					case END_GAME:
						TrainServer.endGame(requestText);
						break;
					default:
						throw new GameException(GameException.INVALID_MESSAGE_TYPE);
					}
				} catch (GameException e) {
					String errorString = e.getMessage();
					log.error("Game exception {}", errorString);
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
		if (startIndex < 0 && startIndex >= requestText.length())
			return "badMessage";
		int endIndex = findNthExprInString(requestText, "\"", 4);
		if (endIndex < 0 && endIndex >= requestText.length())
			return "badMessage";
		return requestText.substring(startIndex + 1, endIndex);
	}

	private boolean writeResponse(HttpObject currentObj,
			ChannelHandlerContext ctx) {
		// Decide whether to close the connection or not.
		boolean keepAlive = isKeepAlive(request);
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
