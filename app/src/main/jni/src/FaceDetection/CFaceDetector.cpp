// FaceDetect.cpp : Defines the entry point for the console application.
//
#include "smart_ptr.h"
#include "FaceDetection/CFaceDetector.h"
#include <fstream>
#include <memory>
#include <algorithm>
#include <assert.h>

int CFaceDetector::ScaleByWHV4(const unsigned char *pImage, int widthStep, int height, unsigned char *pScaledImage,
	int scaleWidthStep, int scaleWidth, int scaleHeight, int scaleInnerWidth, const int *pWRatio, const int *pHRatio, int *pSpaceA, int *pSpaceB)
{
	int *pCurSpace = NULL;
	int *pWorkSpace = NULL;

	int prevH = -1;
	int calcCount = 0;
	for (int k = 0; k < scaleHeight; ++k)
	{
		int ratioHeight = pHRatio[2 * k];
		const int ratioQ10Height = pHRatio[2 * k + 1];
		const bool isInner = ratioQ10Height > 0 && ratioHeight < height - 1;
		int curH = isInner ? ratioHeight + 1 : ratioHeight;
		const int curHBackup = curH;

		if (ratioHeight == prevH)
		{
			pCurSpace = pSpaceB;

			int *pTemp = pSpaceA;
			pSpaceA = pSpaceB;
			pSpaceB = pTemp;
			calcCount = 1;
		}
		else
		{
			pCurSpace = pSpaceA;
			calcCount = 0;
		}

		while (calcCount < 2)
		{
			if (calcCount == 1 && !isInner)
			{
				pWorkSpace = pSpaceB;
				memcpy(pSpaceB, pCurSpace, sizeof(pCurSpace[0]) * scaleWidth);

				++calcCount;
				continue;
			}
			else if (calcCount)
				pWorkSpace = pSpaceB;
			else
			{
				pWorkSpace = pCurSpace;
				curH = ratioHeight;
			}
			const unsigned char *pCurLine = &pImage[widthStep * curH];

			int counter = 0;
			for (; counter < scaleInnerWidth; ++counter)
			{
				int curIndex = pWRatio[2 * counter];
				pWorkSpace[counter] = (pCurLine[curIndex] << 10) + pWRatio[2 * counter + 1] * (pCurLine[curIndex + 1] - pCurLine[curIndex]);
			}

			for (; counter < scaleWidth; ++counter)
			{
				int curIndex = pWRatio[2 * counter];
				pWorkSpace[counter] = pCurLine[curIndex] << 10;
			}

			if (scaleInnerWidth > 0)
			{
				pCurSpace = pSpaceA;
			}

			curH = curHBackup;
			++calcCount;
		}

		if (!isInner)
		{
			for (int i = 0; i < scaleWidth; ++i)
				pScaledImage[i] = (pCurSpace[i] + 512) << 10 >> 20;
		}
		else
		{
			if (scaleWidth > 0)
			{
				for (int i = 0; i < scaleWidth; ++i)
				{
					pScaledImage[i] = (((pCurSpace[i] + 512) << 10) + ratioQ10Height * (pSpaceB[i] - pCurSpace[i])) >> 20;
				}
			}
		}
		pScaledImage += scaleWidthStep;

		prevH = curH;
	}

	return 1;
}

