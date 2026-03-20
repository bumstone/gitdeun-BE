# Gitdeun(깃든)
![Gitdeun](https://github.com/user-attachments/assets/b8bbd96b-4467-4e32-b203-209530a0c4ee)


[![Netlify Status](https://api.netlify.com/api/v1/badges/afe103a5-dd09-476e-827d-ecdb118b3314/deploy-status)](https://app.netlify.com/projects/gitdeun-site/deploys) [![Backend Deploy Workflow Status](https://github.com/Gitdeun/gitdeun-BE/actions/workflows/deploy.yml/badge.svg)](https://github.com/Gitdeun/gitdeun-BE/actions/workflows/deploy.yml)
- 🚀배포사이트 : [Gitdeun](https://gitdeun.site/)

## 📜 목차
- [1. 팀원 소개 :technologist:](#1-팀원-소개-technologist)
- [2. 기술 및 개발 환경 🛠️](#2-기술-및-개발-환경-️)
- [3. 시스템 아키텍처 및 데이터 흐름 요약](#3-시스템-아키텍처-및-데이터-흐름-요약)
- [4. ERD](#4-erd)
- [5. API 명세서(Spring Boot)](#5-api-명세서spring-boot)
- [6. 백엔드 기술적 도입 관련](#6-백엔드-기술적-도입-관련)
- [7. 실제 동작화면](#7-실제-동작화면)

<br>

## 🗣 프로젝트 소개
> **Gitdeun(깃든)** 은 비개발자 및 주니어 개발자들을 위한 Github Url을 통한 Repository에 대한 코드 구조의 마인드맵 협업 도구입니다.

<br>

## 🎯 기획 배경
현대 소프트웨어 개발에서 협업은 필수지만, 새 팀원이 기존 프로젝트 구조를 파악하거나 타인의 코드를 이해하는 데 많은 어려움을 겪습니다. AI 코드 생성 기술의 발전으로 개발 속도는 빨라졌지만, 오히려 코드의 전체 구조와 동작 원리를 파악하기는 더 어려워졌습니다.

기존 협업 도구는 버전 관리에는 유용하지만, 프로젝트의 구조, 기능별 역할, 코드 간의 연결성을 한눈에 보여주지 못합니다. 이는 특히 기획자, 디자이너, PM 등 비개발 직군에게 큰 진입 장벽이 되어 팀 내 소통 오류를 유발합니다.

'깃든'은 이 문제를 해결하기 위해, 코드와 협업 과정을 시각적으로 정리하고 AI 프롬프트를 통해 마인드맵을 수정/보완하며, 개발자와 비개발자 모두가 동일한 그림을 보며 소통할 수 있는 플랫폼을 제공하고자 합니다.

<br>

## 🔍 주요 기능 소개
### ✅ 1. Git 레포지토리 기반 AI 마인드맵 시각화
- Github API를 통해 메인 브랜치의 코드와 디렉토리 구조를 분석, AI가 기능 단위로 분류하여 Go.js 기반의 인터랙티브 마인드맵으로 자동 시각화합니다.
- 마인드맵 노드 클릭 시, 해당 코드 블록과 코드 리뷰 히스토리를 바로 확인할 수 있습니다.

<br>

### ✅ 2. AI(Gemini) 프롬프트를 통한 마인드맵 수정
- "핵심 기능 위주로 요약해줘" 같은 자연어 프롬프트를 입력하면, Gemini API가 마인드맵의 내용을 동적으로 수정하고 보완합니다.
- AI가 코드 내 주요 영역을 분석하여 리팩토링 제안이나 설계 의도를 요약하고 추천 코드를 제시합니다.

<br>

### ✅ 3. 실시간 마인드맵 협업 및 코드 리뷰
- 마인드맵의 각 기능 노드에 `[중요]`, `[질문]`, `[수정요청]` 등의 태그를 달아 실시간 피드백을 공유할 수 있습니다.
- 코드 참조(Code Reference) 및 코드 리뷰 기능을 통해 마인드맵 노드와 특정 코드 라인을 연결하여 협업합니다.

<br>

### ✅ 4.  실시간 알림
- 이메일과 연동하여 마인드맵 생성과 새로고침, 초대, 코멘트 등의 이벤트 발생 시 SSE를 통해 실시간 협업 알림을 전송합니다.

<br>

# 1. 팀원 소개 :technologist: 

| <img src="https://avatars.githubusercontent.com/u/73152527?v=4" width="150" height="150"> | <img src="https://avatars.githubusercontent.com/u/157580000?v=4"  width="150" height="150"> | <img src="https://avatars.githubusercontent.com/u/115947715?v=4"  width="150" height="150"> | <img src="https://avatars.githubusercontent.com/u/109144010?v=4"  width="150" height="150"> |
| :---------------------------------------------------------------: | :---------------------------------------------------------------: | :--------------------------------------------------------------: | :--------------------------------------------------------------: | 
|           [고범석](https://github.com/bumstone)         |        [장욱](https://github.com/uk0k)      |           [오채연](https://github.com/oh-chaeyeon)       |            [백승은](https://github.com/s-eun20)       |      


### [맡은 작업]
**고범석** : 메인 백엔드(Spring Boot), 인증/인가(JWT, OAuth2), DB 설계(RDB), 실시간 SSE, AWS 배포 <br>
**장욱** : AI 백엔드(Fast API), ArangoDB(Graph DB), Gemini API 연동 <br>
**오채연** : UI/UX 디자인 및 웹 프론트 구현<br>
**백승은** : UI 설계, Go.js 마인드맵 시각화 및 웹 프론트 구현<br>


-------------------------------------------------------------------------------------
# 2. 기술 및 개발 환경 🛠️
#### [Frontend]
![React](https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB)
![TypeScript](https://img.shields.io/badge/typescript-%233178C6.svg?style=for-the-badge&logo=typescript&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white)
![Axios](https://img.shields.io/badge/axios-671ddf?&style=for-the-badge&logo=axios&logoColor=white)
![GoJS](https://img.shields.io/badge/GoJS-Go.js-orange?style=for-the-badge)

#### [Backend]
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring_Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring_Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?&style=for-the-badge&logo=redis&logoColor=white)

![Python](https://img.shields.io/badge/python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![Google_Gemini](https://img.shields.io/badge/Google_Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white)
![ArangoDB](https://img.shields.io/badge/ArangoDB-D9262C?style=for-the-badge&logo=arangodb&logoColor=white)

#### [Infra & DevOps]
![AWS EC2](https://img.shields.io/badge/Amazon%20EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white)
![AWS RDS](https://img.shields.io/badge/Amazon%20RDS-527FFF?style=for-the-badge&logo=amazon-rds&logoColor=white)
![AWS S3](https://img.shields.io/badge/Amazon%20S3-569A31?style=for-the-badge&logo=amazon-s3&logoColor=white)
![Elastic_Beanstalk](https://img.shields.io/badge/Elastic_Beanstalk-FF9900?style=for-the-badge&logo=aws-elastic-beanstalk&logoColor=white)
![NGINX](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![DockerHub](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)
![GitHub_Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Netlify](https://img.shields.io/badge/Netlify-00C7B7?style=for-the-badge&logo=netlify&logoColor=white)

#### [협력 도구 🛠️]
![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-%23000000.svg?style=for-the-badge&logo=notion&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-%235865F2.svg?style=for-the-badge&logo=discord&logoColor=white)
![Figma](https://img.shields.io/badge/figma-%23F24E1E.svg?style=for-the-badge&logo=figma&logoColor=white)



-------------------------------------------------------------------------------------
# 3. 시스템 아키텍처 및 데이터 흐름 요약

프로젝트는 React 기반 프론트엔드, Spring Boot 기반 메인 백엔드, FastAPI 기반 AI 백엔드로 구성되어 다음과 같은 데이터 흐름 구조를 따릅니다.

<br>

<img width="748" height="428" alt="gitdeun drawio" src="https://github.com/user-attachments/assets/9bf7384a-e59c-4117-9a49-da457c977838" />


### 🔁 데이터 흐름 요약

1.  **사용자 요청 (Frontend)**: 사용자가 Netlify에 배포된 React/Go.js 웹사이트에 접속하여 Github URL을 입력하거나 마인드맵 수정을 위한 프롬프트를 입력합니다.
2.  **메인 서버 (Spring Boot)**: Nginx를 통해 요청을 받은 Spring Boot 백엔드는 사용자 인증(JWT, Google OAuth), 회원 정보 관리, 마인드맵 메타데이터 관리, 초대/알림(SSE) 기능 등 핵심 비즈니스 로직을 처리합니다. 
3.  **AI 서버 (FastAPI)**:
    * **마인드맵 생성**: Spring Boot의 요청을 받아 Github 레포지토리를 분석하고, **Gemini API**를 호출하여 코드 구조를 분석합니다.
    * **그래프 저장**: 분석된 마인드맵(노드, 엣지) 데이터를 Graph DB인 **ArangoDB**에 저장합니다.
    * **프롬프트 처리**: 사용자의 프롬프트 입력을 받아 Gemini API로 전달하고, ArangoDB의 그래프 데이터를 수정/보완합니다. 
4.  **데이터베이스**:
    * **MySQL (AWS RDS)**: 사용자 정보, 마인드맵 메타데이터, 초대/리뷰 정보 등 정형 데이터를 저장합니다. 
    * **ArangoDB (Graph DB)**: 마인드맵의 노드와 엣지, 코드 간의 관계 등 복잡한 그래프 데이터를 저장합니다. 
    * **Redis**: 실시간 접속자 현황, 인증 토큰(Refresh), 캐시 등을 관리합니다.
    * **AWS S3**: 코드 리뷰 시 첨부되는 이미지/파일 등을 저장합니다.
5.  **응답 및 시각화**: 처리된 마인드맵 데이터는 백엔드를 거쳐 프론트엔드로 전달되며, React와 **Go.js**가 이를 인터랙티브 마인드맵으로 시각화합니다.

# 4. ERD

[![ERD Preview](https://github.com/user-attachments/assets/c237ea45-3a79-4f6a-85f4-4b59b3045aea)](https://www.erdcloud.com/d/53acJhQtG5TCXazTP)


# 5. API 명세서(Spring Boot)
<details>
  <summary>👤 유저, 인증, 설정 API</summary>
  
| API URL | Method | 설명 |
|---------|--------|------|
| `/login/oauth2/code/{provider}` | GET | OAuth2 소셜 로그인 (Google, Github) |
| `/api/auth/logout` | POST | 로그아웃 |
| `/api/auth/token/refresh` | GET | 액세스 토큰 재발급 |
| `/api/auth/connect/github/state` | GET | Github 계정 연동을 위한 상태 코드 생성 |
| `/api/auth/oauth/refresh/{provider}` | POST | (구현 예정) 소셜 토큰 갱신 |
| `/api/auth/social` | GET | 현재 연동된 소셜 계정 목록 조회 |
| `/api/users/me` | GET | 내 정보 조회 |
| `/api/users/me` | DELETE | 회원 탈퇴 |
| `/api/users/me/settings` | GET | 내 설정 조회 (테마, 알림 여부) |
| `/api/users/me/settings` | PATCH | 내 설정 수정 |

</details>

<details>
  <summary>🗺️ 마인드맵, 노드, 프롬프트 API</summary>
  
| API URL | Method | 설명 |
|---------|--------|------|
| `/api/mindmaps/async` | POST | 마인드맵 비동기 생성 요청 |
| `/api/mindmaps/{mapId}` | GET | 마인드맵 상세 조회 |
| `/api/mindmaps/{mapId}` | DELETE | 마인드맵 삭제 (Soft Delete) |
| `/api/mindmaps/{mapId}/title` | PATCH | 마인드맵 제목 수정 |
| `/api/mindmaps/{mapId}/refresh` | POST | 마인드맵 비동기 새로고침 |
| `/api/mindmaps/{mapId}/sse` | GET | 마인드맵 실시간 SSE 연결 (접속자, 수정사항) |
| `/api/mindmaps/{mapId}/connections/users` | GET | 마인드맵 현재 접속자 목록 |
| `/api/mindmaps/{mapId}/nodes/{nodeKey}/code` | GET | 특정 노드의 코드 상세 조회 |
| `/api/mindmaps/{mapId}/nodes/list` | GET | (테스트용) 마인드맵 전체 노드 목록 |
| `/api/mindmaps/{mapId}/prompts` | POST | AI 프롬프트 분석 및 적용 요청 |
| `/api/mindmaps/{mapId}/prompts/histories` | GET | 프롬프트 히스토리 목록 조회 (페이징) |
| `/api/mindmaps/{mapId}/prompts/histories/{historyId}` | DELETE | 프롬프트 히스토리 삭제 |
| `/api/mindmaps/{mapId}/prompts/applied` | GET | 현재 적용된 프롬프트 히스토리 조회 |

</details>

<details>
  <summary>✍️ 코드 리뷰, 댓글, 참조 API</summary>
  
| API URL | Method | 설명 |
|---------|--------|------|
| `/api/mindmaps/{mapId}/nodes/{nodeKey}/code-reviews` | POST | 노드에 코드 리뷰 생성 |
| `/api/mindmaps/{mapId}/nodes/{nodeKey}/code-reviews` | GET | 노드의 코드 리뷰 목록 조회 |
| `/api/references/{refId}/code-reviews` | POST | 코드 참조(블록)에 코드 리뷰 생성 |
| `/api/code-reviews/{reviewId}` | GET | 코드 리뷰 상세 조회 (댓글 포함) |
| `/api/code-reviews/{reviewId}/status` | PATCH | 코드 리뷰 상태 변경 (Resolved/Pending) |
| `/api/code-reviews/{reviewId}/comments` | POST | 리뷰에 댓글/대댓글 작성 (파일 첨부) |
| `/api/comments/{commentId}` | PATCH | 댓글 내용 수정 |
| `/api/comments/{commentId}` | DELETE | 댓글 삭제 |
| `/api/comments/{commentId}/resolve` | PATCH | 댓글 스레드 해결/미해결 토글 |
| `/api/comments/{commentId}/attachments` | POST | 기존 댓글에 파일 추가 첨부 |
| `/api/comments/attachments/{attachmentId}` | DELETE | 첨부파일 개별 삭제 |
| `/api/comments/{commentId}/emoji` | PATCH | 댓글에 이모지 추가/변경/삭제 |
| `/api/mindmaps/{mapId}/nodes/{nodeKey}/code-references` | POST | 노드에 코드 참조(블록) 생성 |
| `/api/mindmaps/{mapId}/nodes/{nodeKey}/code-references` | GET | 노드의 코드 참조 목록 조회 |
| `/api/mindmaps/{mapId}/code-references/{refId}/detail` | GET | 코드 참조 상세 조회 (코드 스니펫 포함) |
| `/api/mindmaps/{mapId}/code-references/{refId}` | PATCH | 코드 참조 수정 (라인 변경 등) |
| `/api/mindmaps/{mapId}/code-references/{refId}` | DELETE | 코드 참조 삭제 |

</details>

<details>
  <summary>🤝 초대, 알림, 방문기록 API</summary>
  
| API URL | Method | 설명 |
|---------|--------|------|
| `/api/invitations/mindmaps/{mapId}` | POST | 이메일로 멤버 초대 |
| `/api/invitations/mindmaps/{mapId}` | GET | 마인드맵의 초대 목록 조회 (페이징) |
| `/api/invitations/mindmaps/{mapId}/accepted` | GET | 수락된 초대 목록 |
| `/api/invitations/mindmaps/{mapId}/pending` | GET | 대기중인 초대 목록 |
| `/api/invitations/{invitationId}/accept` | POST | (수신자) 이메일 초대 수락 |
| `/api/invitations/{invitationId}/reject` | POST | (수신자) 초대 거절 |
| `/api/invitations/mindmaps/{mapId}/link` | POST | (Owner) 초대 링크 생성 |
| `/api/invitations/link/{token}/accept` | POST | (수신자) 초대 링크 수락 (참여 요청) |
| `/api/invitations/{invitationId}/approve` | POST | (Owner) 링크 참여 요청 승인 |
| `/api/invitations/{invitationId}/reject-link` | POST | (Owner) 링크 참여 요청 거절 |
| `/api/notifications/sse` | GET | 알림 실시간 SSE 연결 (개수, 새 알림) |
| `/api/notifications` | GET | 내 알림 목록 조회 (페이징) |
| `/api/notifications/unread-count` | GET | 읽지 않은 알림 개수 |
| `/api/notifications/{notificationId}/read` | PATCH | 알림 읽음 처리 |
| `/api/notifications/read-all` | PATCH | 모든 알림 읽음 처리 |
| `/api/notifications/{notificationId}` | DELETE | 알림 삭제 |
| `/api/history/sse` | GET | 방문 기록 실시간 SSE 연결 (핀 고정) |
| `/api/history/visits` | GET | 최근 방문 기록 조회 (페이징, 핀 제외) |
| `/api/history/pins` | GET | 핀 고정된 방문 기록 조회 |
| `/api/history/visits/{historyId}` | DELETE | 방문 기록 삭제 (및 멤버 탈퇴) |
| `/api/history/{historyId}/pin` | POST | 방문 기록 핀 고정 |
| `/api/history/{historyId}/pin` | DELETE | 방문 기록 핀 해제 |

</details>

<details>
  <summary>📢 모집, 지원, 기술스택 API</summary>
  
| API URL | Method | 설명 |
|---------|--------|------|
| `/api/recruitments` | POST | 모집 공고 생성 (이미지 첨부) |
| `/api/recruitments` | GET | 모집 공고 필터링/검색 (페이징) |
| `/api/users/me/recruitments` | GET | 내가 작성한 모집 공고 |
| `/api/recruitments/{id}` | GET | 모집 공고 상세 조회 |
| `/api/recruitments/{id}` | PUT | 모집 공고 수정 |
| `/api/recruitments/{id}` | DELETE | 모집 공고 삭제 |
| `/api/recruitments/recommendations` | GET | 맞춤 추천 공고 목록 |
| `/api/recruitments/{recruitmentId}/applications` | POST | 모집 공고에 지원하기 |
| `/api/recruitments/{recruitmentId}/applications` | GET | (작성자) 공고의 지원자 목록 조회 |
| `/api/users/me/applications` | GET | (지원자) 내 지원 목록 조회 |
| `/api/applications/{applicationId}` | GET | 지원 상세 조회 (양측) |
| `/api/applications/{applicationId}/withdraw` | PATCH | (지원자) 지원 철회 |
| `/api/applications/{applicationId}/accept` | PATCH | (작성자) 지원 수락 |
| `/api/applications/{applicationId}/reject` | PATCH | (작성자) 지원 거절 |
| `/api/skills` | GET | 선택 가능한 개발 기술 목록 |
| `/api/skills/me` | GET | 내 개발 기술 목록 (선택 여부 포함) |
| `/api/skills/me` | POST | 내 개발 기술 저장 |

</details>

<details>
  <summary>⚙️ 기타 (S3, Webhook, Repo) API</summary>
  
| API URL | Method | 설명 |
|---------|--------|------|
| `/api/s3/bucket/upload` | POST | S3 파일 업로드 |
| `/api/s3/bucket/delete` | DELETE | S3 파일 삭제 |
| `/api/s3/bucket/download` | GET | S3 파일 다운로드 |
| `/api/repos/check` | GET | URL로 레포지토리 등록 여부 확인 |
| `/api/repos/{repoId}` | GET | 레포지토리 정보 조회 |
| `/api/webhook/mindmaps/update` | POST | (FastAPI용) Webhook 업데이트 수신 |

</details>


-------------------------------------------------------------------------------------
# 6. 백엔드 기술적 도입 관련

### 1. WebClient (vs. RestTemplate)
- **도입 배경**: Spring Boot 메인 서버는 AI 분석을 위해 FastAPI 서버와 통신해야 했습니다. 이 AI 분석 작업은 1분 이상 소요될 수 있는 매우 긴 I/O 작업입니다.
- **고민**: `RestTemplate`은 동기(Blocking) 방식이라, API 응답을 기다리는 동안 해당 스레드가 멈춥니다. 만약 동시 접속자가 10명만 되어도 서버 스레드가 모두 고갈되어 다른 요청을 처리할 수 없는 '스레드 고갈' 문제가 발생합니다.
- **해결**: 비동기(Non-Blocking) 방식인 `WebClient`를 채택했습니다. FastAPI에 요청을 보낸 후 스레드를 즉시 반납하고, 나중에 응답이 오면 콜백으로 처리합니다. 긴 I/O 작업을 기다리는 동안에도 Spring Boot 서버가 최소한의 스레드로 다른 사용자들의 요청(로그인, 댓글 작성 등)을 효율적으로 처리할 수 있게 되었습니다.

### 2. @Async & ThreadPool 분리
- **도입 배경**: 마인드맵 생성/새로고침처럼 오래 걸리는 `@Async` 작업과, 알림 전송처럼 빠르게 끝나는 `@Async` 작업이 모두 필요했습니다.
- **고민**: 만약 모든 `@Async` 작업이 동일한 스레드 풀을 사용한다면, AI 분석 작업 몇 개가 스레드 풀을 모두 점유하여 간단한 알림 전송조차 지연될 수 있었습니다.
- **해결**: `AsyncTransactionConfig.java`를 설정하여 스레드 풀을 명시적으로 분리했습니다.
    - **`mindmapExecutor`**: AI 분석, 마인드맵 생성 등 무겁고 오래 걸리는 핵심 작업을 위한 전용 스레드 풀입니다.
    - **`generalExecutor`**: 알림 전송, 이메일 발송 등 가볍고 빠른 작업을 위한 별도 스레드 풀을 두어, 무거운 작업이 가벼운 작업을 방해하지 않도록 격리했습니다.

### 3. Server-Sent Events (SSE)
- **도입 배경**: 사용자에게 실시간 알림(마인드맵 생성 완료, 초대 등)과 마인드맵 내 실시간 동시 접속자 목록을 제공해야 했습니다.
- **고민**:
    - **Polling**: 클라이언트가 1초마다 서버에 "새 소식 있어?"라고 묻는 방식은 불필요한 트래픽을 유발합니다.
    - **WebSocket**: 양방향 통신이 가능하지만, '깃든'의 요구사항은 대부분 서버가 클라이언트에게 **단방향**으로 데이터를 밀어주는(Push) 것이었습니다. WebSocket은 SSE보다 구현이 복잡하다고 판단했습니다.
- **해결**:
    - 구현이 비교적 간단하고 요구사항에 부합하는 **SSE**를 선택했습니다.
    - **`NotificationSseService`**: 사용자 ID(`userId`)별로 연결을 관리하며, 개인 알림(초대, 생성 완료 등)을 전송합니다.
    - **`MindmapSseService`**: 마인드맵 ID(`mapId`)별로 연결을 관리하고, `Redis` Set으로 실시간 접속자를 추적하여, 접속/종료 시 해당 마인드맵의 모든 사용자에게 최신 접속자 목록을 브로드캐스트합니다.

### 4. Full-Text Search (모집 공고 검색)
- **도입 배경**: 모집 공고(Recruitment)의 `title`과 `content` 필드는 텍스트 양이 많아 검색 기능이 필수적이었습니다.
- **고민**: JPA의 기본 `LIKE '%keyword%'` 검색은 인덱스를 활용하지 못하고 전체 테이블을 스캔(Full Table Scan)하여 데이터가 많아질수록 성능이 매우 저하됩니다.
- **해결**:
    1. MySQL의 네이티브 기능인 **Full-Text Search**를 도입했습니다. DB의 `title`, `content` 컬럼에 Full-Text 인덱스를 생성했습니다.
    2. `MySqlFunctionContributor.java`를 통해 `MATCH(...) AGAINST(... IN BOOLEAN MODE)` 함수를 Hibernate가 인식할 수 있도록 `match_against_boolean`라는 이름으로 등록했습니다.
    3. `RecruitmentRepositoryImpl` (QueryDSL 구현체)에서 `Expressions.booleanTemplate`을 사용하여 이 네이티브 함수를 호출함으로써, 인덱스를 활용한 고속 검색 및 관련도(Score) 기반 정렬을 구현했습니다.



-------------------------------------------------------------------------------------
# 7. 실제 동작화면

### ✅ 메인페이지 및 OAuth 로그인

https://github.com/user-attachments/assets/7467f245-7b5e-4a73-898b-cf12ad823e39

---

### ✅ 관심 개발 언어 선택

https://github.com/user-attachments/assets/7112c4df-7f19-40e1-9ad8-fbdd8945c43d

---

### ✅ 마인드맵 기록 및 핀 고정 유무 조회

https://github.com/user-attachments/assets/4b741e4f-f2ff-43c9-a957-7e6e8859d204

---

### ✅ 마인드맵 생성 요청과 알림 확인

https://github.com/user-attachments/assets/2f53a7fb-09ed-479c-9fad-10346aa6acfc

---

### ✅ 마인드맵 생성 완료 시 SSE로 자동 전환

https://github.com/user-attachments/assets/171ecf08-d764-489f-b484-644fab7f112a

---

### ✅ 마인드맵 상세 조회

https://github.com/user-attachments/assets/f23f9287-bb7f-4c58-9234-6b6cecd13332

---

### ✅ 마인드맵 코드 파일 및 리뷰 확인

https://github.com/user-attachments/assets/fcf3f06d-66d5-4c34-bab4-f0461e825a3f

---

### ✅ 코드 블록 작성

https://github.com/user-attachments/assets/2a097d07-921c-4fb5-ba77-3f8fa4394bdd

---

### ✅ 초대하기 (이메일)

https://github.com/user-attachments/assets/410acad2-7452-4a84-9e2b-5e25360ab9eb

<img width="489" height="338" alt="초대 이메일 확인" src="https://github.com/user-attachments/assets/f8377e6a-9682-415b-b8ab-d41b1b65bc58" />

---

### ✅ 모집 공고 작성

<img width="1078" height="1871" alt="모집공고 작성" src="https://github.com/user-attachments/assets/95f3bf8d-480f-403f-83e1-9f6f1e08ebe7" />

---

### ✅ 모집 공고 상세 조회

<img width="1090" height="1159" alt="모집공고 상세조회" src="https://github.com/user-attachments/assets/7815d11d-ec82-4bca-8bf1-02681473e344" />

---

### ✅ 모집 신청 

<img width="657" height="545" alt="모집신청" src="https://github.com/user-attachments/assets/3901ed41-e73b-461e-9e25-6b47ad75c27c" />

<img width="1023" height="909" alt="모집신청2" src="https://github.com/user-attachments/assets/9b890be4-8dfb-45ac-9c82-08812d5410dd" />

---

### ✅ 마이페이지 정보 조회

<img width="1086" height="956" alt="마이페이지" src="https://github.com/user-attachments/assets/46eb1746-a3b1-4557-9e33-f11f5eacaf88" />
