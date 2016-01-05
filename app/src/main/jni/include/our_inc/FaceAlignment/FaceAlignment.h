#pragma once

#include "FaceDetection/FaceDetection.h"

#ifndef WIN32
#define __stdcall
#endif

#ifndef _FACERECT_
#define _FACERECT_
struct FaceRect 
{
    int left;
    int top;
    int right;
    int bottom;
    float confidence;
    float yaw;
    float pitch;
    float roll;
};
#endif

#ifndef FEAPOINTF
#define FEAPOINTF
struct FeaPointF
{
    float x;
    float y;
};
#endif

#ifndef FEATURE_POINTS_NUMBER
#define FEATURE_POINTS_NUMBER 29
#endif

#ifdef __cplusplus
extern "C"
{
#endif	//	__cplusplus

	int __stdcall FaceAlignmentSetLibPath(const char *szLibPath);

    int __stdcall FaceAlignmentInit();
	
	int __stdcall FaceAlignmentUninit();

    /*
        Using pDetectROI to choose an ROI area where you want to find the faces in the image.
        If pDetectROI is set to 0, the whole image area is used to find faces.
    */
    int __stdcall FaceAlignment(const unsigned char *pbyImageData, int nImgWidth, int nImgHeight, 
       const FaceRect *pFaceRect, FeaPointF *pFeaPoints);
#ifdef __cplusplus
}
#endif
