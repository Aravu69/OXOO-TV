#include <jni.h>
#include <string>
#include <jni.h>

std::string SERVER_URL          = "https://dream-films.eu/rest-api/";
std::string API_KEY             = "74200fbdba5df4e";

std::string PURCHASE_CODE       = "0458eeb2-d84e-41bb-bffb-0c720eaca99b";


extern "C" jstring
Java_com_files_codes_AppConfig_getApiServerUrl(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SERVER_URL.c_str());
}

extern "C" jstring
Java_com_files_codes_AppConfig_getApiKey(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(API_KEY.c_str());
}

extern "C" jstring
Java_com_files_codes_AppConfig_getPurchaseCode(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(PURCHASE_CODE.c_str());
}