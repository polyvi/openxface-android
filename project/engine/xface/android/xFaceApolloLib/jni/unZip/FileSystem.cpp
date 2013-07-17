#include <jni.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <limits.h>
#include <mntent.h>
#include <dirent.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/sysinfo.h>
#include <sys/statfs.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#include "utils.h"
#include "FileSystem.h"

#define PHONE_STORAGE_PATH "/data/"
#define READ_BUFFER_SIZE 1024
#define XF_RMDIR    0x1
#define XF_RMFILE   0x2

#define XF_OK 0
#define XF_ERROR -1

#define XF_TRUE 1
#define XF_FALSE 0

#define S_IDIR 		(S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH)

#define S_IFILE 	(S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH)

extern int getFileFlag(unsigned int xfFlag) {
	int lflag = O_RDONLY;
	if ((xfFlag & XF_FILE_WRONLY) != 0) {
		lflag |= O_WRONLY;
	}
	if ((xfFlag & XF_FILE_RDWR) != 0) {
		lflag |= O_RDWR;
	}
	if ((xfFlag & XF_FILE_APPEND) != 0) {
		lflag |= O_APPEND;
	}
	if ((xfFlag & XF_FILE_CREAT) != 0) {
		lflag |= O_CREAT;
	}

	return lflag;
}

extern mode_t getFileMode(unsigned int xfFlag) {
	mode_t lmode = 0;
	if (xfFlag & XF_FILE_CREAT) {
		lmode = S_IFILE;
	}
	return lmode;
}

//删除一个文件或文件夹（包括子文件或文件夹）
static int removeFile(const char* path) {
	// if the parameter is NULL or empty string (""), the function considers the path does not
	// exist, so it returns XF_OK.
	if (path == NULL) {
		return XF_OK;
	}
	const size_t pathLen = strlen(path);
	if (pathLen == 0 || access(path, 0) != 0) {
		return XF_OK;
	}

	const bool isSlashEnded = (path[pathLen - 1] == '/');

	int ret = XF_ERROR;
	// test whether the path is a directory, remove all its contents, including subdirectories
	// and files.
	struct stat st = { 0 };
	if ((stat(path, &st) == 0) && S_ISDIR(st.st_mode)) {
		// the path is a directory.
		DIR* dir = opendir(path);
		if (dir != NULL) {
			// find all the subdirectories and files, and delete them
			dirent ent = { 0 };
			dirent* pEntry = NULL;
			while (readdir_r(dir, &ent, &pEntry) == 0) {
				if (pEntry == NULL) {
					break;
				}

				// remove the subdirectories or files which are not "." or ".."
				if ((strcmp(ent.d_name, ".") != 0) && (strcmp(ent.d_name, "..")
						!= 0)) {
					// get the full name of a subdirectory or file. the full name
					// will be stored in temp_dir.
					char fullSubDir[PATH_MAX] = { 0 };
					strncpy(fullSubDir, path, PATH_MAX);
					if (!isSlashEnded) {
						strncat(fullSubDir, "/", PATH_MAX);
					}
					strncat(fullSubDir, ent.d_name, PATH_MAX);
					removeFile(fullSubDir);
				}
			}
			closedir(dir);

			// try to remove the directory, after all its subdirectories and file were removed
			ret = (rmdir(path) == 0) ? XF_OK : XF_ERROR;
		}
	} else {
		// delete the file.
		ret = unlink(path) == 0 ? XF_OK : XF_ERROR;
	}

	return ret;
}

static int doesFileExist(const char *pPath) {
	if (pPath == NULL) {
		return XF_FALSE;
	}
	if (access(pPath, 0) == 0) {
		return XF_TRUE;
	}

#ifdef DEBUG
	__android_log_print(ANDROID_LOG_DEBUG, "xface",
			"File %s doesn't exist!", mbsBuffer);
#endif
	return XF_FALSE;
}

