package com.teamEWSN.gitdeun.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.jwt.*;
import com.teamEWSN.gitdeun.common.jwt.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

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
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers((headerConfig) -> headerConfig
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

    // 경로별 인가 작업
    http
        .authorizeHttpRequests((auth) -> auth
            .requestMatchers(SecurityPath.ADMIN_ENDPOINTS).hasRole("ADMIN")
            .requestMatchers(SecurityPath.USER_ENDPOINTS).hasAnyRole("USER", "ADMIN")
            .requestMatchers(SecurityPath.PUBLIC_ENDPOINTS).permitAll()
            .anyRequest().permitAll()
            // .anyRequest().authenticated()
        );

    // 예외 처리
    http
        .exceptionHandling(exceptionHandling -> exceptionHandling
            .authenticationEntryPoint(customAuthenticationEntryPoint) // 인증 실패 처리
            .accessDeniedHandler(customAccessDeniedHandler)); // 인가 실패 처리

    // JwtFilter 추가
    http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, objectMapper), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // CORS 설정을 위한 Bean 등록
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = getCorsConfiguration();
    configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
    configuration.setExposedHeaders(java.util.List.of("Authorization"));
    configuration.setAllowCredentials(true); // 인증 정보 허용 (쿠키 등)

    org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 적용
    return source;
  }


  private static CorsConfiguration getCorsConfiguration() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOrigin("http://localhost:3000"); // 개발 환경
    configuration.addAllowedOrigin("https://gitdeun.netlify.app");
    configuration.addAllowedOrigin("https://gitdeun.site"); // 혜택온 도메인
    configuration.addAllowedOrigin("https://www.gitdeun.site");
    configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
    configuration.addAllowedHeader("*"); // 모든 헤더 허용
    return configuration;
  }

}