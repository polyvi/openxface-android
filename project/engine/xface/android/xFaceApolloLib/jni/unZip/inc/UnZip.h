#ifndef _UNZIP_MANAGER_H_
#define _UNZIP_MANAGER_H_

#include "LiteUnzip.h"

class UnZip
{
private:

	ZIPENTRY* getZipEntry(int index);

public:
	UnZip(const unsigned short* pPkgPath);
	~UnZip();

	int getPkgItemNumber();

	unsigned short* getFileNameByIndex(int index);

	int getFileLength(int index);

	int getTotalFilesSize();

	int unpackFileToPath(unsigned short* path, int index);
	
	int getlen(char *result);
	
	int getDirLen();

private:
	HUNZIP			huz;
	int				itemNum;
	int				unpackSize;
	char*	        pkgDir;
	int             dirLen;
	ZIPENTRY		cur_entry;
};

#endif