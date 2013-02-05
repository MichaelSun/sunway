package com.subway.ditu;

import java.io.File;

import net.youmi.push.android.YoumiPush;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.subway.ditu.utils.AssetsIOHelper;
import com.subway.ditu.utils.image.ImageUtils;
import com.subway.ditu.view.TouchImageView;

public class SubwayMainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private TouchImageView mTouchImageView;
    private ViewGroup.LayoutParams lp;

    private ProgressDialog mProgressDialog;

    private final static String MAP_FILE_NAME = "map1.jpg";
    
    private String mMapFullPath;
    
    private static final int LOAD_IMAGE = 1000;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case LOAD_IMAGE:
                checkLoadData();
                break;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        YoumiPush.startYoumiPush(this, "704e52750fbdf951", "a6dcf86615b8d9a4", false);

        lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mTouchImageView = new TouchImageView(this);
        this.setContentView(mTouchImageView, lp);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("正在加载中...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mHandler.sendEmptyMessageDelayed(LOAD_IMAGE, 150);
    }

    private void checkLoadData() {
        mMapFullPath = this.getFilesDir().getAbsolutePath() + File.separator + MAP_FILE_NAME;
        if (!TextUtils.isEmpty(mMapFullPath)) {
            if (ImageUtils.isBitmapData(mMapFullPath)) {
                new LoadFilesTask().execute(mMapFullPath);
            } else {
                new ExtraFilesTask().execute(mMapFullPath);
            }
        }
    }

    private class LoadFilesTask extends AsyncTask<String, Integer, Bitmap> {
        protected Bitmap doInBackground(String... paths) {
            return ImageUtils.loadBitmapWithSizeCheck(new File(paths[0]));
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                mTouchImageView.setImageFullPath(mMapFullPath);
                mTouchImageView.setImageBitmap(result);
            } else {
                Toast.makeText(getApplicationContext(), "加载失败", Toast.LENGTH_SHORT).show();
            }
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }

    private class ExtraFilesTask extends AsyncTask<String, Integer, Boolean> {
        private String mPath;

        protected Boolean doInBackground(String... paths) {
            mPath = paths[0];
            return AssetsIOHelper.saveAssetsFileToDest(getApplicationContext(), "subway.jpg", paths[0]);
        }

        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "[[onPostExecute]] result = " + result + " path = " + mPath + " ????????????");

            if (result) {
                new LoadFilesTask().execute(mPath);
            } else {
                Toast.makeText(getApplicationContext(), "加载失败", Toast.LENGTH_SHORT).show();
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }
        }
    }
}
