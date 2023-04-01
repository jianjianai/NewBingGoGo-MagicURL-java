package cn.jja8.newbinggogo;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

public class NewBingGoGoServerWebSocket extends NanoWSD.WebSocket {
    NewBingGoGoClientWebSocket newBingGoGoClientWebSocket;
    LinkedList<String> messList = new LinkedList<>();
    public NewBingGoGoServerWebSocket(NanoHTTPD.IHTTPSession handshakeRequest) {
        super(handshakeRequest);
        URI url;
        try {
            url = new URI("wss://sydney.bing.com/sydney/ChatHub");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);//这个异常这辈子都不会出的
        }

        newBingGoGoClientWebSocket = new NewBingGoGoClientWebSocket(url,this,messList);
    }

    @Override
    protected void onOpen() {
        newBingGoGoClientWebSocket.connect();
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        newBingGoGoClientWebSocket.close();
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        if(newBingGoGoClientWebSocket.isOpen()){
            newBingGoGoClientWebSocket.send(message.getTextPayload());
        }else {
            messList.addLast(message.getTextPayload());
        }
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {
        newBingGoGoClientWebSocket.sendPing();
    }

    @Override
    protected void onException(IOException exception) {
        newBingGoGoClientWebSocket.close();
    }
}
