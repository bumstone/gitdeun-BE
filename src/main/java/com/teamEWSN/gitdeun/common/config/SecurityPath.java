package com.teamEWSN.gitdeun.common.config;


public class SecurityPath {

  // permitAll
  public static final String[] PUBLIC_ENDPOINTS = {
      "/api/auth/token/refresh",
      "/api/auth/oauth/refresh/*",
      "/oauth/logout",
      "/",

      "/api/webhook/**",
      "/actuator/health",
      "/actuator/health/**",
      "/actuator/info"
  };

  // hasRole("USER")
  public static final String[] USER_ENDPOINTS = {
      "/api/auth/connect/github/state",
      "/api/auth/logout",
      "/api/auth/social",
      "/api/users/me",
      "/api/users/me/**",
      "/api/repos",
      "/api/repos/**",
      "/api/mindmaps/**",
      "/api/history/**",
      "/api/invitations/**",
      "/api/notifications/**",
      "/api/skills/**",
      "/api/recruitments/**",
      "/api/applications/**",
      "/api/comments/**",
      "/api/code-reviews/**",
      "/api/references/**",
      "/api/s3/bucket/**",
  };

  // hasRole("ADMIN")
  public static final String[] ADMIN_ENDPOINTS = {
      "/api/admin/**"
  };
}

