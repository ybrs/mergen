package com.plexobject.netty.server;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;
 
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.CharsetUtil;
 
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
 
public class WebSocketServerHandler extends ChannelInboundMessageHandlerAdapter<Object> {
    @SuppressWarnings("unused")
    private static final InternalLogger LOGGER = InternalLoggerFactory
            .getInstance(WebSocketServerHandler.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final JsonFactory jsonFactory = jsonMapper.getJsonFactory();
 
    private static final String WEBSOCKET_PATH = "/";
    private WebSocketServerHandshaker handshaker;
    private static Map<String, Set<ChannelHandlerContext>> subscriptions = new ConcurrentHashMap<String, Set<ChannelHandlerContext>>();
    private static Timer timer = new Timer(true);
    private static final AtomicLong REQUEST_ID = new AtomicLong();
    private static final Metrics METRICS = new Metrics();
 
    static {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
               while (true) {
                  sendMessageForRandomIdentifier();
               }
            }
        }, 1000);
    }
 
    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }
 
    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req)
            throws Exception {
        // Allow only GET methods.
        if (req.getMethod() != GET) {
            sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1,
                    FORBIDDEN));
            return;
        }
        if (req.getUri().equals("/favicon.ico")) {
            HttpResponse res = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
            sendHttpResponse(ctx, req, res);
            return;
        }
 
        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }
 
    private void handleWebSocketFrame(ChannelHandlerContext ctx,
            WebSocketFrame frame) throws IOException {
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame);
            return;
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.getBinaryData()));
            return;
        } else if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format(
                    "%s frame types not supported", frame.getClass().getName()));
        }
 
        String jsonText = ((TextWebSocketFrame) frame).getText();
        JsonParser jp = jsonFactory.createJsonParser(jsonText);
        JsonNode actualObj = jsonMapper.readTree(jp);
        String action = actualObj.get("action").getTextValue();
        String identifier = actualObj.get("identifier").getTextValue();
        if (action == null || identifier == null) {
              return;
        }
        if ("subscribe".equals(action)) {
            Set<ChannelHandlerContext> contexts = subscriptions.get(identifier);
            if (contexts == null) {
                contexts = new HashSet<ChannelHandlerContext>();
                subscriptions.put(identifier, contexts);
            }
            contexts.add(ctx);
        } else if ("unsubscribe".equals(action)) {
            Set<ChannelHandlerContext> contexts = subscriptions.get(identifier);
            if (contexts == null) {
                return;
            }
            contexts.remove(ctx);
            if (contexts.size() == 0) {
                subscriptions.remove(identifier);
            }
        }
    }
 
    private static String getRandomIdentifier() {
        List<String> identifiers = new ArrayList<String>(subscriptions.keySet());
        if (identifiers.size() == 0) {
            return null;
        }
        int n = new Random().nextInt(identifiers.size());
        return identifiers.get(n);
    }
 
    public static void sendMessageForRandomIdentifier() {
        String identifier = getRandomIdentifier();
        if (identifier == null) {
            return;
        }
        Set<ChannelHandlerContext> contexts = subscriptions.get(identifier);
        if (contexts == null || contexts.size() == 0) {
            return;
        }
        SecureRandom random = new SecureRandom();
        String elements = new BigInteger(500, random).toString(32);
        String json = "{\"request\":" + REQUEST_ID.incrementAndGet() + ", \"timestamp\":" + System.currentTimeMillis() + ", \"identifier\":\"" + identifier + "\", \"elements\": \"" + elements + "\"}";
        TextWebSocketFrame frame = new TextWebSocketFrame(json);
        METRICS.update(json);
        for (ChannelHandlerContext ctx : contexts) {
            try {
                ctx.channel().write(frame);
            } catch (Exception e) {
                subscriptions.remove(ctx);
            }
        }
        if (System.currentTimeMillis() % 1000 == 0) {
            System.out.println("identifiers," + Metrics.getHeading());
            System.out.println(subscriptions.size() + METRICS.getSummary().toString());
        }
    }
 
    private static void sendHttpResponse(ChannelHandlerContext ctx,
            HttpRequest req, HttpResponse res) {
        // Generate an error page if response status code is not OK (200).
        if (res.getStatus().getCode() != 200) {
            res.setContent(Unpooled.copiedBuffer(res.getStatus().toString(),
                    CharsetUtil.UTF_8));
            setContentLength(res, res.getContent().readableBytes());
        }
 
        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().write(res);
        if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
 
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        if (cause instanceof java.nio.channels.ClosedChannelException == false) {
            //cause.printStackTrace();
        }
        ctx.close();
    }
 
    private static String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
    }
}
