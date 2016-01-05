// THIDFaceDeepFeat.cpp : Defines the exported functions for the DLL application.
//
#include "FaceBeauty.h"
#include "DeepFeat/stdafx.h"
#include <cstring>
#include <map>
#include <string>
#include <vector>
#include <memory>
#include <fstream>

#define GLOG_NO_ABBREVIATED_SEVERITIES

//#include "glog/logging.h"
//#include <boost/algorithm/string.hpp>
#include "caffe/caffe.hpp"
#include "caffe/util/upgrade_proto.hpp"
//#include <boost/algorithm/string.hpp>
#include "smart_ptr.h"
#include "ErrorDefine.h"
#include "DeepFeat/DeepFeat.h"

using caffe::Blob;
using caffe::Caffe;
using caffe::Net;
using caffe::Layer;
using caffe::shared_ptr;
//using caffe::Timer;
using caffe::vector;

char g_szDeepFeatSDKPath[_MAX_PATH] = {0};

namespace
{
	// for imagenet, normalize image size is 224 * 224
	// const float g_scale = 224.0f / 128.0f;
	const float g_scale = 1.0f;// 224.0f / 128.0f;	// normal image resized to 128 * 128 , 
	const int g_shiftBits = 11;
	// rotate shift right by moves bits
	template<typename T> T ror(T x, unsigned int moves)
	{
		return (x >> moves) | (x << (sizeof(T) * 8 - moves));
	}

	// rotate shift left by moves bits
	template<typename T> T rol(T x, unsigned int moves)
	{
		return (x << moves) | (x >> (sizeof(T) * 8 - moves));
	}
}

int __stdcall InnerDeepFeat(BeautyHandle handle, const unsigned char *pNormImage, int batchSize, int channels,
    int imageHeight, int imageWidth, float *pFeatures)
{
    int nRet = ERR_NONE;
    try
    {
        Net<float> *pCaffeNet = reinterpret_cast<Net<float> *>(handle);
        int length = batchSize * channels * imageHeight * imageWidth;
        smart_ptr<float> normRealImage(length);
        if (channels == 1)
        {
            const float Scale_Factor = 0.00390625f;
            for (int i = 0; i < length; ++i)
                normRealImage[i] = static_cast<float>(pNormImage[i]) * Scale_Factor;
        }
        else if (channels == 3)
        {
            for (int i = 0; i < batchSize; ++i)
            {
                for (int j = 0; j < channels; ++j)
                {
                    for (int k = 0; k < imageHeight * imageWidth; ++k)
                    {
                        int index = i * channels * imageHeight * imageWidth + j * imageHeight * imageWidth + k;
                        normRealImage[index] = static_cast<float>(pNormImage[index]);
                    }
                }
            }
        }

        std::vector<caffe::Blob<float>*> bottom_vec;
        bottom_vec.push_back(new caffe::Blob<float>);
        bottom_vec[0]->Reshape(batchSize, channels, imageHeight, imageWidth);
        bottom_vec[0]->set_cpu_data(normRealImage);

        float iter_loss;
        const vector<Blob<float>*>& result = pCaffeNet->Forward(bottom_vec, &iter_loss);

        for (int i = 0; i < result[0]->count(); ++i)
        {
            pFeatures[i] = result[0]->cpu_data()[i];
        }
    }
    catch (const std::bad_alloc &)
    {
        nRet = ERR_MEMORYALLOC;
    }
    catch (const int &errCode)
    {
        nRet = errCode;
    }
    catch (...)
    {
        nRet = ERR_UNKNOWN;
    }

    return nRet;
}


int __stdcall SetDeepFeatLibPath(const char *szLibPath)
{
	if (szLibPath == NULL)
		return ERR_INVALIDCALL;
	strcpy_s(g_szDeepFeatSDKPath, _MAX_PATH, szLibPath);
	
	size_t len = strlen(g_szDeepFeatSDKPath);
	if (len != 0)
	{
	#ifdef WIN32
		if (g_szDeepFeatSDKPath[len - 1] != '\\')
			strcat_s(g_szDeepFeatSDKPath, "\\");
	#else
	    if (g_szDeepFeatSDKPath[len - 1] != '/')
	        strcat_s(g_szDeepFeatSDKPath, "/");
	#endif
	}

	return ERR_NONE;
}

int __stdcall InitDeepFeat(const char *szResName, BeautyHandle *pHandle)
{
	if (pHandle == NULL)
		return ERR_INVALIDCALL;
	
	// initialize deep face network
	*pHandle = NULL;
	std::locale::global(std::locale(""));

	int retValue = ERR_NONE;

#ifndef WIN32	
	if (strlen(g_szDeepFeatSDKPath) == 0)
		strcpy_s(g_szDeepFeatSDKPath, _MAX_PATH, "./");
#endif

	try
	{
		std::string strDllPath;
		strDllPath = g_szDeepFeatSDKPath;
        strDllPath += szResName;

        std::fstream fileModel;
        fileModel.open(strDllPath.c_str(), std::fstream::in | std::fstream::binary);
        if (false == fileModel.is_open())
          return ERR_FILEIO;
          
        fileModel.seekg(0, std::fstream::end);
        int dataSize = int(fileModel.tellg());
        fileModel.seekg(0, std::fstream::beg);

		//CMyFile fileModel(strDllPath.c_str(), CMyFile::modeRead);
		//int dataSize = static_cast<int>(fileModel.GetLength());
		smart_ptr<char> encryptedData(dataSize);
		//fileModel.Read(encryptedData, dataSize);
		fileModel.read(encryptedData, dataSize);
		//fileModel.Close();
		fileModel.close();

		int *pBuffer = reinterpret_cast<int *>(encryptedData.get());
		// encrypt data by shift left		
		int numOfData = dataSize / sizeof(pBuffer[0]);
		for (int i = 0; i < numOfData; ++i)
		{
			int tempData = pBuffer[i];
			pBuffer[i] = ror(static_cast<unsigned int>(tempData), g_shiftBits);
		}

		const int protoTxtLen = pBuffer[0];
		const int modelSize = pBuffer[1];
		const unsigned char *pDataBuf = 
		    reinterpret_cast<unsigned char *>(encryptedData.get() + sizeof(int) * 2);

		//FLAGS_minloglevel = 2;	// INFO(=0)<WARNING(=1)<ERROR(=2)<FATAL(=3)

		// initialize network structure
		Caffe::set_mode(Caffe::CPU);
		caffe::NetParameter param;

		retValue = caffe::ReatNetParamsFromBuffer(pDataBuf, protoTxtLen, &param);
		if (retValue != ERR_NONE)
			return retValue;

		param.mutable_state()->set_phase(caffe::TEST);
		Net<float> *pCaffeNet = new Net<float>(param);

		msg_Err("lymd6");
		// initialize network parameters
		pCaffeNet->CopyTrainedLayersFrom(pDataBuf + protoTxtLen, modelSize);
		*pHandle = reinterpret_cast<BeautyHandle>(pCaffeNet);
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

	return retValue;
}

int __stdcall UninitDeepFeat(BeautyHandle handle)
{
	Net<float> *pCaffeNet = reinterpret_cast<Net<float> *>(handle);
	delete pCaffeNet;

	return ERR_NONE;
}

int __stdcall GetDeepFeatSize(BeautyHandle handle)
{
	Net<float> *pCaffeNet = reinterpret_cast<Net<float> *>(handle);
	const vector<Blob<float>*>& result = pCaffeNet->output_blobs();
	int len = result[0]->count() * sizeof(float);
	return len;
}
