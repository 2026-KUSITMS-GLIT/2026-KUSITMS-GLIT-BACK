# Contributing Guide

본 문서는 Backend 팀의 협업 규칙을 정의합니다. 작업 흐름을 표준화하여 협업 시 혼선을 줄이고, 코드 리뷰·테스트·배포 과정을 일관되게 유지하는 것을 목표로 합니다.

- `1. Workflow 개요` — 전체적인 워크플로우 이해용
- `2. Git Convention` — 실제 작업 시 컨벤션 확인용
- `3. 프로젝트 폴더 구조` — 아키텍처 기반 디렉터리 규칙
- `4. Swagger 작성 가이드` — API 작업 시 Swagger 어노테이션 규칙
- `5. 테스트 커버리지 (JaCoCo)` — 테스트/커버리지 기준 및 실행 방법

---

## 1. Workflow 개요

본 프로젝트는 **GitHub Flow**를 기본으로 하되, **GitFlow**의 `dev` / `master` 분리 개념을 부분 적용합니다.

### 1.1 고정 브랜치

| 브랜치 | 용도 |
| --- | --- |
| `master` | 프로덕션 및 릴리즈 브랜치. **직접 커밋 금지** |
| `dev` | 통합 및 테스트 브랜치. 모든 기능은 `dev`를 거쳐 `master`로 반영 |

### 1.2 작업 흐름

모든 작업은 아래 순서를 반드시 따릅니다.

#### 1.2.1 Issue 생성

작업 단위는 Issue 단위로 관리하며, 모든 작업은 Issue 생성으로 시작합니다.

- **이슈 템플릿**: 기능 / 버그 / 태스크 중 선택
- **이슈 제목**: `prefix: 작업과 관련된 한국어 설명`
  ```
  feat: 회원가입 API 추가
  docs: swagger 설명 보완
  ```
- **이슈 라벨**: `prefix` / `status` / `priority` 라벨을 각각 1개씩 필수 설정

#### 1.2.2 브랜치 생성

Issue 생성 후 반드시 **Issue 번호를 포함한 브랜치**를 생성합니다.

- **네이밍 규칙**: `prefix/#issue-description-with-dash-english`
- **규칙**
  - `prefix`는 Git Convention과 동일
  - `description`은 영문 kebab-case
  ```
  feat/#35-add-login
  test/#120-archive
  docs/#88-update-swagger
  ```

#### 1.2.3 브랜치에서 작업 및 커밋

모든 작업은 해당 브랜치에서 수행합니다.

- **커밋 메시지**: `prefix: 커밋 내용 (한국어)`
  ```
  docs: swagger 설명 추가
  feat: 회원가입 API 구현
  fix: 저장 시 NPE 발생 문제 수정
  ```
- **주의사항**
  - 하나의 커밋에는 **하나의 논리적 변경**만 포함
  - `prefix`는 반드시 **소문자** 사용

#### 1.2.4 Pull Request 생성 → `dev` 머지

작업 완료 후 대상 브랜치를 `dev`로 하여 PR을 생성합니다.

- **PR 제목**: `prefix: 한국어 설명 (#issue)`
  ```
  feat: 회원관리 기능 개발 (#32)
  ```
- **PR 본문**: 사전 정의된 PR 템플릿에 맞춰 작성
- **담당자 / 리뷰어**: Assignee는 본인, Reviewer는 타 파트원
- **PR 라벨**: `prefix` / `status` / `priority` 각각 1개씩 필수 설정
- **PR 커밋 메시지**: GitHub 기본 설정에 따름

#### 1.2.5 `dev` 브랜치에서 테스트

`dev` 브랜치는 기능 통합 및 릴리즈 전 검증을 위한 기준 브랜치입니다.

- `dev` 코드는 **staging 서버**에 배포되며 다음 목적에 사용됩니다.
  - 기능 간 충돌 여부 확인
  - 전체 시나리오 기반 통합 테스트
  - 테스트 코드로 커버되지 않는 수동 QA
  - 프론트엔드 연동 테스트를 위한 안정적인 API 제공
  - Swagger 기반 API 스펙 및 동작 확인
- staging 서버는 실제 사용자 트래픽을 받지 않으며, 운영 서버와 동일한 환경 구성을 최대한 유지합니다.
- 문제 발생 시
  - 기존 Issue를 **reopen**하여 작업을 이어가거나
  - 신규 `fix` Issue를 생성하여 별도 작업으로 분리

#### 1.2.6 `dev` → `master` 머지

테스트 완료 후 `dev` 브랜치를 `master`로 머지합니다.

- `master`에는 `dev`를 통한 PR만 허용
- `master` 머지는 **릴리즈 단위**로 진행

---

## 2. Git Convention

