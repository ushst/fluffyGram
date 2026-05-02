#include <jni.h>
#include "third_party/bsdiff.c"

extern "C" {
    JNIEXPORT jint JNICALL
    Java_org_ushastoe_fluffy_utils_FluffyPatchUtils_applyPatchNative(JNIEnv *env, jclass clazz,
                                                                   jstring old_path,
                                                                   jstring new_path,
                                                                   jstring patch_path) {
        const char *oldPath = env->GetStringUTFChars(old_path, nullptr);
        const char *newPath = env->GetStringUTFChars(new_path, nullptr);
        const char *patchPath = env->GetStringUTFChars(patch_path, nullptr);

        int result = bspatch(oldPath, newPath, patchPath);

        env->ReleaseStringUTFChars(old_path, oldPath);
        env->ReleaseStringUTFChars(new_path, newPath);
        env->ReleaseStringUTFChars(patch_path, patchPath);

        return (jint)result;
    }
}