void CFaceDetector::ScaleImageV4(int scaleHeight, int scaleWidth, const unsigned char *pImage, int width, int height,
	int widthStep, unsigned char *pScaledImage, int scaleWidthStep)
{	
	int scaleInnerWidth = scaleWidth;
	const int widthRatio = ((width << 15) + scaleWidth / 2) / scaleWidth;
	const int heightRatio = ((height << 15) + scaleHeight / 2) / scaleHeight;
	smart_ptr<int> workSpace(2 * scaleHeight + 4 * scaleWidth);	
	
	int *pWRatio = &workSpace[scaleWidth * 2];
	if (scaleWidth > 0)
	{		
		const int mulTwoWidthRatio = 2 * widthRatio;
		int curWidthRatio = widthRatio - 0x8000;
		for (int i = 0; i < scaleWidth; ++i)
		{
			int curRawWRatio = curWidthRatio / 2 >> 15;
			int curQ10WRatio = (curWidthRatio / 2 - (curRawWRatio << 15)) >> 5;
			if (curRawWRatio < 0)
			{
				curRawWRatio = 0;
				curQ10WRatio = 0;
			}
			if (curRawWRatio >= width - 1)
			{
				curQ10WRatio = 0;
				curRawWRatio = width - 1;
				if (scaleInnerWidth >= scaleWidth)
					scaleInnerWidth = i;
			}
			pWRatio[2 * i] = curRawWRatio;
			pWRatio[2 * i + 1] = curQ10WRatio;
			curWidthRatio += mulTwoWidthRatio;			
		}
	}

	int *pHRatio = &pWRatio[2 * scaleWidth];
	if (scaleHeight > 0)
	{		
		const int mulTwoHeightRatio = 2 * heightRatio;
		int curHeightRatio = heightRatio - 0x8000;		
		for (int i = 0; i < scaleHeight; ++i)
		{
			int curRawHRatio = curHeightRatio / 2 >> 15;
			int curQ10HRatio = (curHeightRatio / 2 - (curRawHRatio << 15)) >> 5;
			if (curRawHRatio < 0)
			{
				curRawHRatio = 0;
				curQ10HRatio = 0;
			}			
			pHRatio[2 * i] = curRawHRatio;
			pHRatio[2 * i + 1] = curQ10HRatio;
			curHeightRatio += mulTwoHeightRatio;
		}
	}
	ScaleByWHV4(pImage, widthStep, height, pScaledImage, scaleWidthStep, scaleWidth, scaleHeight, scaleInnerWidth, 
		pWRatio, pHRatio, workSpace, &workSpace[scaleWidth]);
}

void CFaceDetector::MirrorImage(unsigned char *pDstImage, const unsigned char *pSrcImage, int scaledHeight, int scaledWidth, int scaleWidthStep)
{
	for (int i = 0; i < scaledHeight; ++i)
	{		
		for (int j = 0; j < scaledWidth; ++j)
			pDstImage[j] = pSrcImage[scaledWidth - 1 - j];
		pSrcImage += scaleWidthStep;
		pDstImage += scaleWidthStep;
	}
}

int CFaceDetector::GetIntImage(const unsigned char * image, int height, int width, int step, int *IntImage, int itwidth)
{
	const unsigned char * pimg = image;
	if (!image || !IntImage || width <= 0 || height <= 0 || step <= 0)
	{
		return -1;
	}
	for (int i = 0; i < itwidth; i++)
	{
		IntImage[i] = 0;
	}
	for (int i = 0; i < height + 1; i++)
	{
		IntImage[i*itwidth] = 0;
	}

	//fast method
	int *p = IntImage + itwidth + 1;
	for (int i = 0; i < height; i++)
	{
		int linev = 0;
		for (int j = 1; j < itwidth; j++)
		{
			linev += pimg[j - 1];
			*p = *(p - itwidth) + linev;
			p++;
		}
		p++;
		pimg += step;
	}
	return 0;
}

void CFaceDetector::GetIntPtr(const int *pIntImage, const FaceDetectParam * pParam, int itwidth, IntegalPointer *pIntPtr)
{
	if (!pIntImage || !pParam)
	{
		return;
	}

	ParamTable *pParamTable = pParam->pParamTable;

	int counter = 0;
	for (int i = 0; i < pParam->tableNum; i++)
	{
		const int num = pParamTable[i].num;
		LBPDetectFeatures *pFeatures = pParamTable[i].pFeatures;
		for (int j = 0; j < num; ++j)
		{
			const int left = pFeatures[j].pos[0];
			const int top = pFeatures[j].pos[1];
			const int width = pFeatures[j].pos[2];
			const int height = pFeatures[j].pos[3];

			const int v16 = itwidth * top;
			pIntPtr[counter].ptr[0] = &pIntImage[left + v16];
			pIntPtr[counter].ptr[1] = &pIntImage[left + v16 + width];
			pIntPtr[counter].ptr[2] = &pIntImage[left + v16 + 2 * width];
			pIntPtr[counter].ptr[3] = &pIntImage[left + v16 + 3 * width];

			const int v17 = itwidth * (top + height);
			pIntPtr[counter].ptr[4] = &pIntImage[left + v17];
			pIntPtr[counter].ptr[5] = &pIntImage[left + v17 + width];
			pIntPtr[counter].ptr[6] = &pIntImage[left + v17 + 2 * width];
			pIntPtr[counter].ptr[7] = &pIntImage[left + v17 + 3 * width];

			const int v18 = itwidth * (top + 2 * height);
			pIntPtr[counter].ptr[8] = &pIntImage[left + v18];
			pIntPtr[counter].ptr[9] = &pIntImage[left + v18 + width];
			pIntPtr[counter].ptr[10] = &pIntImage[left + v18 + 2 * width];
			pIntPtr[counter].ptr[11] = &pIntImage[left + v18 + 3 * width];

			const int v19 = itwidth * (height + top + 2 * height);
			pIntPtr[counter].ptr[12] = &pIntImage[left + v19];
			pIntPtr[counter].ptr[13] = &pIntImage[left + v19 + width];
			pIntPtr[counter].ptr[14] = &pIntImage[left + v19 + 2 * width];
			pIntPtr[counter].ptr[15] = &pIntImage[left + v19 + 3 * width];
			++counter;
		}
	}
}

