#include <fstream>
#include <math.h>
#include <memory>
#include <string.h>
#include <exception>
#include "Normalization/Normalization.h"


namespace
{
    int AlignmentFeaturePoints(const FeaPointF *plfFeap1, const FeaPointF *plfFeap2, const double *plfWeight, int dPointNum, double *plfRotA, double &plfShiftX, double &plfShiftY)
    {
        // ����Ϸ��Լ��
        if (0 == plfRotA || 0 == plfFeap1 || 0 == plfFeap2 || 0 == plfWeight)
            return -99;

        if (0 >= dPointNum)
            return -99;

        // ��ʼ��������
        int errorCode = 0;
        double xx1 = 0.0, xx2 = 0.0, yy1 = 0.0, yy2 = 0.0, z = 0.0, w = 0.0, c1 = 0.0, c2 = 0.0;

        try
        {
            // �������ϵ��
            for (int i = 0; i < dPointNum; ++i)
            {
                // xx1 = x1'*weight; xx2 = x2'*weight; yy1 = y1'*weight; yy2 = y2'*weight;
                xx1 = xx1 + plfWeight[i] * plfFeap2[i].x;
                xx2 = xx2 + plfWeight[i] * plfFeap1[i].x;
                yy1 = yy1 + plfWeight[i] * plfFeap2[i].y;
                yy2 = yy2 + plfWeight[i] * plfFeap1[i].y;
                w = w + plfWeight[i];

                // c1 = weight' * (x1.*x2 + y1.*y2);
                c1 = c1 + plfWeight[i] * (plfFeap2[i].x*plfFeap1[i].x + plfFeap2[i].y*plfFeap1[i].y);
                // c2 = weight' * (y1.*x2 - x1.*y2);
                c2 = c2 + plfWeight[i] * (plfFeap2[i].y*plfFeap1[i].x - plfFeap2[i].x*plfFeap1[i].y);
                // z  = weight' * (x2.*x2 + y2.*y2);
                z = z + plfWeight[i] * (plfFeap1[i].x*plfFeap1[i].x + plfFeap1[i].y*plfFeap1[i].y);
            }

            double temp = xx2*xx2 + yy2*yy2 - w*z + 0.00000001;

            plfRotA[0] = (xx1*xx2 + yy1*yy2 - w*c1) / temp;
            plfRotA[1] = (xx1*yy2 + w*c2 - yy1*xx2) / temp;
            plfRotA[2] = -plfRotA[1];
            plfRotA[3] = plfRotA[0];
            plfShiftX = (c1*xx2 - z*xx1 - c2*yy2) / temp;
            plfShiftY = (c2*xx2 + c1*yy2 - z*yy1) / temp;
        }
        catch (const std::bad_alloc &)
        {
            errorCode = 2;
        }
        catch (const int &errCode)
        {
            errorCode = errCode;
        }
        catch (...)
        {
            errorCode = 1;
        }

        return errorCode;
    }

