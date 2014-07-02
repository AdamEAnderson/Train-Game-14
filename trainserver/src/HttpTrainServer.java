  
  import io.netty.bootstrap.ServerBootstrap;
  import io.netty.channel.Channel;
  import io.netty.channel.EventLoopGroup;
  import io.netty.channel.nio.NioEventLoopGroup;
  import io.netty.channel.socket.nio.NioServerSocketChannel;
  import io.netty.handler.logging.LogLevel;
  import io.netty.handler.logging.LoggingHandler;
  import io.netty.handler.ssl.SslContext;
  import io.netty.handler.ssl.util.SelfSignedCertificate;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  

  /**
   * Main entry point for the train server.
   */
  public class HttpTrainServer {
  
      static final boolean SSL = System.getProperty("ssl") != null;
      static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));
  
      static private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
      static private EventLoopGroup workerGroup = new NioEventLoopGroup();

      public static void main(String[] args) throws Exception {

    	  // Set up logging
    	  Logger logger = LoggerFactory.getLogger(HttpTrainServer.class);
    	  logger.info("Hello World");

    	    // Configure SSL.
          final SslContext sslCtx;
          if (SSL) {
              SelfSignedCertificate ssc = new SelfSignedCertificate();
              sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
          } else {
              sslCtx = null;
          }
  
          // Configure the server.
          EventLoopGroup bossGroup = new NioEventLoopGroup(1);
          EventLoopGroup workerGroup = new NioEventLoopGroup();
          try {
              ServerBootstrap b = new ServerBootstrap();
              b.group(bossGroup, workerGroup)
               .channel(NioServerSocketChannel.class)
               .handler(new LoggingHandler(LogLevel.INFO))
               .childHandler(new HttpTrainServerInitializer(sslCtx));
  
              Channel ch = b.bind(PORT).sync().channel();
  
              System.err.println("Open your web browser and navigate to " +
                      (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');
  
              ch.closeFuture().sync();
          } finally {
              bossGroup.shutdownGracefully();
              workerGroup.shutdownGracefully();
          }
      }
      
      public static void startServer() throws Exception {
          // Configure SSL.
          final SslContext sslCtx;
          if (SSL) {
              SelfSignedCertificate ssc = new SelfSignedCertificate();
              sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
          } else {
              sslCtx = null;
          }
  
          // Configure the server.
          bossGroup = new NioEventLoopGroup(1);
          workerGroup = new NioEventLoopGroup();
          try {
              ServerBootstrap b = new ServerBootstrap();
              b.group(bossGroup, workerGroup)
               .channel(NioServerSocketChannel.class)
               .handler(new LoggingHandler(LogLevel.INFO))
               .childHandler(new HttpTrainServerInitializer(sslCtx));
  
              Channel ch = b.bind(PORT).sync().channel();
  
              System.err.println("Open your web browser and navigate to " +
                      (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');
  
              ch.closeFuture().sync();
          } finally {
              bossGroup.shutdownGracefully();
              workerGroup.shutdownGracefully();
          }
      }
      
      static public void stopServer() {
          bossGroup.shutdownGracefully();
          workerGroup.shutdownGracefully();
      }

  }