int CFaceDetector::ClassifyFaceV4(const FaceDetectParam *pParam, int startPos, const IntegalPointer *pIntegalPtr, float &score)
{
	int result = 0;
	int v6 = 0;
	int v8 = 0;
	int v9 = 0;
	int v10 = 0;
	int v11 = 0;
	int v12 = 0;
	int v13 = 0;
	int v14 = 0;
	int v15 = 0;
	int v16 = 0;
	int v17 = 0;
	int v18 = 0;
	int v19 = 0;
	int v20 = 0;
	int v21 = 0;
	int v22 = 0;
	int v23 = 0;

	int curIndex = 0;
	for (int j = 0; j < pParam->tableNum; ++j)
	{
		const ParamTable & theTable = pParam->pParamTable[j];
		bool pass = true;
		const IntegalPointer *pPtr = &pIntegalPtr[curIndex];
		const LBPDetectFeatures *pFeatures = theTable.pFeatures;
		v6 = 0;
		//for (int i = 0; i < theTable.num; ++i)
		//{
		//	int xx = -88;
		//	for (int j = 0; j < 59; ++j)
		//	{
		//		if (xx < pFeatures[i].value[j])
		//			xx = pFeatures[i].value[j];
		//	}
		//	v6 += xx;
		//}
		for (int i = 0; i < theTable.num; ++i)
		{
			v8 = pPtr[i].ptr[9][startPos];
			v9 = pPtr[i].ptr[5][startPos];
			v10 = pPtr[i].ptr[6][startPos];
			v11 = pPtr[i].ptr[1][startPos];
			v12 = pPtr[i].ptr[5][startPos];
			v13 = pPtr[i].ptr[2][startPos];
			v14 = pPtr[i].ptr[7][startPos];
			v15 = pPtr[i].ptr[11][startPos];
			v16 = pPtr[i].ptr[14][startPos];
			v17 = pPtr[i].ptr[13][startPos];
			v18 = pPtr[i].ptr[10][startPos] + v9 - v10 - v8;
			v19 = pPtr[i].ptr[8][startPos];
			v20 = pPtr[i].ptr[10][startPos];
			v21 = pPtr[i].ptr[4][startPos];
			v22 = pPtr[i].ptr[0][startPos] + v9 - v21 - v11;

			v23 = v22 >= v18;

			int idx = (v8 + v21 - v19 - v12 >= v18) |
				2 * ((v17 + v19 - pPtr[i].ptr[12][startPos] - v8 >= v18) |
				2 * ((v8 + v16 - v17 - v20 >= v18) |
				2 * ((v20 + pPtr[i].ptr[15][startPos] - v16 - v15 >= v18) |
				2 * ((v10 + v15 - v14 - v20 >= v18) |
				2 * ((v13 + v14 - pPtr[i].ptr[3][startPos] - v10 >= v18) |
				2 * ((v11 + v10 - v13 - v12 >= v18) | 2 * v23))))));

			v6 += pFeatures[i].value[m_LBPUniformIndex[idx]];
			if (v6 < pFeatures[i].threshold)
			{
				pass = false;
				break;
			}
		}

		if (pass)
		{
			++result;

			if (v6 < theTable.threshold)
			{
				result = -1;
				break;
			}

		}
		else
		{
			result = -1;
			break;
		}
		curIndex += theTable.num;

		if (j == pParam->tableNum - 1)
			score = float(double(v6 - theTable.threshold) / double(236042 - theTable.threshold));
	}

	return result;
}

