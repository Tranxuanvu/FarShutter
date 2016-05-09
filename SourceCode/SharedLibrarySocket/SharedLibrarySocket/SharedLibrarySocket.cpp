#include "SharedLibrarySocket.h"

SharedLibrarySocket *lib = NULL;

extern "C" {
	/* This trivial function returns the platform ABI for which this dynamic native library is compiled.*/
	const char * SharedLibrarySocket::getPlatformABI()
	{
#if defined(__arm__)
#if defined(__ARM_ARCH_7A__)
#if defined(__ARM_NEON__)
#define ABI "armeabi-v7a/NEON"
#else
#define ABI "armeabi-v7a"
#endif
#else
#define ABI "armeabi"
#endif
#elif defined(__i386__)
#define ABI "x86"
#else
#define ABI "unknown"
#endif
		LOGI("This dynamic shared library is compiled with ABI: %s", ABI);
		return "This native library is compiled with ABI: %s" ABI ".";
	}

	JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) /* when the native library is loaded (for example, through System.loadLibrary)*/
	{
		JNIEnv *env;
		s_Jvm = vm;
		if (s_Jvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
			LOGE("Failed to get the environment using GetEnv()");
			return -1;
		}
		initClassHelper(env, kInterfacePath, &gCallbackObject);
		LOGI("INIT DONE");

		return JNI_VERSION_1_6;
	}

	//JNIEXPORT jstring JNICALL Java_com_bk_hellocpp_demolibapplication_MainActivity_initClient(JNIEnv* env, jobject thiz, jstring myURL) {
	//	if (lib == NULL) lib = new SharedLibrarySocket();

	//	const char *nativeString = env->GetStringUTFChars(myURL, JNI_FALSE);
	//	LOGI("NativeString: %s", nativeString);
	//	lib->connectToServer(nativeString);

	//	return env->NewStringUTF("Connected!");
	//}

	//JNIEXPORT jstring JNICALL Java_com_bk_hellocpp_demolibapplication_CameraActivity_createServer(JNIEnv* env, jobject thiz) {
	//	if (lib == NULL) lib = new SharedLibrarySocket();
	//	lib->createSocketServer();
	//	return env->NewStringUTF("Created!");
	//}

	//JNIEXPORT jstring JNICALL Java_com_bk_hellocpp_demolibapplication_MainActivity_sendImageData(JNIEnv* env, jobject thiz, jbyteArray data) {
	//	if (lib != NULL) {
	//		/* init send data */
	//		int lenArr = env->GetArrayLength(data);
	//		int len = strlen(IMG_OPEN) + lenArr + strlen(IMG_CLOSE);
	//		jbyte * arrData = env->GetByteArrayElements(data, JNI_FALSE);
	//		jbyte * dataImage = new jbyte[len];
	//		memcpy(dataImage, IMG_OPEN, strlen(IMG_OPEN));
	//		memcpy(dataImage + strlen(IMG_OPEN), arrData, lenArr);
	//		memcpy(dataImage + lenArr + strlen(IMG_OPEN), IMG_CLOSE, strlen(IMG_CLOSE));

	//		lib->sendByteMessage(dataImage, len);

	//		free(dataImage);
	//		dataImage = NULL;
	//		env->ReleaseByteArrayElements(data, arrData, JNI_ABORT);
	//		env->DeleteLocalRef(data);

	//	}
	//	return env->NewStringUTF("Send!");
	//}

	// com.vinhle.mycamera CameraActivity
	JNIEXPORT jstring JNICALL Java_com_thuantan_farshutter_Services_CameraService_createServer(JNIEnv* env, jobject thiz, jint port) {
		if (lib == NULL) lib = new SharedLibrarySocket();

		lib->createSocketServer(port);

		return env->NewStringUTF("Created!");
	}

	JNIEXPORT jstring JNICALL Java_com_thuantan_farshutter_Services_CameraService_initClient(JNIEnv* env, jobject thiz, jstring myURL, jint port) {
		if (lib == NULL) lib = new SharedLibrarySocket();

		const char *nativeString = env->GetStringUTFChars(myURL, JNI_FALSE);
		LOGI("NativeString: %s", nativeString);
		lib->connectToServer(nativeString, port,0);
		env->ReleaseStringUTFChars(myURL, nativeString);

		return env->NewStringUTF("Connected!");
	}

	JNIEXPORT jstring JNICALL Java_com_thuantan_farshutter_Services_CameraService_sendStreamData(JNIEnv* env, jobject thiz, jbyteArray data) {
		if (lib != NULL) {
			/* init send data */
			int lenArr = env->GetArrayLength(data);
			int len = strlen(STREAM_OPEN) + lenArr + strlen(STREAM_CLOSE);
			jbyte * arrData = env->GetByteArrayElements(data, JNI_FALSE);
			jbyte * dataStream = new jbyte[len];
			memcpy(dataStream, STREAM_OPEN, strlen(STREAM_OPEN));
			memcpy(dataStream + strlen(STREAM_OPEN), arrData, lenArr);
			memcpy(dataStream + strlen(STREAM_OPEN) + lenArr, STREAM_CLOSE, strlen(STREAM_CLOSE));

			lib->sendByteMessage(dataStream, len);

			free(dataStream);
			dataStream = NULL;
			env->ReleaseByteArrayElements(data, arrData, JNI_ABORT);
			// env->DeleteLocalRef(data);
		}
		return env->NewStringUTF("Send!");
	}
	
	JNIEXPORT jstring JNICALL Java_com_thuantan_farshutter_Services_CameraService_sendImageData(JNIEnv* env, jobject thiz, jbyteArray data) {
		if (lib != NULL) {
			/* init send data */
			int lenArr = env->GetArrayLength(data);
			int len = strlen(IMG_OPEN) + lenArr + strlen(IMG_CLOSE);
			jbyte * arrData = env->GetByteArrayElements(data, JNI_FALSE);
			jbyte * dataImage = new jbyte[len];
			memcpy(dataImage, IMG_OPEN, strlen(IMG_OPEN));
			memcpy(dataImage + strlen(IMG_OPEN), arrData, lenArr);
			memcpy(dataImage + strlen(IMG_OPEN) + lenArr, IMG_CLOSE, strlen(IMG_CLOSE));

			lib->sendByteMessage(dataImage, len);

			free(dataImage);
			dataImage = NULL;
			env->ReleaseByteArrayElements(data, arrData, JNI_ABORT);
			// env->DeleteLocalRef(data);
		}
		return env->NewStringUTF("Send!");
	}

	JNIEXPORT jstring JNICALL Java_com_thuantan_farshutter_Services_CameraService_sendCodeData(JNIEnv* env, jobject thiz, jbyteArray data) {
		if (lib != NULL) {
			/* init send data */
			int lenArr = env->GetArrayLength(data);
			int len = strlen(CODE_OPEN) + lenArr + strlen(CODE_CLOSE);
			jbyte * arrData = env->GetByteArrayElements(data, JNI_FALSE);
			jbyte * dataImage = new jbyte[len];
			memcpy(dataImage, CODE_OPEN, strlen(CODE_OPEN));
			memcpy(dataImage + strlen(CODE_OPEN), arrData, lenArr);
			memcpy(dataImage + strlen(CODE_OPEN) + lenArr, CODE_CLOSE, strlen(CODE_CLOSE));

			lib->sendByteMessage(dataImage, len);

			free(dataImage);
			dataImage = NULL;
			env->ReleaseByteArrayElements(data, arrData, JNI_ABORT);
			env->DeleteLocalRef(data);
		}

		return env->NewStringUTF("Send!");
	}

	JNIEXPORT jstring JNICALL Java_com_thuantan_farshutter_Services_CameraService_closeSocket(JNIEnv* env, jobject thiz) {
		if (lib != NULL) {
			lib->closeServer();
			lib->closeClient();

			delete lib;
			lib = NULL;
		}
		return env->NewStringUTF("Close!");
	}
}
