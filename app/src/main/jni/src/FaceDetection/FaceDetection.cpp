#include "FaceDetection/FaceDetection.h"
#include "FaceDetection/CFaceDetector.h"
#include "smart_ptr.h"
#include <fstream>
#include <memory>
#include <assert.h>
#include <math.h>

#define _MAX_PATH 256
#define FACE_DETECT_RES_FILE "libfd.dll"

namespace 
{
    char g_szFDLibPath[_MAX_PATH] = {0};
    CFaceDetector g_FD;
    
    static volatile bool g_isFDInited = false;
    static volatile int g_nFDInitCount = 0;
    
    float fConfThreshold = 0.45f;
    
    bool IsSameCandidate(const FaceDetected *faceOne, const FaceDetected *faceTwo)
    {
	    bool result = false;

	    int minWidth = std::min(faceOne->faceSize, faceTwo->faceSize);
	    int minHeight = std::min(faceOne->faceSize, faceTwo->faceSize);
	    int widthPlusHeight = minWidth + minHeight;
	    if (10 * abs(faceTwo->x - faceOne->x) <= widthPlusHeight)
	    {
		    if (10 * abs(faceTwo->y - faceOne->y) <= widthPlusHeight)
		    {
			    int rightDiff = faceTwo->x + faceTwo->faceSize - faceOne->faceSize - faceOne->x;
			    if (10 * abs(rightDiff) <= widthPlusHeight)
			    {
				    int bottomDiff = faceTwo->y + faceTwo->faceSize - faceOne->faceSize - faceOne->y;
				    if (10 * abs(bottomDiff) <= widthPlusHeight)
					    result = true;
			    }
		    }
	    }
	    return result;
    }
    
    bool CompareMergeFace(const MergedFace &face_1, const MergedFace &face_2)
    {
	    return (face_1.score > face_2.score);
    }
    
    float CalculateIntersectRatio(const MergedFace &rect_1, const MergedFace &rect_2)
    {
	    FaceDetected rectDst;
	    rectDst.x = std::max(rect_1.x, rect_2.x);
	    rectDst.y = std::max(rect_1.y, rect_2.y);
	    int right = int(std::min(rect_1.x + rect_1.faceSize, rect_2.x + rect_2.faceSize) + 0.5f);
	    int bottom = int(std::min(rect_1.y + rect_1.faceSize, rect_2.y + rect_2.faceSize) + 0.5f);

	    float intectRatio = 0;
	    int dstW = right - rectDst.x;
	    int dstH = bottom - rectDst.y;
	    if (dstW <= 0 || dstH <= 0)
		    return intectRatio;

	    // calculate area of rect 1
	    float area_1 = float(rect_1.faceSize * rect_1.faceSize);

	    // calculate area of rect 2
	    float area_2 = float(rect_2.faceSize * rect_2.faceSize);

	    float dstArea = float(dstW * dstH);
	    intectRatio = dstArea / std::min(area_1, area_2);

	    return intectRatio;
    }
    
    void MergeFace(const std::vector<FaceDetected> &detectedFace, std::vector<MergedFace> &finalFace)
    {
	    std::vector<int> faceIndex(detectedFace.size());
	    for (std::vector<int>::size_type i = 0; i < faceIndex.size(); ++i)
	    {
		    faceIndex[i] = int(i);
	    }

	    for (std::vector<FaceDetected>::size_type i = 0; i < detectedFace.size(); ++i)
	    {
		    for (std::vector<FaceDetected>::size_type j = i + 1; j < detectedFace.size(); ++j)
		    {
				if (IsSameCandidate(&detectedFace[i], &detectedFace[j]))
				{
					int maxIndex = std::max(faceIndex[i], faceIndex[j]);
					int minIndex = std::min(faceIndex[i], faceIndex[j]);

					for (int k = 0; k < detectedFace.size(); ++k)
					{
						if (faceIndex[k] == maxIndex)
							faceIndex[k] = minIndex;
					}
				}
		    }
		}

	    std::vector<int> tempIndex;
	    tempIndex.reserve(detectedFace.size());
	    for (std::vector<FaceDetected>::size_type i = 0; i < detectedFace.size(); ++i)
	    {
		    bool found = false;
		    for (std::vector<int>::size_type j = 0; j < tempIndex.size(); ++j)
		    {
			    if (tempIndex[j] == faceIndex[i])
			    {
				    found = true;
				    break;
			    }
		    }
		    if (!found)
		    {
			    tempIndex.push_back(faceIndex[i]);
		    }
	    }

	    std::vector<MergedFace> vecMergedFace(tempIndex.size());
	    for (std::vector<int>::size_type i = 0; i < faceIndex.size(); ++i)
	    {
		    std::vector<int>::size_type j;
		    for (j = 0; j < tempIndex.size(); ++j)
		    {
			    if (tempIndex[j] == faceIndex[i])
				    break;
		    }

			vecMergedFace[j].x += float(detectedFace[i].x);
			vecMergedFace[j].y += float(detectedFace[i].y);
			vecMergedFace[j].faceSize += float(detectedFace[i].faceSize);
			vecMergedFace[j].node = detectedFace[i].node;
			vecMergedFace[j].score += detectedFace[i].score;
			vecMergedFace[j].roll += 0.0f;
			vecMergedFace[j].yaw += 90.0f;
			vecMergedFace[j].neighbors = vecMergedFace[j].neighbors + 1.0f;

		    if (vecMergedFace[j].maxscore < detectedFace[i].score)
			    vecMergedFace[j].maxscore = detectedFace[i].score;
	    }

	    for (std::vector<MergedFace>::size_type i = 0; i < vecMergedFace.size(); ++i)
	    {
			vecMergedFace[i].x = vecMergedFace[i].x / vecMergedFace[i].neighbors;
			vecMergedFace[i].y = vecMergedFace[i].y / vecMergedFace[i].neighbors;
			vecMergedFace[i].faceSize = vecMergedFace[i].faceSize / vecMergedFace[i].neighbors;
			vecMergedFace[i].yaw = vecMergedFace[i].yaw / vecMergedFace[i].neighbors;
			vecMergedFace[i].roll = vecMergedFace[i].roll / vecMergedFace[i].neighbors;
		
		    // 计算平均分
		    vecMergedFace[i].score = vecMergedFace[i].score / vecMergedFace[i].neighbors;
	    }

	    for (std::vector<MergedFace>::size_type i = 0; i < vecMergedFace.size(); ++i)
	    {
		    for (std::vector<MergedFace>::size_type j = i + 1; j < vecMergedFace.size(); ++j)
		    {
			    if (CalculateIntersectRatio(vecMergedFace[i], vecMergedFace[j]) > 0.4)
			    {	
					if (vecMergedFace[i].neighbors < vecMergedFace[j].neighbors)
						vecMergedFace[i] = vecMergedFace[j];

					vecMergedFace.erase(j + vecMergedFace.begin());
					--i;
					break;
			    }
		    }
	    }

	    for (std::vector<MergedFace>::size_type i = 0; i < vecMergedFace.size(); ++i)
	    {
		    // 平均分、最高分和合并子集数共同融合为置信度
		    vecMergedFace[i].score = 0.2*vecMergedFace[i].score +
			    0.2*vecMergedFace[i].maxscore +
			    0.6*exp(-1.0 / double(vecMergedFace[i].neighbors));
	    }

        // sort final faces
        std::sort(vecMergedFace.begin(), vecMergedFace.end(), CompareMergeFace);
    }
}

