cmake_minimum_required(VERSION 3.22.1)
project(pleos_whisper LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)

# whisper.cpp (MIT) pinned to a release tarball and verified by SHA-256.
include(FetchContent)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
if (ANDROID_ABI STREQUAL "arm64-v8a")
    # ARMv8.2 FP16 path (Snapdragon automotive SoCs and Apple-silicon-hosted emulators support it).
    set(GGML_CPU_ARM_ARCH "armv8.2-a+fp16" CACHE STRING "" FORCE)
endif ()
FetchContent_Declare(
    whisper
    URL https://github.com/ggml-org/whisper.cpp/archive/refs/tags/v1.9.5.tar.gz
    URL_HASH SHA256=ff1a9053feb509ff9d7729703355541ae9690073a6b1c40eb692c962e0dc1720
)
FetchContent_MakeAvailable(whisper)

add_library(pleos_whisper SHARED whisper_jni.cpp)
target_link_libraries(pleos_whisper PRIVATE whisper log android)
target_compile_options(pleos_whisper PRIVATE -O3 -fvisibility=hidden)
target_link_options(pleos_whisper PRIVATE -Wl,--gc-sections)
