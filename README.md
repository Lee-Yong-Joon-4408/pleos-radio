# Pleos Radio

Pleos Connect(Android Automotive OS)용 라디오 앱. [pleos-steerin](https://github.com/Lee-Yong-Joon-4408/pleos-radio) fork의 빌드 구성·구조(Kotlin · Jetpack Compose · 서비스 로케이터 · 시뮬레이션 폴백)를 그대로 따르며, `docs/design_reference.png` 디자인을 **실행 시 메인 화면**으로 구현했습니다.

## 실행
```bash
./gradlew assembleDebug testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.pleos.radio.debug/com.pleos.radio.presentation.MainActivity
```
검증 환경: Pleos Connect 에뮬레이터 `Pleos_Connect_v2.1` (Android 14, 2560×1440, arm64) · JDK 17 · Gradle 8.10.2 · AGP 8.8.2 · Kotlin 2.0.21 · minSdk 28 / targetSdk 35 · **NDK 27.2.12479018 · CMake 3.22.1** (온디바이스 자막용 네이티브 빌드)

## 기능
| 화면 요소 | 동작 |
| :-- | :-- |
| FM / AM / DAB / DRM / ATSC 탭 | 밴드 전환, 밴드별 마지막 방송 복원 |
| ATSC (실제 방송) | 한국 지상파 TV 4채널 (KBS 1TV·2TV, EBS 1TV·2TV). **정차 중에는 실시간 영상 표시**(누르면 크게 보기), 주행 중에는 소리만 |
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
| 실시간 받아쓰기 (FM·AM) | 한국어 방송의 음성을 번역 없이 그대로 텍스트로 같은 5줄 패널에 표시(On/Off 메뉴). 엔진은 번역 자막과 동일 |
| 수신 경로 배경 · 하이브리드 전환 | 재생 중 배경에 수신 경로를 애니메이션으로 표시: IP망(인터넷 스트림)은 데이터 패킷이 네트워크를 거쳐 수신기로 들어오는 모습, 방송망(튜너)은 송신탑에서 전파가 퍼지는 모습. IP 스트림이 끊기면 방송망으로 자동 전환(설정에서 켤 때) 또는 무음 + "인터넷 연결 없음" 안내(기본), 복구되면 IP로 복귀 |
| 가상 RF 채널 (밴드별 3개) | 프리셋에 RF망으로 수신하는 가상 채널(`· RF`)을 추가. 선택하면 `LIVE · RF`와 전파 배경으로 RF 수신을 표시하고, 오디오는 같은 방송의 IP 스트림에서 가져옴 |
| 좌측 레일 | Home(라디오로 복귀), Radio, Media(미니 플레이어), Settings(자막 엔진·음성 모델·자막 언어·FM/AM 받아쓰기·프리셋 초기화) |
| 스티어링 휠 미디어 키 | 다음/이전/재생·정지 |

| 실시간 번역 자막 | DAB·DRM(영어·독일어·힌디어·벵골어·마라티어·구자라트어 방송)의 음성을 인식해 선택한 언어(한국어·English·日本語·中文·Off)로 번역, 메인 화면 하단 5줄 패널에 표시. 기본은 **무료 온디바이스**(whisper.cpp + ML Kit) |

화면은 1280×768 dp 디자인 공간을 실제 창 크기에 맞춰 균일하게 확대/축소하므로 해상도와 관계없이 같은 레이아웃이 유지됩니다.

## 오디오 소스
서드파티 앱은 차량의 방송 라디오 HAL(`android.hardware.radio`, 시스템 API)에 접근할 수 없습니다.

* **FM · AM: 실제 한국 방송.** 서울 FM 주파수 목록(89.1 KBS 쿨FM ~ 107.7 SBS 파워FM)과 AM 목록(603 KBS 해피FM ~ 1188 극동방송)에 각 방송사의 공식 인터넷 실시간 방송(HLS)을 연결해 Media3 ExoPlayer로 재생합니다. KBS·MBC·SBS는 유효기간이 있는 서명 URL을 쓰므로 재생할 때마다 방송사 API에서 새 URL을 받아옵니다(`StreamResolver`). 재생 오류가 나면 URL을 한 번 다시 받아 재시도합니다. 인터넷 연결이 필요하고, 일부 방송사는 해외 IP에서 재생을 막을 수 있습니다. 방송사가 API나 주소를 바꾸면 `StationCatalog`·`StreamResolver`를 고쳐야 합니다.
  AM은 전파가 아니라 같은 방송국의 인터넷 방송이므로 FM 동시방송과 같은 소리가 나옵니다(극동방송은 서울 FM 106.9 스트림 사용).
* **DAB: 영국 BBC National DAB.** 실제 BBC 전국 DAB 멀티플렉스(12B, 225.648 MHz)의 10개 서비스를 BBC 공식 HLS 스트림으로 재생합니다. **BBC World Service는 전 세계에서 재생**되지만, BBC는 Radio 1·2·3·4·5 Live·6 Music 등 국내 방송을 **영국 IP에만** 제공합니다. 영국 밖에서는 화면에 `not available in your region`이 표시되고 재시도하지 않습니다(지역 제한은 우회하지 않습니다). 국내 방송 주소는 BBC의 영국용 스트림 경로 규칙을 따른 것으로, 영국 밖에서는 CDN이 모든 경로를 403으로 막아 정상 여부를 확인할 수 없습니다.
* **DAB: 독일 DAB+ 10개.** Deutschlandfunk·Deutschlandfunk Kultur·Deutschlandfunk Nova(전국 Bundesmux 5C), 1LIVE·WDR 2(WDR 11D), Bayern 3(BR 11D), NDR 2(NDR 7A, 하노버), SWR3(SWR 9D, 슈투트가르트), hr3(hr 7B), radioeins(rbb 7D). 블록은 독일어 위키백과 「Liste der DAB-Sender in Deutschland」 기준이며, 공영방송 공식 MP3 스트림으로 **해외에서도 재생**됩니다.
* **DRM: 인도 All India Radio(Akashvani).** AIR의 DRM 중파 송신소 10곳(Mumbai B 549 Asmita, Patna 612, Kolkata A 648 Geetanjali, Lucknow 738, Chennai C 783 Vividh Bharati, Rajkot 801, New Delhi A 810 Indraprastha, Kolkata B 999 Sanchayita, Mumbai A 1044 Samvadita, New Delhi C 1368 Vividh Bharati)을 Prasar Bharati 공식 HLS 스트림으로 재생합니다(해외 재생 가능). DRM 주파수는 DRM.org 방송 일정(2026-10-26 갱신), 송신소별 채널은 VU2JOS Akashvani 주파수표 기준입니다. 783·1044·1368 kHz는 순수 DRM, 나머지는 시간대별 동시방송입니다.
* **ATSC: 한국 지상파 TV(오디오).** KBS 2TV(7-1, UHD RF 56), KBS 1TV(9-1, UHD RF 52), EBS 1TV(10-1, RF 18), EBS 2TV(10-2, RF 18)의 공식 실시간 TV 스트림에서 소리만 재생합니다(영상은 디코딩하지 않음). 채널 정보는 각 방송 위키백과 문서 기준이며, MBC·SBS는 외부 앱의 실시간 TV 스트림 이용을 허용하지 않아 제외했습니다.
* **ATSC 영상:** 정차 중(P)에는 Now Playing 자리에 16:9 실시간 영상을 표시하고, 영상을 누르면 크게 보기(다시 누르면 복귀)로 전환합니다. 차량의 운전자 주의 분산 제한(`CarUxRestrictionsManager`, `UX_RESTRICTIONS_NO_VIDEO`)이 걸리면(주행 중) 영상 대신 "Video is paused while driving" 안내를 보여 주고 소리는 계속 재생하며, 영상 디코딩도 멈춥니다. 차량이 아닌 기기에서는 항상 표시합니다.
* `SimulatedBroadcastAudio`(합성 톤)는 현재 쓰이지 않으며, 스트림이 없는 방송을 위한 대체 경로로 남겨 두었습니다.

`HybridRadioAudio`가 방송마다 두 경로 중 하나를 고르고, 미디어 오디오 포커스를 처리합니다(영구 상실 시 일시정지, 일시 상실 시 중단 후 재개, 덕킹).

## 수신 경로 표시와 하이브리드 전환
실제 차량용 하이브리드 라디오처럼 방송을 두 경로로 받습니다.

* **IP망:** 방송사 인터넷 스트림. 평소에는 이 경로로 재생하며, 상태 칩에 `LIVE · IP`, 배경에 네트워크 메시를 따라 데이터 패킷이 수신기로 흘러드는 애니메이션이 표시됩니다.
* **방송망:** 튜너. IP 스트림이 네트워크 문제로 끊기면(ExoPlayer 재시도 2회 후) 같은 방송을 튜너로 자동 전환하고 `LIVE · Broadcast · IP lost`와 송신탑에서 전파가 퍼지는 애니메이션을 보여 줍니다. 20초마다 IP 스트림에 접속해 보고, 되면 IP로 복귀합니다. 지역 제한(BBC 국내 방송 403)은 네트워크 장애가 아니므로 전환하지 않습니다.
* **Settings → Broadcast fallback (simulated tuner)** (기본 **Off**): 실제 튜너가 없는 동안에는 IP 스트림이 끊기면 합성 톤 대신 **무음 + "인터넷 연결 없음"** 큰 안내(방송 정보 자리, `Retry now` 버튼)를 보여 주고, 20초마다 다시 접속해 연결되면 자동으로 재생을 이어갑니다. On으로 바꾸면 위의 방송망(시뮬레이션 튜너) 전환을 사용합니다. 끊긴 상태에서 설정을 바꾸면 바로 적용됩니다.
* 서드파티 앱은 차량의 방송 라디오 HAL을 쓸 수 없어 현재 튜너는 `SimulatedBroadcastAudio`(합성 톤)입니다. HAL 기반 튜너로 교체하면 실제 방송 수신으로 바뀝니다.
* 에뮬레이터에서 비행기 모드로 확인: 네트워크 차단 약 12초 후 방송망 전환, 복구 약 18초 후 IP 복귀.

### 가상 RF 채널
밴드마다 3개씩, 기존 방송을 RF망으로 수신하는 것처럼 보여 주는 가상 채널이 있습니다(이름 끝 `· RF`, 프리셋 빈 칸에 자동 추가).

| 밴드 | 가상 RF 채널 |
| :-- | :-- |
| FM | KBS 쿨FM · RF, MBC FM4U · RF, SBS 파워FM · RF |
| AM | KBS 1라디오 · RF, CBS 표준FM · RF, 극동방송 · RF |
| DAB | BBC World Service · RF, Deutschlandfunk · RF, Bayern 3 · RF |
| DRM | AIR Delhi Indraprastha · RF, AIR Kolkata Geetanjali · RF, Vividh Bharati Delhi · RF |
| ATSC | KBS 1TV · RF, KBS 2TV · RF, EBS 1TV · RF |

선택하면 상태 칩 `LIVE · RF`와 송신탑 전파 배경으로 RF 수신을 표시하지만, 오디오는 원래 방송의 IP 스트림에서 가져옵니다(방송 정보 줄: "Virtual RF channel · audio relayed over IP"). 자막과 하이브리드 전환도 그대로 동작합니다.

### FM ↔ DAB(T-DMB) 동시방송 채널
같은 방송을 FM과 DAB/T-DMB로 동시에 내보내는 채널을 양쪽 밴드에 모두 두고, 프리셋에도 넣었습니다. 방송 제목 아래 줄에 "same as …"로 짝이 되는 채널을 표시합니다.

| 방송 | FM | DAB / T-DMB | 근거 |
| :-- | :-- | :-- | :-- |
| KBS 쿨FM = U-KBS MUSIC | 89.1 MHz (수도권) | T-DMB CH 12B (한국 채널표 207.008 MHz), 표시 `T-DMB` · `RF` | 위키백과 「KBS 제2FM」: U-KBS MUSIC이 쿨FM 프로그램 대부분을 재전송(2026-05 편성표) |
| hr3 (Rhein-Main) | 89.3 MHz (Großer Feldberg) | DAB+ 7B | hr3.de 주파수 목록 |
| SWR3 | 92.2 MHz (Sender Stuttgart) | DAB+ 9D | 독일어 위키백과 「SWR3」 |

U-KBS MUSIC은 RF(DMB) 수신으로 표시하고 소리는 같은 내용인 KBS 쿨FM IP 스트림에서 가져옵니다. 수도권 DMB의 MBC(올댓뮤직)·SBS V-Radio는 FM 동시방송이 아닌 자체 편성이라 넣지 않았습니다.

### IP ⇄ RF handover 데모 (SWR1 BW)
FM 목록과 프리셋에 **SWR1 Baden-Württemberg 94.7 MHz(Sender Stuttgart)** 를 추가했습니다. 이 채널을 선택하면 오디오가 재생되는 동안 **5초마다 수신 경로가 IP → RF → IP …로 바뀝니다.**

* 상태 표시가 `LIVE · IP ⇄ handover #n · seamless`와 `LIVE · RF ⇄ …`로 번갈아 바뀌고, 배경도 IP 패킷 애니메이션과 방송 전파 애니메이션 사이에서 전환됩니다.
* 두 경로가 같은 방송을 전달하므로, handover 때 플레이어를 멈추거나 다시 준비하지 않습니다. 그래서 오디오가 끊기지 않고(새 AudioTrack 생성 없음), 자막도 그대로 이어집니다.
* 실제 차량에서는 RF 튜너와 IP 스트림의 지연을 맞춘(time-aligned) 뒤에 넘겨야 같은 효과가 납니다. 지금은 RF 경로가 가상이라 소리는 IP 스트림에서 나옵니다.
* `adb logcat -s HybridRadioAudio`에서 `handover #n IP -> BROADCAST … (audio uninterrupted)` 로그로 확인할 수 있습니다.
* 재생 중 인터넷이 끊기면 handover를 멈추고, 기존 규칙대로 "인터넷 연결 없음"을 표시하거나 방송망으로 전환합니다.

## 실시간 번역 자막 (DAB · DRM)
재생 중인 오디오(ExoPlayer가 디코딩한 PCM)를 16 kHz 모노로 바꿔 약 6초 단위로 음성 인식 → 번역하고, 메인 화면 하단 5줄 패널에 최신 문장이 아래쪽에 오도록 표시합니다. 무음은 건너뛰고, 처리가 밀리면 가장 최근 구간만 처리합니다. 방송 언어: BBC en-GB, 독일 de-DE, AIR hi-IN / bn-IN / mr-IN / gu-IN.

FM·AM(한국어 방송)은 같은 패널에서 **번역 없이 받아쓰기만** 합니다(헤더 "Live transcript · 한국어", 오른쪽 메뉴는 On/Off, Settings → FM / AM transcript). 음악 위주 방송에서는 인식할 말이 적어 자막이 드물게 나옵니다.

### 기본: 온디바이스 (무료 · 오프라인)
* **음성 인식:** [whisper.cpp](https://github.com/ggml-org/whisper.cpp) v1.9.5 (MIT, CMake FetchContent로 릴리스 tarball을 SHA-256 검증 후 빌드, arm64-v8a). JNI: `app/src/main/cpp/whisper_jni.cpp`.
* **번역:** Google ML Kit Translation (무료, 기기 내 처리). Google Play 서비스가 없는 Pleos 에뮬레이터에서 동작 확인. 언어별 번역 모델(약 30 MB)은 처음 사용할 때 자동으로 내려받습니다.
* **음성 모델:** 앱에 포함하지 않고, 자막 패널의 Download 버튼이나 Settings에서 처음 한 번 내려받습니다(Hugging Face, SHA-256 검증). Tiny 32 MB · **Base 60 MB(기본·권장)** · Small 190 MB.
* **성능(Pleos 에뮬레이터, 2스레드):** Base 모델로 6초 음성을 약 2초에 처리(실시간의 약 3배). 영어·독일어는 쓸 만하고, 힌디어 등 인도 언어와 음악 위주 방송은 인식 품질이 낮습니다.

### 선택: Google Cloud (유료)
Settings → Live caption engine에서 Google Cloud를 고르면 Speech-to-Text + Cloud Translation을 씁니다. Speech-to-Text·Cloud Translation API를 켠 API 키를 `local.properties`의 `GOOGLE_CLOUD_API_KEY`나 Settings에 입력해야 하며, 사용량만큼 과금됩니다(1시간 연속 약 1~1.5달러 + 번역). 방송 오디오가 Google로 전송됩니다.

## 에뮬레이터 네트워크 자동 복구 (개발용)
Pleos 에뮬레이터는 비행기 모드를 켰다 끄거나 과부하가 걸린 뒤 **PC는 인터넷이 되는데 에뮬레이터만 네트워크를 잃는** 경우가 있습니다(기본 네트워크 없음). 그러면 IP 스트림을 받을 수 없어 실제 방송 대신 "인터넷 연결 없음"(또는 시뮬레이션 튜너)이 나옵니다. 앱은 에뮬레이터 안에서 시스템 권한 없이 실행되고 Pleos 이미지는 root를 허용하지 않아 앱이 직접 복구할 수 없으므로, **PC에서 실행하는 감시 스크립트**로 복구합니다.

```bash
./scripts/emulator-network-watchdog.sh            # 실행 중인 에뮬레이터 감시 (Ctrl+C로 종료)
./scripts/emulator-network-watchdog.sh --start    # 에뮬레이터가 없으면 먼저 Cold Boot로 실행
#   --avd NAME (기본 Pleos_Connect_v2.1) · --interval SEC (기본 15) · --once (한 번만 점검)
```

동작:
1. PC가 온라인인데 에뮬레이터에 기본 네트워크가 없거나 외부(8.8.8.8)에 닿지 않는 상태가 **두 번 연속** 확인되면 복구를 시작합니다. PC 자체가 오프라인이면 아무것도 하지 않습니다.
2. **1단계:** 에뮬레이터 재부팅(`adb reboot`) — 시험에서 약 15~20초 만에 네트워크가 돌아왔습니다.
3. **2단계:** 그래도 안 되면 AVD를 종료하고 Cold Boot(스냅샷 없이)로 다시 실행합니다.
4. 복구 전에 Pleos Radio가 실행 중이었으면, 운전자 사용자(Pleos는 user 10)가 준비된 뒤 앱을 다시 실행합니다.

확인 결과: 끊김 감지와 1·2단계 복구는 에뮬레이터에서 여러 번 동작했습니다. 앱 재실행은 스크립트에 앱 화면 이름을 잘못 적어 실패했던 문제를 고쳤으나(디버그 빌드는 패키지에만 `.debug`가 붙으므로 전체 클래스 이름 사용), **수정 후 재실행은 아직 시험하지 못했습니다.** 재실행되지 않으면 앱을 직접 실행하면 됩니다.

## 구조
```
app/src/main/cpp/                whisper.cpp 빌드(CMake) + JNI 브리지
scripts/emulator-network-watchdog.sh   에뮬레이터 네트워크 자동 복구(PC에서 실행)
app/src/main/java/com/pleos/radio/
├─ RadioApplication.kt          서비스 로케이터
├─ domain/  Station.kt, RadioLogic.kt, StreamSource.kt, caption/(Captions, PcmChunker)   (Android 비의존, 단위 테스트 대상)
├─ data/    StationCatalog.kt, RadioPreferences.kt, StreamResolver.kt,
│           translate/(CaptionEngines, WhisperNative, WhisperModelStore, OnDeviceTranslator, GoogleCloudClient)
├─ audio/   RadioAudioOutput.kt(AudioStatus, ReceptionPath), HybridRadioAudio.kt, StreamingRadioAudio.kt, PcmTap.kt, SimulatedBroadcastAudio.kt
└─ presentation/  MainActivity, RadioViewModel, LiveTranslator, ux/UxRestrictionsMonitor, ui/(RadioApp, RadioScreen, TvVideo, ReceptionBackdrop, CaptionPanel, StationListPanel, Artwork, OtherScreens, theme)
```
