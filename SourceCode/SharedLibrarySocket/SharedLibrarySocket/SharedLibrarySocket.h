#pragma once

#pragma region define varible

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "SharedLibrarySocket", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "SharedLibrarySocket", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "SharedLibrarySocket", __VA_ARGS__))

#define MAX_SIZE_BUFFER 3072
#define MAX_LENGTH_IMAGE 1000000
#define MAX_LENGTH_STREAM 1000000
#define MAX_LENGTH_CODE 3072

#define CODE_ERROR -1

#define IMG_OPEN "<image>"
#define IMG_CLOSE "</image>"
#define STREAM_OPEN "<stream>"
#define STREAM_CLOSE "</stream>"
#define CODE_OPEN "<code>"
#define CODE_CLOSE "</code>"

#define CODE_IMG_OPEN 1
#define CODE_IMG_CLOSE 2
#define CODE_STREAM_OPEN 3
#define CODE_STREAM_CLOSE 4
#define CODE_CODE_OPEN 5
#define CODE_CODE_CLOSE 6

#pragma endregion


using namespace std;


#pragma region define varible ---------------------------------------------------------------------

static JavaVM * s_Jvm;
static jobject gCallbackObject;
const char *kInterfacePath = "com/thuantan/farshutter/Net/NativeSocketReceiver";

static jbyte * jImageOpen = new jbyte[strlen(IMG_OPEN)];
static jbyte * jImageClose = new jbyte[strlen(IMG_CLOSE)];
static jbyte * jStreamOpen = new jbyte[strlen(STREAM_OPEN)];
static jbyte * jStreamClose = new jbyte[strlen(STREAM_CLOSE)];
static jbyte * jCodeOpen = new jbyte[strlen(CODE_OPEN)];
static jbyte * jCodeClose = new jbyte[strlen(CODE_CLOSE)];

int sizeImgOpenTag = strlen(IMG_OPEN);
int sizeImgCloseTag = strlen(IMG_CLOSE);
int sizeStreamOpenTag = strlen(STREAM_OPEN);
int sizeStreamCloseTag = strlen(STREAM_CLOSE);
int sizeCodeOpenTag = strlen(CODE_OPEN);
int sizeCodeCloseTag = strlen(CODE_CLOSE);

const char *kInterfaceConnect = "OnConnect";
const char *kInterfaceImageMessage = "OnReceiveImage";
const char *kInterfaceStreamMessage = "OnReceiveStream";
const char *kInterfaceCodeMessage = "OnReceiveCode";
const char *kInterfaceDisConnect = "onDisConnectListener";

static jbyte arrImage[MAX_LENGTH_IMAGE];
static jbyte arrStream[MAX_LENGTH_STREAM];
static jbyte arrCode[MAX_LENGTH_CODE];

#pragma endregion


#pragma region function ---------------------------------------------------------------------------

int beginByCode(jbyte* data, int start, int len) {
	if (data != NULL && start <= len - sizeCodeOpenTag) {
		if (data[start + 1] == '/') { //Check close code
			switch (data[start + 2])
			{
			case 's':
				if (start + sizeStreamCloseTag <= len) {
					for (int i = 3; i < sizeStreamCloseTag; i++)
					{
						if (data[start + i] != jStreamClose[i]) {
							return CODE_ERROR;
						}
					}
					return CODE_STREAM_CLOSE;
				}
				break;
			case 'c':
				if (start + sizeCodeCloseTag <= len) {
					for (int i = 3; i < sizeCodeCloseTag; i++)
					{
						if (data[start + i] != jCodeClose[i]) {
							return CODE_ERROR;
						}
					}
					return CODE_CODE_CLOSE;
				}
				break;
			case 'i':
				if (start + sizeImgCloseTag <= len) {
					for (int i = 3; i < sizeImgCloseTag; i++)
					{
						if (data[start + i] != jImageClose[i]) {
							return CODE_ERROR;
						}
					}
					return CODE_IMG_CLOSE;
				}
				break;
			default:
				break;
			}
		}
		else {
			switch (data[start + 1])
			{
			case 's':
				if (start + sizeStreamOpenTag <= len) {
					for (int i = 2; i < sizeStreamOpenTag; i++)
					{
						if (data[start + i] != jStreamOpen[i]) {
							return CODE_ERROR;
						}
					}
					return CODE_STREAM_OPEN;
				}
				break;
			case 'c':
				if (start + sizeCodeOpenTag <= len) {
					for (int i = 2; i < sizeCodeOpenTag; i++)
					{
						if (data[start + i] != jCodeOpen[i]) {
							return CODE_ERROR;
						}
					}
					return CODE_CODE_OPEN;
				}
				break;
			case 'i':
				if (start + sizeImgOpenTag <= len) {
					for (int i = 2; i < sizeImgOpenTag; i++)
					{
						if (data[start + i] != jImageOpen[i]) {
							return CODE_ERROR;
						}
					}
					return CODE_IMG_OPEN;
				}
				break;
			default:
				break;
			}
		}
	}

	return CODE_ERROR;
}

