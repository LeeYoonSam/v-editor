# 비디오 편집기 앱 계획(Plan)

MVP 단계에서 서버 없이(클라이언트 전용) 구현합니다. 모든 항목은 작은 단위 작업과 TDD를 전제로 합니다.

## 0. 목표/범위
- 목표: 로컬 영상 선택 → 타임라인 구성(트림/분할) → 스티커/자막/음악 오버레이 → 파일로 내보내기
- 비범위: 온라인 동기화, 계정/로그인, 고급 이펙트, 클라우드 렌더링

## 1. 아키텍처 결론
- UI/네비게이션/상태: Slack Circuit(MVI/UDF 스타일)
- 비동기: Kotlin Coroutines/Flow
- 데이터: 로컬 파일 시스템/MediaStore 중심, 단순 메타데이터 저장(Preferences/파일)
- 모듈화: 단일 모듈 시작, 후속 분리 계획 수립

## 2. 참고 레퍼런스
- Now in Android(모듈화, 아키텍처 베스트 프랙티스) — `https://github.com/android/nowinandroid`
- JetNews(Compose UI 패턴) — `https://github.com/android/compose-samples/tree/main/JetNews`
- Sunflower(Jetpack 전반 활용) — `https://github.com/android/sunflower`
- Tivi(Compose + Coroutines + 아키텍처) — `https://github.com/chrisbanes/tivi`
- Compose Samples(컴포넌트/패턴) — `https://github.com/android/compose-samples`

## 3. 작업 보드(TODO)

### A. 프로젝트/도구
- [x] A0. 버전 카탈로그 구성 및 Compose/Circuit 의존성 추가
- [ ] A1. Circuit/Metro 도입 전략서 확정 및 의존성 스니펫 준비
- [~] A2. 코드 스타일/린트/테스트 툴링 확정(Spotless/Detekt/JUnit/Compose test)
- [x] A3. 베이스 네비게이션 구성(Circuit `Navigator`/`BackStack`)
- [x] A4. 매니페스트 런처 액티비티/Compose 스켈레톤 추가(초안)
- [~] A5. 미디어 권한 전략(API33+ Photo Picker 우선, 필요 시 READ_MEDIA_VIDEO)

### B. 도메인/데이터
- [x] B1. 엔티티 정의: `VideoClip`, `Timeline`, `Overlay(Sticker, Subtitle, Music)`
- [x] B2. 로컬 미디어 소스 인터페이스(`MediaRepository`)와 Fake 구현
- [ ] B3. 편집 모델 시리얼라이즈/디시리얼라이즈(로컬 저장)

### C. 유스케이스(UseCases)
- [x] C0. 임포트 플로우 유스케이스(선택→클립 변환→타임라인 생성)
- [x] C1. 영상 불러오기(use case)
- [x] C2. 타임라인 트림/분할/병합
- [ ] C3. 스티커 추가/이동/삭제
- [ ] C4. 자막 추가/스타일 변경/삭제
- [ ] C5. 음악 추가/볼륨 조절/삭제
- [ ] C6. 렌더링(내보내기) 파이프라인 설계(MVP: 단순 병합)

### D. 화면(UI)
- [x] D0. 임포트 화면(디바이스 영상 그리드/다중 선택/확정)
- [x] D1. 홈 스크린(Screen/Presenter/Ui) – 생성 목록/새 편집 버튼
- [x] D2. 에디터 스크린(Screen/Presenter/Ui) – 타임라인/오버레이 팔레트/프리뷰
- [x] D2a. 에디터 빈 상태 UX(Import CTA)
- [x] D2b. Import→Editor 결과 전달 및 연결
- [x] D3. 오버레이 선택/속성 편집 시트(Overlay/Sheet) – 시트 골격/드래프트/확인/취소 + UI 입력/드래그 연동 + 오디오 선택
  - 보완 기록: 스티커 에셋 선택 그리드(간이), 스티커/자막 드래그 이동, 자막 스타일(사이즈/색상), 음악 소스 선택 연동, 타임라인 드래그 핸들 클램프. 오버레이는 서로 겹침 허용(동시 표시 가능)

### E. 테스트
- [x] E1. Presenter 유닛 테스트 템플릿 구축
- [~] E2. UseCase 유닛 테스트(경계/에러 포함) – C2 경계/에러 보강
- [ ] E3. Compose UI 테스트(세맨틱스 기반)
- [ ] E4. 통합 테스트: 가짜 미디어 저장소 + 단순 렌더링
- [ ] E5. 임포트 화면/권한/유스케이스 테스트

### F. 내보내기/성능
- [ ] F1. 기본 렌더 파이프라인 초안(MediaCodec/Muxer 조사, 임시 모의)
- [ ] F2. 썸네일/시크바 프리뷰 최적화

## 4. TDD 작업 흐름(공통)
1) 실패 테스트 작성 → 2) 최소 구현으로 통과 → 3) 리팩토링/최적화 → 4) 문서/샘플 업데이트

## 5. 리스크/결정 대기
- MediaCodec 실구현 난이도 높음: MVP는 메타데이터 기반 미리보기 + 내보내기 최소화
- Metro 적용 범위: DI/코드 생성은 후속, 초기엔 수동 DI

## 6. 의존성 초안(문서용)
- Circuit: `circuit-runtime`, `circuit-runtime-presenter`, `circuit-runtime-ui`, `circuit-backstack`(+`circuit-overlay` 선택)
- Metro: DI/컴파일러 플러그인 평가 후 적용 범위 확정

## 7. 마일스톤
- M1: 홈 스크린 TDD 완료(리스트/네비)
- M2: 에디터 타임라인 최소기능(TDD) + 오버레이 추가
- M3: 내보내기 MVP + 성능 기본 최적화
