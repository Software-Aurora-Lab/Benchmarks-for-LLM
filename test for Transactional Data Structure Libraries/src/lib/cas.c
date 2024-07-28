#include <jni.h>
#include <stdbool.h>

static inline bool compareAndSwap(volatile jobject *addr, jobject old, jobject new_val) {
    bool result = false;
    // Spinlock can be added here for thread-safety if needed
    if ((result = (*addr == old))) {
        *addr = new_val;
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_com_example_CAS_compareAndSwapObject
  (JNIEnv *env, jobject thisObj, jobject obj, jlong offset, jobject expect, jobject update) {
    jobject *addr = (jobject *)((char *) obj + offset);
    return compareAndSwap(addr, expect, update);
}
