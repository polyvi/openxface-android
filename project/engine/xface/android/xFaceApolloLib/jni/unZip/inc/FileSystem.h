#ifndef XF_FILE_H
#define XF_FILE_H

#ifdef __cplusplus
extern "C" {
#endif

#define XF_FILE_RDONLY  0x00   ///<只读
#define XF_FILE_WRONLY  0x01   ///<只写
#define XF_FILE_RDWR    0x02   ///<读和写
#define XF_FILE_APPEND  0x10   ///<追加
#define XF_FILE_CREAT   0x40   ///<创建

#define XF_FILE_BEGIN           0///<文件开始
#define XF_FILE_CURRENT         1///<文件当前位置
#define XF_FILE_END             2///<文件结束位置

/** 递归的创建目录，当目录路径中包含不存在的文件夹时，创建对应的文件夹。
 *  @param[in] pDirPath:  目录的路径（完整路径，分隔符为'/'，unicode编码，串尾带'/'，宽字符'\0'结尾）。
 *  @return
 *     -   XF_OK:       成功（创建成功或对应的文件夹已经存在）
 *     -   XF_ERROR:    失败
 */
int XF_FILE_mkdir(const unsigned short* pDirPath);

typedef void * XF_File;    ///<文件句柄

/** 打开文件
 *  @param[in] pFilePath:  文件路径（完整路径，分隔符为'/'，unicode编码，宽字符'\0'结尾）
 *  @param[in] flags:      文件的打开方式，支持以下几种打开方式：
 *  \n XF_FILE_RDONLY      以只读的方式打开文件。
 *  \n XF_FILE_WRONLY      以只写的方式打开文件。（不会清空文件中已经存在的内容，与标准C的API不一样）
 *  \n XF_FILE_RDWR        以读写的方式打开文件。
 *  \n XF_FILE_CREAT       如果文件不存在，创建该文件。
 *  \n 注：
 *  \n 1 XF_FILE_RDONLY，XF_FILE_WRONLY，XF_FILE_RDWR三种方式不可合用。
 *  \n 2 如果要使用XF_FILE_CREAT，必须与前三种方式的一种合用。
 *  @return
 *     - NOT NULL:  成功（文件句柄）
 *     - NULL:      失败
 */
XF_File XF_FILE_open(const unsigned short* pFilePath, unsigned int flags);

/** 关闭文件
 *  @param[in] fileHandle:  文件句柄（XF_FILE_open返回值）
 *  @return
 *     -   XF_OK:       成功
 *     -   XF_ERROR:    失败
 */
int XF_FILE_close(const XF_File fileHandle);

/** 对被打开的文件进行写操作，文件系统将从缓冲区buffer中写count个字节到该文件中，并且从该文件的当前
 *  操作位置开始写入，如果操作成功，该文件系统调用将返回一个大于或是等于0的整数来表示实际写入的数据
 *  字节数，如果操作失败，则返回XF_ERROR。
 *  @param[in] fileHandle:  文件句柄（XF_FILE_open()返回值）。
 *  @param[in] pBuffer:     要写的数据（缓存）。
 *  @param[in] size:        要写入的数据长度（单位Byte）。
 *  @return
 *     -   >=0:         成功(实际写入的数据字节数)
 *     -   XF_ERROR:    失败
 */
int XF_FILE_write(const XF_File fileHandle, const void *pBuffer, unsigned int size);

/** 移动文件指针,如果offset超过了文件本身的大小，函数将返回XF_ERROR，文件指针位置不变。
 *  @param[in] fileHandle:  文件句柄（XF_FILE_open()的返回值）。
 *  @param[in] offset:      以文件开头为基准的偏移量，单位Byte。（如果值为负则函数直接返回失败）
 *  @param[in] moveMethod:
 *  XF_FILE_BEGIN - 设定位置等于 offset(默认).
 *  XF_FILE_CURRENT - 设定位置为当前位置加上 offset.
 *  XF_FILE_END - 设定位置为文件末尾加上 offset (要移动到文件尾之前的位置, offset必须是一个负值).
 *  @return
 *     -   XF_OK:       成功
 *     -   XF_ERROR:    失败
 */
int XF_FILE_seek(const XF_File fileHandle, int offset, int moveMethod);

/** 对被打开的文件进行读操作，文件系统将从该文件的当前操作位置开始读出length个字节的数据到缓冲区buffer中，
 *  如果操作成功，该文件系统调用将返回一个大于或是等于0的整数来表示实际读出的数据字节数；如果操作失败，则返回XF_ERROR。
 *  @param[in] fileHandle:  文件句柄（XF_FILE_open()返回值）。
 *  @param[out] pBuffer:    读取结果（缓存由调用者分配和释放）。
 *  @param[in] length:      要读取的长度（由调用者保证缓存够用不越界），单位Byte。
 *  @return
 *     -   >=0:         成功(实际读出的数据字节数)
 *     -   XF_ERROR:    失败
 */
int XF_FILE_read(const XF_File fileHandle, void *pBuffer, unsigned int length);

/** 获取文件指针当前偏移量（与XF_FILE_seek()对应）。
 *  @param[in] fileHandle:  文件句柄（XF_FILE_open()返回值）。
 *  @return
 *     -   >=0:         成功(文件指针相对文件头的偏移量,单位Byte)
 *     -   XF_ERROR:    失败
 */
int XF_FILE_tell(const XF_File fileHandle);

/**判断文件是否是一个目录。
*  @param[in] pPath:        文件（夹）路径（完整路径，分隔符为'/'，unicode编码，宽字符'\0'结尾）
*  @return
*     -   XF_TRUE/XF_FALSE:         成功
*     -   XF_ERROR:                 失败
*/
int XF_FILE_isDirectory(const unsigned short *pPath);

/**获取文件大小（单位Byte)。
 *  @param[in] fileHandle:  文件句柄（XF_FILE_open()返回值）。
 *  @return
 *     -   >=0:         成功（文件的大小）
 *     -   XF_ERROR:    失败
 */
int XF_FILE_getLength(const XF_File fileHandle);

#if defined(ANDROID) || defined(STLinux)
#endif

#ifdef __cplusplus
}
#endif


#endif
