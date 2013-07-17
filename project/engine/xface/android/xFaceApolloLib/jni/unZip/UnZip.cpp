#include "UnZip.h"
#include "FileSystem.h"

ZIPENTRY* UnZip::getZipEntry(int index)
{
	if(index < 0)
	{
		return NULL;
	}
	cur_entry.Index = index;
	UnzipGetItem(huz, &cur_entry);
	return &cur_entry;
}

UnZip::UnZip(const unsigned short* pPkgPath)
{
	itemNum = 0;
	unpackSize = 0;
	cur_entry.Index = (DWORD)-1;	
	huz = NULL;

	UnzipOpenFile(&huz, pPkgPath, NULL);		
	char* pkgdir_char = UnzipGetRootDir(huz);
	pkgDir = pkgdir_char;
    dirLen = getlen(pkgdir_char);
	ZIPENTRY ze;
	ze.Index = (DWORD)-1;
	UnzipGetItem(huz, &ze);
	itemNum = ze.Index;
	getTotalFilesSize();
}

UnZip::~UnZip()
{
		UnzipClose(huz);
}

int UnZip::getPkgItemNumber()
{
	return itemNum;
}

unsigned short* UnZip::getFileNameByIndex(int index)
{
	ZIPENTRY* zipen = getZipEntry(index);
	if(!zipen)
	{
		return NULL;
	}

	unsigned short* fname = (unsigned short*)(zipen->Name);
	return fname;
}

int UnZip::getFileLength(int index)
{
	ZIPENTRY* zipen = getZipEntry(index);
	if(!zipen)
	{
		return -1;
	}

	return (int)(zipen->UncompressedSize);
}

int UnZip::getTotalFilesSize()
{
	int totallen = 0;
	for(int i = 0;i < itemNum;i++)
	{
		int len = getFileLength(i);
		if(len >= 0)
		{
			totallen += len;
		}
		else
		{
			unpackSize = -1;
			break;
		}
	}
	unpackSize = totallen;

	return unpackSize;
}

int UnZip::unpackFileToPath(unsigned short* path, int index)
{
	ZIPENTRY* zipen = getZipEntry(index);
	int liteErrorCode;
	int remainLength = zipen->UncompressedSize;
	int datalen = (remainLength > 16384) ? 16384 : remainLength;
	unsigned char* buffer = new unsigned char[datalen];
	void* handle = XF_FILE_open(path, XF_FILE_CREAT | XF_FILE_WRONLY);
	while(1){
		liteErrorCode = UnzipItemToBuffer(huz, buffer, datalen, zipen);
		if (ZR_OK == liteErrorCode
			|| ZR_MORE == liteErrorCode)
		{
			XF_FILE_write(handle,buffer, datalen);
		}
		if (ZR_MORE == liteErrorCode){
			remainLength -= datalen;
			datalen = (remainLength > datalen) ? datalen : remainLength;
			continue;
		}
		break;
	}
	delete buffer;
	XF_FILE_close(handle);

	return (liteErrorCode == ZR_OK) ? 1 : 0;
}

int UnZip::getlen(char *result){  
     int i=0;  
     while(result[i]!='\0'){  
        i++;  
     }    
    return i;  
 }  

int UnZip::getDirLen() {
    return dirLen;
}
