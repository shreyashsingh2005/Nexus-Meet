package Nexus_Meet.com;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalingSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    // Naya map jo track karega ki kis room ka Host kaun hai
    private final Map<String, String> roomHosts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String roomId = (String) payload.get("roomId");

        if (roomId == null) return;

        rooms.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        roomSessions.add(session);
        session.getAttributes().put("roomId", roomId);

        payload.put("sender", session.getId());
        String jsonMessage = objectMapper.writeValueAsString(payload);
        String type = (String) payload.get("type");

        // 1. Join Logic with Host Assignment
        if ("join".equals(type)) {
            boolean isHost = false;
            // Agar room mein koi host nahi hai, toh is user ko host bana do
            if (!roomHosts.containsKey(roomId)) {
                roomHosts.put(roomId, session.getId());
                isHost = true;
            }

            // User ko batao ki wo Host hai ya nahi
            Map<String, Object> ackMsg = new HashMap<>();
            ackMsg.put("type", "join_ack");
            ackMsg.put("isHost", isHost);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ackMsg)));

            // Baaki sabko join ka message bhejo
            for (WebSocketSession peer : roomSessions) {
                if (!peer.getId().equals(session.getId())) {
                    peer.sendMessage(new TextMessage(jsonMessage));
                }
            }
            return;
        }

        // 2. Mute All Logic (Sirf Host kar sakta hai)
        if ("mute_all".equals(type)) {
            if (session.getId().equals(roomHosts.get(roomId))) {
                Map<String, Object> muteMsg = new HashMap<>();
                muteMsg.put("type", "mute_force");
                String muteJson = objectMapper.writeValueAsString(muteMsg);
                for (WebSocketSession peer : roomSessions) {
                    if (!peer.getId().equals(session.getId())) {
                        peer.sendMessage(new TextMessage(muteJson));
                    }
                }
            }
            return;
        }

        // 3. Chat Messages
        if ("chat".equals(type)) {
            for (WebSocketSession peer : roomSessions) {
                if (!peer.getId().equals(session.getId())) {
                    peer.sendMessage(new TextMessage(jsonMessage));
                }
            }
            return;
        }

        // 4. Standard WebRTC Signaling
        String targetId = (String) payload.get("target");
        if (targetId != null) {
            for (WebSocketSession peer : roomSessions) {
                if (peer.getId().equals(targetId)) {
                    peer.sendMessage(new TextMessage(jsonMessage));
                    break;
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId != null) {
            Set<WebSocketSession> roomSessions = rooms.get(roomId);
            if (roomSessions != null) {
                roomSessions.remove(session);
                
                // Agar host ne leave kiya, toh agle bande ko host bana do ya room khali kar do
                if (session.getId().equals(roomHosts.get(roomId))) {
                    if (!roomSessions.isEmpty()) {
                        roomHosts.put(roomId, roomSessions.iterator().next().getId());
                    } else {
                        roomHosts.remove(roomId);
                    }
                }

                Map<String, Object> leaveMsg = new HashMap<>();
                leaveMsg.put("type", "leave");
                leaveMsg.put("sender", session.getId());
                String leaveJson = objectMapper.writeValueAsString(leaveMsg);

                for (WebSocketSession peer : roomSessions) {
                    try {
                        peer.sendMessage(new TextMessage(leaveJson));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (roomSessions.isEmpty()) {
                    rooms.remove(roomId);
                }
            }
        }
    }
}