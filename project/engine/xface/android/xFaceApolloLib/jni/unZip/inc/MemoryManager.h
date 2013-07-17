#ifndef XF_MEMORY_H
#define XF_MEMORY_H

#ifdef __cplusplus 
extern "C" { 
#endif 

/**分配内存块。
 * 从堆中分配一个大小为size字节的内存空间，如果没有足够的内存空间或size为0，返回NULL。 
 * @param[in] size:    需要分配的内存空间大小（单位Byte）
 * @return
 *    -   NOT NULL: 成功（新分配的内存地址）
 *    -   NULL:     失败
 */
void * XF_MEM_malloc(unsigned int size);

/**重新分配内存块，缩小或扩大已分配内存块。返回重新分配块的地址，并把原地址内容拷贝到新位置上，
 * 如果块不能被重新分配或者size为0，返回NULL。
 * @param[in] pMemBlock 原内存空间地址，指向通过调用XF_MEM_malloc()、XF_MEM_calloc()或XF_MEM_realloc()已得到的内存块，
 *                      如果pMemBlock是一个空指针，那么XF_MEM_realloc()与XF_MEM_malloc()相同。 
 * @param[in] size 重新分配的空间大小
 * @return
 *    -   NOT NULL: 成功（新分配的内存地址）
 *    -   NULL:     失败
 */
void * XF_MEM_realloc(void *pMemBlock, unsigned int size);

/**分配多个连续内存块。
 * 分配count个长度为size的连续空间，该块被清除为0。如果没有足够的内存空间或在count或size为0，则返回NULL。 
 * @param[in] count:   分配的块个数
 * @param[in] size:    每块的大小（单位Byte）
 * @return
 *    -   NOT NULL: 成功（新分配的内存地址）
 *    -   NULL:     失败
 */
void * XF_MEM_calloc(unsigned int count, unsigned int size);

/**释放内存块。
 * 释放由XF_MEM_malloc()、XF_MEM_calloc()或XF_MEM_realloc()函数调用所分配的内存空间，当pMemBlock为NULL时不做任何操作。 
 * @param[in] pMemBlock:     需要释放的内存空间地址
 */
void XF_MEM_free(void *pMemBlock);


#ifdef __cplusplus 
} 
#endif  

#endif
