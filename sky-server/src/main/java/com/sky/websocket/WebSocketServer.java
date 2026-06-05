package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    private static Map<String, Session> sessionMap = new HashMap();

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("WebSocket客户端连接成功，sid: {}", sid);
        sessionMap.put(sid, session);
        log.info("当前WebSocket连接数: {}", sessionMap.size());
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到来自客户端 {} 的消息: {}", sid, message);
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("WebSocket客户端断开连接，sid: {}", sid);
        sessionMap.remove(sid);
        log.info("当前WebSocket连接数: {}", sessionMap.size());
    }

    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        log.info("开始群发消息，当前连接数: {}, 消息内容: {}", sessions.size(), message);

        if (sessions.isEmpty()) {
            log.warn("⚠️ 没有WebSocket客户端连接，消息未发送！");
            return;
        }

        int successCount = 0;
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendText(message);
                successCount++;
                log.info("消息发送成功，sid: {}", session.getId());
            } catch (Exception e) {
                log.error("消息发送失败，sid: {}", session.getId(), e);
            }
        }
        log.info("群发完成，成功: {}/{}", successCount, sessions.size());
    }

}