/* find img tag in array byte */
int findTag(jbyte* data, int start, int len, int* foundCode)
{
	for (int i = start; i < len; i++)
	{
		if (data[i] == '<') {
			*foundCode = beginByCode(data, i, len);
			if (*foundCode != CODE_ERROR) {
				return i;
			}
		}
	}

	return len;
}

#pragma endregion


#pragma region method -----------------------------------------------------------------------------

void initClassHelper(JNIEnv *env, const char *path, jobject *objptr) {

	memcpy(jImageOpen, IMG_OPEN, strlen(IMG_OPEN));
	memcpy(jImageClose, IMG_CLOSE, strlen(IMG_CLOSE));
	memcpy(jStreamOpen, STREAM_OPEN, strlen(STREAM_OPEN));
	memcpy(jStreamClose, STREAM_CLOSE, strlen(STREAM_CLOSE));
	memcpy(jCodeOpen, CODE_OPEN, strlen(CODE_OPEN));
	memcpy(jCodeClose, CODE_CLOSE, strlen(CODE_CLOSE));

	bzero(arrImage, MAX_LENGTH_IMAGE);

	jclass cls = env->FindClass(path);
	if (!cls) {
		LOGE("initClassHelper: failed to get %s class reference", path);
		return;
	}
	jmethodID constr = env->GetMethodID(cls, "<init>", "()V");
	if (!constr) {
		LOGE("initClassHelper: failed to get %s constructor", path);
		return;
	}
	jobject obj = env->NewObject(cls, constr);
	if (!obj) {
		LOGE("initClassHelper: failed to create a %s object", path);
		return;
	}
	(*objptr) = env->NewGlobalRef(obj);
}

void callbackReceive(jbyte * val, const char *kInterfaceMessage, int len) {

	int status;
	JNIEnv *env;
	bool isAttached = false;

	status = s_Jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
	if (status < 0) {
		// LOGE("callback_handler: failed to get JNI environment, assuming native thread");
		status = s_Jvm->AttachCurrentThread(&env, NULL);
		if (status < 0) {
			LOGE("callback_handler: failed to attach current thread");
			return;
		}
		isAttached = true;
	}

	/* Construct a Java string */
	jbyteArray jarray = env->NewByteArray(len);
	env->SetByteArrayRegion(jarray, 0, len, val);
	jclass interfaceClass = env->GetObjectClass(gCallbackObject);
	if (!interfaceClass) {
		// LOGE("callback_handler: failed to get class reference");
		if (isAttached) s_Jvm->DetachCurrentThread();
		return;
	}

	/* Find the callBack method ID */
	jmethodID method = env->GetStaticMethodID(interfaceClass, kInterfaceMessage, /*"(Ljava/lang/String;)V"*/ "([B)V");
	if (!method) {
		// LOGE("callback_handler: failed to get method ID");
		if (isAttached) s_Jvm->DetachCurrentThread();
		return;
	}
	env->CallStaticVoidMethod(interfaceClass, method, jarray);
	// env->ReleaseByteArrayElements(jarray, val, JNI_ABORT);
	env->DeleteLocalRef(jarray);
	if (isAttached) s_Jvm->DetachCurrentThread();
}

