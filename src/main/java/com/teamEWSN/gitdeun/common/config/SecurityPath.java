package com.teamEWSN.gitdeun.common.config;


public class SecurityPath {

  // permitAll
  public static final String[] PUBLIC_ENDPOINTS = {
      "/api/token/refresh",
      "/api/auth/oauth/refresh/*",
      "/",

  };

  // hasRole("USER")
  public static final String[] USER_ENDPOINTS = {
      "/api/auth/connect/github/state",
      "/api/users/me",
      "/api/users/me/**",
      "/api/logout",
      "/api/repos/**"
  };

  // hasRole("ADMIN")
  public static final String[] ADMIN_ENDPOINTS = {
      "/api/admin/**"
  };
}

