#include "FaceBeauty.h"
#include "DeepFeat/DeepFeat.h"
#include "FaceDetection/FaceDetection.h"
#include "Normalization/Normalization.h"
#include "FaceAlignment/FaceAlignment.h"
#include <fstream>
#include <iostream>
#include <string>
#include <memory>
#include <assert.h>
#include "ErrorDefine.h"
#include "smart_ptr.h"
#include "opencv2/opencv.hpp"

#define FEATURE_POINTS_NUM 88
static char mFullPathname[256];
static float mTotalScore;  //总体分
static int mFlawLabel;  //瑕疵
static int mExpressionLabel; // 表情
static float mAge; //年龄
static float mSkin; //肤色

extern "C" {
	JNIEXPORT void JNICALL NAME(SetFaceBeautyPath)(JNIEnv * jenv, jclass s, jstring jPathName);
	JNIEXPORT jint JNICALL NAME(Calculate)(JNIEnv * jenv, jclass s, jstring jPicName);
	JNIEXPORT jfloat JNICALL NAME(GetTotalScore)(JNIEnv * jenv, jclass s);
	JNIEXPORT jint JNICALL NAME(GetFlawLabel)(JNIEnv * jenv, jclass s);
	JNIEXPORT jint JNICALL NAME(GetExpressionLabel)(JNIEnv * jenv, jclass s);
	JNIEXPORT jfloat JNICALL NAME(GetAge)(JNIEnv * jenv, jclass s);
	JNIEXPORT jfloat JNICALL NAME(GetSkin)(JNIEnv * jenv, jclass s);
};

JNIEXPORT jfloat JNICALL NAME(GetTotalScore)(JNIEnv * jenv, jclass s)
{
	return mTotalScore;
}

JNIEXPORT jint JNICALL NAME(GetFlawLabel)(JNIEnv * jenv, jclass s)
{
	return mFlawLabel;
}

JNIEXPORT jint JNICALL NAME(GetExpressionLabel)(JNIEnv * jenv, jclass s)
{
	return mExpressionLabel;
}

JNIEXPORT jfloat JNICALL NAME(GetAge)(JNIEnv * jenv, jclass s)
{
	return mAge;
}

JNIEXPORT jfloat JNICALL NAME(GetSkin)(JNIEnv * jenv, jclass s)
{
	return mSkin;
}

JNIEXPORT void JNICALL NAME(SetFaceBeautyPath)(JNIEnv * jenv, jclass s, jstring jPathName)
{
	const char* jnamestr = jenv->GetStringUTFChars(jPathName, NULL);
	//int jnamelen = jenv->GetStringLength(jPathName);
	mFullPathname[0]=0;
	strcpy(mFullPathname,jnamestr);
	jenv->ReleaseStringUTFChars(jPathName,jnamestr);
}

