#include "FaceAlignment/FaceAlignment.h"
#include "FaceAlignment/ShapeRegressor.h"
#include "ErrorDefine.h"
#include "smart_ptr.h"
#include <fstream>
#include <memory>
#include <assert.h>
#include <math.h>

#define _MAX_PATH 256
#define FACE_ALIGNMENT_RES_FILE "libfa.dll"
#define FEATURE_POINTS_NUMBER 29

namespace 
{
    char g_szFALibPath[_MAX_PATH] = {0};
    static volatile bool g_isFAInited = false;
    static volatile int g_nFAInitCount = 0;
    ShapeRegressor g_FA;
}

int __stdcall FaceAlignmentSetLibPath(const char *szLibPath)
{
    if (g_isFAInited)
        return ERR_NONE;
    
    if (szLibPath == NULL)
		return ERR_INVALIDCALL;
		
	strncpy(g_szFALibPath, szLibPath, _MAX_PATH);
	
	size_t len = strlen(g_szFALibPath);
	if (len != 0)
	{
	#ifdef WIN32
		if (g_szFALibPath[len - 1] != '\\')
			strcat(g_szFALibPath, "\\");
	#else
	    if (g_szFALibPath[len - 1] != '/')
	        strcat(g_szFALibPath, "/");
	#endif
	}

	return ERR_NONE;
}

int __stdcall FaceAlignmentInit()
{
    std::locale::global(std::locale(""));
    
    if (g_isFAInited)
    {
        ++g_nFAInitCount;
        return ERR_NONE;
    }

	int retValue = ERR_NONE;

#ifndef WIN32	
	if (strlen(g_szFALibPath) == 0)
		strncpy(g_szFALibPath, "./", _MAX_PATH);
#endif

	try
	{
		std::string strDllPath;
		strDllPath = g_szFALibPath;
        strDllPath += FACE_ALIGNMENT_RES_FILE;

        // Put your initial alignment function here
        g_FA.Load(strDllPath);
	}
	catch (const std::bad_alloc &)
	{
		retValue = ERR_MEMORYALLOC;
	}
	catch (const int &errCode)
	{
		retValue = errCode;
	}
	catch (...)
	{
		retValue = ERR_UNKNOWN;
	}
	
	if (ERR_NONE == retValue)
    {
        ++g_nFAInitCount;
        g_isFAInited = true;
    }

	return retValue;
}
	
int __stdcall FaceAlignmentUninit()
{
    if (!g_isFAInited)
        return ERR_NONE;
    
    --g_nFAInitCount;
    if (0 == g_nFAInitCount)
    {
        // Put your uninitial alignment function here
        g_FA.Release();
        g_isFAInited = false;
    }
    
    return ERR_NONE;
}

/*
    Using pDetectROI to choose an ROI area where you want to find the faces in the image.
    If pDetectROI is set to 0, the whole image area is used to find faces.
*/
int __stdcall FaceAlignment(const unsigned char *pbyImageData, int nImgWidth, int nImgHeight, 
    const FaceRect *pFaceRect, FeaPointF *pFeaPoints)
{
    if (!g_isFAInited)
        return ERR_SDKNOINIT;
        
    if (0 == pbyImageData || 0 >= nImgWidth || 0 >= nImgHeight || 0 == pFaceRect || 0 == pFeaPoints)
        return ERR_INVALIDCALL;
        
    int retValue = ERR_NONE;
    // Put your alignment function here
    cv::Mat imgData(nImgHeight, nImgWidth, CV_8UC1, (void *)pbyImageData);
    BoundingBox faceBox;
    faceBox.start_x = pFaceRect->left;
    faceBox.start_y = pFaceRect->top;
    faceBox.width = pFaceRect->right - pFaceRect->left + 1;
    faceBox.height = pFaceRect->bottom - pFaceRect->top + 1;
    faceBox.centroid_x = faceBox.start_x + faceBox.width/2.0;
    faceBox.centroid_y = faceBox.start_y + faceBox.height/2.0;
    cv::Mat_<double> current_shape = g_FA.Predict((cv::Mat_<uchar> &)imgData,faceBox,20);
    
    for (int i = 0; i < FEATURE_POINTS_NUMBER; ++i)
    {
        pFeaPoints[i].x = current_shape(i,0);
        pFeaPoints[i].y = current_shape(i,1);
    }

	return retValue;
}