### 2.1 Prefix

| Prefix | 용도 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (README, Swagger 설명 등) |
| `style` | 코드 포맷팅, 세미콜론 누락 등 (로직 변경 없음) |
| `refactor` | 리팩토링 (기능 변경 없이 구조 개선) |
| `test` | 테스트 코드 추가·수정 |
| `chore` | 빌드 업무, 패키지 매니저 설정 등 기타 변경 |
| `build` | 빌드 시스템/외부 의존성 변경 (gradle, CI/CD 등) |

### 2.2 Issue

- **제목**: `prefix: 한국어 설명`
- **라벨**: `prefix` / `status` / `priority` 각 1개 필수

### 2.3 Branch

- **형식**: `prefix/#issue-description-with-dash-english`
- **규칙**
  - Issue 번호 필수 포함
  - description은 영문 kebab-case
- **예시**
  ```
  feat/#35-add-login
  chore/#11-architecture-setup
  ```

### 2.4 Commit

- **형식**: `prefix: 커밋 내용 (한국어)`
- **규칙**
  - prefix는 소문자
  - 하나의 커밋 = 하나의 논리적 변경
- **예시**
  ```
  feat: 회원가입 API 구현
  fix: 저장 시 NPE 발생 문제 수정
  ```

### 2.5 Pull Request

- **제목**: `prefix: 한국어 설명 (#issue)`
- **대상**: `dev` 브랜치 (단, 릴리즈 시 `dev` → `master`)
- **Assignee**: 본인
- **Reviewer**: 타 파트원
- **라벨**: `prefix` / `status` / `priority` 각 1개 필수
- **본문**: PR 템플릿에 맞춰 작성

---

## 3. 프로젝트 폴더 구조

본 프로젝트는 **헥사고날 + 레이어드 혼합 아키텍처**를 따릅니다.

- **Layered**: CRUD 성격이 강하고 비즈니스 규칙이 단순한 도메인 (`auth`, `user`, `home`, `calendar`)
- **Hexagonal**: 외부 의존성(AI, 캐시, 스토리지 등)이 많고 도메인 규칙이 복잡한 핵심 도메인 (`record`, `report`)

### 3.1 전체 구조

```
src/main/java/com/groute/groute_server/
├── GrouteServerApplication.java
│
├── common/                        # 공통 설정·예외·응답
│   ├── config/
│   ├── exception/
│   └── response/
│
├── auth/                          # [Layered] 소셜 로그인
│   ├── controller/
│   ├── service/
│   │   └── oauth/                 # OAuthClient interface + 구현체
│   ├── repository/
│   ├── entity/
│   ├── jwt/
│   └── dto/
│
├── user/                          # [Layered] 온보딩·마이페이지
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── record/                        # [Hexagonal] 핵심 도메인
│   ├── domain/                    # 순수 모델·규칙 (Scrum, Star 등)
│   ├── application/
│   │   ├── port/in/               # Use Case 인터페이스
│   │   ├── port/out/              # 외부 의존성 인터페이스
│   │   └── service/               # Use Case 구현
│   └── adapter/
│       ├── in/web/                # REST Controller
│       └── out/
│           ├── persistence/       # JPA Repository 구현
│           ├── ai/                # AI 클라이언트
│           ├── cache/             # 캐시
│           └── storage/           # 파일 스토리지
│
├── report/                        # [Hexagonal] 리포트
│   ├── domain/
│   ├── application/
│   │   ├── port/in/
│   │   ├── port/out/
│   │   └── service/
│   └── adapter/
│       ├── in/
│       │   ├── web/
│       │   └── event/             # 도메인 이벤트 구독
│       └── out/
│           ├── persistence/
│           ├── ai/
│           └── client/            # 타 도메인 조회용
│
├── home/                          # [Layered] 조회 최적화
│   ├── controller/
│   ├── service/
│   ├── repository/                # QueryDSL Projection 전용
│   └── dto/
│
└── calendar/                      # [Layered] 캘린더
    ├── controller/
    ├── service/
    ├── repository/
    └── dto/
```

### 3.2 아키텍처 규칙

#### Layered 모듈

- 흐름: `Controller → Service → Repository → Entity`
- DTO는 `controller` 계층 입출력에서만 사용하며, `service` 내부에서는 Entity 또는 도메인 객체를 사용합니다.

#### Hexagonal 모듈

- **의존성 방향**: `adapter → application → domain`
- `domain`은 어떤 외부 기술에도 의존하지 않는 순수 코드여야 합니다 (JPA, Spring 애너테이션 금지).
- `application/port/in`: 외부에서 application을 호출하는 Use Case 인터페이스
- `application/port/out`: application이 외부 세계를 호출하기 위한 인터페이스
- `application/service`: `port/in` 구현체. `port/out`을 주입받아 사용
- `adapter/in`: 외부 요청을 `port/in`으로 변환 (Web, Event 등)
- `adapter/out`: `port/out` 구현체 (DB, AI, Cache, 외부 API 등)

