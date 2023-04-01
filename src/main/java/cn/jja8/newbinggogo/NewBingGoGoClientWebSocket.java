package cn.jja8.newbinggogo;

import fi.iki.elonen.NanoWSD;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;

public class NewBingGoGoClientWebSocket extends WebSocketClient {
    NewBingGoGoServerWebSocket newBingGoGoServerWebSocket;
    public NewBingGoGoClientWebSocket(URI serverUri,NewBingGoGoServerWebSocket newBingGoGoServerWebSocket) {
        super(serverUri);
        this.newBingGoGoServerWebSocket = newBingGoGoServerWebSocket;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {
        try {
            newBingGoGoServerWebSocket.send(message);
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        NanoWSD.WebSocketFrame.CloseCode rcode = NanoWSD.WebSocketFrame.CloseCode.find(code);
        if(rcode==null){
            rcode = NanoWSD.WebSocketFrame.CloseCode.NormalClosure;
        }
        try {
            newBingGoGoServerWebSocket.close(rcode,reason,false);
        } catch (IOException e) {

        }
    }

    @Override
    public void onError(Exception ex) {
        close();
        try {
            String errorMessage = "{\"type\": 2,\"result\":{\"value\":\"Error\",\"message\":\"魔法服务器连接到bing聊天时发生错误"+ex+"\"}}";
            newBingGoGoServerWebSocket.send(errorMessage);
            newBingGoGoServerWebSocket.close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure,"error",false);
        } catch (IOException e) {

        }

    }
}
