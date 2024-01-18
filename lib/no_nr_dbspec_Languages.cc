#include "no_nr_dbspec_Languages.h"
#include <jni.h>
#include <string.h>
#include <tree_sitter/api.h>

#ifdef TS_LANGUAGE_DBSPEC
extern "C" TSLanguage* tree_sitter_dbspec();
JNIEXPORT jlong JNICALL
Java_no_nr_dbspec_Languages_dbspec(JNIEnv* env, jclass self) {
  return (jlong)tree_sitter_dbspec();
}
#endif
