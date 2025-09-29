package com.teamEWSN.gitdeun.mindmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptPreviewResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapSseService {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private record SseConnection(Long userId, String nickname, String profileImage, SseEmitter emitter) {}
    private final Map<Long, List<SseConnection>> connectionsByMapId = new ConcurrentHashMap<>();

    // 타임아웃 설정(1시간)
    private static final long TIMEOUT_MS = 60L * 60L * 1000L;

    /**
     * 마인드맵 실시간 연결 생성
     */
    public SseEmitter createConnection(Long mapId, CustomUserDetails userDetails) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // 사용자 정보와 Emitter를 함께 SseConnection 객체로 감싸서 저장
        var connection = new SseConnection(
            userDetails.getId(),
            userDetails.getNickname(),
            userDetails.getProfileImage(),
            emitter
        );

        connectionsByMapId.computeIfAbsent(mapId, k -> new CopyOnWriteArrayList<>()).add(connection);

        try {
            // Redis Set에 현재 접속자 정보 추가
            String redisKey = "mindmap:" + mapId + ":users";
            String userData = objectMapper.writeValueAsString(
                new ConnectedUserDto(userDetails.getId(), userDetails.getNickname(), userDetails.getProfileImage())
            );
            redisTemplate.opsForSet().add(redisKey, userData);

            // Redis Set의 만료 시간을 설정하여 비정상 종료된 연결 처리
            redisTemplate.expire(redisKey, 1, TimeUnit.HOURS);

        } catch (JsonProcessingException e) {
            log.error("사용자 정보 직렬화 실패", e);
        }

        // 연결 종료 시 정리 (기존 로직 개선)
        emitter.onCompletion(() -> removeConnection(mapId, connection));
        emitter.onTimeout(() -> removeConnection(mapId, connection));
        emitter.onError(throwable -> {
            log.error("SSE 연결 오류 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userDetails.getId(), throwable);
            removeConnection(mapId, connection);
        });

        // 연결 확인용 초기 메시지
        sendToEmitter(emitter, "마인드맵 " + mapId + " 실시간 연결 성공");
        log.info("SSE 연결 생성 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userDetails.getId());

        broadcastUserListUpdate(mapId);
        return emitter;
    }

    /**
     * 마인드맵 업데이트 브로드캐스트
     */
    public void broadcastUpdate(Long mapId, MindmapDetailResponseDto data) {
        sendToMapSubscribers(mapId, "mindmap-update", data);
    }

    /**
     * 새로운 프롬프트 미리보기 준비 완료 브로드캐스트
     */
    public void broadcastPromptReady(Long mapId, PromptPreviewResponseDto data) {
        sendToMapSubscribers(mapId, "prompt-ready", data);
    }


    /**
     * 프롬프트 적용 브로드캐스트
     */
    public void broadcastPromptApplied(Long mapId, Long historyId) {
        Map<String, Object> eventData = Map.of(
            "type", "prompt_applied",
            "historyId", historyId,
            "message", "새로운 프롬프트가 적용되었습니다."
        );
        sendToMapSubscribers(mapId, "prompt-applied", eventData);
    }

    /**
     * 제목 변경 브로드캐스트
     */
    public void broadcastTitleChanged(Long mapId, String newTitle) {
        Map<String, Object> eventData = Map.of(
            "type", "title_changed",
            "newTitle", newTitle,
            "message", "마인드맵 제목이 변경되었습니다."
        );
        sendToMapSubscribers(mapId, "title-changed", eventData);
    }

    /**
     * 특정 마인드맵의 모든 구독자에게 이벤트 전송
     */
    private void sendToMapSubscribers(Long mapId, String eventName, Object data) {
        List<SseConnection> connections = connectionsByMapId.get(mapId);
        if (connections == null || connections.isEmpty()) {
            log.debug("마인드맵 ID {} 에 연결된 클라이언트가 없음", mapId);
            return;
        }

        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("SSE 데이터 직렬화 실패 - 마인드맵 ID: {}", mapId, e);
            return;
        }

        List<SseConnection> deadConnections = new CopyOnWriteArrayList<>();

        connections.forEach(connection -> {
            try {
                connection.emitter().send(SseEmitter.event().name(eventName).data(jsonData));
            } catch (IOException e) {
                deadConnections.add(connection);
                log.warn("SSE 전송 실패 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, connection.userId(), e);
            }
        });

        // 실패한 연결 정리
        deadConnections.forEach(connection -> removeConnection(mapId, connection));

        log.debug("SSE 브로드캐스트 완료 - 마인드맵 ID: {}, 이벤트: {}, 성공: {}, 실패: {}",
            mapId, eventName, connections.size() - deadConnections.size(), deadConnections.size());
    }

    /**
     * 개별 emitter에게 초기 메시지 전송
     */
    private void sendToEmitter(SseEmitter emitter, Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name("connected").data(jsonData));
        } catch (IOException e) {
            log.warn("초기 SSE 메시지 전송 실패", e);
        }
    }

    // 연결 종료 시 SseConnection 객체를 찾아 제거
    private void removeConnection(Long mapId, SseConnection connection) {
        List<SseConnection> connections = connectionsByMapId.get(mapId);
        if (connections != null && connections.remove(connection)) {
            try {
                // Redis Set에서 접속 종료한 사용자 정보 제거
                String redisKey = "mindmap:" + mapId + ":users";
                String userData = objectMapper.writeValueAsString(
                    new ConnectedUserDto(connection.userId(), connection.nickname(), connection.profileImage())
                );
                redisTemplate.opsForSet().remove(redisKey, userData);

            } catch (JsonProcessingException e) {
                log.error("사용자 정보 직렬화 실패 (연결 종료)", e);
            }
            broadcastUserListUpdate(mapId);
            log.info("SSE 연결 해제 및 Redis 업데이트 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, connection.userId());
        }
    }

    // 사용자 목록 변경을 모든 클라이언트에게 브로드캐스트
    public void broadcastUserListUpdate(Long mapId) {
        // 현재 접속자 목록
        List<ConnectedUserDto> connectedUsers = getConnectedUsers(mapId);
        // "user-list-updated" 라는 이벤트 이름으로 현재 접속자 목록을 전송
        sendToMapSubscribers(mapId, "user-list-updated", connectedUsers);
    }

    // 접속된 사용자 수 조회
    public int getConnectionCount(Long mapId) {
        List<SseConnection> connections = connectionsByMapId.get(mapId);
        return connections != null ? connections.size() : 0;
    }

    // 접속자 정보 DTO
    public record ConnectedUserDto(Long userId, String nickname, String profileImage) {}

    // 현재 접속 중인 사용자 목록을 반환하는 서비스 메서드
    public List<ConnectedUserDto> getConnectedUsers(Long mapId) {
        String redisKey = "mindmap:" + mapId + ":users";
        Set<String> usersJson = redisTemplate.opsForSet().members(redisKey);

        if (usersJson == null) {
            return Collections.emptyList();
        }

        return usersJson.stream()
            .map(json -> {
                try {
                    return objectMapper.readValue(json, ConnectedUserDto.class);
                } catch (JsonProcessingException e) {
                    log.error("사용자 정보 역직렬화 실패", e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}