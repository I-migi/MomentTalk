package personal_project.moment_talk.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import personal_project.moment_talk.common.webSocket.ChatWebSocketHandler;
import personal_project.moment_talk.common.webSocket.WebSocketSessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final QueueService queueService;
    // activeMatches -> Chat 중인 쌍
    private final Map<String, String> activeMatches = new HashMap<>();
    private final WebSocketSessionManager webSocketSessionManager;
    /*
    attemptChatMatch -> 사용자 세션 ID 를 대기열에서 매칭할 상대방과 연결
    상대방을 찾을 수 없으면 대기열에 다시 사용자를 추가하고 null 반환

    1. opponentSessionId -> 대기열의 가장 오른쪽에서 GET
    2. null 이면 대기열에 본인 sessionId 추가 + return null
    3. opponentSessionId 가 본인의 sessionId 와 다르고 && activeMatches 에서 activeMatches 를 포함하고 있지 않으면 -> break

    4. 매칭이 성공됐으면 activeMatches 에 put 본인 sessionId 키, 상대 sessionId value, 상대 sessionId 키, 본인 sessionId value
    5. 상대 sessionId return

    -> 매칭된 상대방의 세션 ID return , 매칭 상대가 없으면 null return

    메서드 시작 부분에서 사용자의 sessionId 를 대기열에 넣지 않는 이유
    -> attemptChatMatch 를 실행했을 때 대기열에 사용자가 있다면 굳이 사용자를 대기열에 넣지 않고
    바로 Chat 을 시작할 수 있기 때문.

    */
    public synchronized String attemptChatMatch(String sessionId) {
        log.info("Trying to match for session: {}", sessionId);

        String opponentSessionId;
        while (true) {
            opponentSessionId = queueService.getFromQueue();

            if (opponentSessionId == null) {
                log.info("Queue is empty. Adding back to queue: {}", sessionId);
                queueService.addToQueue(sessionId);
                return null;
            }

            if (!webSocketSessionManager.isSessionValid(opponentSessionId)) {
                log.info("Invalid or closed session found in queue: {}", opponentSessionId);
                queueService.removeFromQueue(opponentSessionId);
                continue;
            }

            if (!opponentSessionId.equals(sessionId) && !activeMatches.containsKey(opponentSessionId)) {
                break;
            }
            log.info("Skipping invalid opponent: {}", opponentSessionId);
        }

        // 매칭 성공
        activeMatches.put(sessionId, opponentSessionId);
        activeMatches.put(opponentSessionId, sessionId);
        log.info("Matched: {} with {}", sessionId, opponentSessionId);

        return opponentSessionId;
    }


    /*
    activeMatches 에서 사용자의 sessionId 를 이용해 상대방의 sessionId GET
     */
    public String getOpponentUser(String sessionId) {
        return activeMatches.get(sessionId);
    }

    /*
    1. activeMatches 에서 특정 사용자의 세션 ID 를 제거하고, 상대방 세션 ID GET
    2. 상대방 세션 ID 가 null 이 아니면 상대방 세션 ID 도 똑같이 제거
     */
    public void removeMatch(String sessionId) {
        String opponentUser = activeMatches.remove(sessionId);
        if (opponentUser != null) {
            activeMatches.remove(opponentUser);
        }
        log.info("Removed match for session: {} and opponent: {}", sessionId, opponentUser);
    }
}
