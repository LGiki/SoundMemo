#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cstdint>
#include <new>
#include <vector>

extern "C" {
#include "lame.h"
}

namespace {
constexpr const char* kTag = "SoundMemoLame";

lame_global_flags* fromHandle(jlong handle) {
    return reinterpret_cast<lame_global_flags*>(handle);
}

jbyteArray toByteArray(JNIEnv* env, const unsigned char* data, int size) {
    if (size <= 0) {
        return env->NewByteArray(0);
    }
    jbyteArray result = env->NewByteArray(size);
    if (result == nullptr) {
        return nullptr;
    }
    env->SetByteArrayRegion(result, 0, size, reinterpret_cast<const jbyte*>(data));
    return result;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_net_lgiki_soundmemo_service_audio_LameMp3Encoder_nativeInit(
    JNIEnv*,
    jobject,
    jint sampleRate,
    jint bitrateKbps
) {
    lame_global_flags* gfp = lame_init();
    if (gfp == nullptr) {
        return 0;
    }
    lame_set_in_samplerate(gfp, sampleRate);
    lame_set_out_samplerate(gfp, sampleRate);
    lame_set_num_channels(gfp, 1);
    lame_set_mode(gfp, MONO);
    lame_set_brate(gfp, bitrateKbps);
    lame_set_quality(gfp, 2);
    lame_set_write_id3tag_automatic(gfp, 0);
    if (lame_init_params(gfp) < 0) {
        lame_close(gfp);
        __android_log_write(ANDROID_LOG_ERROR, kTag, "lame_init_params failed");
        return 0;
    }
    return reinterpret_cast<jlong>(gfp);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_net_lgiki_soundmemo_service_audio_LameMp3Encoder_nativeEncode(
    JNIEnv* env,
    jobject,
    jlong handle,
    jshortArray pcm,
    jint sampleCount
) {
    lame_global_flags* gfp = fromHandle(handle);
    if (gfp == nullptr || pcm == nullptr || sampleCount <= 0) {
        return env->NewByteArray(0);
    }

    jshort* pcmData = env->GetShortArrayElements(pcm, nullptr);
    if (pcmData == nullptr) {
        return nullptr;
    }

    const int mp3BufferSize = static_cast<int>(1.25 * sampleCount) + 7200;
    std::vector<unsigned char> mp3Buffer(mp3BufferSize);
    int encoded = lame_encode_buffer(
        gfp,
        pcmData,
        pcmData,
        sampleCount,
        mp3Buffer.data(),
        mp3BufferSize
    );
    env->ReleaseShortArrayElements(pcm, pcmData, JNI_ABORT);

    if (encoded < 0) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "lame_encode_buffer failed: %d", encoded);
        return nullptr;
    }
    return toByteArray(env, mp3Buffer.data(), encoded);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_net_lgiki_soundmemo_service_audio_LameMp3Encoder_nativeFlush(
    JNIEnv* env,
    jobject,
    jlong handle
) {
    lame_global_flags* gfp = fromHandle(handle);
    if (gfp == nullptr) {
        return env->NewByteArray(0);
    }
    std::vector<unsigned char> mp3Buffer(7200);
    int encoded = lame_encode_flush(gfp, mp3Buffer.data(), static_cast<int>(mp3Buffer.size()));
    if (encoded < 0) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "lame_encode_flush failed: %d", encoded);
        return nullptr;
    }
    return toByteArray(env, mp3Buffer.data(), encoded);
}

extern "C" JNIEXPORT void JNICALL
Java_net_lgiki_soundmemo_service_audio_LameMp3Encoder_nativeClose(
    JNIEnv*,
    jobject,
    jlong handle
) {
    lame_global_flags* gfp = fromHandle(handle);
    if (gfp != nullptr) {
        lame_close(gfp);
    }
}
