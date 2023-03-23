package cn.jja8.newbinggogo;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NewBingGoGoClientWebSocket extends NanoWSD.WebSocket {
    ScheduledFuture<?> task;
    WebSocketClient webSocketClient;
    boolean webSocketClientOpen = false;
    LinkedList<String> webSocketClientOpenedMessage = new LinkedList<>();
    public NewBingGoGoClientWebSocket(NanoHTTPD.IHTTPSession handshakeRequest, ScheduledExecutorService executor) {
        super(handshakeRequest);
        task = executor.scheduleAtFixedRate(() -> {
            try {
                ping(new byte[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    protected void onOpen() {
        try {
            webSocketClient = new WebSocketClient(new URI("wss://sydney.bing.com/sydney/ChatHub")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    webSocketClientOpen = true;
                    webSocketClientOpenedMessage.forEach(this::send);
                }

                @Override
                public void onMessage(String message) {
                    try {
                        NewBingGoGoClientWebSocket.this.send(message);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    try {
                        NewBingGoGoClientWebSocket.this.close(NanoWSD.WebSocketFrame.CloseCode.find(code),reason,remote);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    try {
                        NewBingGoGoClientWebSocket.this.send("{\"type\":\"error\",\"mess\":\"workers接到bing错误\"}");
                        NewBingGoGoClientWebSocket.this.close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure,"ok",true);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            };
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        task.cancel(true);
        webSocketClient.close();
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        if (!webSocketClientOpen) {
            webSocketClientOpenedMessage.addLast(message.getTextPayload());
        }else {
            webSocketClient.send(message.getTextPayload());
        }
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {

    }

    @Override
    protected void onException(IOException exception) {
        webSocketClient.close();
    }
}
