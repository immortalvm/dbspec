/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class no_nr_dbspec_TreeSitter */

#ifndef _Included_no_nr_dbspec_TreeSitter
#define _Included_no_nr_dbspec_TreeSitter
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    nodeChild
 * Signature: (Lno/nr/dbspec/Node;I)Lno/nr/dbspec/Node;
 */
JNIEXPORT jobject JNICALL Java_no_nr_dbspec_TreeSitter_nodeChild
  (JNIEnv *, jclass, jobject, jint);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    nodeChildCount
 * Signature: (Lno/nr/dbspec/Node;)I
 */
JNIEXPORT jint JNICALL Java_no_nr_dbspec_TreeSitter_nodeChildCount
  (JNIEnv *, jclass, jobject);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    nodeHasError
 * Signature: (Lno/nr/dbspec/Node;)I
 */
JNIEXPORT jboolean JNICALL Java_no_nr_dbspec_TreeSitter_nodeHasError
  (JNIEnv *, jclass, jobject);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    nodeEndByte
 * Signature: (Lno/nr/dbspec/Node;)I
 */
JNIEXPORT jint JNICALL Java_no_nr_dbspec_TreeSitter_nodeEndByte
  (JNIEnv *, jclass, jobject);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    nodeStartByte
 * Signature: (Lno/nr/dbspec/Node;)I
 */
JNIEXPORT jint JNICALL Java_no_nr_dbspec_TreeSitter_nodeStartByte
  (JNIEnv *, jclass, jobject);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    nodeString
 * Signature: (Lno/nr/dbspec/Node;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_no_nr_dbspec_TreeSitter_nodeString
  (JNIEnv *, jclass, jobject);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    nodeType
 * Signature: (Lno/nr/dbspec/Node;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_no_nr_dbspec_TreeSitter_nodeType
  (JNIEnv *, jclass, jobject);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    parserNew
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_no_nr_dbspec_TreeSitter_parserNew
  (JNIEnv *, jclass);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    parserDelete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_no_nr_dbspec_TreeSitter_parserDelete
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    parserSetLanguage
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_no_nr_dbspec_TreeSitter_parserSetLanguage
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    parserParseBytes
 * Signature: (J[BI)J
 */
JNIEXPORT jlong JNICALL Java_no_nr_dbspec_TreeSitter_parserParseBytes
  (JNIEnv *, jclass, jlong, jbyteArray, jint);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeCursorNew
 * Signature: (Lno/nr/dbspec/Node;)J
 */
JNIEXPORT jlong JNICALL Java_no_nr_dbspec_TreeSitter_treeCursorNew
  (JNIEnv *, jclass, jobject);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeCursorCurrentTreeCursorNode
 * Signature: (J)Lno/nr/dbspec/TreeCursorNode;
 */
JNIEXPORT jobject JNICALL Java_no_nr_dbspec_TreeSitter_treeCursorCurrentTreeCursorNode
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeCursorCurrentFieldName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_no_nr_dbspec_TreeSitter_treeCursorCurrentFieldName
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeCursorCurrentNode
 * Signature: (J)Lno/nr/dbspec/Node;
 */
JNIEXPORT jobject JNICALL Java_no_nr_dbspec_TreeSitter_treeCursorCurrentNode
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeCursorDelete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_no_nr_dbspec_TreeSitter_treeCursorDelete
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeCursorGotoFirstChild
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_no_nr_dbspec_TreeSitter_treeCursorGotoFirstChild
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeCursorGotoNextSibling
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_no_nr_dbspec_TreeSitter_treeCursorGotoNextSibling
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeCursorGotoParent
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_no_nr_dbspec_TreeSitter_treeCursorGotoParent
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeDelete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_no_nr_dbspec_TreeSitter_treeDelete
  (JNIEnv *, jclass, jlong);

/*
 * Class:     no_nr_dbspec_TreeSitter
 * Method:    treeRootNode
 * Signature: (J)Lno/nr/dbspec/Node;
 */
JNIEXPORT jobject JNICALL Java_no_nr_dbspec_TreeSitter_treeRootNode
  (JNIEnv *, jclass, jlong);

JNIEXPORT jobject JNICALL Java_no_nr_dbspec_TreeSitter_nodeChildByFieldName(
    JNIEnv* env, jclass self, jobject node, jbyteArray field_name_bytes, jint length);

#ifdef __cplusplus
}
#endif
#endif
