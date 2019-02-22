package rpf.study.websocket.server.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
@Component
public class NioWebSocketServer implements Runnable {
    private final Logger logger=Logger.getLogger(this.getClass());
    private static int port;
    @Value("${websocket.port}")
    public void setPort(int port) {
        this.port = port;
    }

    public void run(){
        logger.info("正在启动websocket服务器");
        NioEventLoopGroup boss=new NioEventLoopGroup();
        NioEventLoopGroup work=new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap=new ServerBootstrap();
            bootstrap.group(boss,work);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new NioWebSocketChannelInitializer());
            Channel channel = bootstrap.bind(port).sync().channel();
            logger.info("webSocket服务器启动成功："+channel);
            logger.info("webSocket服务器启动成功："+port);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.info("运行出错："+e);
        }finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
            logger.info("websocket服务器已关闭");
        }
    }
    @PostConstruct
    public  void main() {
        new Thread(new NioWebSocketServer()).start();
    }
}
