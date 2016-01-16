package com.phhc.beauty;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.os.Handler;

public class JNILib {
	private static String mMyPath=null;
	private static Thread mInitThread=null;
	private static Context mContext=null;
	private static Handler mHandler=null;
	public static final int FACE_DETECTED = 1234;
	public static final int FACE_NOTDETECTED = 4321;
	public static final int CalculateNotRet = -1234;

	static {
		System.loadLibrary("caffe");
		System.loadLibrary("FaceBeauty");
	}

	public void setHandler(Handler _hdl)
	{
		mHandler = _hdl;
	}
	public static void onFaceDeceted(boolean isDetected)
	{
		if(mHandler!=null)
		{
			mHandler.sendEmptyMessage(isDetected?FACE_DETECTED:FACE_NOTDETECTED);
		}
	}

	private boolean copyModelFile(Context context,String filename) throws IOException
	{
		InputStream is = context.getAssets().open(filename);
		FileOutputStream os = new FileOutputStream(new File(context.getFilesDir() + "/" + filename));

		final byte[] buffer = new byte[1024 * 80];
		int readLen = 0;
		while ((readLen = is.read(buffer, 0, 1024 * 80)) != -1)
			os.write(buffer, 0, readLen);
		is.close();
		os.flush();
		os.close();

		return true;
	}

	private void setDone(Context context)
	{
		File of = new File(context.getFilesDir() + "/CopyDone.ok");
		try {
			of.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean checkDone(Context context)
	{
		mMyPath = context.getFilesDir().getAbsolutePath();
		if(!mMyPath.endsWith("/"))
			mMyPath+="/";
		File of = new File(mMyPath + "CopyDone.ok");
		return of.exists();
	}

	private boolean copyAllModelFile(Context context)
	{
		if(!checkDone(context))
		{
			try {
				copyModelFile(context,"BeautyModel.dat");
				copyModelFile(context,"XCModel.dat");
				copyModelFile(context,"PFModel.dat");
				copyModelFile(context,"NNModel.dat");
				copyModelFile(context,"BQModel.dat");
				copyModelFile(context,"libfd.so");
				copyModelFile(context,"libfa.so");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			setDone(context);
		}
		return true;
	}
	
	public void InitFaceBeauty(Context context)
	{
		if(mContext!=null)  //just init once
			return;
		mContext = context.getApplicationContext();
		mInitThread = new Thread(new Runnable() {
			@Override
			public void run() {
				copyAllModelFile(mContext);
				SetFaceBeautyPath(mMyPath);
				Init();
			}
		});
		mInitThread.start();
	}

	public void DeInitFaceBeauty()
	{
		DeInit();
	}

	public int Calculate(String sPicName)
	{
		try {
			if(mInitThread!=null) {
				mInitThread.join();
				mInitThread = null;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return NativeCalculate(sPicName);
	}
	
	private native void SetFaceBeautyPath(String sPathName);
	public native int NativeCalculate(String sPicName);
	public native float GetTotalScore();
	public native int GetFlawLabel();
	public native int GetExpressionLabel();
	public native float GetAge();
	public native float GetSkin();
	private native void Init();
	private native void DeInit();
}
