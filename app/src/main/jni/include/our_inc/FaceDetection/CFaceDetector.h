#pragma once

#include "ErrorDefine.h"
#include "FaceStructure.h"
#include <vector>
#include <iostream>
#include <algorithm>

class CFaceDetector
{
private:
	FaceDetectParam m_FrontalParam;
	FaceDetectParam m_SideParam;
	unsigned char m_LBPUniformIndex[256];

public:
	int InitFaceDetect(const char *pResName);

	int ReadFaceDetectParam(std::fstream &fp);

	int UninitFaceDetect();

	int FaceDetect(const unsigned char *pGrayImage, int imageWidth, int imageHeight, int minFaceSize, int maxFaceSize,
		std::vector<FaceDetected> &pFaceRect, const Rect *pDetectROI = NULL);

	int MultiViewFaceDetect(const unsigned char *pGrayImage,
		int imageWidth, int imageHeight, int minFaceSize, int maxFaceSize,
		std::vector<FaceDetected> &pFaceRect, const Rect *pDetectROI = NULL);

	void ScaleImageV4(int scaleHeight, int scaleWidth, const unsigned char *pImage, int width, int height,
		int widthStep, unsigned char *pScaledImage, int scaleWidthStep);

	void DetectFaceInOneScaleV4(int height, const FaceDetectParam * pDetectParam, const unsigned char* image, int width, int widthStep,
		int stepH, int stepW, int factor, int angle, bool flag, Rect rectROI, std::vector<FaceDetected> &vecFaceCandidate);

	void GetIntPtr(const int *pIntImage, const FaceDetectParam * pParam, int itwidth, IntegalPointer *pIntPtr);

	int FindFaceV4(int innerWidth, int innerHeight, int stepHeight, int stepWidth, int integalWidth, int width,
		const FaceDetectParam * pParam, int factor, int angle, bool flag, const IntegalPointer *pIntPtr, Rect rectROI, std::vector<FaceDetected> &vecFaceCandidate);

	int ClassifyFaceV4(const FaceDetectParam *pParam, int startPos, const IntegalPointer *pIntegalPtr, float &score);

	int ScaleByWHV4(const unsigned char *pImage, int widthStep, int height, unsigned char *pScaledImage,
		int scaleWidthStep, int scaleWidth, int scaleHeight, int scaleInnerWidth, const int *pWRatio, const int *pHRatio, int *pSpaceA, int *pSpaceB);

	int GetIntImage(const unsigned char * image, int height, int width, int step, int *IntImage, int itwidth);

	int ReadParam(std::fstream &fp, FaceDetectParam &param);

	void MirrorImage(unsigned char *pDstImage, const unsigned char *pSrcImage, int scaledHeight, int scaledWidth, int scaleWidthStep);
};

