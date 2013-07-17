#ifndef _LITEUNZIP_H
#define _LITEUNZIP_H


#ifdef __cplusplus
extern "C" {
#endif

#if (defined(_WIN32)||defined(WIN32)) && defined(__BADA__)
#undef _WIN32
#undef WIN32
#endif

#if defined(WINCE)

#include <windows.h>

#define HUNZIP	void *


	typedef struct
	{
		DWORD			Index;					// index of this entry within the zip archive
		DWORD			Attributes;				// attributes, as in GetFileAttributes.
		FILETIME		AccessTime, CreateTime, ModifyTime;	// access, create, modify filetimes
		unsigned long	CompressedSize;			// sizes of entry, compressed and uncompressed. These
		unsigned long	UncompressedSize;		// may be -1 if not yet known (e.g. being streamed in)
		unsigned short	Name[MAX_PATH];			// entry name
	} ZIPENTRY;

	// Functions for opening a ZIP archive
	DWORD WINAPI UnzipOpenFileA(HUNZIP *, const char *, const char *);
	DWORD WINAPI UnzipOpenFileW(HUNZIP *, const unsigned short *, const char *);
	DWORD WINAPI UnzipOpenFileRawA(HUNZIP *, const char *, const char *);
	DWORD WINAPI UnzipOpenFileRawW(HUNZIP *, const unsigned short *, const char *);

#define UnzipOpenFile UnzipOpenFileW
#define UNZIPOPENFILENAME "UnzipOpenFileW"
	typedef DWORD WINAPI UnzipOpenFilePtr(HUNZIP *, const unsigned short *, const char *);
#define UnzipOpenFileRaw UnzipOpenFileRawW
#define UNZIPOPENFILERAWNAME "UnzipOpenFileRawW"
	typedef DWORD WINAPI UnzipOpenFileRawPtr(HUNZIP *, const unsigned short *, const char *);

	DWORD WINAPI UnzipOpenBuffer(HUNZIP *, void *, DWORD, const char *);
#define UNZIPOPENBUFFERNAME "UnzipOpenBuffer"
	typedef DWORD WINAPI UnzipOpenBufferPtr(HUNZIP *, void *, DWORD, const char *);
	DWORD WINAPI UnzipOpenBufferRaw(HUNZIP *, void *, DWORD, const char *);
#define UNZIPOPENBUFFERRAWNAME "UnzipOpenBufferRaw"
	typedef DWORD WINAPI UnzipOpenBufferRawPtr(HUNZIP *, void *, DWORD, const char *);
	DWORD WINAPI UnzipOpenHandle(HUNZIP *, HANDLE, const char *);
#define UNZIPOPENHANDLENAME "UnzipOpenHandle"
	typedef DWORD WINAPI UnzipOpenHandlePtr(HUNZIP *, HANDLE, const char *);
	DWORD WINAPI UnzipOpenHandleRaw(HUNZIP *, HANDLE, const char *);
#define UNZIPOPENHANDLERAWNAME "UnzipOpenHandleRaw"
	typedef DWORD WINAPI UnzipOpenHandleRawPtr(HUNZIP *, HANDLE, const char *);
	DWORD WINAPI UnzipOpenHttpGzipBuffer(HUNZIP *, void *, DWORD, const char *);		//LHC add for decompressing http gzip data
#define UNZIPOPENHTTPGZIPBUFFERNAME "UnzipOpenHttpGzipBuffer"
	typedef DWORD WINAPI UnzipOpenHttpGzipBufferPtr(HUNZIP *, HANDLE, const char *);

	// Functions to get information about an "entry" within a ZIP archive		//LHC
	char* WINAPI UnzipGetRootDir(HUNZIP);
#define UNZIPGETROOTDIR "UnzipGetRootDir"										//LHC

	// Functions to get information about an "entry" within a ZIP archive
	DWORD WINAPI UnzipGetItemW(HUNZIP, ZIPENTRY *);
	DWORD WINAPI UnzipGetItemA(HUNZIP, ZIPENTRY *);

#define UnzipGetItem UnzipGetItemW
#define UNZIPGETITEMNAME "UnzipGetItemW"

	typedef DWORD WINAPI UnzipGetItemPtr(HUNZIP, ZIPENTRY *);

	DWORD WINAPI UnzipFindItemW(HUNZIP, ZIPENTRY *, BOOL);
	DWORD WINAPI UnzipFindItemA(HUNZIP, ZIPENTRY *, BOOL);

#define UnzipFindItem UnzipFindItemW
#define UNZIPFINDITEMNAME "UnzipFindItemW"

	typedef DWORD WINAPI UnzipFindItemPtr(HUNZIP, ZIPENTRY *, BOOL);

	// Functions to unzip an "entry" within a ZIP archive
	DWORD WINAPI UnzipItemToFileW(HUNZIP, const unsigned short *, ZIPENTRY *);
	DWORD WINAPI UnzipItemToFileA(HUNZIP, const char *, ZIPENTRY *);

#define UnzipItemToFile UnzipItemToFileW
#define UNZIPITEMTOFILENAME "UnzipItemToFileW"
	typedef DWORD WINAPI UnzipItemToFilePtr(HUNZIP, const unsigned short *, ZIPENTRY *);

	DWORD WINAPI UnzipItemToHandle(HUNZIP, HANDLE, ZIPENTRY *);
#define UNZIPITEMTOHANDLENAME "UnzipItemToHandle"
	typedef DWORD WINAPI UnzipItemToHandlePtr(HUNZIP, HANDLE, ZIPENTRY *);

	DWORD WINAPI UnzipItemToBuffer(HUNZIP, void *, DWORD, ZIPENTRY *);
#define UNZIPITEMTOBUFFERNAME "UnzipItemToBuffer"
	typedef DWORD WINAPI UnzipItemToBufferPtr(HUNZIP, void *, DWORD, ZIPENTRY *);

	// Function to set the base directory
	DWORD WINAPI UnzipSetBaseDirW(HUNZIP, const unsigned short *);
	DWORD WINAPI UnzipSetBaseDirA(HUNZIP, const char *);

#define UnzipSetBaseDir UnzipSetBaseDirW
#define UNZIPSETBASEDIRNAME "UnzipSetBaseDirW"
	typedef DWORD WINAPI UnzipSetBaseDirPtr(HUNZIP, const unsigned short *);

	// Function to close an archive
	DWORD WINAPI UnzipClose(HUNZIP);
#define UNZIPCLOSENAME "UnzipClose"
	typedef DWORD WINAPI UnzipClosePtr(HUNZIP);

	// Function to get an appropriate error message for a given error code return by Unzip functions
	DWORD WINAPI UnzipFormatMessageW(DWORD, unsigned short *, DWORD);
	DWORD WINAPI UnzipFormatMessageA(DWORD, char *, DWORD);

#define UnzipFormatMessage UnzipFormatMessageW
#define UNZIPFORMATMESSAGENAME "UnzipFormatMessageW"
	typedef DWORD WINAPI UnzipFormatMessagePtr(DWORD, unsigned short *, DWORD);

#if !defined(ZR_OK)
	// These are the return codes from Unzip functions
#define ZR_OK			0		// Success
	// The following come from general system stuff (e.g. files not openable)
#define ZR_NOFILE		1		// Can't create/open the file
#define ZR_NOALLOC		2		// Failed to allocate memory
#define ZR_WRITE		3		// A general error writing to the file
#define ZR_NOTFOUND		4		// Can't find the specified file in the zip
#define ZR_MORE			5		// There's still more data to be unzipped
#define ZR_CORRUPT		6		// The zipfile is corrupt or not a zipfile
#define ZR_READ			7		// An error reading the file
#define ZR_NOTSUPPORTED	8		// The entry is in a format that can't be decompressed by this Unzip add-on
	// The following come from mistakes on the part of the caller
#define ZR_ARGS			9		// Bad arguments passed
#define ZR_NOTMMAP		10		// Tried to ZipGetMemory, but that only works on mmap zipfiles, which yours wasn't
#define ZR_MEMSIZE		11		// The memory-buffer size is too small
#define ZR_FAILED		12		// Already failed when you called this function
#define ZR_ENDED		13		// The zip creation has already been closed
#define ZR_MISSIZE		14		// The source file size turned out mistaken
#define ZR_ZMODE		15		// Tried to mix creating/opening a zip 
	// The following come from bugs within the zip library itself
#define ZR_SEEK			16		// trying to seek in an unseekable file
#define ZR_NOCHANGE		17		// changed its mind on storage, but not allowed
#define ZR_FLATE		18		// An error in the de/inflation code
#define ZR_PASSWORD		19		// Password is incorrect
#endif

//#ifdef WINCE	//LHC
#elif defined(__BREW_3X__) || defined(__SYMBIAN32__) || defined(__JUNZHENPLATFORM__) || defined(ANDROID) || defined(__MTK_TARGET__)|| defined(__ECOS__) || defined(__BADA__)	//LHC for brew

#ifdef AEE_SIMULATOR	//LHC for brew //若是使用模拟器调试环境
//#ifndef WIN32
//#define WIN32
//#endif
#else
#define PATH_MAX 128	//LHC for brew
#endif

#if defined(WIN32) && !defined(__BADA__)
#include <windows.h>
//
//#if defined(AEE_SIMULATOR) && defined(__BREW_3X__)
//#define PATH_MAX MAX_PATH
//#endif

#else

#ifndef AEE_SIMULATOR		//LHC need to modify this
#if defined(__SYMBIAN32__) || defined(__MTK_TARGET__)
#include <time.h>
#else
#include <sys/time.h>
#endif
#include <limits.h>
#else
#define PATH_MAX 256
#include <sys/timeb.h>
#include <sys/utime.h>
#include <limits.h>
#endif
#ifndef DWORD
#define WINAPI
typedef unsigned long DWORD;
typedef void * HANDLE;
#define MAX_PATH PATH_MAX
typedef unsigned char BYTE;
typedef unsigned short WORD;
typedef unsigned int BOOL;
#endif
#endif

// An HUNZIP identifies a zip archive that has been opened
#define HUNZIP	void *

// Struct used to retrieve info about an entry in an archive
typedef struct
{
	DWORD			Index;					// index of this entry within the zip archive
	DWORD			Attributes;				// attributes, as in GetFileAttributes.
#if defined(WIN32)// || defined(AEE_SIMULATOR)
	FILETIME		AccessTime, CreateTime, ModifyTime;	// access, create, modify filetimes
#else
	time_t		AccessTime, CreateTime, ModifyTime;
#endif
	unsigned long	CompressedSize;			// sizes of entry, compressed and uncompressed. These
	unsigned long	UncompressedSize;		// may be -1 if not yet known (e.g. being streamed in)
	unsigned short	Name[MAX_PATH];			// entry name
} ZIPENTRY;

// Functions for opening a ZIP archive
DWORD WINAPI UnzipOpenFileA(HUNZIP *, const char *, const char *);
DWORD WINAPI UnzipOpenFileW(HUNZIP *, const unsigned short *, const char *);
DWORD WINAPI UnzipOpenFileRawA(HUNZIP *, const char *, const char *);
DWORD WINAPI UnzipOpenFileRawW(HUNZIP *, const unsigned short *, const char *);

#define UnzipOpenFile UnzipOpenFileW
#define UNZIPOPENFILENAME "UnzipOpenFileW"
typedef DWORD WINAPI UnzipOpenFilePtr(HUNZIP *, const unsigned short *, const char *);
#define UnzipOpenFileRaw UnzipOpenFileRawW
#define UNZIPOPENFILERAWNAME "UnzipOpenFileRawW"
typedef DWORD WINAPI UnzipOpenFileRawPtr(HUNZIP *, const unsigned short *, const char *);

DWORD WINAPI UnzipOpenBuffer(HUNZIP *, void *, DWORD, const char *);
#define UNZIPOPENBUFFERNAME "UnzipOpenBuffer"
typedef DWORD WINAPI UnzipOpenBufferPtr(HUNZIP *, void *, DWORD, const char *);
DWORD WINAPI UnzipOpenBufferRaw(HUNZIP *, void *, DWORD, const char *);
#define UNZIPOPENBUFFERRAWNAME "UnzipOpenBufferRaw"
typedef DWORD WINAPI UnzipOpenBufferRawPtr(HUNZIP *, void *, DWORD, const char *);
DWORD WINAPI UnzipOpenHandle(HUNZIP *, HANDLE, const char *);
#define UNZIPOPENHANDLENAME "UnzipOpenHandle"
typedef DWORD WINAPI UnzipOpenHandlePtr(HUNZIP *, HANDLE, const char *);
DWORD WINAPI UnzipOpenHandleRaw(HUNZIP *, HANDLE, const char *);
#define UNZIPOPENHANDLERAWNAME "UnzipOpenHandleRaw"
typedef DWORD WINAPI UnzipOpenHandleRawPtr(HUNZIP *, HANDLE, const char *);
DWORD WINAPI UnzipOpenHttpGzipBuffer(HUNZIP *, void *, DWORD, const char *);		//LHC add for decompressing http gzip data
#define UNZIPOPENHTTPGZIPBUFFERNAME "UnzipOpenHttpGzipBuffer"
typedef DWORD WINAPI UnzipOpenHttpGzipBufferPtr(HUNZIP *, HANDLE, const char *);

// Functions to get information about an "entry" within a ZIP archive		//LHC
char* WINAPI UnzipGetRootDir(HUNZIP);
#define UNZIPGETROOTDIR "UnzipGetRootDir"									//LHC

// Functions to get information about an "entry" within a ZIP archive
DWORD WINAPI UnzipGetItemW(HUNZIP, ZIPENTRY *);
DWORD WINAPI UnzipGetItemA(HUNZIP, ZIPENTRY *);

#define UnzipGetItem UnzipGetItemW
#define UNZIPGETITEMNAME "UnzipGetItemW"

typedef DWORD WINAPI UnzipGetItemPtr(HUNZIP, ZIPENTRY *);

DWORD WINAPI UnzipFindItemW(HUNZIP, ZIPENTRY *, BOOL);
DWORD WINAPI UnzipFindItemA(HUNZIP, ZIPENTRY *, BOOL);

#define UnzipFindItem UnzipFindItemW
#define UNZIPFINDITEMNAME "UnzipFindItemW"

typedef DWORD WINAPI UnzipFindItemPtr(HUNZIP, ZIPENTRY *, BOOL);

// Functions to unzip an "entry" within a ZIP archive
DWORD WINAPI UnzipItemToFileW(HUNZIP, const unsigned short *, ZIPENTRY *);
DWORD WINAPI UnzipItemToFileA(HUNZIP, const char *, ZIPENTRY *);

#define UnzipItemToFile UnzipItemToFileW
#define UNZIPITEMTOFILENAME "UnzipItemToFileW"
typedef DWORD WINAPI UnzipItemToFilePtr(HUNZIP, const unsigned short *, ZIPENTRY *);

DWORD WINAPI UnzipItemToHandle(HUNZIP, HANDLE, ZIPENTRY *);
#define UNZIPITEMTOHANDLENAME "UnzipItemToHandle"
typedef DWORD WINAPI UnzipItemToHandlePtr(HUNZIP, HANDLE, ZIPENTRY *);

DWORD WINAPI UnzipItemToBuffer(HUNZIP, void *, DWORD, ZIPENTRY *);
#define UNZIPITEMTOBUFFERNAME "UnzipItemToBuffer"
typedef DWORD WINAPI UnzipItemToBufferPtr(HUNZIP, void *, DWORD, ZIPENTRY *);

// Function to set the base directory
DWORD WINAPI UnzipSetBaseDirW(HUNZIP, const unsigned short *);
DWORD WINAPI UnzipSetBaseDirA(HUNZIP, const char *);

#define UnzipSetBaseDir UnzipSetBaseDirW
#define UNZIPSETBASEDIRNAME "UnzipSetBaseDirW"
typedef DWORD WINAPI UnzipSetBaseDirPtr(HUNZIP, const unsigned short *);

// Function to close an archive
DWORD WINAPI UnzipClose(HUNZIP);
#define UNZIPCLOSENAME "UnzipClose"
typedef DWORD WINAPI UnzipClosePtr(HUNZIP);

// Function to get an appropriate error message for a given error code return by Unzip functions
DWORD WINAPI UnzipFormatMessageW(DWORD, unsigned short *, DWORD);
DWORD WINAPI UnzipFormatMessageA(DWORD, char *, DWORD);

#define UnzipFormatMessage UnzipFormatMessageW
#define UNZIPFORMATMESSAGENAME "UnzipFormatMessageW"
typedef DWORD WINAPI UnzipFormatMessagePtr(DWORD, unsigned short *, DWORD);

#if !defined(ZR_OK)
// These are the return codes from Unzip functions
#define ZR_OK			0		// Success
// The following come from general system stuff (e.g. files not openable)
#define ZR_NOFILE		1		// Can't create/open the file
#define ZR_NOALLOC		2		// Failed to allocate memory
#define ZR_WRITE		3		// A general error writing to the file
#define ZR_NOTFOUND		4		// Can't find the specified file in the zip
#define ZR_MORE			5		// There's still more data to be unzipped
#define ZR_CORRUPT		6		// The zipfile is corrupt or not a zipfile
#define ZR_READ			7		// An error reading the file
#define ZR_NOTSUPPORTED	8		// The entry is in a format that can't be decompressed by this Unzip add-on
// The following come from mistakes on the part of the caller
#define ZR_ARGS			9		// Bad arguments passed
#define ZR_NOTMMAP		10		// Tried to ZipGetMemory, but that only works on mmap zipfiles, which yours wasn't
#define ZR_MEMSIZE		11		// The memory-buffer size is too small
#define ZR_FAILED		12		// Already failed when you called this function
#define ZR_ENDED		13		// The zip creation has already been closed
#define ZR_MISSIZE		14		// The source file size turned out mistaken
#define ZR_ZMODE		15		// Tried to mix creating/opening a zip 
// The following come from bugs within the zip library itself
#define ZR_SEEK			16		// trying to seek in an unseekable file
#define ZR_NOCHANGE		17		// changed its mind on storage, but not allowed
#define ZR_FLATE		18		// An error in the de/inflation code
#define ZR_PASSWORD		19		// Password is incorrect
#endif

#else	//#if defined(WINCE) //#elif defined(__BREW_3X__) || defined(__SYMBIAN32__)

#if defined(_WIN32) && !defined(WIN32)		//LHC
#define WIN32
#endif

#ifdef WIN32
#include <windows.h>
#else
#include <sys/time.h>
#include <limits.h>
#ifndef DWORD
#define WINAPI
	typedef unsigned long DWORD;
	typedef void * HANDLE;
#define MAX_PATH	PATH_MAX
	typedef unsigned char BYTE;
	typedef unsigned short WORD;
	typedef unsigned int BOOL;
#endif
#endif

	// An HUNZIP identifies a zip archive that has been opened
#define HUNZIP	void *

	// Struct used to retrieve info about an entry in an archive
#ifdef UNICODE
	typedef struct
	{
		DWORD			Index;					// index of this entry within the zip archive
		DWORD			Attributes;				// attributes, as in GetFileAttributes.
#ifdef WIN32
		FILETIME		AccessTime, CreateTime, ModifyTime;	// access, create, modify filetimes
#else
		time_t		AccessTime, CreateTime, ModifyTime;
#endif
		unsigned long	CompressedSize;			// sizes of entry, compressed and uncompressed. These
		unsigned long	UncompressedSize;		// may be -1 if not yet known (e.g. being streamed in)
		unsigned short			Name[MAX_PATH];			// entry name
	} ZIPENTRY;
#else
	typedef struct
	{
		DWORD			Index;
		DWORD			Attributes;
#ifdef WIN32
		FILETIME		AccessTime, CreateTime, ModifyTime;	// access, create, modify filetimes
#else
		time_t		AccessTime, CreateTime, ModifyTime;
#endif
		unsigned long	CompressedSize;
		unsigned long	UncompressedSize;
		char			Name[MAX_PATH];
	} ZIPENTRY;
#endif

	// Functions for opening a ZIP archive
	DWORD WINAPI UnzipOpenFileA(HUNZIP *, const char *, const char *);
	DWORD WINAPI UnzipOpenFileW(HUNZIP *, const unsigned short *, const char *);
	DWORD WINAPI UnzipOpenFileRawA(HUNZIP *, const char *, const char *);
	DWORD WINAPI UnzipOpenFileRawW(HUNZIP *, const unsigned short *, const char *);
#ifdef UNICODE
#define UnzipOpenFile UnzipOpenFileW
#define UNZIPOPENFILENAME "UnzipOpenFileW"
	typedef DWORD WINAPI UnzipOpenFilePtr(HUNZIP *, const unsigned short *, const char *);
#define UnzipOpenFileRaw UnzipOpenFileRawW
#define UNZIPOPENFILERAWNAME "UnzipOpenFileRawW"
	typedef DWORD WINAPI UnzipOpenFileRawPtr(HUNZIP *, const unsigned short *, const char *);
#else
#define UnzipOpenFile UnzipOpenFileA
#define UNZIPOPENFILENAME "UnzipOpenFileA"
	typedef DWORD WINAPI UnzipOpenFilePtr(HUNZIP *, const char *, const char *);
#define UnzipOpenFileRaw UnzipOpenFileRawA
#define UNZIPOPENFILERAWNAME "UnzipOpenFileRawA"
	typedef DWORD WINAPI UnzipOpenFileRawPtr(HUNZIP *, const char *, const char *);
#endif
	DWORD WINAPI UnzipOpenBuffer(HUNZIP *, void *, DWORD, const char *);
#define UNZIPOPENBUFFERNAME "UnzipOpenBuffer"
	typedef DWORD WINAPI UnzipOpenBufferPtr(HUNZIP *, void *, DWORD, const char *);
	DWORD WINAPI UnzipOpenBufferRaw(HUNZIP *, void *, DWORD, const char *);
#define UNZIPOPENBUFFERRAWNAME "UnzipOpenBufferRaw"
	typedef DWORD WINAPI UnzipOpenBufferRawPtr(HUNZIP *, void *, DWORD, const char *);
	DWORD WINAPI UnzipOpenHandle(HUNZIP *, HANDLE, const char *);
#define UNZIPOPENHANDLENAME "UnzipOpenHandle"
	typedef DWORD WINAPI UnzipOpenHandlePtr(HUNZIP *, HANDLE, const char *);
	DWORD WINAPI UnzipOpenHandleRaw(HUNZIP *, HANDLE, const char *);
#define UNZIPOPENHANDLERAWNAME "UnzipOpenHandleRaw"
	typedef DWORD WINAPI UnzipOpenHandleRawPtr(HUNZIP *, HANDLE, const char *);
	DWORD WINAPI UnzipOpenHttpGzipBuffer(HUNZIP *, void *, DWORD, const char *);		//LHC add for decompressing http gzip data
#define UNZIPOPENHTTPGZIPBUFFERNAME "UnzipOpenHttpGzipBuffer"
	typedef DWORD WINAPI UnzipOpenHttpGzipBufferPtr(HUNZIP *, HANDLE, const char *);

	// Functions to get information about an "entry" within a ZIP archive		//LHC
	char* WINAPI UnzipGetRootDir(HUNZIP);
#define UNZIPGETROOTDIR "UnzipGetRootDir"										//LHC

	// Functions to get information about an "entry" within a ZIP archive
	DWORD WINAPI UnzipGetItemW(HUNZIP, ZIPENTRY *);
	DWORD WINAPI UnzipGetItemA(HUNZIP, ZIPENTRY *);
#ifdef UNICODE
#define UnzipGetItem UnzipGetItemW
#define UNZIPGETITEMNAME "UnzipGetItemW"
#else
#define UnzipGetItem UnzipGetItemA
#define UNZIPGETITEMNAME "UnzipGetItemA"
#endif
	typedef DWORD WINAPI UnzipGetItemPtr(HUNZIP, ZIPENTRY *);

	DWORD WINAPI UnzipFindItemW(HUNZIP, ZIPENTRY *, BOOL);
	DWORD WINAPI UnzipFindItemA(HUNZIP, ZIPENTRY *, BOOL);
#ifdef UNICODE
#define UnzipFindItem UnzipFindItemW
#define UNZIPFINDITEMNAME "UnzipFindItemW"
#else
#define UnzipFindItem UnzipFindItemA
#define UNZIPFINDITEMNAME "UnzipFindItemA"
#endif
	typedef DWORD WINAPI UnzipFindItemPtr(HUNZIP, ZIPENTRY *, BOOL);

	// Functions to unzip an "entry" within a ZIP archive
	DWORD WINAPI UnzipItemToFileW(HUNZIP, const unsigned short *, ZIPENTRY *);
	DWORD WINAPI UnzipItemToFileA(HUNZIP, const char *, ZIPENTRY *);
#ifdef UNICODE
#define UnzipItemToFile UnzipItemToFileW
#define UNZIPITEMTOFILENAME "UnzipItemToFileW"
	typedef DWORD WINAPI UnzipItemToFilePtr(HUNZIP, const unsigned short *, ZIPENTRY *);
#else
#define UnzipItemToFile UnzipItemToFileA
#define UNZIPITEMTOFILENAME "UnzipItemToFileA"
	typedef DWORD WINAPI UnzipItemToFilePtr(HUNZIP, const char *, ZIPENTRY *);
#endif

	DWORD WINAPI UnzipItemToHandle(HUNZIP, HANDLE, ZIPENTRY *);
#define UNZIPITEMTOHANDLENAME "UnzipItemToHandle"
	typedef DWORD WINAPI UnzipItemToHandlePtr(HUNZIP, HANDLE, ZIPENTRY *);

	DWORD WINAPI UnzipItemToBuffer(HUNZIP, void *, DWORD, ZIPENTRY *);
#define UNZIPITEMTOBUFFERNAME "UnzipItemToBuffer"
	typedef DWORD WINAPI UnzipItemToBufferPtr(HUNZIP, void *, DWORD, ZIPENTRY *);

	// Function to set the base directory
	DWORD WINAPI UnzipSetBaseDirW(HUNZIP, const unsigned short *);
	DWORD WINAPI UnzipSetBaseDirA(HUNZIP, const char *);
#ifdef UNICODE
#define UnzipSetBaseDir UnzipSetBaseDirW
#define UNZIPSETBASEDIRNAME "UnzipSetBaseDirW"
	typedef DWORD WINAPI UnzipSetBaseDirPtr(HUNZIP, const unsigned short *);
#else
#define UnzipSetBaseDir UnzipSetBaseDirA
#define UNZIPSETBASEDIRNAME "UnzipSetBaseDirA"
	typedef DWORD WINAPI UnzipSetBaseDirPtr(HUNZIP, const char *);
#endif

	// Function to close an archive
	DWORD WINAPI UnzipClose(HUNZIP);
#define UNZIPCLOSENAME "UnzipClose"
	typedef DWORD WINAPI UnzipClosePtr(HUNZIP);

	// Function to get an appropriate error message for a given error code return by Unzip functions
	DWORD WINAPI UnzipFormatMessageW(DWORD, unsigned short *, DWORD);
	DWORD WINAPI UnzipFormatMessageA(DWORD, char *, DWORD);
#ifdef UNICODE
#define UnzipFormatMessage UnzipFormatMessageW
#define UNZIPFORMATMESSAGENAME "UnzipFormatMessageW"
	typedef DWORD WINAPI UnzipFormatMessagePtr(DWORD, unsigned short *, DWORD);
#else
#define UnzipFormatMessage UnzipFormatMessageA
#define UNZIPFORMATMESSAGENAME "UnzipFormatMessageA"
	typedef DWORD WINAPI UnzipFormatMessagePtr(DWORD, char *, DWORD);
#endif

#if !defined(ZR_OK)
	// These are the return codes from Unzip functions
#define ZR_OK			0		// Success
	// The following come from general system stuff (e.g. files not openable)
#define ZR_NOFILE		1		// Can't create/open the file
#define ZR_NOALLOC		2		// Failed to allocate memory
#define ZR_WRITE		3		// A general error writing to the file
#define ZR_NOTFOUND		4		// Can't find the specified file in the zip
#define ZR_MORE			5		// There's still more data to be unzipped
#define ZR_CORRUPT		6		// The zipfile is corrupt or not a zipfile
#define ZR_READ			7		// An error reading the file
#define ZR_NOTSUPPORTED	8		// The entry is in a format that can't be decompressed by this Unzip add-on
	// The following come from mistakes on the part of the caller
#define ZR_ARGS			9		// Bad arguments passed
#define ZR_NOTMMAP		10		// Tried to ZipGetMemory, but that only works on mmap zipfiles, which yours wasn't
#define ZR_MEMSIZE		11		// The memory-buffer size is too small
#define ZR_FAILED		12		// Already failed when you called this function
#define ZR_ENDED		13		// The zip creation has already been closed
#define ZR_MISSIZE		14		// The source file size turned out mistaken
#define ZR_ZMODE		15		// Tried to mix creating/opening a zip 
	// The following come from bugs within the zip library itself
#define ZR_SEEK			16		// trying to seek in an unseekable file
#define ZR_NOCHANGE		17		// changed its mind on storage, but not allowed
#define ZR_FLATE		18		// An error in the de/inflation code
#define ZR_PASSWORD		19		// Password is incorrect
#endif

#endif	//#if defined(WINCE) //#elif defined(__BREW_3X__) || defined(__SYMBIAN32__)

#ifdef __cplusplus
}
#endif

#endif // _LITEUNZIP_H
