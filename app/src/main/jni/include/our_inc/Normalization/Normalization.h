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
	    const unsigned char *pbySrcImage,		// 输入图像
		int dSrcImgWidth,						// 输入图像的宽
		int dSrcImgHeight,						// 输入图像的高
		int dImgChannel,					    // 图像的颜色通道（输入图像和输出图像颜色通道相同）
		const FeaPointF *ptpfSrcImgFeaPoint,	// 输入图像上的特征点坐标
		const FeaPointF *ptpfNormFeaPoint,		// 用于相似变换归一化的标准特征点的位置
		const double *plfFeaPointWeight,		// 特征点的权重，范围[0~1]，特征点越稳定，权重越接近1
		int dFeaPointNumber,					// 进行相似变换的特征点数目, 大于0
		int dDstImgWidth,						// 变换后的图像的宽
		int dDstImgHeight,						// 变换后的图像的高
		double *plfDstImage 		            // 输出变换后的图像（如果不需要输出图像，设置为空即可）double 类型
		);
		
	int __stdcall NormliazeFaceII(const unsigned char *pbySrcImage, int ImgWidth, int ImgHeight, const FeaPointF *pFeaPoints, int dFeaPointNumber, unsigned char *pNormFace);

#ifdef __cplusplus
}
#endif

#endif	//	__DETECTFACEINIRIMAGES_H_INCLUDE__
