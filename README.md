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
| FM / AM / DAB / DRM / ATSC 탭 | 밴드 전환, 밴드별 마지막 방송 복원 |
| DRM (실제 방송) | 인도 All India Radio DRM 송신소 10곳 (해외 재생) |
| DAB (실제 방송) | 영국 BBC National DAB 10개(World Service만 해외 재생) + 독일 DAB+ 10개(해외 재생) |
| FM · AM (실제 방송) | 서울 FM 12개 채널(KBS·MBC·SBS·CBS·EBS·YTN)과 AM 6개 채널(KBS 해피FM 603·1라디오 711·한민족방송 972·3라디오 1134, CBS 837, 극동방송 1188)의 공식 인터넷 실시간 방송 재생, `LIVE`/`Connecting…` 표시, 오류 시 `Retry` |
| ⏮ ⏭ | 이전/다음 방송 (DAB 서비스 / FM·AM·DRM·ATSC 탐색), 양 끝에서 순환 |
| ⏯ | 재생/일시정지 (오디오 포커스 연동) |
| ☆ | 즐겨찾기 토글 (기기에 저장) |
| 프리셋 1~36 | 탭: 선국 · 길게 누르기: 현재 방송 저장 · 6개씩 6페이지, `>` 또는 좌우 스와이프로 페이지 이동(`1 / 6` 표시) |
| Station List | 전체/즐겨찾기 필터, 밴드 스캔(진행 표시), 신호 세기, 목록에서 즐겨찾기 |
| 방송 정보 | DAB+ 배지, 채널·주파수, 프로그램명, DLS(라디오텍스트) 6초 순환 |
| 상단 바 | Radio › 밴드 경로, 실시간 시계 |
| 좌측 레일 | Home(라디오로 복귀), Media/Phone/Navigation(미니 플레이어), Settings(ATSC 시뮬레이션 오디오 ON/OFF, 프리셋 초기화) |
| 스티어링 휠 미디어 키 | 다음/이전/재생·정지 |

화면은 1280×768 dp 디자인 공간을 실제 창 크기에 맞춰 균일하게 확대/축소하므로 해상도와 관계없이 같은 레이아웃이 유지됩니다.

## 오디오 소스
서드파티 앱은 차량의 방송 라디오 HAL(`android.hardware.radio`, 시스템 API)에 접근할 수 없습니다.

* **FM · AM: 실제 한국 방송.** 서울 FM 주파수 목록(89.1 KBS 쿨FM ~ 107.7 SBS 파워FM)과 AM 목록(603 KBS 해피FM ~ 1188 극동방송)에 각 방송사의 공식 인터넷 실시간 방송(HLS)을 연결해 Media3 ExoPlayer로 재생합니다. KBS·MBC·SBS는 유효기간이 있는 서명 URL을 쓰므로 재생할 때마다 방송사 API에서 새 URL을 받아옵니다(`StreamResolver`). 재생 오류가 나면 URL을 한 번 다시 받아 재시도합니다. 인터넷 연결이 필요하고, 일부 방송사는 해외 IP에서 재생을 막을 수 있습니다. 방송사가 API나 주소를 바꾸면 `StationCatalog`·`StreamResolver`를 고쳐야 합니다.
  AM은 전파가 아니라 같은 방송국의 인터넷 방송이므로 FM 동시방송과 같은 소리가 나옵니다(극동방송은 서울 FM 106.9 스트림 사용).
* **DAB: 영국 BBC National DAB.** 실제 BBC 전국 DAB 멀티플렉스(12B, 225.648 MHz)의 10개 서비스를 BBC 공식 HLS 스트림으로 재생합니다. **BBC World Service는 전 세계에서 재생**되지만, BBC는 Radio 1·2·3·4·5 Live·6 Music 등 국내 방송을 **영국 IP에만** 제공합니다. 영국 밖에서는 화면에 `not available in your region`이 표시되고 재시도하지 않습니다(지역 제한은 우회하지 않습니다). 국내 방송 주소는 BBC의 영국용 스트림 경로 규칙을 따른 것으로, 영국 밖에서는 CDN이 모든 경로를 403으로 막아 정상 여부를 확인할 수 없습니다.
* **DAB: 독일 DAB+ 10개.** Deutschlandfunk·Deutschlandfunk Kultur·Deutschlandfunk Nova(전국 Bundesmux 5C), 1LIVE·WDR 2(WDR 11D), Bayern 3(BR 11D), NDR 2(NDR 7A, 하노버), SWR3(SWR 9D, 슈투트가르트), hr3(hr 7B), radioeins(rbb 7D). 블록은 독일어 위키백과 「Liste der DAB-Sender in Deutschland」 기준이며, 공영방송 공식 MP3 스트림으로 **해외에서도 재생**됩니다.
* **DRM: 인도 All India Radio(Akashvani).** AIR의 DRM 중파 송신소 10곳(Mumbai B 549 Asmita, Patna 612, Kolkata A 648 Geetanjali, Lucknow 738, Chennai C 783 Vividh Bharati, Rajkot 801, New Delhi A 810 Indraprastha, Kolkata B 999 Sanchayita, Mumbai A 1044 Samvadita, New Delhi C 1368 Vividh Bharati)을 Prasar Bharati 공식 HLS 스트림으로 재생합니다(해외 재생 가능). DRM 주파수는 DRM.org 방송 일정(2026-10-26 갱신), 송신소별 채널은 VU2JOS Akashvani 주파수표 기준입니다. 783·1044·1368 kHz는 순수 DRM, 나머지는 시간대별 동시방송입니다.
* **ATSC: 시뮬레이션.** `SimulatedBroadcastAudio`가 방송별 톤, 선국 시 잡음, 약전계 잡음을 만듭니다(ATSC 채널은 가상 서비스).

`HybridRadioAudio`가 방송마다 두 경로 중 하나를 고르고, 미디어 오디오 포커스를 처리합니다(영구 상실 시 일시정지, 일시 상실 시 중단 후 재개, 덕킹).

## 구조
```
app/src/main/java/com/pleos/radio/
├─ RadioApplication.kt          서비스 로케이터
├─ domain/  Station.kt, RadioLogic.kt, StreamSource.kt   (Android 비의존, 단위 테스트 대상)
├─ data/    StationCatalog.kt, RadioPreferences.kt, StreamResolver.kt
├─ audio/   RadioAudioOutput.kt, HybridRadioAudio.kt, StreamingRadioAudio.kt, SimulatedBroadcastAudio.kt
└─ presentation/  MainActivity, RadioViewModel, ui/(RadioApp, RadioScreen, StationListPanel, Artwork, OtherScreens, theme)
```
