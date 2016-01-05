#ifndef __THIDFACENORMBYAFFINEWARP_H_INCLUDE__
#define __THIDFACENORMBYAFFINEWARP_H_INCLUDE__

#ifndef WIN32
#define __stdcall
#endif

#ifndef FEAPOINTF
#define FEAPOINTF
struct FeaPointF
{
    float x;
    float y;
};
#endif

#ifdef __cplusplus
extern "C"
{
#endif

	int __stdcall NormalizeFace(
	    const unsigned char *pbySrcImage,		// ����ͼ��
		int dSrcImgWidth,						// ����ͼ��Ŀ�
		int dSrcImgHeight,						// ����ͼ��ĸ�
		int dImgChannel,					    // ͼ�����ɫͨ��������ͼ������ͼ����ɫͨ����ͬ��
		const FeaPointF *ptpfSrcImgFeaPoint,	// ����ͼ���ϵ�����������
		const FeaPointF *ptpfNormFeaPoint,		// �������Ʊ任��һ���ı�׼�������λ��
		const double *plfFeaPointWeight,		// �������Ȩ�أ���Χ[0~1]��������Խ�ȶ���Ȩ��Խ�ӽ�1
		int dFeaPointNumber,					// �������Ʊ任����������Ŀ, ����0
		int dDstImgWidth,						// �任���ͼ��Ŀ�
		int dDstImgHeight,						// �任���ͼ��ĸ�
		double *plfDstImage 		            // ����任���ͼ���������Ҫ���ͼ������Ϊ�ռ��ɣ�double ����
		);
		
	int __stdcall NormliazeFaceII(const unsigned char *pbySrcImage, int ImgWidth, int ImgHeight, const FeaPointF *pFeaPoints, int dFeaPointNumber, unsigned char *pNormFace);

#ifdef __cplusplus
}
#endif

#endif	//	__DETECTFACEINIRIMAGES_H_INCLUDE__
