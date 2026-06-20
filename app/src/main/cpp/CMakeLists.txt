cmake_minimum_required(VERSION 3.22.1)

project(soundmemo_audio)

set(LAME_DIR ${CMAKE_CURRENT_SOURCE_DIR}/lame)

add_library(
    mp3lame
    STATIC
    ${LAME_DIR}/libmp3lame/VbrTag.c
    ${LAME_DIR}/libmp3lame/bitstream.c
    ${LAME_DIR}/libmp3lame/encoder.c
    ${LAME_DIR}/libmp3lame/fft.c
    ${LAME_DIR}/libmp3lame/gain_analysis.c
    ${LAME_DIR}/libmp3lame/id3tag.c
    ${LAME_DIR}/libmp3lame/lame.c
    ${LAME_DIR}/libmp3lame/mpglib_interface.c
    ${LAME_DIR}/libmp3lame/newmdct.c
    ${LAME_DIR}/libmp3lame/presets.c
    ${LAME_DIR}/libmp3lame/psymodel.c
    ${LAME_DIR}/libmp3lame/quantize.c
    ${LAME_DIR}/libmp3lame/quantize_pvt.c
    ${LAME_DIR}/libmp3lame/reservoir.c
    ${LAME_DIR}/libmp3lame/set_get.c
    ${LAME_DIR}/libmp3lame/tables.c
    ${LAME_DIR}/libmp3lame/takehiro.c
    ${LAME_DIR}/libmp3lame/util.c
    ${LAME_DIR}/libmp3lame/vbrquantize.c
    ${LAME_DIR}/libmp3lame/version.c
)

target_compile_definitions(mp3lame PRIVATE HAVE_CONFIG_H)
target_include_directories(
    mp3lame
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${LAME_DIR}/include
    ${LAME_DIR}/libmp3lame
)

add_library(soundmemo_lame SHARED soundmemo/lame_jni.cpp)
target_include_directories(soundmemo_lame PRIVATE ${LAME_DIR}/include)
target_link_libraries(soundmemo_lame PRIVATE mp3lame log m)
