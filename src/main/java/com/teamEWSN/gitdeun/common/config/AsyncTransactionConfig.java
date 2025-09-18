package com.teamEWSN.gitdeun.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 및 트랜잭션 설정
 *
 * 마치 대형 공장의 생산라인을 설계하는 것과 같습니다.
 * - ThreadPool: 작업자들의 팀 (적절한 인원수로 효율성 극대화)
 * - Transaction: 품질 검사 체크포인트 (문제 발생시 이전 단계로 롤백)
 * - Exception Handler: 비상 대응팀 (예외 상황 발생시 적절한 조치)
 */
@Slf4j
@Configuration
@EnableAsync
@EnableTransactionManagement
public class AsyncTransactionConfig implements AsyncConfigurer {

    @Value("${app.async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${app.async.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${app.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${app.async.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * 마인드맵 전용 비동기 실행자
     * 특징:
     * - 코어 풀 크기: 10개 (항상 활성 상태의 스레드)
     * - 최대 풀 크기: 50개 (피크 시간 대응)
     * - 큐 용량: 100개 (대기 중인 작업 수)
     * - 거부 정책: CallerRuns (호출자 스레드에서 직접 실행)
     */
    @Bean(name = "mindmapExecutor")
    public Executor mindmapTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("Mindmap-Async-");

        // 거부 정책: 큐가 가득 찰 때 호출자 스레드에서 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 스레드 풀 초기화 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("마인드맵 비동기 실행자 초기화 완료 - 코어: {}, 최대: {}, 큐: {}",
            corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    /**
     * 일반적인 비동기 작업용 실행자
     *
     * 알림 전송, 로그 처리 등 가벼운 작업에 사용
     */
    @Bean(name = "generalExecutor")
    public Executor generalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("General-Async-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        executor.initialize();

        log.info("일반 비동기 실행자 초기화 완료");
        return executor;
    }

    /**
     * 기본 비동기 실행자 (AsyncConfigurer 인터페이스 구현)
     */
    @Override
    public Executor getAsyncExecutor() {
        return mindmapTaskExecutor();
    }

    /**
     * 비동기 예외 처리기
     *
     * 비유: 공장의 안전 관리자
     * 작업 중 예외가 발생하면 즉시 파악하고 기록하여
     * 향후 개선점을 도출할 수 있도록 합니다.
     *
     * 실제 예시:
     * GitHub API 호출 실패, 메모리 부족, 네트워크 타임아웃 등
     * 다양한 예외 상황을 적절히 로깅하고 모니터링합니다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    /**
     * 커스텀 비동기 예외 처리기
     */
    private static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

            log.error("비동기 작업 예외 발생 - 메서드: {}, 매개변수: {}",
                methodName, formatParams(params), ex);

            // 중요한 예외의 경우 추가 알림 처리
            if (isCriticalException(ex)) {
                handleCriticalException(ex, methodName, params);
            }
        }

        private String formatParams(Object... params) {
            if (params == null || params.length == 0) {
                return "없음";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");

                Object param = params[i];
                if (param == null) {
                    sb.append("null");
                } else {
                    // 민감 정보 마스킹
                    String paramStr = param.toString();
                    if (paramStr.contains("password") || paramStr.contains("token")) {
                        sb.append("[MASKED]");
                    } else {
                        sb.append(paramStr.length() > 100 ?
                            paramStr.substring(0, 100) + "..." : paramStr);
                    }
                }
            }
            return sb.toString();
        }

        private boolean isCriticalException(Throwable ex) {
            return ex instanceof OutOfMemoryError ||
                ex instanceof StackOverflowError ||
                (ex.getMessage() != null && ex.getMessage().contains("database"));
        }

        private void handleCriticalException(Throwable ex, String methodName, Object... params) {
            // 치명적 문제 발생 알림
            log.error("⚠️ 치명적 예외 발생 - 즉시 대응 필요: {} in {}", ex.getMessage(), methodName);


            // TODO: 모니터링 시스템 연동
        }
    }
}
