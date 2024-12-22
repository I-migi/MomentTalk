package personal_project.moment_talk.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String QUEUE_KEY = "random_chat_queue";

    /*
    addToQueue -> 주어진 sessionId 를 Redis 의 큐(리스트)에 추가

    1. QUEUE_Key 의 값을 가진 큐에 해당 sessionId 가 존재하는지 확인
    2. 존재하지 않으면 큐의 왼쪽에 데이터 추가
    3. 이미 큐에 있다면 "Session already in queue" log 출력

    synchronized -> 멀테스레드 환경에서 동기화하여 데이터 일관성을 보장
    멀티 스레드 -> 여러 스레드가 동시에 동일한 메서드나 데이터에 접근 가능

    addToQueue 메서드에서는 다수의 스레드가 모두 고유한 sessionId 를 가지고 있다면 synchronized 가 필요하지 않을 수 있음.

    그러나 예외 존재 ->
    isInQueue() 는 Redis 리스트의 전체 상태 조회
    1. 스레드 A가 Redis 리스트의 상태를 조회하는 동안, 스레드 B가 리스트에 값 추가 가능
    2. 결과적으로 스레드 A의 조회 결과와 Redis의 실제 상태가 불일치 가능


    의문점  ->
    A스레드가 addToQueue 메서드를 실행하고 있으면 B스레드가 addToQueue 메서드
    뿐만이 아니라 isInQueue 스레드도 사용 못하는거야?
    이게 아니면 이런 동시성 문제가 또 터질 수 있는거 아니야? 다른 메서드에서

    -> 현재의 synchronized 는 메서드 수준으로, 해당 메서드가 호출될 때 객체 단위로 락
    -> 락은 클래스의 인스턴스에 적용

    QueueService 의 여러 인스턴스가 각각 만들어져 있으면 -> 각 인스턴스는 독립적으로 락 관리
    -> 서로 다른 인스턴스가 Redis 에 동시에 접근하면서 동시성 문제 발생 가능

    -->> 분산 락을 사용해야 한다!! ✏️

    -> 하지만 현재 QueueService 는 @Service 어노테이션이 적용 -> 싱글톤으로 관리 -> 단일 인스턴스이므로 괜찮다


     */
    public synchronized void addToQueue(String sessionId) {
        if (!isInQueue(sessionId)) {
            redisTemplate.opsForList().leftPush(QUEUE_KEY, sessionId);
            log.info("Session added to queue: {}", sessionId);
        } else {
            log.info("Session already in queue: {}", sessionId);
        }
    }

    /*
    getFromQueue -> Redis 큐의 가장 오른쪽에 있는 값 POP(제거하고 반환)

    synchronized 의 필요성
    -> 여러 스레드가 동시에 큐에서 값을 가져가려고 하면 데이터의 충돌 발생 가능성 유
    1. 스레드 A와 B가 동시에 rightPop() 호출
    2. 둘 다 큐에서 동일한 값을 가져가려고 시도
    3. 같은 값이 두 번 처리되거나 값이 없는 상태에서 처리
    -> synchronized 무조건 필요!
     */
    public synchronized String getFromQueue() {
        String sessionId = redisTemplate.opsForList().rightPop(QUEUE_KEY);
        if (sessionId != null) {
            log.info("Popped from queue: {}", sessionId);
        } else {
            log.info("Queue is empty.");
        }
        return sessionId;
    }

    /*
    파라미터의 sessionId 가 Redis 의 큐에 존재하는지 확인
    Redis 큐(리스트)에서 0번 인덱스부터 -1번 인덱스까지의 모든 값을 GET
     */
    public boolean isInQueue(String sessionId) {
        return redisTemplate.opsForList().range(QUEUE_KEY, 0, -1).contains(sessionId);
    }

    /*
    Redis 리스트에서 특정 sessionId 제거
     */
    public void removeFromQueue(String sessionId) {
        redisTemplate.opsForList().remove(QUEUE_KEY, 1, sessionId);
        log.info("Removed from queue: {}", sessionId);
    }



}
