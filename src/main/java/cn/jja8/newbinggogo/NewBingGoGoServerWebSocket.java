package cn.jja8.newbinggogo;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NewBingGoGoServerWebSocket extends NanoWSD.WebSocket {
    NewBingGoGoClientWebSocket newBingGoGoClientWebSocket;
    ScheduledFuture<?> task;
    public NewBingGoGoServerWebSocket(NanoHTTPD.IHTTPSession handshakeRequest, ScheduledExecutorService executor) {
        super(handshakeRequest);
        task = executor.scheduleAtFixedRate(() -> {
            try {
                ping(new byte[1]);
            } catch (IOException e) {
                task.cancel(false);
            }
        }, 2, 2, TimeUnit.SECONDS);

        URI url;
        try {
            url = new URI("wss://sydney.bing.com/sydney/ChatHub");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);//这个异常这辈子都不会出的
        }

        newBingGoGoClientWebSocket = new NewBingGoGoClientWebSocket(url,this);
        newBingGoGoClientWebSocket.connect();
    }

    @Override
    protected void onOpen() {

    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        task.cancel(false);
        newBingGoGoClientWebSocket.close();
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        newBingGoGoClientWebSocket.send(message.getTextPayload());
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {

    }

    @Override
    protected void onException(IOException exception) {
        task.cancel(false);
        newBingGoGoClientWebSocket.close();
    }
}
