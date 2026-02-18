# One Day Pomodoro (원데이 뽀모도로)

하루의 시작과 끝을 함께하는 효율적인 시간 관리 도구입니다. **One Day Pomodoro**는 집중과 휴식의 적절한 조화를 통해 사용자의 생산성을 극대화할 수 있도록 설계된 Android 애플리케이션입니다.

## 🌟 주요 기능

- **정밀한 뽀모도로 타이머**: 집중 시간(Focus)과 휴식 시간(Break)의 사이클을 체계적으로 관리합니다.
- **백그라운드 지속성**: Foreground Service를 통해 앱이 백그라운드에 있거나 화면이 꺼진 상태에서도 타이머가 중단 없이 정확하게 작동합니다.
- **일일 성과 요약**: 당일 완료한 집중 시간과 세션 수를 한눈에 확인하여 매일의 성취를 기록합니다.
- **목표 설정 (Purpose)**: 세션을 시작하기 전, 집중할 목표를 설정하여 몰입감을 높여줍니다.
- **세션 자동 루프**: 유연한 설정에 따라 집중과 휴식 사이클을 자동으로 반복할 수 있습니다.
- **다국어 지원**: 한국어를 포함한 다국어 환경을 지원하여 전 세계 어디서나 편리하게 사용 가능합니다.
- **광고 통합 (AdMob)**: 지속적인 서비스 제공을 위해 세션 완료 시 또는 적절한 시점에 광고를 노출합니다.

## 🛠 기술 스택

- **Programming Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (완전 선언형 UI)
- **Architecture**: **Clean Architecture with Multi-Module**
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Asynchronous Programming**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Data Storage**: 
  - **Room**: 일별 기록 등 정형 데이터 저장
  - **DataStore**: 사용자 설정 등 간단한 Key-Value 데이터 저장
- **Navigation**: [Jetpack Navigation Component](https://developer.android.com/guide/navigation) (Compose Navigation)
- **Image/Animation**: Lottie (선택적 사용)
- **Ads Integration**: Google Mobile Ads (AdMob)

## 📁 프로젝트 구조 (Multi-Module)

이 프로젝트는 관심사 분리(SoC)와 테스트 용이성을 극대화하기 위해 4가지 핵심 모듈로 나뉘어 있습니다.

- **`:app`**: 애플리케이션의 진입점. Hilt 모듈 설정, `Application` 클래스, 그리고 타이머를 유지하는 `TimerService`가 포함되어 있습니다.
- **`:presentation`**: Jetpack Compose 기반의 UI 레이어입니다. 각 화면(Home, Timer, Break, Summary, Settings)과 각 화면의 상태를 관리하는 `ViewModel`이 위치합니다.
- **`:domain`**: 앱의 비즈니스 로직이 담긴 곳입니다. 순수 Kotlin 모듈로, `UseCase`와 `Entity`, 그리고 Interface 정의가 포함됩니다. (외부 프레임워크 의존성 없음)
- **`:data`**: 데이터 소스와의 상호작용을 담당합니다. `Repository`의 구체적인 구현체와 Room Database, DataStore 접근 로직이 위치합니다.

## ⚙️ 설정 및 실행 방법

1.  **Repository Clone**
    ```bash
    git clone https://github.com/your-repo/one_day_pomodoro.git
    ```
2.  **Open Project**
    Android Studio (Koala 이상 권장)에서 프로젝트를 엽니다.
3.  **Gradle Sync**
    `libs.versions.toml`을 기반으로 의존성 동기화를 진행합니다.
4.  **Run Application**
    `:app` 모듈을 타겟으로 빌드 및 실행합니다. (최소 SDK: API 26+)

## 🤝 기여 방법

프로젝트의 규칙과 아키텍처 가이드를 준수해 주십시오. 
- UI 변경 시에는 반드시 **Jetpack Compose**를 사용합니다.
- 새로운 비즈니스 로직 추가 시 **domain** 모듈의 UseCase를 통해 구현합니다.
- 모든 기능은 **Clean Architecture**의 단방향 데이터 흐름을 따릅니다.