XF_File XF_FILE_open(const unsigned short* pFilePath, unsigned int flags) {
	if (pFilePath == NULL || pFilePath[0] == 0) {
		return NULL;
	}

	char mbsPath[PATH_MAX] = { 0 };
	if (ucsToMbs(pFilePath, mbsPath, PATH_MAX) <= 0) {
		return NULL;
	}

	// If file does not exist and XF_FILE_CREAT is not specified, the
	// function will not create the file automatically.
	int doesExist = doesFileExist(mbsPath);
	if ((doesExist != XF_TRUE) && ((flags & XF_FILE_CREAT) == 0)) {
		return NULL;
	}

	// if the name specifies a directory, the function will not open it.
	if (doesExist == XF_TRUE && XF_FILE_isDirectory(pFilePath) == XF_TRUE) {
		return NULL;
	}

	int fileFlag = getFileFlag(flags);
	mode_t mode = getFileMode(flags);
	int fd = open(mbsPath, fileFlag, mode);
	XF_File fileHandle = (fd == -1) ? NULL : (void*) fd;
#ifdef DEBUG
	__android_log_print(ANDROID_LOG_DEBUG, "xface", "Open file: %s, fd = %d",
			mbsPath, fileHandle);
#endif
	if (fileHandle == NULL) {
	}
	return fileHandle;
}

int XF_FILE_close(const XF_File fileHandle) {
	int ret = XF_ERROR;
	if (fileHandle != NULL) {
		ret = (close((int) fileHandle) == 0) ? XF_OK : XF_ERROR;
	}
	return ret;
}

static int createDir(const unsigned short* dir, int mode) {
	if (dir == NULL) {
		return XF_ERROR;
	}

	int ret = XF_ERROR;
	char mbsBuf[PATH_MAX] = { 0 };
	if (ucsToMbs(dir, mbsBuf, PATH_MAX) <= 0) {
		return XF_ERROR;
	}

	if (doesFileExist(mbsBuf) == XF_TRUE) {
		return XF_OK;
	}

	if (mkdir(mbsBuf, mode) == 0) {
		ret = XF_OK;
	}
#ifdef DEBUG
	__android_log_print(ANDROID_LOG_DEBUG, "xface", "create dir: %s, ret = %d",
			mbsBuf, ret);
#endif
	return ret;
}

int XF_FILE_mkdir(const unsigned short* pDirPath) {
	// test whether the path name is valid.
	if (pDirPath == NULL) {
		return XF_ERROR;
	}
	int dirLen = getUcsLen(pDirPath);

	if (dirLen == 0) {
		return XF_ERROR;
	}
	// if the path name does not ends of a slash, then append a slash character
	// to the path name.
	unsigned short dir[PATH_MAX];
	memset(dir, 0, sizeof(dir));
	memcpy(dir, pDirPath, dirLen * sizeof(unsigned short));

	if (dir[dirLen - 1] != '/') {
		dir[dirLen] = '/';
		dirLen += 1;
	}

	for (int i = 1; i < dirLen; i++) {
		if (dir[i] == '/') {
			dir[i] = 0;
			if (createDir(dir, S_IDIR) == XF_ERROR) {
				return XF_ERROR;
			}
			dir[i] = '/';
		}
	}
	return XF_OK;
}

/**
 * Remove a path.
 * If the path specifies a file, the function will delete it. If the path specifies a
 * directory, the function will delete the directory and all its contents recurisively,
 * including subdirectories and files.
 * If the function succeed, it will return XF_OK, otherwise it returns XF_ERROR. If the path
 * does not exist, it will return XF_OK. If the path is NULL or an empty string (""), it will
 * consider the path not exist, so it will return XF_OK.
 */
