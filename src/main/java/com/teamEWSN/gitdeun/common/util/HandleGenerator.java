package com.teamEWSN.gitdeun.common.util;

import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class HandleGenerator {
    private final UserRepository userRepository;

    private static final int MAX_LEN = 30;
    private static final int MAX_SEQ = 999;
    private static final int MAX_RETRY = 3;

    /**
     * base를 받아 전역 유니크 핸들을 생성합니다.
     * @param base 후보 문자열 (null 가능)
     * @param selfId 본인 사용자 ID (신규 생성 시 null)
     */
    public String generateUniqueHandle(String base, Long selfId) {
        String normalized = sanitize(base);
        String candidate = normalized;
        int seq = 0;

        while (conflicts(candidate, selfId)) {
            seq++;
            if (seq <= MAX_SEQ) {
                candidate = appendSeq(normalized, seq);
            } else {
                candidate = appendRandom(normalized);
                // 랜덤도 충돌 가능하므로 계속 검사
            }
        }
        return candidate;
    }

    /**
     * UNIQUE 위반 발생 시 재시도하는 안전 래퍼
     */
    public <T> T withUniqueRetry(HandleSetter<T> action, String base, Long selfId) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            String handle = generateUniqueHandle(base, selfId);
            try {
                return action.apply(handle);
            } catch (DataIntegrityViolationException e) {
                if (attempt == MAX_RETRY - 1) {
                    throw new IllegalStateException(
                        "Failed to generate unique handle after " + MAX_RETRY + " attempts", e
                    );
                }
            }
        }
        throw new IllegalStateException("Should not reach here");
    }

    private boolean conflicts(String handle, Long selfId) {
        return (selfId == null)
            ? userRepository.existsByHandle(handle)
            : userRepository.existsByHandleAndIdNot(handle, selfId);
    }

    private String sanitize(String raw) {
        String s = (raw == null ? "" : raw).trim().toLowerCase();
        s = s.replaceAll("\\s+", "_");
        s = s.replaceAll("[^a-z0-9_.-]", "");
        s = s.replaceAll("_+", "_");
        if (s.isBlank()) s = "user";
        return s.length() > MAX_LEN ? s.substring(0, MAX_LEN) : s;
    }

    private String appendSeq(String base, int seq) {
        String suffix = "_" + String.format("%03d", seq);
        int room = MAX_LEN - suffix.length();
        String head = base.length() > room ? base.substring(0, room) : base;
        return head + suffix;
    }

    private String appendRandom(String base) {
        String rand = random6();
        String suffix = "_" + rand;
        int room = MAX_LEN - suffix.length();
        String head = base.length() > room ? base.substring(0, room) : base;
        return head + suffix;
    }

    private String random6() {
        long random = ThreadLocalRandom.current().nextLong() & 0xFFFFFFFFL;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteBuffer.allocate(4).putInt((int)random).array());
    }

    @FunctionalInterface
    public interface HandleSetter<T> {
        T apply(String uniqueHandle);
    }
}
