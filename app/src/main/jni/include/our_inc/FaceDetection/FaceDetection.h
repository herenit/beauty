#pragma once

#ifndef WIN32
#define __stdcall
#endif

#ifndef _RECT_
#define _RECT_
struct Rect
{
    int left;
    int top;
    int right;
    int bottom;
};
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

#ifdef __cplusplus
extern "C"
{
#endif	//	__cplusplus

	int __stdcall FaceDetectSetLibPath(const char *szLibPath);

    int __stdcall FaceDetectInit();
	
	int __stdcall FaceDetectUninit();

    /*
        Using pDetectROI to choose an ROI area where you want to find the faces in the image.
        If pDetectROI is set to 0, the whole image area is used to find faces.
    */
    int __stdcall FaceDetect(const unsigned char *pbyImageData, int nImgWidth, int nImgHeight, 
        int nMinFaceSize, int nMaxFaceSize, FaceRect *pFaceRect, int *pFaceNum, 
        const Rect *pDetectROI = 0);
#ifdef __cplusplus
}
#endif