### 3.3 신규 모듈 추가 가이드

- 단순 CRUD 성격 → **Layered** 구조로 추가
- 외부 의존성이 다수이거나 도메인 규칙이 복잡 → **Hexagonal** 구조로 추가
- 구조 결정이 모호할 경우 PR 전에 팀 내 논의를 거쳐 결정합니다.

---

## 4. Swagger 작성 가이드

API 작업 시 프론트엔드가 Swagger UI에서 스펙을 바로 확인할 수 있도록 아래 어노테이션을 반드시 작성합니다.

> **자동으로 처리되는 것** (직접 설정 불필요)
> - 도메인별 그룹화 — `GroupedOpenApi`가 패키지 기준으로 자동 분리
> - JWT 인증 — 글로벌 `@SecurityScheme` 적용 완료
> - 서버 URL — 환경별(local/stg/prod) 자동 적용

### 4.1 Controller 레벨

| 어노테이션 | 용도 |
| --- | --- |
| `@Tag(name = "...", description = "...")` | 컨트롤러 그룹 이름 및 설명. Swagger UI에서 그룹 제목으로 표시 |

```java
@Tag(name = "회원", description = "온보딩·마이페이지 API")
@RestController
@RequestMapping("/api/v1/users")
public class UserController { }
```

### 4.2 엔드포인트 레벨

| 어노테이션 | 용도 |
| --- | --- |
| `@Operation(summary = "...", description = "...")` | API 한 줄 요약 + 상세 설명 |
| `@ApiResponse(responseCode = "...", description = "...")` | 응답 코드별 설명 |

```java
@Operation(summary = "닉네임 변경", description = "마이페이지에서 닉네임을 변경합니다.")
@ApiResponse(responseCode = "200", description = "변경 성공")
@ApiResponse(responseCode = "400", description = "유효하지 않은 닉네임")
@PutMapping("/nickname")
public ApiResponse<Void> updateNickname(...) { }
```

**인증이 불필요한 엔드포인트**의 경우에만 `@SecurityRequirement`를 빈 값으로 설정하여 글로벌 인증을 제외합니다.

```java
@Operation(summary = "소셜 로그인")
@SecurityRequirement(name = "")
@PostMapping("/auth/login")
public ApiResponse<LoginResponse> login(...) { }
```

### 4.3 DTO 레벨

| 어노테이션 | 용도 |
| --- | --- |
| `@Schema(description = "...", example = "...")` | 필드 설명 + 예시값 |

```java
public record CreateUserRequest(
        @Schema(description = "닉네임", example = "겨레")
        String nickname,

        @Schema(description = "직군", example = "DEVELOPER")
        String jobGroup
) { }
```

---

## 5. 테스트 커버리지 (JaCoCo)

서비스 레이어 비즈니스 로직을 중심으로 단위 테스트를 작성하며, 커버리지는 JaCoCo로 측정합니다.

### 5.1 합의된 기준

- **라인 커버리지 60% 이상**
- 브랜치 커버리지는 현 단계에서 강제하지 않습니다.
- 기준은 `jacocoTestCoverageVerification`으로 자동 검증되며, 미달 시 로컬 `./gradlew check` 단계에서 빌드가 실패합니다.

### 5.2 실행 방법

| 명령 | 용도 |
| --- | --- |
| `./gradlew test` | 테스트 실행 + HTML 리포트 생성 (`finalizedBy`로 자동) |
| `./gradlew check` | 위 + 커버리지 기준(60%) 검증까지 수행. PR 올리기 전 권장 |

리포트 경로: `build/reports/jacoco/index.html`

### 5.3 커버리지 측정 제외 패키지

로직이 거의 없거나 자동 생성되는 클래스는 커버리지 지표에서 제외됩니다.

- `**/*Application*` — Spring Boot 엔트리 클래스
- `**/dto/**`, `**/response/**` — 요청/응답 DTO 및 래퍼
- `**/config/**` — 설정 클래스
- `**/exception/**` — 예외/에러코드 정의
- `**/Q*.class` — QueryDSL 자동 생성 클래스

새 패키지 추가 또는 제외 정책 변경이 필요하면 `build.gradle`의 `jacocoExcludes`를 수정하고 팀에 공유합니다.

### 5.4 PR 연동

PR 템플릿의 **`커버리지 (JaCoCo)`** 섹션에 라인 커버리지 수치를 기입합니다. 자세한 체크리스트는 `.github/pull_request_template.md` 참조.