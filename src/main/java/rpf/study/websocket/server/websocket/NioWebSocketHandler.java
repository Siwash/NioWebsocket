package rpf.study.websocket.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rpf.study.websocket.pojo.PayLoad;
import rpf.study.websocket.pojo.User;
import rpf.study.websocket.server.global.ChannelSupervise;
import rpf.study.websocket.server.global.ChatGroup;
import rpf.study.websocket.server.global.OUC;

import java.io.IOException;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
@Component
public class NioWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private final Logger logger=Logger.getLogger(this.getClass());

    private WebSocketServerHandshaker handshaker;

    private static String address;
    @Value("${websocket.address}")
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("收到消息："+msg);
        if (msg instanceof FullHttpRequest){
            //以http请求形式接入，但是走的是websocket
                handleHttpRequest(ctx, (FullHttpRequest) msg);
        }else if (msg instanceof  WebSocketFrame){
            //处理websocket客户端的消息
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //添加连接
        logger.debug("客户端加入连接："+ctx.channel());

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //断开连接
        logger.debug("客户端断开连接："+ctx.channel());
        ChannelSupervise.removeChannel(ctx.channel());
        OUC.removeUser(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame){
        // 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(
                    new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 本例程仅支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            logger.debug("本例程仅支持文本消息，不支持二进制消息");
            throw new UnsupportedOperationException(String.format(
                    "%s frame types not supported", frame.getClass().getName()));
        }
        // 返回应答消息
        String request = ((TextWebSocketFrame) frame).text();
        logger.debug("服务端收到：" + request);
        //消息分发
        WebSocketDispatcher(ctx,(TextWebSocketFrame)frame);
    }
    /**
     * 唯一的一次http请求，用于创建websocket
     * */
    private void handleHttpRequest(ChannelHandlerContext ctx,
                                   FullHttpRequest req) {
        //要求Upgrade为websocket，过滤掉get/Post等http请求
        if (!req.decoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {
            //若不是websocket方式，则创建BAD_REQUEST的req，返回给客户端
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        String uri = req.uri();
        System.out.println();
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                address, null, false);
        handshaker = wsFactory.newHandshaker(req);
        logger.debug("地址："+address);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory
                    .sendUnsupportedVersionResponse(ctx.channel());
        } else {
            User user = new User(uri.substring(uri.indexOf("?") + 1), ctx.channel().id().toString());
            OUC.addUser(user);
            handshaker.handshake(ctx.channel(), req);
            ChannelSupervise.addChannel(ctx.channel());
            //保存用户名和channelId
            PayLoad payLoad = new PayLoad(PayLoad.SYS,user.toString());
            TextWebSocketFrame tws = new TextWebSocketFrame(payLoad.toString());
            ctx.channel().writeAndFlush(tws);
        }
    }
    /**
     * 拒绝不合法的http请求，并返回
     * */
    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(),
                    CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        // 如果是非Keep-Alive，关闭连接
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 消息分发器
     * **/
    private static void WebSocketDispatcher(ChannelHandlerContext ctx, TextWebSocketFrame frame){
        String request = frame.text();
        ObjectMapper objectMapper = new ObjectMapper();
        PayLoad payLoad = new PayLoad();
        try {
            JsonNode jsonNode = objectMapper.readTree(request);
            String type=jsonNode.path("payLoad").path("type").asText();
            if (type.equals(PayLoad.PVP)){
                String channelId=jsonNode.path("target").asText();
                Channel channel = ChannelSupervise.findChannel(channelId);
                if (channel!=null){
                    channel.writeAndFlush(new TextWebSocketFrame(request));
                }else{
                    throw  new IOException();
                }
            }else if (type.equals(PayLoad.PVG)){
                String groupChatName=jsonNode.path("target").asText();
                String channelId = jsonNode.path("source").asText();
                ChannelGroup channels = ChatGroup.getChatGroup(groupChatName);
                if (channels!=null){
                    Channel channel = ChannelSupervise.findChannel(channelId);
                    //不给自己发消息
                    channels.remove(channel);
                    channels.writeAndFlush(new TextWebSocketFrame(request));
                    channels.add(channel);
                }else {
                    throw  new IOException();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            payLoad.setType(PayLoad.ERROR);
            payLoad.setData("消息发送失败！[ "+request+" ]");
            ctx.writeAndFlush(new TextWebSocketFrame(payLoad.toString()));
        } finally {

        }
    }
}
