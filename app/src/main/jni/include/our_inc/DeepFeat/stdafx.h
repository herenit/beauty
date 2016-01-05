// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once

#ifdef WIN32
#include "targetver.h"

#define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers
// Windows Header Files:
#include <windows.h>

// TODO: reference additional headers your program requires here
#ifndef SLASH	// SLASH
#define SLASH _T("\\")
#define SLASHCHAR _T('\\')
#define XSLASHCHAR _T('/')
#endif	// SLASH
#else		// linux version
#include <vector>  // min max used befor C header 
#include <fstream> // min max used befor C header
#include <sstream> // min max used befor C header

#define NOMINMAX   // define in std::vector
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>

#include <locale>
#include <cerrno>
#include <cstddef>

#define strcpy_s(x,y,z) strncpy((x), (z), (y))
#define strcat_s strcat
#define memcpy_s memcpy
#define nullptr NULL
#define _tcscpy_s strcpy
#define _tcscat_s strcat
#define _T(x)  (x)

#ifdef __THID_LICENSE_DEBUG 
#define OutputDebugString printf
#else
#define OutputDebugString
#endif

#define __time64_t long long
#define __stdcall
#define _countof(x) (sizeof(x)/sizeof(x[0]))  //works in sampke usage

#ifndef SLASH
#define SLASH "/"
#define SLASHCHAR '/'
#define XSLASHCHAR '/'
#endif

#define FLT_MAX             3.40282347e+38F
#define FLT_MIN             1.17549435e-38F

#define _MAX_PATH           256

inline void * _aligned_malloc(size_t size, size_t alignment)
{
	return valloc(size);
}

inline void _aligned_free(void *memblock)
{
	if (memblock)
		free(memblock);
}


#endif	//	WIN32
