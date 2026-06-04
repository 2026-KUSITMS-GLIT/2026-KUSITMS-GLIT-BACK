# GLIT Backend

> 하루 5분 커리어 기록 서비스 **GLIT**의 백엔드 레포지토리.
> 데일리 스크럼 → STAR 심화 기록 → AI 역량 태깅 → 커리어 리포트로 이어지는 도메인을 제공합니다.

<!-- TODO: build · coverage · license 배지 -->

## 목차

- [기술 스택](#-기술-스택)
- [아키텍처 & 디렉토리 구조](#-아키텍처--디렉토리-구조)
- [핵심 설계 포인트](#-핵심-설계-포인트)
- [API 문서](#-api-문서)
- [인프라 & CI/CD](#️-인프라--cicd)
- [로컬 실행](#-로컬-실행)
- [협업 가이드](#-협업-가이드)

## 🛠 기술 스택

| 구분 | 스택 | 선정 이유 |
|------|------|----------|
| **Language & Framework** | Java 21 / Spring Boot 3.5.13 / Spring MVC / Spring Data JPA | Java 21 LTS + 성숙한 Spring 생태계로 인증·ORM·검증을 표준 의존성으로 빠르게 구성. 헥사고날+레이어드 구조에 적합하고 팀 숙련도 높음 |
| **DB / Storage** | PostgreSQL / Redis / AWS RDS, S3 / Flyway | PostgreSQL=관계형 정합성(트랜잭션·제약·인덱스), Redis=토큰·세션 등 휘발성 상태 외부화(무상태 서버), S3=이미지 바이너리 분리 저장(presigned 직접 업로드), Flyway=스키마 버전 관리로 환경 간 일관성 |
| **Infra / CI-CD** | GitHub Actions / Docker / AWS EC2 / ECR / SSM / Route 53 | 레포 통합 CI/CD(브랜치→환경 결정론 매핑), 불변 이미지(ECR)로 동일 배포, EC2=소규모 저비용 운영, SSM=무에이전트 배포+SecureString 시크릿 주입(키 노출 최소화), Route 53=도메인 라우팅 |
| **Observability / Logging** | AWS CloudWatch | AWS 네이티브라 추가 인프라 없이 EC2 OS 메트릭·로그 수집. 운영 부담 없이 기본 관측 확보 |
| **Security** | Spring Security / OAuth 2.0 (Kakao·Google·Naver) / JWT | 검증된 인증·인가 표준 + 국내 3사 소셜 로그인으로 가입 마찰 최소화. JWT=무상태 인증으로 매 요청 DB 조회 0 → 수평 확장 용이 |
| **Documentation** | Swagger | springdoc로 코드와 동기화되는 API 문서 자동 생성. 프론트와 계약 공유 + Try-it-out으로 협업 효율 |
| **Testing** | JUnit 5 / Spring Security Test / JaCoCo / spotless | JUnit5+Mockito 단위 테스트, Security Test로 인증 흐름 검증, JaCoCo 커버리지 게이트, spotless 포맷 일관성(CI 게이트) |

## 🏗 아키텍처 & 디렉토리 구조

도메인은 8개 패키지로 분리되어 있으며, **변경이 잦고 외부 연동(AI·S3)이 많은 `record`·`report` 도메인은 헥사고날 아키텍처**로, 그 외 도메인은 레이어드(controller·service·repository)로 구성해 복잡도에 맞춰 구조를 차등 적용했습니다.

```
src/main/java/com/groute/groute_server
├── auth/          # 소셜 OAuth2 로그인 · JWT 발급/재발급 · 디바이스 토큰
├── user/          # 온보딩 · 프로필 · 알림 설정 · 회원 탈퇴(스케줄러)
├── record/        # 스크럼 · STAR 심화 기록 · AI 태깅   ← 헥사고날
│   ├── adapter/
│   │   ├── in/web/        # REST 컨트롤러 (인바운드)
│   │   └── out/           # persistence · cache · storage(S3) · ai(FastAPI) (아웃바운드)
│   ├── application/
│   │   ├── port/in,out/   # 유스케이스 경계 인터페이스
│   │   └── service/       # 유스케이스 구현
│   └── domain/            # 순수 도메인 엔티티 · enum (프레임워크 의존 최소)
├── report/        # 커리어 리포트 생성   ← 헥사고날
├── calendar/      # 캘린더(잔디) 조회
├── home/          # 홈 요약 · 역량 통계(레이더)
├── notification/  # 푸시 알림 스케줄러
└── common/        # 보안(jwt·filter) · 예외 · 공통 응답 · 설정 · 유틸
```

- **ERD**: <!-- TODO: dbdiagram 링크 -->

## ⚡ 핵심 설계 포인트

**성능 최적화**
- 핵심 조회 경로(캘린더·잡 폴링·알림 스케줄러)를 복합 인덱스로 커버하고, `projects`·`scrum_titles`에 부분/유니크 인덱스 적용
- 비정규화 카운터(`title_count`·`scrum_count`)와 일자별 집계 테이블(`daily_competency_stats`)로 통계 쿼리 제거
- AI 태깅은 **비동기 잡 큐 + `SELECT … FOR UPDATE SKIP LOCKED`** 폴링으로 동시성 안전하게 처리
- 리포트·캘린더 조회는 **fetch join으로 N+1 제거**, `open-in-view: false`로 영속성 컨텍스트 수명 단축
- refresh 토큰 등 휘발성 상태를 Redis로 외부화 → **무상태 서버(수평 확장 용이)**

**보안**
- OAuth2 소셜 로그인 → 자체 **JWT(무상태, access/refresh 분리)**, 매 요청 DB 조회 없이 인증
- refresh 토큰은 **SHA-256 해시로 저장 + Lua 스크립트 기반 원자적 회전(CAS)** 으로 재사용·동시 요청 경합 방어
- 모든 시크릿은 **SSM SecureString(KMS)** 으로 런타임 주입 — 이미지·레포에 평문 미탑재
- 이중 `SecurityFilterChain`(OAuth2 / JWT 분리), CORS 화이트리스트, BE↔AI 내부 토큰(`X-Internal-Token`)

**테스트 전략**
- **단위**(Mockito, 서비스 계층 비즈니스 로직) + **통합**(`@WebMvcTest` 컨트롤러·인증, `@DataJpaTest` + Testcontainers 실 PostgreSQL 쿼리 검증)
- 외부 연동(AI)은 **MockWebServer**로 격리해 DB·네트워크 의존 최소화
- **JaCoCo 커버리지 게이트** + spotless 포맷 검사를 CI 필수 통과 조건으로 운영

**비용/확장 트레이드오프 (MVP)**
- t3.micro 단일 인스턴스 · ElastiCache 대신 self-host Redis · RDS Single-AZ로 초기 비용 최소화
- 트래픽 증가 시 ElastiCache·Multi-AZ·오토스케일/무중단 배포로 전환 가능한 구조 유지

## 📖 API 문서

- **Swagger UI**: <!-- TODO: 배포 Swagger URL --> (로컬: `http://localhost:8080/swagger-ui.html`)
- **OpenAPI 스펙**: `/v3/api-docs`
- 모든 응답은 공통 포맷 `ApiResponse<T>`(상태·메시지·데이터)로 감싸지며, 에러는 `ErrorCode` 기반 일관된 코드 체계로 반환됩니다. (에러코드 목록: `/docs/error-code`)

## ☁️ 인프라 & CI/CD

<!-- TODO: 아키텍처 다이어그램 이미지 -->

> **Push → Gitleaks·Trivy 스캔 → 테스트 + JaCoCo 게이트 → OIDC 인증 → SHA 태그 ECR 이미지 → stg/prod 자동 분기 → SSM 무에이전트 배포 → 상태 폴링**

- **결정론적 배포**: 브랜치(`dev`→stg / `master`→prod)가 환경·IAM Role·SSM 경로에 1:1 매핑
- **키리스(OIDC)**: GitHub↔AWS 장기 액세스키 없이 단기 AssumeRole 토큰만 사용
- **보안 내장 CI**: Gitleaks(시크릿) · Trivy(이미지 취약점) · JaCoCo(커버리지) 자동 검사
- **무에이전트 배포**: SSH 개방 없이 SSM SendCommand로 EC2 배포, 시크릿은 배포 순간 SecureString 주입
- **롤백**: 특정 SHA 이미지로 되돌리는 수동 롤백 워크플로(`workflow_dispatch`) 제공

## 💻 로컬 실행

요구사항: **JDK 21**, **Docker**(PostgreSQL·Redis)

```bash
# 1. 로컬 인프라(PostgreSQL · Redis) 기동  <!-- TODO: 로컬 compose/실행 안내 보완 -->
# 2. 애플리케이션 실행 (local 프로파일)
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 환경 변수는 `local` 프로파일 기본값으로 동작하며, 외부 연동(OAuth·S3·FCM)은 미설정 시 비활성/폴백됩니다.
- 빌드·테스트: `./gradlew check` (테스트 + JaCoCo + spotless)

## 🤝 협업 가이드

브랜치 전략·커밋 컨벤션·PR 규칙은 [CONTRIBUTING.md](./CONTRIBUTING.md)를 참고해 주세요.
