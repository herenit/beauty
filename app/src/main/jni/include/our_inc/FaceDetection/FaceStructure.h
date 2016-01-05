#pragma once

// 全局定义的变量
#define FD_PI 3.1415926535897932384626433832795	// 定义的圆周率
#define DEFAULT_FACESIZE 24						//定义检测最小的人脸尺寸;
#define RESIZEFACTOR   1.25						//定义图像放缩的比例因子; 一个比较好的选择值为1.148698354997035; 另外一个比较好的选择值为1.25

//*************************************
// 检测到人脸候选结构定义
//*************************************
#ifndef _FACEDETECTED
#define _FACEDETECTED
struct FaceDetected
{
	int x, y;			// 检测到的人脸框的左上角
	int faceSize;		// 人脸的尺度
	int node;			// 人脸的节点（隐含人脸的姿态角和旋转角，直立正面人脸对应的node = 121）
	double score;		// 人脸的置信度
	FaceDetected()
	{
		x = -1;
		y = -1;
		faceSize = -1;
		node = -1;
		score = -1.0;
	}
	~FaceDetected()
	{
		x = -1;
		y = -1;
		faceSize = -1;
		node = -1;
		score = -1.0;
	}
};
#endif

//*************************************
// 合并后的人脸候选结构定义
//*************************************
#ifndef _MERGEDFACE
#define _MERGEDFACE
struct MergedFace
{
	float x, y;			// 合并后的人脸框的左上角
	float faceSize;	// 人脸的尺度
	int node;			// 人脸的节点（隐含人脸的姿态角和旋转角，直立正面人脸对应的node = 121）
	float score;		// 人脸的置信度
	float maxscore;    // 合并人脸框的最大置信度
	float neighbors;  // 合并的人脸框数目
	float yaw;			// 合并人脸的姿态角
	float roll;			// 合并人脸的旋转角
	MergedFace()
	{
		x = 0;
		y = 0;
		faceSize = 0;
		node = 0;
		score = 0.0;
		maxscore = -1000.0;
		neighbors = 0;

		yaw = 0;
		roll = 0;
	}
	~MergedFace()
	{
		x = 0;
		y = 0;
		faceSize = 0;
		node = 0;
		score = 0.0;
		maxscore = -1000.0;
		neighbors = 0;

		yaw = 0;
		roll = 0;
	}
};
#endif

#ifndef LBPDETECTFEATURES
#define LBPDETECTFEATURES
struct LBPDetectFeatures
{
	int pos[4];
	int threshold;
	int value[59];
};
#endif

#ifndef PARAMTABLE
#define PARAMTABLE
struct ParamTable
{
	int num;
	int threshold;
	LBPDetectFeatures *pFeatures;
	ParamTable()
	{
		pFeatures = 0;
	}
	~ParamTable()
	{
		FreeMemory();
	}
	void FreeMemory()
	{
		delete[] pFeatures;
		pFeatures = 0;
	}
};
#endif

#ifndef FACEDETECTPARAM
#define FACEDETECTPARAM
struct FaceDetectParam
{
	int tableNum;
	int normWidth;
	int normHeight;
	int totalFeatures;
	ParamTable *pParamTable;
	FaceDetectParam()
	{
		pParamTable = 0;
	}
	~FaceDetectParam()
	{
		FreeMemory();
	}
	void FreeMemory()
	{
		delete[] pParamTable;
		pParamTable = 0;
	}
	void CalcTotalLBPFeatures()
	{
		totalFeatures = 0;
		for (int i = 0; i < tableNum; ++i)
		{
			totalFeatures += pParamTable[i].num;
		}
	}
};
#endif

#ifndef INTEGALPOINTER
#define INTEGALPOINTER
struct IntegalPointer
{
	const int *ptr[16];
};
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