    // ����ǷŴ󣬲���˫���Բ�ֵ
    double ResizeImagePixels(const unsigned char * pbyData, int nH, int nW, int nC, double dx, double dy, int dc, double dScale_X, double dScale_Y)
    {
        //��¼�����ű���
        double dHalfScale_X = dScale_X / 2;
        double dHalfScale_Y = dScale_Y / 2;

        double p0, p1, a, b, a1;

        int nX, nY, nWS = nW*nC;

        const unsigned char *ptrH = 0, *ptrW = 0; //pbySrcImage + nImgWidth*iys;

        if (1 >= dScale_X && 1 >= dScale_Y)
        {
            // ����˫���Բ�ֵ
            int ixs = int(dx);
            int iys = int(dy);
            ptrW = pbyData + nWS*iys + nC*ixs + dc;
            a = dx - ixs; b = dy - iys; a1 = 1.f - a;
            if (ixs == nW - 1)
                p0 = double(ptrW[0]);
            else
                p0 = double(ptrW[0])*a1 + double(ptrW[nC])*a;
            if (iys == nH - 1)
                p1 = p0;
            else
                p1 = double(ptrW[nWS])*a1 + double(ptrW[nWS + nC])*a;

            return p0 + b * (p1 - p0);
        }
        else
        {
            //��¼����Ȩ�����ص� X ������ʼ��
            int nStart_X = int(dx - dHalfScale_X - 0.5) + 1;
            if (nStart_X  <  0)
                nStart_X = 0;

            //��¼����Ȩ�����ص� X �����յ�
            int nEnd_X = int(dx + dHalfScale_X + 0.5);
            if (nEnd_X >= nW)
                nEnd_X = nW - 1;

            //��¼����Ȩ�����ص� Y ������ʼ��
            int nStart_Y = int(dy - dHalfScale_Y - 0.5) + 1;
            if (nStart_Y < 0)
                nStart_Y = 0;

            //��¼����Ȩ�����ص� Y �����յ�
            int nEnd_Y = int(dy + dHalfScale_Y + 0.5);
            if (nEnd_Y >= nH)
                nEnd_Y = nH - 1;

            //��¼��ǰ��Ȩ��
            double dWeight_Cur = 0;

            //��¼��Ȩ��	
            double dWeight_Sum = 0;

            //��¼ X ���������ܵ��غ϶�
            double dMaxOverlap_X = dScale_X > 1 ? dScale_X : 1;

            //��¼ Y ���������ܵ��غ϶�
            double dMaxOverlap_Y = dScale_Y > 1 ? dScale_Y : 1;

            //��¼ X ����ǰ���غ϶�
            double dCurOverlap_X = 0;

            //��¼ Y ����ǰ���غ϶�
            double dCurOverlap_Y = 0;

            //��¼�ܵĻҶ�ֵ
            double dGrayVal_Sum = 0;

            //
            ptrH = pbyData + nWS*nStart_Y;
            for (nY = nStart_Y; nY <= nEnd_Y; ++nY, ptrH += nWS)
            {
                ptrW = ptrH + nC*nStart_X;
                for (nX = nStart_X; nX <= nEnd_X; ++nX, ptrW += nC)
                {
                    //�õ� dCurOverlap_X
                    dCurOverlap_X = dHalfScale_X + 0.5 - fabs(dx - nX);
                    if (dCurOverlap_X  > dMaxOverlap_X)
                        dCurOverlap_X = dMaxOverlap_X;

                    //�õ� dCurOverlap_Y
                    dCurOverlap_Y = dHalfScale_Y + 0.5 - fabs(dy - nY);
                    if (dCurOverlap_Y  > dMaxOverlap_Y)
                        dCurOverlap_Y = dMaxOverlap_Y;

                    //�õ� dWeight_Cur
                    dWeight_Cur = dCurOverlap_X * dCurOverlap_Y;

                    //���� dGrayVal_Sum
                    dGrayVal_Sum += dWeight_Cur * double(ptrW[dc]);

                    //���� dWeight_Sum
                    dWeight_Sum += dWeight_Cur;
                }
            }

            ptrH = 0;
            ptrW = 0;

            //�������ĻҶ�ֵ
            if (dWeight_Sum < 0.00000001)
                return 0;
            else
                return dGrayVal_Sum / dWeight_Sum;
        }
    }

