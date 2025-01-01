package personal_project.moment_talk.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import personal_project.moment_talk.common.webSocket.WebSocketSessionManager;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final QueueService queueService;
    private final Map<String, String> activeMatches = new HashMap<>();
    private final WebSocketSessionManager webSocketSessionManager;

    public synchronized String attemptChatMatch(String httpSessionId) {
        log.info("Trying to match for session: {}", httpSessionId);

        String opponentHttpSessionId;
        while (true) {
            // 대기열에서 httpSessionId pop()
            opponentHttpSessionId = queueService.getFromQueue();

            // 대기열에 사람이 없으면 대기열에 해당 유저의 httpSessionId 추가 후 대기
            if (opponentHttpSessionId == null) {
                log.info("Queue is empty. Adding back to queue: {}", httpSessionId);
                queueService.addToQueue(httpSessionId);
                return null;
            }

            if (!opponentHttpSessionId.equals(httpSessionId) && !activeMatches.containsKey(opponentHttpSessionId)) {
                break;
            }
            log.info("Skipping invalid opponent: {}", opponentHttpSessionId);
        }

        // 매칭 성공
        activeMatches.put(httpSessionId, opponentHttpSessionId);
        activeMatches.put(opponentHttpSessionId, httpSessionId);
        log.info("Matched: {} with {}", httpSessionId, opponentHttpSessionId);

        return opponentHttpSessionId;
    }


    /*
    activeMatches 에서 사용자의 sessionId 를 이용해 상대방의 sessionId GET
     */
    public String getOpponentHttpSessionId(String httpSessionId) {
        return activeMatches.get(httpSessionId);
    }

    public void removeMatch(String httpSessionId) {
        String opponentHttpSessionId = activeMatches.remove(httpSessionId);
        if (opponentHttpSessionId != null) {
            activeMatches.remove(opponentHttpSessionId);
        }
        log.info("Removed match for session: {} and opponent: {}", httpSessionId, opponentHttpSessionId);
    }
}
