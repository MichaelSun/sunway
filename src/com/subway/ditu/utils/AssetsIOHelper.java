package com.subway.ditu.utils;

import java.io.File;
import java.io.InputStream;

import android.content.Context;
import android.text.TextUtils;

public class AssetsIOHelper {

    public static final boolean saveAssetsFileToDest(Context context, String fileName, String destPath) {
        if (!TextUtils.isEmpty(fileName) && !TextUtils.isEmpty(destPath)) {
            try {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));
//                if (FileOperatorHelper.copyFile(reader, destPath)) {
//                    return true;
//                }
                File saveFile = new File(destPath);
                if (saveFile.exists()) {
                    saveFile.delete();
                }
                saveFile.createNewFile();
                
                InputStream is = context.getAssets().open(fileName);
                if (!TextUtils.isEmpty(FileOperatorHelper.saveFileByISSupportAppend(destPath, is))) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return false;
    }
    
}