static int removePath(const char* path, int flag = XF_RMDIR | XF_RMFILE) {
	// if the parameter is NULL or empty string (""), the function considers the path does not
	// exist, so it returns XF_OK.
	if (path == NULL) {
		return XF_OK;
	}
	size_t pathLen = strlen(path);
	if (pathLen == 0) {
		return XF_OK;
	}

	// if the path does not exist, the function returns XF_OK.
	if (access(path, 0) != 0) {
		return XF_OK;
	}

	pathLen = strlen(path);
	bool isSlashEnded = (path[pathLen - 1] == '/');

	int ret = XF_ERROR;
	// test whether the path is a directory, remove all its contents, including subdirectories
	// and files.
	struct stat st;
	if ((stat(path, &st) == 0) && S_ISDIR(st.st_mode)) {
		// the path is a directory.
		DIR* dir = opendir(path);
		if ((flag & XF_RMDIR) && (dir != NULL)) {
			// find all the subdirectories and files, and delete them
			dirent ent;
			dirent* entPtr = NULL;
			while (readdir_r(dir, &ent, &entPtr) == 0) {
				if (entPtr == NULL) {
					break;
				}

				// remove the subdirectories or files which are not "." or ".."
				if ((strcmp(ent.d_name, ".") != 0) && (strcmp(ent.d_name, "..")
						!= 0)) {
					// get the full name of a subdirectory or file. the full name
					// will be stored in temp_dir.
					char tempDir[PATH_MAX] = { 0 };
					strcpy(tempDir, path);
					if (!isSlashEnded) {
						strcat(tempDir, "/");
					}
					strncat(tempDir, ent.d_name, PATH_MAX);
					removePath(tempDir, flag | XF_RMFILE);
				}
			}
			closedir(dir);

			// try to remove the directory, after all its subdirectories and file were removed
			ret = (rmdir(path) == 0) ? XF_OK : XF_ERROR;
		}
	} else {
		// assume the path is a file.
		if (flag & XF_RMFILE) {
			ret = unlink(path) == 0 ? XF_OK : XF_ERROR;
		}
	}

	return ret;
}

int XF_FILE_write(const XF_File fileHandle, const void *pBuffer,
		unsigned int size) {
	int ret = XF_ERROR;
	if ((fileHandle != NULL) && (pBuffer != NULL)) {
		int fd = (int) fileHandle;
		size_t bytes = write(fd, pBuffer, size);
		if (bytes >= 0) {
			ret = (int) bytes;
		}
	}
	return ret;
}

int XF_FILE_read(const XF_File fileHandle, void *pBuffer, unsigned int length) {
	int ret = XF_ERROR;
	if ((fileHandle != NULL) && (pBuffer != NULL)) {
		int fd = (int) fileHandle;
		ssize_t bytes = read(fd, pBuffer, (size_t) length);
		if (bytes >= 0) {
			ret = (int) bytes;
		}
	}
#ifdef DEBUG
	__android_log_print(ANDROID_LOG_DEBUG, "xface", "Read ret = %d", ret);
#endif
	return ret;
}

int XF_FILE_seek(const XF_File fileHandle, int offset, int moveMethod) {
	int ret = XF_ERROR;
	if ((fileHandle != NULL) && (offset >= 0)) {
		int fd = (int) fileHandle;
		if (flock(fd, LOCK_EX) == 0) {
			int mode = 0;
			switch (moveMethod) {
			case XF_FILE_BEGIN:
				mode = SEEK_SET;
				break;
			case XF_FILE_END:
				mode = SEEK_END;
				break;
			case XF_FILE_CURRENT:
				mode = SEEK_CUR;
				break;
			default:
				mode = SEEK_SET;
			}
			off_t pos = (off_t) offset;
			off_t cur = lseek(fd, 0, SEEK_CUR);
			off_t end = lseek(fd, 0, SEEK_END);
			if ((cur != -1) && (end != -1)) {
				if (offset > end) {
					// if the offset is out of the file, then move the file
					// pointer to the original position.
					lseek(fd, cur, SEEK_SET);
				} else if (lseek(fd, pos, mode) != -1) {
					ret = XF_OK;
				}
			}
			flock(fd, LOCK_UN);
		}
	}
	return ret;
}

/**
 * Get the file position.
 */
int XF_FILE_tell(const XF_File fileHandle) {
	int ret = XF_ERROR;
	if (fileHandle != NULL) {
		int fd = (int) fileHandle;
		off_t of = lseek(fd, 0, SEEK_CUR);
		if (of >= 0) {
			ret = (int) of;
		}
	}
	return ret;
}

