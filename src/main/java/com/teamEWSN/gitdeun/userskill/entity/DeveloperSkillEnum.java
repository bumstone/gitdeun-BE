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
    R("R", "LANGUAGE"),
    OTHER("기타", "LANGUAGE");

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
//    FLUTTER("Flutter", "FRAMEWORK"),
//    SINATRA("sinatra", "FRAMEWORK"),
//    RAILS("Rails", "FRAMEWORK"),
//    SWIFTUI("SwiftUI", "FRAMEWORK"),
//    LARAVEL("Laravel", "FRAMEWORK"),
//    GIN("Gin", "FRAMEWORK"),
//    OTHER("기타", "FRAMEWORK");

    private final String displayName;
    private final String category;


}