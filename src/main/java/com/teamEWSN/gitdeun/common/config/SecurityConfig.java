package com.teamEWSN.gitdeun.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.jwt.*;
import com.teamEWSN.gitdeun.common.oauth.handler.CustomOAuth2FailureHandler;
import com.teamEWSN.gitdeun.common.oauth.handler.CustomOAuth2SuccessHandler;
import com.teamEWSN.gitdeun.common.oauth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // ★
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List; // ★

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private static final String SSE_ENDPOINT = "/api/history/sse"; // ★

  private final CustomOAuth2UserService customOAuth2UserService;
  private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
  private final CustomOAuth2FailureHandler customOAuthFailureHandler;
  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // SSE는 서버가 세션을 유지하지 않아도 되지만, 앱 전체 정책 유지
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .headers((headerConfig) -> headerConfig.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

    // OAuth2 로그인
    http
            .oauth2Login((oauth2) -> oauth2
                    .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userService(customOAuth2UserService))
                    .successHandler(customOAuth2SuccessHandler)
                    .failureHandler(customOAuthFailureHandler))
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/oauth/logout")
                    .clearAuthentication(true)
                    .deleteCookies("refreshToken")
            );

    // 경로별 인가
    http
            .authorizeHttpRequests((auth) -> auth
                    // ★ 프리플라이트(OPTIONS) 전부 허용
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // ★ SSE 엔드포인트 허용 (로그 없이도 브라우저에서 직접 연결 가능해야 함)
                    .requestMatchers(HttpMethod.GET, SSE_ENDPOINT).permitAll()
                    // 내부 webhook
                    .requestMatchers("/api/webhook/**").permitAll()
                    // 그 외 정책
                    .requestMatchers(SecurityPath.ADMIN_ENDPOINTS).hasRole("ADMIN")
                    .requestMatchers(SecurityPath.USER_ENDPOINTS).hasAnyRole("USER", "ADMIN")
                    .requestMatchers(SecurityPath.PUBLIC_ENDPOINTS).permitAll()
                    .anyRequest().permitAll()
            );

    // 예외 처리
    http
            .exceptionHandling(exceptionHandling -> exceptionHandling
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler));

    // JWT 필터
    http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, objectMapper),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // CORS 설정
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = getCorsConfiguration();

    // ★ SSE/프리플라이트에서 필요한 헤더들 보강
    configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Cache-Control",
            "X-Requested-With",
            "Last-Event-ID" // SSE 재연결용
    ));
    configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Type"
    ));
    configuration.setAllowCredentials(true);

    org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
            new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private static CorsConfiguration getCorsConfiguration() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOrigin("http://localhost:5173"); // 개발
    configuration.addAllowedOrigin("https://gitdeun.netlify.app");
    configuration.addAllowedOrigin("https://gitdeun.site");
    configuration.addAllowedOrigin("https://www.gitdeun.site");
    configuration.addAllowedMethod("*");   // 모든 메서드 허용 (GET/POST/PUT/DELETE/OPTIONS)
    configuration.addAllowedHeader("*");   // 헤더 전부 허용 (위에서 setAllowedHeaders로 다시 구체화)
    return configuration;
  }
}