int CFaceDetector::FindFaceV4(int innerWidth, int innerHeight, int stepHeight, int stepWidth, int integalWidth, int width,
	const FaceDetectParam * pParam, int factor, int angle, bool flag, const IntegalPointer *pIntPtr, Rect rectROI, std::vector<FaceDetected> &vecFaceCandidate)
{
	int result = 0;

	for (int i = 0; i < innerHeight; i += stepHeight)
	{
		for (int j = 0; j < innerWidth; j += stepWidth)
		{
			float score = 0;
			int ret = ClassifyFaceV4(pParam, i * integalWidth + j, pIntPtr, score);
			if (ret > 0)
			{
				int y = j;
				if (flag)
				{
					y = width - pParam->normWidth - j - 1;
				}

				short left = (factor * y + 512) >> 10;
				short faceWidth = (factor * pParam->normWidth + 512) >> 10;
				short top = (factor * i + 512) >> 10;
				short faceHeight = (factor * pParam->normHeight + 512) >> 10;
				//short neighbors = 1;

				FaceDetected theCandidate;
				theCandidate.x = left + rectROI.left;
				theCandidate.y = top + rectROI.top;
				theCandidate.faceSize = std::max(faceWidth, faceHeight);
				theCandidate.node = angle;
				theCandidate.score = score;
				vecFaceCandidate.push_back(theCandidate);
			}
		}
	}
	return result;
}

void CFaceDetector::DetectFaceInOneScaleV4(int height, const FaceDetectParam * pDetectParam, const unsigned char* image, int width, int widthStep,
	int stepH, int stepW, int factor, int angle, bool flag, Rect rectROI, std::vector<FaceDetected> &vecFaceCandidate)
{
	if (!image || !pDetectParam)
	{
		return;
	}
	if (pDetectParam->normWidth <= width && pDetectParam->normHeight <= height)
	{
		smart_ptr<int> integalImage((width + 1) * (height + 1));

		smart_ptr<IntegalPointer> intPtr(pDetectParam->totalFeatures);
		//Calculate integrate image
		GetIntImage(image, height, width, widthStep, integalImage, width + 1);
		GetIntPtr(integalImage, pDetectParam, width + 1, intPtr);
		const int innerHeight = height - pDetectParam->normHeight - 1;
		const int innerWidth = width - pDetectParam->normWidth - 1;
		FindFaceV4(innerWidth, innerHeight, stepH, stepW, width + 1, width, pDetectParam, factor, angle, flag, intPtr, rectROI, vecFaceCandidate);
	}
}

int CFaceDetector::ReadParam(std::fstream &fp, FaceDetectParam &param)
{
	short tableNum;
	    
	fp.read(reinterpret_cast<char *>(&tableNum), sizeof(short));
	if (fp.bad())
	    return ERR_FILEIO;
	param.tableNum = tableNum;

	char width, height;
	fp.read(&width, sizeof(char));
	if (fp.bad())
	    return ERR_FILEIO;
	param.normWidth = width;

	fp.read(&height, sizeof(char));
	if (fp.bad())
	    return ERR_FILEIO;
	param.normHeight = height;

	param.pParamTable = new ParamTable[tableNum];
	ParamTable *v7 = param.pParamTable;

	for (int i = 0; i < param.tableNum; i++)
	{
		short v9;
		fp.read(reinterpret_cast<char *>(&v9), sizeof(short));
		v7[i].num = v9;

		fp.read(reinterpret_cast<char *>(&v7[i].threshold), sizeof(int));
		if (fp.bad())
	        return ERR_FILEIO;
			
		v7[i].pFeatures = new LBPDetectFeatures[v9];
		for (int j = 0; j < v9; ++j)
		{			
			char v11;
			for (int k = 0; k < 4; ++k)
			{
				fp.read(&v11, sizeof(char));
				if (fp.bad())
					return ERR_FILEIO;
				v7[i].pFeatures[j].pos[k] = v11;
			}
			int v15;
			fp.read(reinterpret_cast<char *>(&v15), sizeof(int));
			if (fp.bad())
				return ERR_FILEIO;
			v7[i].pFeatures[j].threshold = v15;

			for (int k = 0; k < 59; ++k)
			{
				short valShort;
				fp.read(reinterpret_cast<char *>(&valShort), sizeof(short));
				if (fp.bad())
					return ERR_FILEIO;
				v7[i].pFeatures[j].value[k] = valShort;
			}
		}
	}
	return ERR_NONE;
}

