# v-editor (Video Editor MVP)

로컬 전용 MVP 비디오 편집기. Android + Jetpack Compose + Coroutines 기반, Slack Circuit로 화면/상태를 구성합니다.

## 핵심 스택
- Android, Kotlin, Jetpack Compose, Coroutines/Flow
- 아키텍처: UDF(MVI 스타일) + Slack Circuit(Screen/Presenter/Ui)
- 테스트: JUnit, Turbine, Compose UI Test

## 아키텍처 개요
- Presentation: Circuit `Screen`(네비), `Presenter`(상태 기계), `Ui`(순수 Composable)
- Domain: UseCase 중심, 순수 Kotlin
- Data: 로컬 파일/MediaStore 접근, 편집 메타데이터 저장, `MediaRepository` 추상화로 디바이스 미디어 접근

## 화면
- 홈: 생성된 비디오 목록, 새 비디오 생성
- 에디터: 타임라인(트림/분할), 스티커/자막/음악 추가, 미리보기

## 라이브러리
- Circuit: Compose 중심의 아키텍처/네비게이션 — `https://github.com/slackhq/circuit`
- Metro: DI/코드 생성 관련 도구(적용 범위 평가 예정) — `https://github.com/ZacSweers/metro`

## 의존성 스니펫(참고용)
```kotlin
// libs.versions.toml (예시)
# circuit = "0.30.0" (실제 최신 버전 확인 필요)

// app/build.gradle.kts
dependencies {
  implementation("com.slack.circuit:circuit-runtime:<version>")
  implementation("com.slack.circuit:circuit-runtime-presenter:<version>")
  implementation("com.slack.circuit:circuit-runtime-ui:<version>")
  implementation("com.slack.circuit:circuit-backstack:<version>")
}
```

## 개발 규칙
- `.cursor/rules/method.mdc`에 상세 규칙 명시(TDD, Compose, Coroutines, Circuit, Git 등)
- 모든 PR은 테스트 포함, 작은 단위로 머지

## 테스트 컨벤션
- **클래스명**: `<SUT명>Test` (예: `TimelineTest`)
- **함수명 패턴**: `given_<상태>_when_<행위>_then_<결과>()`, `should_<행위>_when_<상태>()`, 또는 백틱 자연어(`` `should reject negative time` ``)
- **구조**: Arrange / Act / Assert 3단계
- **패키지**: 프로덕션 패키지 경로 미러링
- **UI 테스트**: `ui_<보이는현상>_when_<상태>()`

## 코드 스타일/포맷팅
- Spotless + ktlint 적용(4칸 들여쓰기). `.editorconfig` 참조
- 명령: `./gradlew spotlessApply`

## 커밋 전 체크리스트
- `Plan.md`의 TODO 상태 갱신(진행/완료)
- `README.md` 변경 필요 시 반영
- 포맷/테스트 통과: `./gradlew spotlessApply :app:testDebugUnitTest`

## 문서 동기화 워크플로
- 코드 변경 시 다음을 함께 갱신합니다.
  - `Plan.md`: TODO 상태, Progress, Changelog
  - `README.md`: 아키텍처/스택/설치/레퍼런스 변경점
  - `.cursor/rules`: 관련 규칙 보완(자동첨부/전역/요청형)
- 규칙 참조: `.cursor/rules/docs-sync.mdc`, `.cursor/rules/progress-tracking.mdc`

## 빌드/실행
- Android Studio 최신 버전(조건: JDK 17+)
- `./gradlew :app:assembleDebug` 또는 IDE 실행

## 품질 게이트
- 최소: `:app:testDebugUnitTest` 통과

## Progress
- 2025-09-12: 초기 문서화(README, Plan, Cursor Rules) 완료. 규칙 Always/Auto/Requested 구분 반영.

## 레퍼런스
- Circuit — `https://github.com/slackhq/circuit`
- Metro — `https://github.com/ZacSweers/metro`
- Now in Android — `https://github.com/android/nowinandroid`
- JetNews — `https://github.com/android/compose-samples/tree/main/JetNews`
- Sunflower — `https://github.com/android/sunflower`
- Tivi — `https://github.com/chrisbanes/tivi`
- Compose Samples — `https://github.com/android/compose-samples`
