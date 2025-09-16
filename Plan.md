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
- [x] A6. 디자인 규칙/용어집 문서 추가(`.cursor/rules/design.mdc`, `.cursor/rules/glossary.mdc`)
  - 보완 기록: 색상/간격/타임라인 UX/시트/이벤트 시그니처 규칙 정의. 모든 구현은 문서 우선 참조.

### B. 도메인/데이터
- [x] B1. 엔티티 정의: `VideoClip`, `Timeline`, `Overlay(Sticker, Subtitle, Music)`
- [x] B2. 로컬 미디어 소스 인터페이스(`MediaRepository`)와 Fake 구현
- [x] B3. 편집 모델 시리얼라이즈/디시리얼라이즈(로컬 저장)
  - 보완 기록: kotlinx-serialization 도입, 모델 @Serializable, 파일 기반 저장/불러오기 리포지토리 및 라운드트립 테스트 추가.
 - [ ] B4. 저장 콘텐츠 목록 리포지토리(프로젝트/내보낸 비디오) 및 홈 데이터 소스
   - 보완 기록: 기존 파일 기반 저장소에서 리스트 API 제공, 최근 수정 정렬, 삭제 API 초안.

### C. 유스케이스(UseCases)
- [x] C0. 임포트 플로우 유스케이스(선택→클립 변환→타임라인 생성)
- [x] C1. 영상 불러오기(use case)
- [x] C2. 타임라인 트림/분할/병합
- [x] C3. 스티커 추가/이동/삭제
  - 보완 기록: Presenter 삭제 API(`deleteSelectedOverlay`, `deleteOverlayById`), 시트 UI에 삭제 버튼 연결, 단위 테스트 추가
- [x] C4. 자막 추가/스타일 변경/삭제
- [x] C5. 음악 추가/볼륨 조절/삭제
  - 보완 기록: Presenter 음악 추가/편집 플로우 테스트(추가/업데이트), `updateMusicDraft`/`confirmOverlay` 경로 검증 완료
- [ ] C6. 렌더링(내보내기) 파이프라인 설계(MVP: 단순 병합)
  - 보완 기록: `ExportUseCase`/`FakeExportUseCase` 추가, `EditorPresenter`에 isExporting 토글 연동 및 단위 테스트.

### D. 화면(UI)
- [x] D0. 임포트 화면(디바이스 영상 그리드/다중 선택/확정)
  - 변경 기록: D1c 적용으로 홈에서 Photo Picker 직접 호출. Import 화면 및 경로 제거.
- [x] D1. 홈 스크린(Screen/Presenter/Ui) – 생성 목록/새 편집 버튼
- [ ] D1a. 홈 리스트를 "저장 완료 항목" 목록으로 전환(프로젝트/비디오)
  - 보완 기록: 빈 상태 UX, 최근 수정 기준 정렬, 삭제/편집 액션 노출.
- [ ] D1b. 홈 리스트 항목 클릭 시 저장된 편집 불러오기→에디터 진입
  - 보완 기록: `Navigator` 파라미터에 프로젝트/타임라인 ID 전달, Presenter에서 로드.
- [x] D1c. 홈 "+" 버튼 → 시스템 갤러리(Photo Picker/SAF) 직접 호출(Import 화면 우회)
  - 보완 기록: Home FAB에서 Photo Picker 실행 → 선택 URI로 `EditorScreen` 네비게이트. ImportScreen 제거.
- [x] D2. 에디터 스크린(Screen/Presenter/Ui) – 타임라인/오버레이 팔레트/프리뷰
- [x] D2a. 에디터 빈 상태 UX(Import CTA)
- [x] D2b. Import→Editor 결과 전달 및 연결
- [x] D3. 오버레이 선택/속성 편집 시트(Overlay/Sheet) – 시트 골격/드래프트/확인/취소 + UI 입력/드래그 연동 + 오디오 선택
  - 보완 기록: 스티커 에셋 선택 그리드(간이), 자막 스타일(사이즈/색상), 음악 소스 선택 연동, 타임라인 드래그 핸들 클램프. 오버레이는 서로 겹침 허용(동시 표시 가능)
  - 추가 보완: 오버레이 리스트 트랙 UX 개선(어두운 트랙/선택 노란 테두리), 선택 시 가로 스크롤 비활성화 및 좌/우 핫스팟/트랙 내부 드래그 동작(길이 조절/전체 이동) 지원. 클릭 시 편집 시트 재오픈. 신규 추가 버튼 클릭 시 기존 선택 해제 후 신규 추가 플로우 유지, 선택 상태에서 확인 시 기존 오버레이 업데이트.
- [x] D4. 프리뷰/재생/정지, 썸네일 스트립, 플레이헤드 실선(기본)
  - 완료: Media3 ExoPlayer 재생/정지, 드래그 시 일시정지·시킹, 플레이헤드 고정, 썸네일은 유효 너비·길이 기반 frameCount로 등분 추출
- [ ] D4a. 썸네일 캐시/비동기 최적화(메모리 상한/전처리)
- [ ] D4b. 자동 스크롤/플레이헤드 가시성 유지(긴 타임라인)
- [ ] D4c. 타임라인 트리밍/오버레이 고급 UI 재도입(간소 버전 제거됨)
- [ ] D4d. 확대/축소 설계(썸네일/오버레이 동기)
- [ ] D4e. 트랙 헤더/고정 사이드바
- [ ] D4f. 트랜스포트 보강(이전/다음, 시간 표시)
- [ ] D4g. 스냅/마그넷 정밀화
- [ ] D4h. 선택/포커스 시각화 개선
- [ ] D4i. 성능: 썸네일/웨이브폼 캐시 및 비동기 로딩

### E. 테스트
- [x] E1. Presenter 유닛 테스트 템플릿 구축
- [~] E2. UseCase 유닛 테스트(경계/에러 포함) – C2 경계/에러 보강
- [x] E3. Compose UI 테스트(세맨틱스 기반)
  - 보완 기록: 에디터 오버레이 선택/드래그/삭제 시나리오 UI 테스트 추가(`EditorUiTest`). 테스트 태깅(`testTag`)을 `EditorUi`에 도입해 안정적인 선택자 확보(팔레트 버튼/시트/트랙 범위/핸들). `:app:assembleDebugAndroidTest` 통과 확인.
  - 문서 동기화: `.cursor/rules/testing.mdc`에 UI testTag 가이드와 given_when_then 네이밍 통합 명시.
- [ ] E4. 통합 테스트: 가짜 미디어 저장소 + 단순 렌더링
- [ ] E5. 임포트 화면/권한/유스케이스 테스트
  - 보완 기록: 홈 "+"→Photo Picker e2e, 홈 리스트 클릭→Editor 로드 UI 테스트 추가.
- [ ] E6. 플레이백/타임라인 UI 테스트: 자동 스크롤/시크/트리밍/스냅/플레이헤드 가시성

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