int CFaceDetector::ReadFaceDetectParam(std::fstream &fp)
{
	int retValue = ReadParam(fp, m_FrontalParam);
	if (retValue != ERR_NONE)
	{
		m_FrontalParam.FreeMemory();
		return retValue;
	}
	m_FrontalParam.CalcTotalLBPFeatures();

	retValue = ReadParam(fp, m_SideParam);
	if (retValue != ERR_NONE)
	{
		m_FrontalParam.FreeMemory();
		m_SideParam.FreeMemory();
		return retValue;
	}
	m_SideParam.CalcTotalLBPFeatures();

	short num = 256;
	fp.read(reinterpret_cast<char *>(&num), sizeof(short));
	if (fp.bad())
	{
		m_FrontalParam.FreeMemory();
		m_SideParam.FreeMemory();
		return ERR_FILEIO;
	}

	assert(num == 256);
	fp.read(reinterpret_cast<char *>(m_LBPUniformIndex), sizeof(char) * num);
	if (fp.bad())
	{
		m_FrontalParam.FreeMemory();
		m_SideParam.FreeMemory();
		return ERR_FILEIO;
	}
	return ERR_NONE;
}

int CFaceDetector::InitFaceDetect(const char *pResName)
{
	std::fstream fp;
	fp.open(pResName, std::fstream::in | std::fstream::binary);
	if (!fp.is_open())
	    return ERR_FILEIO;
	    
	return ReadFaceDetectParam(fp);
}

int CFaceDetector::UninitFaceDetect()
{
	m_FrontalParam.FreeMemory();
	m_SideParam.FreeMemory();
	return ERR_NONE;
}

int CFaceDetector::FaceDetect(const unsigned char *pGrayImage, int imageWidth, int imageHeight, int minFaceSize, int maxFaceSize,
	std::vector<FaceDetected> &pFaceRect, const Rect *pDetectROI/* = NULL*/)
{
	if (pGrayImage == NULL || imageWidth <= 0 || imageHeight <= 0)
		return ERR_INVALIDCALL;

	if (imageWidth <= m_FrontalParam.normWidth || imageHeight <= m_FrontalParam.normWidth)
		return ERR_NONE;
	
	Rect rectROI = { 0, 0, imageWidth-1, imageHeight-1 };
	if (pDetectROI != NULL)
	{
		rectROI.left = std::max(rectROI.left, pDetectROI->left);
		rectROI.top = std::max(rectROI.top, pDetectROI->top);
		rectROI.right = std::min(rectROI.right, pDetectROI->right);
		rectROI.bottom = std::min(rectROI.bottom, pDetectROI->bottom);
	}

	int roiWidth = rectROI.right - rectROI.left + 1;
	int roiHeight = rectROI.bottom - rectROI.top + 1;

	const unsigned char *pGrayPtr = pGrayImage + rectROI.top * imageWidth + rectROI.left;

	// 
	minFaceSize = std::max(m_FrontalParam.normWidth, minFaceSize);
	maxFaceSize = std::min(maxFaceSize, roiWidth);
	maxFaceSize = std::min(maxFaceSize, roiHeight);

	if (maxFaceSize < minFaceSize)
		return ERR_INVALIDCALL;

	// 
	int factor = ((minFaceSize << 10) + m_FrontalParam.normWidth / 2) / m_FrontalParam.normWidth;
	const int maxFactor = (maxFaceSize << 10) / m_FrontalParam.normWidth;

	const int shiftedWidth = roiWidth << 10;
	const int shiftedHeight = roiHeight << 10;

	const int theWidth = (factor / 2 + shiftedWidth) / factor;
	const int theHeight = (shiftedHeight + factor / 2) / factor;
	const int theWidthStep = ((theWidth + 3) / 4) * 4;
	smart_ptr<unsigned char> tempImage(theHeight * theWidthStep);

	while (factor <= maxFactor)
	{
		const int scaledWidth = (factor / 2 + shiftedWidth) / factor;
		const int scaledHeight = (shiftedHeight + factor / 2) / factor;
		const int scaleWidthStep = ((scaledWidth + 3) / 4) * 4;

		ScaleImageV4(scaledHeight, scaledWidth, pGrayPtr, roiWidth, roiHeight, imageWidth, tempImage, scaleWidthStep);
		const int curStep = (factor <= 2048) ? 2 : 1;
		DetectFaceInOneScaleV4(scaledHeight, &m_FrontalParam, tempImage, scaledWidth, scaleWidthStep, curStep, curStep, factor, 121, 0, rectROI, pFaceRect);

		factor = (1229 * factor + 512) >> 10;
	}

	return ERR_NONE;
}