void *threadByteListener(void *arg) {
	/*Socket client variable*/
	jbyte buffer[MAX_SIZE_BUFFER];
	int currentLength = 0;
	int newsockfd = *((int *)arg);
	bool isStatus = true;
	int last_recieve_code = CODE_ERROR;

	/*Waiting recieve and send data to client*/
	while (1) {
		bzero(buffer, MAX_SIZE_BUFFER);
		int n = read(newsockfd, buffer, MAX_SIZE_BUFFER - 1);
		if (n <= 0)
		{
			isStatus = false;
			LOGE("ERROR reading from socket\n");
			break;
		}

		int start_index = -1;
		int last_start_index = -1;
		int found_code = CODE_ERROR;
		int last_found_code = CODE_ERROR;
		while (start_index < n)
		{
			last_found_code = found_code;
			last_start_index = start_index;

			start_index = findTag(buffer, start_index + 1, n, &found_code);

			if (found_code != CODE_ERROR) {
				switch (found_code)
				{
				case CODE_STREAM_OPEN:
					bzero(arrStream, MAX_LENGTH_STREAM);
					currentLength = 0;
					break;
				case CODE_STREAM_CLOSE:
					if (last_found_code != CODE_ERROR) {
						currentLength = start_index - last_start_index - sizeStreamOpenTag;
						memcpy(arrStream, buffer + last_start_index + sizeStreamOpenTag, currentLength);
					}
					else {
						memcpy(arrStream + currentLength, buffer, start_index);
						currentLength += start_index;
					}
					callbackReceive(arrStream, kInterfaceStreamMessage, currentLength);
					break;
				case CODE_CODE_OPEN:
					bzero(arrCode, MAX_LENGTH_CODE);
					currentLength = 0;
					break;
				case CODE_CODE_CLOSE:
					if (last_found_code != CODE_ERROR) {
						currentLength = start_index - last_start_index - sizeCodeOpenTag;
						memcpy(arrCode, buffer + last_start_index + sizeCodeOpenTag, currentLength);
					}
					else {
						memcpy(arrCode + currentLength, buffer, start_index);
						currentLength += start_index;
					}
					callbackReceive(arrCode, kInterfaceCodeMessage, currentLength);
					break;
				case CODE_IMG_OPEN:
					bzero(arrImage, MAX_LENGTH_IMAGE);
					currentLength = 0;
					break;
				case CODE_IMG_CLOSE:
					if (last_found_code != CODE_ERROR) {
						currentLength = start_index - last_start_index - sizeImgOpenTag;
						memcpy(arrImage, buffer + last_start_index + sizeImgOpenTag, currentLength);
					}
					else {
						memcpy(arrImage + currentLength, buffer, start_index);
						currentLength += start_index;
					}
					callbackReceive(arrImage, kInterfaceImageMessage, currentLength);
					break;
				default:
					break;
				}
			}
			else if (last_found_code != CODE_ERROR) {
				switch (last_found_code)
				{
				case CODE_STREAM_OPEN:
					memcpy(arrStream, buffer + last_start_index + sizeStreamOpenTag, n - last_start_index - sizeStreamOpenTag);
					currentLength += n - last_start_index - sizeStreamOpenTag;
					break;
				case CODE_CODE_OPEN:
					memcpy(arrCode, buffer + last_start_index + sizeCodeOpenTag, n - last_start_index - sizeCodeOpenTag);
					currentLength += n - last_start_index - sizeCodeOpenTag;
					break;
				case CODE_IMG_OPEN:
					memcpy(arrImage, buffer + last_start_index + sizeImgOpenTag, n - last_start_index - sizeImgOpenTag);
					currentLength += n - last_start_index - sizeImgOpenTag;
					break;
				default:
					break;
				}
			}
			else if (last_recieve_code != CODE_ERROR) {
				switch (last_recieve_code)
				{
				case CODE_STREAM_OPEN:
					memcpy(arrStream + currentLength, buffer, n);
					currentLength += n;
					break;
				case CODE_CODE_OPEN:
					memcpy(arrCode + currentLength, buffer, n);
					currentLength += n;
					break;
				case CODE_IMG_OPEN:
					memcpy(arrImage + currentLength, buffer, n);
					currentLength += n;
					break;
				default:
					break;
				}
			}
		}

		if (last_found_code != CODE_ERROR) {
			last_recieve_code = last_found_code;
		}
		/*int code = findTag(buffer, n);
		if (code == CODE_IMG_OPEN) {
			bzero(arrImage, MAX_LENGTH_IMAGE);
			memcpy(arrImage, buffer + sizeImgOpenTag, n - sizeImgOpenTag);
			currentLength += n - sizeImgOpenTag;
		}
		else if (code == CODE_IMG_CLOSE) {
			memcpy(arrImage + currentLength, buffer, n - sizeImgCloseTag);
			currentLength += n - sizeImgCloseTag;
			callbackReceive(arrImage, kInterfaceImageMessage, currentLength);
			currentLength = 0;
		}
		else if (code == CODE_INPUT) {
			bzero(arrCode, MAX_LENGTH_CODE);
			memcpy(arrCode, buffer + sizeInputOpenTag, n - sizeInputCloseTag);
			callbackReceive(arrCode, kInterfaceCodeMessage, n - sizeInputOpenTag - sizeInputCloseTag);
		}
		else {
			memcpy(arrImage + currentLength, buffer, n);
			currentLength += n;
		}*/

	}
	LOGI("THREAD CLOSED");
	pthread_exit(NULL);
}

#pragma endregion


