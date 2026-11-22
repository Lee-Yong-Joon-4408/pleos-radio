# Pleos Radio

Pleos Connect(Android Automotive OS)용 라디오 앱. [pleos-steerin](https://github.com/Lee-Yong-Joon-4408/pleos-radio) fork의 빌드 구성·구조(Kotlin · Jetpack Compose · 서비스 로케이터 · 시뮬레이션 폴백)를 그대로 따르며, `docs/design_reference.png` 디자인을 **실행 시 메인 화면**으로 구현했습니다.

## 실행
```bash
./gradlew assembleDebug testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.pleos.radio.debug/com.pleos.radio.presentation.MainActivity
```
검증 환경: Pleos Connect 에뮬레이터 `Pleos_Connect_v2.1` (Android 14, 2560×1440) · JDK 17 · Gradle 8.10.2 · AGP 8.8.2 · Kotlin 2.0.21 · minSdk 28 / targetSdk 35

## 기능
| 화면 요소 | 동작 |
| :-- | :-- |
| DAB / DRM / AM 탭 | 밴드 전환, 밴드별 마지막 방송 복원 |
| ⏮ ⏭ | 이전/다음 방송 (DAB 서비스 / DRM·AM 탐색), 양 끝에서 순환 |
| ⏯ | 재생/일시정지 (오디오 포커스 연동) |
| ☆ | 즐겨찾기 토글 (기기에 저장) |
| 프리셋 1~12 | 탭: 선국 · 길게 누르기: 현재 방송 저장 · `>`: 7~12번 페이지 |
| Station List | 전체/즐겨찾기 필터, 밴드 스캔(진행 표시), 신호 세기, 목록에서 즐겨찾기 |
| 방송 정보 | DAB+ 배지, 채널·주파수, 프로그램명, DLS(라디오텍스트) 6초 순환 |
| 상단 바 | Radio › 밴드 경로, 실시간 시계 |
| 좌측 레일 | Home(라디오로 복귀), Media/Phone/Navigation(미니 플레이어), Settings(오디오 시뮬레이션 ON/OFF, 프리셋 초기화) |
| 스티어링 휠 미디어 키 | 다음/이전/재생·정지 |

화면은 1280×768 dp 디자인 공간을 실제 창 크기에 맞춰 균일하게 확대/축소하므로 해상도와 관계없이 같은 레이아웃이 유지됩니다.

## 튜너에 대해
서드파티 앱은 차량의 방송 라디오 HAL(`android.hardware.radio`, 시스템 API)에 접근할 수 없습니다. 그래서 `StationCatalog`(방송 목록)와 `SimulatedBroadcastAudio`(방송별 톤, 선국 시 잡음, AM/DRM 약전계 잡음)로 시뮬레이션합니다. 오디오는 `USAGE_MEDIA` 포커스를 요청하며, 영구 상실 시 일시정지, 일시 상실(내비 안내·통화) 시 중단 후 자동 재개, 덕킹 요청 시 볼륨을 낮춥니다. 실제 튜너가 생기면 이 두 클래스만 교체하면 됩니다.

## 구조
```
app/src/main/java/com/pleos/radio/
├─ RadioApplication.kt          서비스 로케이터
├─ domain/  Station.kt, RadioLogic.kt   (Android 비의존, 단위 테스트 대상)
├─ data/    StationCatalog.kt, RadioPreferences.kt
├─ audio/   RadioAudioOutput.kt, SimulatedBroadcastAudio.kt
└─ presentation/  MainActivity, RadioViewModel, ui/(RadioApp, RadioScreen, StationListPanel, Artwork, OtherScreens, theme)
```
