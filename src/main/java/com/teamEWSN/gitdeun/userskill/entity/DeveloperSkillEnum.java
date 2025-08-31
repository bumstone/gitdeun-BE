package com.teamEWSN.gitdeun.userskill.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeveloperSkillEnum {
    // 프로그래밍 언어
    JAVASCRIPT("JavaScript", "LANGUAGE"),
    TYPESCRIPT("TypeScript", "LANGUAGE"),
    PYTHON("Python", "LANGUAGE"),
    JAVA("Java", "LANGUAGE"),
    KOTLIN("Kotlin", "LANGUAGE"),
    GO("Go", "LANGUAGE"),
    RUST("Rust", "LANGUAGE"),
    CPP("C++", "LANGUAGE"),
    CSHARP("C#", "LANGUAGE"),
    SWIFT("Swift", "LANGUAGE"),
    DART("Dart", "LANGUAGE"),
    PHP("PHP", "LANGUAGE"),
    RUBY("Ruby", "LANGUAGE"),
    R("R", "LANGUAGE");

//    // 프레임워크/라이브러리
//    SPRING("Spring", "FRAMEWORK"),
//    REACT("React", "FRAMEWORK"),
//    VUE("Vue.js", "FRAMEWORK"),
//    ANGULAR("Angular", "FRAMEWORK"),
//    NEXTJS("Next.js", "FRAMEWORK"),
//    EXPRESS("Express.js", "FRAMEWORK"),
//    NESTJS("NestJS", "FRAMEWORK"),
//    DJANGO("Django", "FRAMEWORK"),
//    FLASK("Flask", "FRAMEWORK"),
//    FASTAPI("FastAPI", "FRAMEWORK"),
//
//    // 데이터베이스
//    MYSQL("MySQL", "DATABASE"),
//    POSTGRESQL("PostgreSQL", "DATABASE"),
//    MONGODB("MongoDB", "DATABASE"),
//    REDIS("Redis", "DATABASE"),
//    ORACLE("Oracle", "DATABASE"),
//
//    // 클라우드/인프라
//    AWS("AWS", "CLOUD"),
//    GCP("Google Cloud", "CLOUD"),
//    AZURE("Azure", "CLOUD"),
//    DOCKER("Docker", "DEVOPS"),
//    KUBERNETES("Kubernetes", "DEVOPS"),
//
//    // 개발 분야
//    BACKEND("백엔드", "FIELD"),
//    FRONTEND("프론트엔드", "FIELD"),
//    FULLSTACK("풀스택", "FIELD"),
//    MOBILE("모바일", "FIELD"),
//    AI_ML("AI/ML", "FIELD"),
//    DATA_ENGINEERING("데이터 엔지니어링", "FIELD"),
//    DEVOPS_FIELD("DevOps", "FIELD"),
//    GAME_DEV("게임 개발", "FIELD");

    private final String displayName;
    private final String category;


}