#include "MemoryManager.h"
#include <malloc.h>

void * XF_MEM_malloc(unsigned int size)
{
    if ( size == 0 )
    {
        return NULL;
    }

    void* mem = NULL;
    void* block = malloc(size);
    if ( block != NULL )
    {
        mem = block;
    }
    return mem;
}

void * XF_MEM_realloc(void *pMemBlock, unsigned int size)
{
    if ( size == 0 )
    {
        return NULL;
    }

    void* block = NULL;
    if ( pMemBlock == NULL )
    {
        block = XF_MEM_malloc(size);
    }
    else
    {
        block = realloc(pMemBlock, size);
    }
    return block;
}

void * XF_MEM_calloc(unsigned int count, unsigned int size)
{
    if ( (count == 0) || (size == 0) )
    {
        return NULL;
    }
    return calloc(count, size);
}

void XF_MEM_free(void *pMemBlock)
{
    if ( pMemBlock != NULL )
    {
        free(pMemBlock);
    }
}
