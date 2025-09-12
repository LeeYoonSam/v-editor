# v-editor (Video Editor MVP)

로컬 전용 MVP 비디오 편집기. Android + Jetpack Compose + Coroutines 기반, Slack Circuit로 화면/상태를 구성합니다.

## 핵심 스택
- Android, Kotlin, Jetpack Compose, Coroutines/Flow
- 아키텍처: UDF(MVI 스타일) + Slack Circuit(Screen/Presenter/Ui)
- 테스트: JUnit, Turbine, Compose UI Test

## 아키텍처 개요
- Presentation: Circuit `Screen`(네비), `Presenter`(상태 기계), `Ui`(순수 Composable)
- Domain: UseCase 중심, 순수 Kotlin
- Data: 로컬 파일/MediaStore 접근, 편집 메타데이터 저장

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
  // 선택: overlay/sharing 등 필요 시 추가
  // implementation("com.slack.circuit:circuit-overlay:<version>")

  // Metro: 프로젝트 정책에 따라 적용(문서화 우선)
  // implementation("dev.zacsweers.metro:metro-core:<version>")
}
```

## 개발 규칙
- `method.mdc`에 상세 규칙 명시(TDD, Compose, Coroutines, Circuit, Git 등)
- 모든 PR은 테스트 포함, 작은 단위로 머지

## 빌드/실행
- Android Studio 최신 버전(조건: JDK 17+)
- `./gradlew :app:assembleDebug` 또는 IDE 실행

## 품질 게이트
- 최소: `:app:testDebugUnitTest` 통과
- 코드 스타일/린트: 추후 도입(Spotless/Detekt)

## 레퍼런스
- Circuit — `https://github.com/slackhq/circuit`
- Metro — `https://github.com/ZacSweers/metro`
- Now in Android — `https://github.com/android/nowinandroid`
- JetNews — `https://github.com/android/compose-samples/tree/main/JetNews`
- Sunflower — `https://github.com/android/sunflower`
- Tivi — `https://github.com/chrisbanes/tivi`
- Compose Samples — `https://github.com/android/compose-samples`
