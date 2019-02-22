package rpf.study.websocket.server.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class NioWebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast("logging",new LoggingHandler("LOG"));//设置log监听器，并且日志级别为debug，方便观察运行流程
        ch.pipeline().addLast("http-codec",new HttpServerCodec());
        ch.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
        ch.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
        ch.pipeline().addLast("handler",new NioWebSocketHandler());
    }
}