class SharedLibrarySocket
{
private: // Server
	int sockfd, newsockfd, portno;
	socklen_t clilen;
	struct sockaddr_in serv_addr, cli_addr;

public:
	void createSocketServer(int port) {
		sockfd = socket(AF_INET, SOCK_STREAM, 0);
		if (sockfd < 0) {
			LOGE("ERROR opening socket");
			return;
		}

		bzero((char *)&serv_addr, sizeof(serv_addr));

		portno = port;
		serv_addr.sin_family = AF_INET;
		serv_addr.sin_addr.s_addr = INADDR_ANY;
		serv_addr.sin_port = htons(portno);
		if (bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
			LOGE("ERROR on binding");
			return;
		}
		listen(sockfd, 5);
		clilen = sizeof(cli_addr);

		LOGI("Server Created");

		while (1 && sockfd > 0) {
			int newsock = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
			LOGI("New client connected");

			if (newsock < 0) {
				LOGE("ERROR on accept");
				continue;
			}
			else {
				if (newsockfd > 0) {
					close(newsockfd);
				}
				newsockfd = newsock;
			}

			int s;
			pthread_t thread;
			s = pthread_create(&thread, NULL, /*threadListener*/ threadByteListener, &newsockfd);
			if (s != 0)
				LOGE("pthread_create fail");
		}
		closeServer();
	}

	void closeServer() {
		LOGI("CLOSE SERVER");

		if (sockfd > 0) {
			close(sockfd);
			sockfd = -1;
		}

		if (newsockfd > 0) {
			close(newsockfd);
			newsockfd = -1;
		}
	}

private: // Client
	int cli_portno, cli_sockfd;/*, cli_n*/
	struct sockaddr_in cli_serv_addr;
	struct hostent *cli_server;

private: // Global
	// char *buffer;
	jbyte buffer[MAX_SIZE_BUFFER];
	int getCurrentSockfd() {
		if (newsockfd < 0) return cli_sockfd;
		return newsockfd;
	}
	int send_message_fail_count = 0;
public:
	const char * getPlatformABI();
	SharedLibrarySocket() : sockfd(-1), newsockfd(-1), cli_sockfd(-1) {}

	~SharedLibrarySocket() {
	}

	void sendByteMessage(jbyte * data, int size) {
		if (getCurrentSockfd() < 0) return;
		// int size = strlen(data);
		int sendfail = true;
		int length = 0;
		while (length < size)
		{
			bzero(buffer, MAX_SIZE_BUFFER);
			int delta = size - length;
			int sizeCopy = MAX_SIZE_BUFFER - 1 > delta ? delta : MAX_SIZE_BUFFER - 1;
			memcpy(buffer, data + length, sizeCopy);
			int n = write(getCurrentSockfd(), buffer, sizeCopy);
			// LOGI("Writing to socket %d", length);
			if (n <= 0)
			{
				LOGE("ERROR writing to socket\n");
				break;
			}
			else {
				sendfail = false;
			}
			length += sizeCopy;
		}

		if (sendfail) send_message_fail_count++;
		else send_message_fail_count = 0;

		if (send_message_fail_count > 5) {
			LOGI("SEND 5 MESSAGE FAIL!");
			if (newsockfd > 0) {
				close(newsockfd);
				newsockfd = -1;
			}
			closeClient();
		}
	}

	void connectToServer(const char host[], int port, int try_count) {
		if (try_count > 10) return;

		// char buffer[MAX_SIZE_BUFFER];
		cli_portno = port;
		cli_sockfd = socket(AF_INET, SOCK_STREAM, 0);
		if (cli_sockfd < 0)
			LOGE("ERROR opening socket");

		cli_server = gethostbyname(host);
		if (cli_server == NULL) {
			LOGE("ERROR, no such host\n");
			exit(0);
		}

		bzero((char *)&cli_serv_addr, sizeof(cli_serv_addr));
		cli_serv_addr.sin_family = AF_INET;

		bcopy((char *)cli_server->h_addr, (char *)&cli_serv_addr.sin_addr.s_addr, cli_server->h_length);
		cli_serv_addr.sin_port = htons(cli_portno);

		LOGI("connecting: %d %d, try: %d", cli_serv_addr.sin_addr.s_addr, cli_serv_addr.sin_port, try_count);

		if (connect(cli_sockfd, (struct sockaddr *) &cli_serv_addr, sizeof(cli_serv_addr)) < 0) {
			LOGE("ERROR connecting");
			closeClient();
			connectToServer(host, port, try_count + 1);
		}

		pthread_t thread;
		int s = pthread_create(&thread, NULL, threadByteListener, &cli_sockfd);
		if (s != 0)
			LOGE("pthread_create fail");
	}

	void closeClient() {
		LOGI("CLOSE CLIENT");

		if (cli_sockfd > 0) {
			close(cli_sockfd);
			cli_sockfd = -1;
		}
	}
};

