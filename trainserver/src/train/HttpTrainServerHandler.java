package train;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;


/** Translates incoming http GET and PUT into calls on TrainGame interface */
public class HttpTrainServerHandler extends SimpleChannelInboundHandler<Object> {

	private HttpRequest request;
	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();
	private boolean isPost = false;
	
	private static TrainServer trainServer = new TrainServer();
	
	private static Logger log = LoggerFactory.getLogger(HttpTrainServerHandler.class);
	
	public HttpTrainServerHandler() {
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
				trainServer.addMessage(this, ctx, msg, query, false);
				return;
			}
			else if (request.getMethod() == HttpMethod.POST) 
				isPost = true;
		} 
		if (isPost && requestText != null) {
			log.info("requestText: {}", requestText);
			trainServer.addMessage(this, ctx, msg, requestText, true);
		}
	}

	public void appendToResponse(String message) {
		buf.append(message);
	}
	
	public void sendError(String jsonError, ChannelHandlerContext ctx) {
        FullHttpResponse result = new DefaultFullHttpResponse(HTTP_1_1, 
            	BAD_REQUEST, Unpooled.copiedBuffer(jsonError, CharsetUtil.UTF_8));
        result.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        result.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        sendHttpResponse(ctx, result);
	}
	
	public boolean writeResponse(HttpObject currentObj,
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