int __stdcall FaceDetectSetLibPath(const char *szLibPath)
{
    if (g_isFDInited)
        return ERR_NONE;
    
    if (szLibPath == NULL)
		return ERR_INVALIDCALL;
		
	strncpy(g_szFDLibPath, szLibPath, _MAX_PATH);
	
	size_t len = strlen(g_szFDLibPath);
	if (len != 0)
	{
	#ifdef WIN32
		if (g_szFDLibPath[len - 1] != '\\')
			strcat(g_szFDLibPath, "\\");
	#else
	    if (g_szFDLibPath[len - 1] != '/')
	        strcat(g_szFDLibPath, "/");
	#endif
	}

	return ERR_NONE;
}

int __stdcall FaceDetectInit()
{
    std::locale::global(std::locale(""));
    
    if (g_isFDInited)
    {
        ++g_nFDInitCount;
        return ERR_NONE;
    }

	int retValue = ERR_NONE;

#ifndef WIN32	
	if (strlen(g_szFDLibPath) == 0)
		strncpy(g_szFDLibPath, "./", _MAX_PATH);
#endif

	try
	{
		std::string strDllPath;
		strDllPath = g_szFDLibPath;
        strDllPath += FACE_DETECT_RES_FILE;

        retValue = g_FD.InitFaceDetect(strDllPath.c_str());
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
        ++g_nFDInitCount;
        g_isFDInited = true;
    }

	return retValue;
}
	
int __stdcall FaceDetectUninit()
{
    if (!g_isFDInited)
        return ERR_NONE;
    
    --g_nFDInitCount;
    if (0 == g_nFDInitCount)
    {
        g_FD.UninitFaceDetect();
        g_isFDInited = false;
    }
    
    return ERR_NONE;
}

/*
    Using pDetectROI to choose an ROI area where you want to find the faces in the image.
    If pDetectROI is set to 0, the whole image area is used to find faces.
*/
int __stdcall FaceDetect(const unsigned char *pbyImageData, int nImgWidth, int nImgHeight,
    int nMinFaceSize, int nMaxFaceSize, FaceRect *pFaceRect, int *pFaceNum, 
    const Rect *pDetectROI)
{
    if (!g_isFDInited)
        return ERR_SDKNOINIT;
        
    if (0 == pbyImageData || 0 >= nImgWidth || 0 >= nImgHeight || 0 == pFaceRect || 0 == pFaceNum)
        return ERR_INVALIDCALL;
    
    if (0 >= (*pFaceNum))
        return ERR_INVALIDCALL;
        
    std::vector<FaceDetected> OrgFaces;
    OrgFaces.reserve(50000);
    
    int retValue = g_FD.FaceDetect(pbyImageData, nImgWidth, nImgHeight, nMinFaceSize, 
        nMaxFaceSize, OrgFaces, pDetectROI);
        
    std::vector<MergedFace> vecFaces;
	vecFaces.clear();

	MergeFace(OrgFaces, vecFaces);

    int maxFaceNum = (*pFaceNum);
	*pFaceNum = 0;
	for (int i = 0; i < std::min(maxFaceNum, (int)vecFaces.size()); ++i)
	{
		if (vecFaces[i].score < fConfThreshold)
			continue;

		pFaceRect[*pFaceNum].left = int(vecFaces[i].x);
		pFaceRect[*pFaceNum].top = int(vecFaces[i].y);
		pFaceRect[*pFaceNum].right = int(vecFaces[i].x + vecFaces[i].faceSize);
		pFaceRect[*pFaceNum].bottom = int(vecFaces[i].y + vecFaces[i].faceSize);
		pFaceRect[*pFaceNum].confidence = vecFaces[i].score;
		pFaceRect[*pFaceNum].yaw = vecFaces[i].yaw;
		pFaceRect[*pFaceNum].roll = vecFaces[i].roll;
		pFaceRect[*pFaceNum].pitch = 0.0f;
		++(*pFaceNum);
	}

	return retValue;
}