JNIEXPORT jint JNICALL NAME(Calculate)(JNIEnv * jenv, jclass s, jstring jPicName)
{
	const char* strImgName = jenv->GetStringUTFChars(jPicName, NULL);
	int retValue = ERR_NONE;

	try
	{
		// Initialize
		msg_Err("#---00000 mFullPathname=%s",mFullPathname);
		retValue = FaceDetectSetLibPath(mFullPathname);
		retValue |= FaceDetectInit();
		if (ERR_NONE != retValue)
			throw retValue;

		retValue = FaceAlignmentSetLibPath(mFullPathname);
		retValue |= FaceAlignmentInit();
		if (ERR_NONE != retValue)
		{
			FaceDetectUninit();
			throw retValue;
		}

		msg_Err("#---1111");
		retValue = SetDeepFeatLibPath(mFullPathname);
		msg_Err("#---22222");
		BeautyHandle hFace, hAge, hSkin, hXiaci, hHappy;
		retValue |= InitDeepFeat("BeautyModel.dat", &hFace);
		retValue |= InitDeepFeat("XCModel.dat", &hXiaci);
		retValue |= InitDeepFeat("PFModel.dat", &hSkin);
		retValue |= InitDeepFeat("NNModel.dat", &hAge);
		retValue |= InitDeepFeat("BQModel.dat", &hHappy);
		if (ERR_NONE != retValue)
		{
			FaceDetectUninit();
			FaceAlignmentUninit();
			throw retValue;
		}

		msg_Err("#---strImgName=%s",strImgName);
		// Read Image
		cv::Mat garyImgData = cv::imread(strImgName, CV_LOAD_IMAGE_GRAYSCALE);
		cv::Mat oriImgData = cv::imread(strImgName, CV_LOAD_IMAGE_COLOR);
		msg_Err("#---444444,garyImgData.cols,rows=%d,%d",garyImgData.cols,garyImgData.rows);
		// Face detection
		int ndFaceNum = 100;
		smart_ptr<FaceRect> pFaceRects(100);
		int nRetCode = FaceDetect(garyImgData.data, garyImgData.cols, garyImgData.rows,
								  24, std::min(garyImgData.cols, garyImgData.rows), pFaceRects, &ndFaceNum);

		if (0 >= ndFaceNum || ERR_NONE != nRetCode)
		{
			if(0 >= ndFaceNum && nRetCode==ERR_NONE)
				nRetCode = -666;
			msg_Err("#---Can not detect any faces!");
			std::cout << "Can not detect any faces!" << std::endl;
			throw nRetCode;
		}

		msg_Err("#---55555");
		// Pick up the largest one as the true face
		int faceSize = -1;
		int faceIdx = 0;
		for (int i = 0; i < ndFaceNum; ++i)
		{
			if (ndFaceNum > 1 && pFaceRects[i].confidence < 0.45f)
				continue;

			if (faceSize < pFaceRects[i].right - pFaceRects[i].left)
			{
				faceSize = pFaceRects[i].right - pFaceRects[i].left;
				faceIdx = i;
			}
		}

		msg_Err("#---66666");
		// Face aligment
        smart_ptr<FeaPointF> FeaPoints(FEATURE_POINTS_NUMBER);
		nRetCode = FaceAlignment(garyImgData.data, garyImgData.cols, garyImgData.rows, pFaceRects + faceIdx, FeaPoints);
		if (ERR_NONE != nRetCode)
		{
			std::cout << "Failed to locate feature points on faces!" << std::endl;
			throw nRetCode;
		}

		// Choose eye corner and lip corner, suppose1cdhmopruw
		// The normalization method should be considered by some days
		// Use old normalization method
        smart_ptr<FeaPointF> NormPoint(FEATURE_POINTS_NUMBER);
        memset(NormPoint, 0, FEATURE_POINTS_NUMBER * sizeof(FeaPointF));
        NormPoint[8].x = 70; NormPoint[8].y = 100;
        NormPoint[10].x = 120; NormPoint[10].y = 100;
        NormPoint[11].x = 180; NormPoint[11].y = 100;
        NormPoint[9].x = 230; NormPoint[9].y = 100;
        NormPoint[22].x = 110; NormPoint[22].y = 200;
        NormPoint[23].x = 190; NormPoint[23].y = 200;
        double weight[88] = { 0 };
        weight[8] = 1;
        weight[10] = 1;
        weight[11] = 1;
        weight[9] = 1;
        weight[22] = 1;
        weight[23] = 1;
        smart_ptr<double> pDstImage(300 * 300 * oriImgData.channels());
        nRetCode = NormalizeFace(oriImgData.data, oriImgData.cols, oriImgData.rows, oriImgData.channels(), FeaPoints, NormPoint,
            weight, FEATURE_POINTS_NUMBER, 300, 300, pDstImage);
        if (ERR_NONE != nRetCode)
		{
			std::cout << "Failed to normalize faces!" << std::endl;
			throw nRetCode;
		}

		msg_Err("#---77777");
		smart_ptr<unsigned char> pNormFace(300 * 300 * 3);
		for (int p = 0; p < 300 * 300 * 3; ++p)
			pNormFace[p] = (unsigned char)(int(pDstImage[p]));

		smart_ptr<unsigned char> pCropNormFace(256 * 256 * 3);
		smart_ptr<unsigned char> pCropGrayNormFace(256 * 256);

		cv::Mat NormFaceImage(300, 300, CV_8UC3, pNormFace);
		cv::Rect roi(22, 22, 256, 256);
		cv::Mat CropImage = NormFaceImage(roi);
		cv::Mat GrayCropImage;
		cv::cvtColor(CropImage, GrayCropImage, cv::COLOR_BGR2GRAY);

		for (int h = 0; h < CropImage.rows; ++h) {
			const uchar* ptr = CropImage.ptr<uchar>(h);
			int img_index = 0;
			for (int w = 0; w < CropImage.cols; ++w) {
				for (int c = 0; c < CropImage.channels(); ++c) {
					int datum_index = (c * CropImage.rows + h) * CropImage.cols + w;
					pCropNormFace[datum_index] = static_cast<char>(ptr[img_index++]);
				}
			}
		}

		for (int h = 0; h < GrayCropImage.rows; ++h) {
			const uchar* ptr = GrayCropImage.ptr<uchar>(h);
			int img_index = 0;
			for (int w = 0; w < GrayCropImage.cols; ++w) {
				for (int c = 0; c < GrayCropImage.channels(); ++c) {
					int datum_index = (c * GrayCropImage.rows + h) * GrayCropImage.cols + w;
					pCropGrayNormFace[datum_index] = static_cast<char>(ptr[img_index++]);
				}
			}
		}

		msg_Err("#---888888");
		// 1、总体分
		int featDim = GetDeepFeatSize(hFace) / 4;
		smart_ptr<float> pFeatures(featDim);
		nRetCode = InnerDeepFeat(hFace, pCropNormFace, 1, 3, 256, 256, pFeatures);

		float score = pFeatures[0] * 1.11f;
		if (score > 100.0f)
			score = 100.0f;

		std::cout << "Total score: " << score << std::endl;
		mTotalScore = score;

		msg_Err("#---99999");
		// 2、瑕疵
		featDim = GetDeepFeatSize(hXiaci) / 4;
		pFeatures.reset(featDim);
		nRetCode = InnerDeepFeat(hXiaci, pCropNormFace, 1, 3, 256, 256, pFeatures);

		float maxR = -10000.0f;
		int label = 15;
		for (int j = 0; j < featDim; ++j)
		{
			//std::cout << pFeatures[j] << " ";
			if (maxR < pFeatures[j])
			{
				maxR = pFeatures[j];
				label = j;
			}
		}

		if (0 == label)
			std::cout << "The flaws' number: " << "none!" << std::endl;
		else if (1 == label)
			std::cout << "The flaws' number: " << "a little!" << std::endl;
		else if (2 == label)
			std::cout << "The flaws' number: " << "small!" << std::endl;
		else if (3 == label)
			std::cout << "The flaws' number: " << "a lot!" << std::endl;
		else if (4 == label)
			std::cout << "The flaws' number: " << "very much!" << std::endl;

		mFlawLabel = label;

		msg_Err("#---aaaaa");
		// 3、开心
		featDim = GetDeepFeatSize(hHappy) / 4;
		pFeatures.reset(featDim);
		nRetCode = InnerDeepFeat(hHappy, pCropNormFace, 1, 3, 256, 256, pFeatures);

		maxR = -10000.0f;
		label = 15;
		for (int j = 0; j < featDim; ++j)
		{
			//std::cout << pFeatures[j] << " ";
			if (maxR < pFeatures[j])
			{
				maxR = pFeatures[j];
				label = j;
			}
		}
		// std::cout << std::endl;

		if (0 == label)
			std::cout << "Angry!" << std::endl;
		else if (1 == label)
			std::cout << "Unhappy!" << std::endl;
		else if (2 == label)
			std::cout << "normal!" << std::endl;
		else if (3 == label)
			std::cout << "happy!" << std::endl;
		else if (4 == label)
			std::cout << "smile!" << std::endl;

		mExpressionLabel = label;

		msg_Err("#---bbbbbb");
		// 4、年龄
        smart_ptr<unsigned char> pDstImageII(128*128);
        nRetCode = NormliazeFaceII(oriImgData.data, oriImgData.cols, oriImgData.rows, FeaPoints, FEATURE_POINTS_NUMBER, pDstImageII);
        featDim = GetDeepFeatSize(hAge) / 4;
        pFeatures.reset(featDim);
        nRetCode = InnerDeepFeat(hAge, pDstImageII, 1, 1, 128, 128, pFeatures);

		score = pFeatures[0];
		std::cout << "Age: " << score << std::endl;
		mAge = score;

		msg_Err("#---ccccc");
		// 5、肤色
		featDim = GetDeepFeatSize(hSkin) / 4;
		pFeatures.reset(featDim);
		nRetCode = InnerDeepFeat(hSkin, pCropNormFace, 1, 3, 256, 256, pFeatures);

		score = pFeatures[0] * 1.11f;
		if (score > 100.0f)
			score = 100.0f;
		std::cout << "Skin score: " << score << std::endl;
		mSkin = score;

		msg_Err("#---dddddd");
		// Uninitialized
		FaceDetectUninit();
		FaceAlignmentUninit();
		UninitDeepFeat(hFace);
		UninitDeepFeat(hSkin);
		UninitDeepFeat(hXiaci);
		UninitDeepFeat(hHappy);
		UninitDeepFeat(hAge);

		msg_Err("#---eeeee");
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

	jenv->ReleaseStringUTFChars(jPicName,strImgName);
	return retValue;
}