/**
 * Get the length of a file, in bytes.
 */
int XF_FILE_getLength(const XF_File fileHandle) {
	int ret = XF_ERROR;
	if (fileHandle != NULL) {
		int fd = (int) fileHandle;
		struct stat st;
		if (fstat(fd, &st) == 0) {
			ret = (int) st.st_size;
		}
	}
	return ret;
}

/*比较srcStr 和 destStr的大小。              *
 * srcStr = destStr 0  函数返回 0             *
 * srcStr > destStr    函数返回大于0的整数    *
 * srcStr < destStr    函数返回小于0的整数    */
static int xfStrcmp(const unsigned short* srcStr, const unsigned short *destStr) {
	if (srcStr == destStr) {
		return 0;
	}
	if (srcStr == NULL) {
		return -1;
	} else if (destStr == NULL) {
		return 1;
	}

	while (*srcStr == *destStr) {
		if ((*srcStr == '\0') || (*destStr == '\0')) {
			break;
		}
		srcStr++;
		destStr++;
	}

	return (int) (*srcStr - *destStr);
}

static int copyFile(const char* src, const char* dst) {
	if ((src == NULL) || (dst == NULL)) {
		return XF_ERROR;
	}

	size_t srcLen = strlen(src);
	size_t dstLen = strlen(dst);
	if ((srcLen == 0) || (dstLen == 0)) {
		return XF_ERROR;
	}

	// Get the mode of the source file. The destination file will have
	// the same mode as the source file.
	struct stat st;
	if (stat(src, &st) != 0) {
		return XF_ERROR;
	}

	int ret = XF_ERROR;
	// open the source file
	int srcFd = open(src, O_RDONLY);
	if (srcFd != -1) {
		// create the destination file
		int dstFd = open(dst, O_WRONLY| O_CREAT, st.st_mode | S_IRUSR | S_IWUSR
				| S_IRGRP | S_IROTH);
		if (dstFd != -1) {
			// read data from the source file and write them into the newly created
			// destination file.
			unsigned char buffer[READ_BUFFER_SIZE] = { 0 };
			int bytesRead = (int) read(srcFd, buffer, READ_BUFFER_SIZE);
			if (bytesRead > 0) {
				ret = XF_OK;
			}

			while (bytesRead > 0) {
				int bytesWrite = write(dstFd, buffer, bytesRead);
				if (bytesWrite != bytesRead || bytesWrite < 0) {
					ret = XF_ERROR;
					break;
				}
				bytesRead = (int) read(srcFd, buffer, READ_BUFFER_SIZE);
			}
			if (ret == XF_ERROR) {
				unlink(dst);
			}
			close(dstFd);
		}
		close(srcFd);
	}
	return ret;
}

/**
 * Rename a file as a new name.
 */
static int renameFile(const char* oldName, const char* newName) {
	int ret = XF_ERROR;
	if (oldName != NULL && newName != NULL) {
		ret = rename(oldName, newName) == 0 ? XF_OK : XF_ERROR;
	}
	return ret;
}

//从ctime函数格式化后的字符串中获取year的值。
static int getYear(const time_t* time_sec) {
	int year = 0;
	char *pDate = NULL;

	pDate = ctime(time_sec);
	pDate = pDate + strlen(pDate) - 1;
	while (*pDate != ' ') {
		pDate--;
	}
	pDate++;
	sscanf(pDate, "%d", &year);

	return year;
}

int XF_FILE_isDirectory(const unsigned short *pPath) {
	if (pPath == NULL || pPath[0] == 0) {
		return XF_ERROR;
	}

	char mbsBuffer[PATH_MAX] = { 0 };
	int len = ucsToMbs(pPath, mbsBuffer, PATH_MAX);

	if (len > 0) {
		struct stat st = { 0 };
		if (stat(mbsBuffer, &st) == 0) {
			return S_ISDIR(st.st_mode) ? XF_TRUE : XF_FALSE;
		}
	}
	return XF_ERROR;
}
