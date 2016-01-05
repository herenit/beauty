#pragma once

// ȫ�ֶ���ı���
#define FD_PI 3.1415926535897932384626433832795	// �����Բ����
#define DEFAULT_FACESIZE 24						//��������С�������ߴ�;
#define RESIZEFACTOR   1.25						//����ͼ������ı�������; һ���ȽϺõ�ѡ��ֵΪ1.148698354997035; ����һ���ȽϺõ�ѡ��ֵΪ1.25

//*************************************
// ��⵽������ѡ�ṹ����
//*************************************
#ifndef _FACEDETECTED
#define _FACEDETECTED
struct FaceDetected
{
	int x, y;			// ��⵽������������Ͻ�
	int faceSize;		// �����ĳ߶�
	int node;			// �����Ľڵ㣨������������̬�Ǻ���ת�ǣ�ֱ������������Ӧ��node = 121��
	double score;		// ���������Ŷ�
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
// �ϲ����������ѡ�ṹ����
//*************************************
#ifndef _MERGEDFACE
#define _MERGEDFACE
struct MergedFace
{
	float x, y;			// �ϲ��������������Ͻ�
	float faceSize;	// �����ĳ߶�
	int node;			// �����Ľڵ㣨������������̬�Ǻ���ת�ǣ�ֱ������������Ӧ��node = 121��
	float score;		// ���������Ŷ�
	float maxscore;    // �ϲ��������������Ŷ�
	float neighbors;  // �ϲ�����������Ŀ
	float yaw;			// �ϲ���������̬��
	float roll;			// �ϲ���������ת��
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
