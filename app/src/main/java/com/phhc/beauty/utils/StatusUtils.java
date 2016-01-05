package com.phhc.beauty.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.phhc.beauty.R;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by lzw on 15/5/4.
 */
public class StatusUtils {


  public static void toast(Context context, String str) {
    Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
  }

  public static boolean filterException(Context ctx, Exception e) {
    if (e != null) {
      toast(ctx, e.getMessage());
      return false;
    } else {
      return true;
    }
  }

  public static void goActivity(Context cxt, Class<?> clz) {
    Intent intent = new Intent(cxt, clz);
    cxt.startActivity(intent);
  }


  public static Uri startAvatarCrop(Activity activity, Uri uri, int outputX, int outputY,
                                    int requestCode, String outputPath) {
    Intent intent = null;
    intent = new Intent("com.android.camera.action.CROP");
    intent.setDataAndType(uri, "image/*");
    intent.putExtra("crop", "true");
    intent.putExtra("aspectX", 1);
    intent.putExtra("aspectY", 1);
    intent.putExtra("outputX", outputX);
    intent.putExtra("outputY", outputY);
    intent.putExtra("scale", true);
    intent.putExtra("return-data", true);
    intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
    intent.putExtra("noFaceDetection", false);
    activity.startActivityForResult(intent, requestCode);
    return null;
  }

  public static void pickImage(Activity activity, int requestCode) {
    Intent intent = new Intent(Intent.ACTION_PICK, null);
    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
    activity.startActivityForResult(intent, requestCode);
  }

  public static void toast(Context context, int strId) {
    toast(context, context.getString(strId));
  }

  static Map<String, Integer> userId2randomAvatar = new HashMap<String, Integer>();





  public static ImageLoaderConfiguration getImageLoaderConfig(Context context, File cacheDir) {
    return new ImageLoaderConfiguration.Builder(
        context)
        .threadPoolSize(3).threadPriority(Thread.NORM_PRIORITY - 2)
        .memoryCache(new WeakMemoryCache())
        .denyCacheImageMultipleSizesInMemory()
        .discCacheFileNameGenerator(new Md5FileNameGenerator())
        .tasksProcessingOrder(QueueProcessingType.LIFO)
        .discCache(new UnlimitedDiscCache(cacheDir))
        .build();
  }


  public static DisplayImageOptions normalImageOptions = new DisplayImageOptions.Builder()
      .showImageOnLoading(R.mipmap.empty_photo)
      .showImageForEmptyUri(R.mipmap.empty_photo)
      .showImageOnFail(R.mipmap.empty_photo)
      .cacheInMemory(true)
      .cacheOnDisc(true)
      .considerExifParams(true)
      .imageScaleType(ImageScaleType.EXACTLY)
      .bitmapConfig(Bitmap.Config.RGB_565)
      .resetViewBeforeLoading(true)
      .build();

}
