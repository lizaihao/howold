package com.rocky.howold;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 *Created by Rocky
 */
public class FaceDetect {
    public interface CallBack {
        void success(JSONObject result);

        void error(FaceppParseException e);
    }

    public static void detect(final Bitmap bm, final CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("向face++服务器发送请求..");
                    HttpRequests httpRequests = new HttpRequests(Constant.KEY, Constant.SECRT, true, true);
                    Bitmap bmsmall = Bitmap.createBitmap(bm, 0, 0,
                            bm.getWidth(), bm.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmsmall.compress(Bitmap.CompressFormat.JPEG, 50, stream);

                    byte[] bytes = stream.toByteArray();
                    PostParameters params = new PostParameters();
                    params.setImg(bytes);
                    JSONObject json = httpRequests.detectionDetect(params);
                    Log.e("Tag", json.toString());

                    if (null != callBack) {
                        callBack.success(json);
                    }

                }catch (FaceppParseException e){
                    System.out.println(e.toString());
                    e.printStackTrace();
                    if (null != callBack) {
                        callBack.error(e);
                    }
                }


            }
        }).start();
    }
}
