package com.subway.ditu.utils.image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final boolean DEBUG = false;

    private static final int MAX_SIZE = 600 * 1024;

    private static final double CIRCLE_RATE = 0.5;
    
    private static final double ROUND_RATE = 0.6;
    
    /**
     * 创建一个原型的图片
     * 
     * @param source
     * @return
     */
    public static Bitmap createCircleBitmap(Bitmap source) {
        return createRoundedCornerBitmap(source, (float) (source.getWidth() * CIRCLE_RATE), true, true, true, true);
    }

    public static Bitmap createCircleBitmapWithScale(Bitmap src, int targetWidth, int targetHeight) {
        if (src == null || targetWidth <= 0 || targetHeight <= 0) {
            return null;
        }

        try {
            return createRoundedCornerBitmap(src, targetWidth, targetHeight, (float) (targetWidth* CIRCLE_RATE), true,
                    true, true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 创建一个固定圆角大小的图片
     * 
     * @param source
     * @return
     */
    public static Bitmap createRoundedBitmap(Bitmap source) {
        return createRoundedCornerBitmap(source, (float) (source.getWidth() * ROUND_RATE), true, true, true, true);
    }

    /**
     * 获得一张圆角图片，原来的图片会被释放
     * 
     * @param bitmap
     * @param roundPx
     * @param isRoundLT
     * @param isRoundRT
     * @param isRoundRB
     * @param isRoundLB
     * @return
     */
    private static Bitmap createRoundedCornerBitmap(Bitmap bitmap, float roundPx, boolean isRoundLT, boolean isRoundRT,
            boolean isRoundRB, boolean isRoundLB) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, w, h);
        final RectF rectF = new RectF(0, 0, w, h);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xff424242);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        if (!isRoundLT) {
            canvas.drawRect(0, 0, roundPx, roundPx, paint);
        }
        if (!isRoundRT) {
            canvas.drawRect(roundPx, 0, w, roundPx, paint);
        }
        if (!isRoundLB) {
            canvas.drawRect(0, roundPx, roundPx, h, paint);
        }
        if (!isRoundRB) {
            canvas.drawRect(roundPx, roundPx, w, h, paint);
        }

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private static Bitmap createRoundedCornerBitmap(Bitmap bitmap, int targetWidth, int targetHeight, float roundPx,
            boolean isRoundLT, boolean isRoundRT, boolean isRoundRB, boolean isRoundLB) {
        if (bitmap == null || bitmap.isRecycled() || targetWidth <= 0 || targetHeight <= 0) {
            return null;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, w, h);
        RectF rectF = new RectF(0, 0, targetWidth, targetHeight);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xff424242);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        if (!isRoundLT) {
            canvas.drawRect(0, 0, roundPx, roundPx, paint);
        }
        if (!isRoundRT) {
            canvas.drawRect(roundPx, 0, w, roundPx, paint);
        }
        if (!isRoundLB) {
            canvas.drawRect(0, roundPx, roundPx, h, paint);
        }
        if (!isRoundRB) {
            canvas.drawRect(roundPx, roundPx, w, h, paint);
        }

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, new Rect(0, 0, targetWidth, targetHeight), paint);

        return output;
    }

    /**
     * 从指定路径加载制定宽高的图片
     * 
     * @param path
     * @param width
     * @param height
     * @return
     */
    public static Bitmap loadScaledBitmap(String path, int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        if (TextUtils.isEmpty(path) || !(new File(path).exists())) {
            return null;
        }

        // try {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // opts.inJustDecodeBounds = true;
        // BitmapFactory.decodeFile(path, opts);
        // int xScale = opts.outWidth / width;
        // int yScale = opts.outHeight / height;
        // int scale = xScale < yScale ? xScale : yScale;
        // opts.inSampleSize = (scale == 0 ? 1 : scale);
        // opts.inJustDecodeBounds = false;
        Bitmap bmp = BitmapFactory.decodeFile(path, opts);
        if (bmp != null) {
            Bitmap destBmp = Bitmap.createScaledBitmap(bmp, width, height, false);
            if (!(bmp.getWidth() == destBmp.getWidth() && bmp.getHeight() == destBmp.getHeight())) {
                bmp.recycle();
            }
            return destBmp;
        }

        return null;
    }

    /**
     * 获得一个张图片的字节数组，在获取数组的时候不会做图片的压缩
     * 
     * @param src
     * @return
     */
    public static byte[] getBitmapBytes(Bitmap src) {
        if (src == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] srcSize = baos.toByteArray();

        return srcSize;
    }

    public static boolean compressBitmapToFile(Bitmap src, String saveFullPath) {
        if (!TextUtils.isEmpty(saveFullPath) && src != null) {
            File saveFile = new File(saveFullPath);
            if (saveFile.exists()) {
                saveFile.delete();
            }

            try {
                FileOutputStream out = new FileOutputStream(saveFile);
                src.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();

                return isBitmapData(saveFullPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static BitmapFactory.Options getBitmapHeaderInfo(String fileFullPath) {
        if (!TextUtils.isEmpty(fileFullPath)) {
            try {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inPurgeable = true;
                opt.inJustDecodeBounds = true;

                BitmapFactory.decodeFile(fileFullPath, opt);
                if (opt.outWidth > 0 && opt.outHeight > 0) {
                    return opt;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static boolean isBitmapData(byte[] data) {
        if (data == null) {
            return false;
        }

        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, opt);
            if (opt.outWidth > 0 && opt.outHeight > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isBitmapData(String fileFullPath) {
        if (!TextUtils.isEmpty(fileFullPath)) {
            File btFile = new File(fileFullPath);
            if (!btFile.exists()) {
                return false;
            }
            
            try {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inPurgeable = true;
                opt.inJustDecodeBounds = true;

                BitmapFactory.decodeFile(fileFullPath, opt);
                if (opt.outWidth > 0 && opt.outHeight > 0) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 根据图片的全路径来获取一张图片，在获取图片的时候会对图片做就地压缩，同时会将图片 的旋转角度准换成0度。
     * 
     * @param fileFullPath
     * @return
     */
    public static Bitmap loadBitmapWithSizeOrientation(String fileFullPath) {
        if (TextUtils.isEmpty(fileFullPath)) {
            return null;
        }
        return loadBitmapWithSizeCheck(new File(fileFullPath), ExifHelper.getRotationFromExif(fileFullPath));
    }

    public static Bitmap loadBitmapWithSizeCheck(File bitmapFile, int orientataion) {
        return loadBitmapWithSizeCheckAndBitmapReuse(bitmapFile, null, orientataion);
    }

    public static Bitmap loadBitmapWithSizeCheck(File bitmapFile) {
        return loadBitmapWithSizeCheckAndBitmapReuse(bitmapFile, null, 0);
    }

    public static Bitmap loadBitmapWithSizeCheckAndBitmapReuse(File bitmapFile, Bitmap reuseBt, int orientataion) {
        Bitmap bmp = null;
        FileInputStream fis = null;

        try {
            bitmapFile.setLastModified(System.currentTimeMillis());

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPurgeable = true;
            opt.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), opt);
            int width = opt.outWidth;
            int height = opt.outHeight;

            BitmapFactory.Options newOpt = new BitmapFactory.Options();
            long fileSize = bitmapFile.length();
            if (fileSize <= MAX_SIZE) {
                newOpt.inSampleSize = 1;
            } else if (fileSize <= MAX_SIZE * 4) {
                newOpt.inSampleSize = 2;
            } else {
                long times = fileSize / MAX_SIZE;
                newOpt.inSampleSize = (int) (Math.log(times) / Math.log(2.0)) + 1;
            }


            newOpt.inTempStorage = new byte[16 * 1024];
            newOpt.inScaled = true;
            newOpt.inPurgeable = true;
            newOpt.inInputShareable = true;

            boolean reusedBt = false;
            if (reuseBt != null && !reuseBt.isRecycled() && width == reuseBt.getWidth()
                    && height == reuseBt.getHeight()) {
                // need to check target sdk version
                reusedBt = true;
                newOpt.inBitmap = reuseBt;
            }

            newOpt.outHeight = height;
            newOpt.outWidth = width;
            fis = new FileInputStream(bitmapFile);

            long curTime = System.currentTimeMillis();
            bmp = BitmapFactory.decodeStream(fis, null, newOpt);
            if (DEBUG) {
                long cost = System.currentTimeMillis() - curTime;
            }

            if (orientataion != 0 && bmp != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate((float) orientataion);
                Bitmap tmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                if (tmp != null) {
                    bmp.recycle();
                    bmp = null;
                    bmp = tmp;
                }
            }

            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                    fis = null;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 根据图片的全路径来获取一张图片，在获取图片的时候会对图片做就地压缩，同时会将图片 的旋转角度准换成0度。
     * 
     * @param fileFullPath
     * @return
     */
    public static Bitmap loadBitmapWithMemSizeOrientation(String fileFullPath, int memSize) {
        if (TextUtils.isEmpty(fileFullPath)) {
            return null;
        }
        int maxsize = MAX_SIZE;
        if (memSize < 64) {
            maxsize = (int) MAX_SIZE * memSize * 2 / 64 / 3;
        }
        return loadBitmapWithMemSizeCheck(new File(fileFullPath), ExifHelper.getRotationFromExif(fileFullPath),
                maxsize, 0);
    }

    private static Bitmap loadBitmapWithMemSizeCheck(File bitmapFile, int orientataion, int maxsize, int errortimes) {
        Bitmap bmp = null;
        FileInputStream fis = null;

        try {
            bitmapFile.setLastModified(System.currentTimeMillis());

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPurgeable = true;
            opt.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), opt);
            int width = opt.outWidth;
            int height = opt.outHeight;
            // if (DEBUG) {
            // Log.d(TAG, "Information_source : width =  " + width +
            // " height = " + height + "size = " + bitmapFile.length()/1024 +
            // "memory size = " + memSize);
            // }
            BitmapFactory.Options newOpt = new BitmapFactory.Options();
            long fileSize = bitmapFile.length();
            if (fileSize <= maxsize) {
                newOpt.inSampleSize = 1;
            } else if (fileSize <= maxsize * 4) {
                newOpt.inSampleSize = 2;
            } else {
                long times = fileSize / maxsize;
                newOpt.inSampleSize = (int) (Math.log(times) / Math.log(2.0)) + 1;
            }
            // 在手机内存较小情况下，若图像需要翻转，则继续压缩图片
            if (orientataion != 0 && maxsize < MAX_SIZE) {
                newOpt.inSampleSize = newOpt.inSampleSize * 2;
            }
            if (errortimes == 1) {
                int scale = (int) (Math.log(newOpt.inSampleSize) / Math.log(2.0)) + 1;
                scale = (int) Math.scalb(1, scale);
                newOpt.inSampleSize = scale;
            }
            if (DEBUG) {
                Log.d(TAG, "dest insamplesize = " + newOpt.inSampleSize);
            }

            // rest the byte
            newOpt.inScaled = true;
            newOpt.inPurgeable = true;
            newOpt.inInputShareable = true;

            // if (width == 0 || height == 0 ||
            // Environment.DEVICE_INFO.screenWidth == 0
            // || Environment.DEVICE_INFO.screenHeight == 0) {
            // return null;
            // }
            //
            // float scale = (float) ((Environment.DEVICE_INFO.screenWidth *
            // 1.0) / width);
            // if (scale > 1.0) {
            // scale = (float) 1.0;
            // }
            // newOpt.outHeight = (int) (height * scale);
            // newOpt.outWidth = (int) (width * scale);

            newOpt.outHeight = height;
            newOpt.outWidth = width;
            fis = new FileInputStream(bitmapFile);
            bmp = BitmapFactory.decodeStream(fis, null, newOpt);

            // if (DEBUG) {
            // Config.LOGD("++++++++ [[loadBitmapWithSizeCheck]] +++++++++++\n");
            // Config.LOGD("      load bit info : { bmp = " + bmp + " width = "
            // + bmp.getWidth()
            // + " height = " + bmp.getHeight() + " opt size, width = " +
            // newOpt.outWidth
            // + " height = " + newOpt.outHeight
            // + "  scale = " + scale);
            // Config.LOGD("\n");
            // }

            if (orientataion != 0 && bmp != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate((float) orientataion);
                Bitmap tmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                if (tmp != null) {
                    bmp.recycle();
                    bmp = null;
                    bmp = tmp;
                }
            }

            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            if (errortimes == 1) {
                return null;
            }
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                    fis = null;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return loadBitmapWithMemSizeCheck(bitmapFile, orientataion, maxsize, 1);
    }

    public static void printBitmapInfo(String filePath) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPurgeable = true;
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opt);
        int width = opt.outWidth;
        int height = opt.outHeight;
    }

}