    int ImageTranByRST(const unsigned char *pbySrcImage,								// ����ͼ��
        int dSrcImgWidth,															// ����ͼ��Ŀ�
        int dSrcImgHeight,															// ����ͼ��ĸ� 
        const double *plfRotA,													// �任��Ӧ����ת����
        double lfShiftX,																// �任��Ӧ��X�����ϵ�ƽ��
        double lfShiftY,																// �任��Ӧ��Y�����ϵ�ƽ��
        double lfScaleX,																// �任��Ӧ��X�����ϵ����ų߶�
        double lfScaleY,																// �任��Ӧ��Y�����ϵ����ų߶�
        int dDstImgWidth,
        int dDstImgHeight,
        double *plfDstImage,
        int dImgChannel)
    {
        // ����Ϸ��Լ��
        if (0 == pbySrcImage || 0 > dSrcImgWidth || 0 > dSrcImgHeight || 0 == plfDstImage || 0 > dDstImgWidth || 0 > dDstImgHeight || 0 >= lfScaleX || 0 >= lfScaleY || 0 >= dImgChannel)
            return -99;

        if (lfScaleX != lfScaleY)
            return -99;

        // �������
        int errorCode = 0, x, y, cs;
        double xs, ys, A11, A21, A12, A22, A13, A23, *dst = 0;

        try
        {
            /*  RotA*Src+T=Dst
            Src = inv(RotA)*(Dst-T)*/
            A11 = plfRotA[0] / (lfScaleX*lfScaleY), A12 = plfRotA[2] / (lfScaleX*lfScaleY), A13 = -A11*lfShiftX - A12*lfShiftY;
            A21 = plfRotA[1] / (lfScaleX*lfScaleY), A22 = plfRotA[3] / (lfScaleX*lfScaleY), A23 = -A21*lfShiftX - A22*lfShiftY;

            dst = plfDstImage;
            for (y = 0; y < dDstImgHeight; ++y)
            {
                xs = A12*y + A13;
                ys = A22*y + A23;
                for (x = 0; x < dDstImgWidth; ++x, dst += dImgChannel)
                {
                    if (xs < 0 || ys < 0 || xs > dSrcImgWidth - 1 || ys > dSrcImgHeight - 1)
                    {
                        xs += A11;
                        ys += A21;
                        continue;
                    }
                    else
                    {
                        for (cs = 0; cs < dImgChannel; ++cs)
                            dst[cs] = ResizeImagePixels(pbySrcImage, dSrcImgHeight, dSrcImgWidth, dImgChannel, xs, ys, cs, lfScaleX, lfScaleY);

                        xs += A11;
                        ys += A21;
                    }
                }
            }
        }
        catch (const std::bad_alloc &)
        {
            errorCode = 2;
        }
        catch (const int &errCode)
        {
            errorCode = errCode;
        }
        catch (...)
        {
            errorCode = 1;
        }

        dst = 0;

        return errorCode;
    }
}

int __stdcall NormalizeFace(const unsigned char *pbySrcImage, int dSrcImgWidth, int dSrcImgHeight,
    int dImgChannel, const FeaPointF *ptpfSrcImgFeaPoint, const FeaPointF *ptpfNormFeaPoint,
    const double *plfFeaPointWeight, int dFeaPointNumber, int dDstImgWidth, int dDstImgHeight,
    double *plfDstImage)
{
    // ����Ϸ��Լ��
    int i = 0;

    if (0 == pbySrcImage || 0 > dSrcImgWidth || 0 > dSrcImgHeight || 0 == plfFeaPointWeight || 0 >= dFeaPointNumber)
        return -99;

    if (0 == ptpfSrcImgFeaPoint || 0 == ptpfNormFeaPoint || 0 == plfDstImage)
        return -99;

    for (i = 0; i < dFeaPointNumber; i++)
    {
        if (0 > plfFeaPointWeight[i] || 1 < plfFeaPointWeight[i])
            return -99;
    }

    // ����һ�ѱ���
    int errorCode = 0;
    double lfShiftX, lfShiftY, lfScale;

    try
    {
        double plfRotA[4] = { 0.0 };
        errorCode = AlignmentFeaturePoints(ptpfSrcImgFeaPoint, ptpfNormFeaPoint, plfFeaPointWeight, dFeaPointNumber, plfRotA, lfShiftX, lfShiftY);
        if (0 != errorCode)
            return errorCode;

        memset(plfDstImage, 0, sizeof(double)*dDstImgWidth*dDstImgHeight*dImgChannel);
        lfScale = sqrt(plfRotA[0] * plfRotA[0] + plfRotA[1] * plfRotA[1]);
        errorCode = ImageTranByRST(pbySrcImage, dSrcImgWidth, dSrcImgHeight, plfRotA, lfShiftX, lfShiftY, lfScale, lfScale, dDstImgWidth, dDstImgHeight, plfDstImage, dImgChannel);
    }
    catch (const std::bad_alloc &)
    {
        errorCode = 2;
    }
    catch (const int &errCode)
    {
        errorCode = errCode;
    }
    catch (...)
    {
        errorCode = 1;
    }

    return errorCode;
}