int CFaceDetector::MultiViewFaceDetect(const unsigned char *pGrayImage,
	int imageWidth, int imageHeight, int minFaceSize, int maxFaceSize,
	std::vector<FaceDetected> &pFaceRect, const Rect *pDetectROI/* = NULL*/)
{
	if (pGrayImage == NULL || imageWidth <= 0 || imageHeight <= 0)
		return ERR_INVALIDCALL;

	if (imageWidth <= 24 || imageHeight <= 24)
		return ERR_NONE;

	Rect rectROI = { 0, 0, imageWidth, imageHeight };
	if (pDetectROI != NULL)
	{
		rectROI.left = std::max(rectROI.left, pDetectROI->left);
		rectROI.top = std::max(rectROI.top, pDetectROI->top);
		rectROI.right = std::min(rectROI.right, pDetectROI->right);
		rectROI.bottom = std::min(rectROI.bottom, pDetectROI->bottom);
	}

	int roiWidth = rectROI.right - rectROI.left;
	int roiHeight = rectROI.bottom - rectROI.top;
	const unsigned char *pGrayPtr = pGrayImage + rectROI.top * imageWidth + rectROI.left;

	const FaceDetectParam *pDetectParam[2] = { &m_FrontalParam, &m_SideParam };

	const int angleValue[2] = { 121, 18 };
	bool flag[2] = { false, true };

	if (pGrayPtr == 0 || pDetectParam == 0)
	{
		return 0;
	}

	const int winSize = pDetectParam[0]->normWidth;

	if (pDetectParam[1]->normWidth != winSize)
	{
		return 0;
	}
	minFaceSize = std::max(winSize, minFaceSize);
	if (maxFaceSize <= 0)
	{
		maxFaceSize = std::min(roiWidth, roiHeight);
	}
	maxFaceSize = std::min(maxFaceSize, roiWidth);
	maxFaceSize = std::min(maxFaceSize, roiHeight);

	if (maxFaceSize < minFaceSize)
		return 0;

	int factor = ((minFaceSize << 10) + winSize / 2) / winSize;
	const int maxFactor = (maxFaceSize << 10) / winSize;

	const int shiftedWidth = roiWidth << 10;
	const int shiftedHeight = roiHeight << 10;

	const int theWidth = (factor / 2 + shiftedWidth) / factor;
	const int theHeight = (shiftedHeight + factor / 2) / factor;
	const int theWidthStep = ((theWidth + 3) / 4) * 4;
	smart_ptr<unsigned char> tempImage(theHeight * theWidthStep * 2);
	while (factor <= maxFactor)
	{
		const int scaledWidth = (factor / 2 + shiftedWidth) / factor;	// scaled width		
		const int scaledHeight = (shiftedHeight + factor / 2) / factor;	// scaled height		
		const int scaleWidthStep = ((scaledWidth + 3) / 4) * 4;
		const size_t scaledSize = scaledHeight * scaleWidthStep;

		unsigned char *pMirrorImage = tempImage + scaledSize;

		ScaleImageV4(scaledHeight, scaledWidth, pGrayPtr, roiWidth, roiHeight, imageWidth, tempImage, scaleWidthStep);
		MirrorImage(pMirrorImage, tempImage, scaledHeight, scaledWidth, scaleWidthStep);

		const int curStep = (factor <= 2048) ? 2 : 1;
		for (int i = 0; i < 2; ++i)
		{
			int angle = angleValue[i];
			DetectFaceInOneScaleV4(scaledHeight, pDetectParam[i], tempImage, scaledWidth, scaleWidthStep,
				curStep, curStep, factor, angle, false, rectROI, pFaceRect);
			if (flag[i])
			{
				DetectFaceInOneScaleV4(scaledHeight, pDetectParam[i], pMirrorImage, scaledWidth, scaleWidthStep,
					curStep, curStep, factor, angle, true, rectROI, pFaceRect);
			}
		}

		factor = (1229 * factor + 512) >> 10;
	}

	return ERR_NONE;
}

