package com.rocky.howold;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final int PIC_CODE = 0x001;
    private String mphotoPath;
    private Bitmap mphoto;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
        mPaint = new Paint();
    }

    private static final int MSG_SUCESS = 0x001;
    private static final int MSG_ERROR = 0x002;
    //在调用detece方法时候创建了一个子线程，所以这边用handler
    private Handler mHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCESS:
                    lodingView.setVisibility(View.GONE);
                    JSONObject json = (JSONObject) msg.obj;

                    prepareResultBitmap(json);

                    img.setImageBitmap(mphoto);

                    break;
                case MSG_ERROR:
                    lodingView.setVisibility(View.GONE);
                    String error = (String) msg.obj;
                    if (TextUtils.isEmpty("error")) {
                        personnum.setText("Error");
                    } else {
                        personnum.setText(error);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void prepareResultBitmap(JSONObject json) {
        Bitmap bitmap = Bitmap.createBitmap(mphoto.getWidth(), mphoto.getHeight(), mphoto.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mphoto, 0, 0, null);
        try {
            JSONArray faces = json.getJSONArray("face");
            int facenum = faces.length();
            personnum.setText("找到人脸数：" + facenum);
            if (facenum == 0) {
                Toast.makeText(getApplicationContext(), "这张图上好像没有人呢 =-=..",
                        Toast.LENGTH_SHORT).show();
            } else {
                if (facenum==1){Toast.makeText(getApplicationContext(), "找到一只~~~",
                        Toast.LENGTH_SHORT).show();}
                for (int i = 0; i < facenum; i++) {
                    //拿到单独的face对象
                    JSONObject face = faces.getJSONObject(i);
                    int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                    String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");
                    System.out.println(age + "+++++++++++++++++++" + gender);
                    //拿到人脸的坐标
                    JSONObject position = face.getJSONObject("position");
                    float x = (float) position.getJSONObject("center").getDouble("x");
                    float y = (float) position.getJSONObject("center").getDouble("y");
                    float w = (float) position.getDouble("width");
                    float h = (float) position.getDouble("height");
                    System.out.println(x + "+++++++++++++++++++" + y + "+++++++++++++++++++" + w + "+++++" + h);
                    mPaint.setColor(getResources().getColor(R.color.White));
                    mPaint.setStrokeWidth(3);
                    //拿到的只是百分比，要转化为图上的位置
                    x = x / 100 * bitmap.getWidth();
                    y = y / 100 * bitmap.getHeight();

                    w = w / 100 * bitmap.getWidth();
                    h = h / 100 * bitmap.getHeight();
                    canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2, mPaint);
                    canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2, mPaint);
                    canvas.drawLine(x + w / 2, y - h / 2, x + w / 2, y + h / 2, mPaint);
                    canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2, mPaint);

                    Bitmap agebitmap = buildBitmap(age, "male".equals(gender));

                    int infoWidth = agebitmap.getWidth();
                    int infoHeight = agebitmap.getHeight();

                    if ((bitmap.getWidth() < bitmap.getWidth()) && (bitmap.getHeight() < bitmap.getHeight())) {
                        float ratio = Math.max(bitmap.getWidth() * 1.0f / bitmap.getWidth() / 2, bitmap.getHeight() * 1.0f / bitmap.getHeight() / 2);
                        agebitmap = Bitmap.createScaledBitmap(agebitmap, (int) (infoWidth * ratio), (int) (infoHeight * ratio), false);
                    }
                    canvas.drawBitmap(agebitmap, x - agebitmap.getWidth() / 2, y - h / 2 - agebitmap.getHeight(), null);
                    mphoto = bitmap;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private Bitmap buildBitmap(int age, boolean isMale) {
        TextView tv = (TextView) lodingView.findViewById(R.id.age_and_gender);
        tv.setText(age + "");
        if (isMale) {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
        }
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();
        return bitmap;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detect_image:
                lodingView.setVisibility(View.VISIBLE);

                if (mphotoPath != null && !mphotoPath.trim().equals("")) {
                    resizePhoto();
                } else {
                    mphoto = BitmapFactory.decodeResource(getResources(), R.drawable.flower);
                }
                FaceDetect.detect(mphoto, new FaceDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message message = Message.obtain();
                        message.what = MSG_SUCESS;
                        message.obj = result;
                        mHander.sendMessage(message);
                    }

                    @Override
                    public void error(FaceppParseException e) {
                        Message message = Message.obtain();
                        message.what = MSG_ERROR;
                        message.obj = e.getErrorMessage();
                        mHander.sendMessage(message);
                    }
                });
                break;
            case R.id.get_image:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PIC_CODE);
                personnum.setText("找到人脸数：" + "0");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            //获取图片路径
            case PIC_CODE:
                if (intent != null) {
                    Uri uri = intent.getData();
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    cursor.moveToFirst();

                    int ur = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    mphotoPath = cursor.getString(ur);
                    cursor.close();
                    resizePhoto();
                    img.setImageBitmap(mphoto);
                }
                break;
            default:

        }
    }

    //用于压缩图片，以免在加载照片的时候过大，图片要控制在3M以内
    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mphotoPath, options);
        double radio = Math.max(options.outWidth * 1.0d / 1024, options.outHeight * 1.0d / 1024);
        options.inSampleSize = (int) Math.ceil(radio);
        options.inJustDecodeBounds = false;
        mphoto = BitmapFactory.decodeFile(mphotoPath, options);
    }

    private void initEvent() {
        dec_img.setOnClickListener(this);
        get_img.setOnClickListener(this);
    }

    private void initView() {
        dec_img = (Button) findViewById(R.id.detect_image);
        get_img = (Button) findViewById(R.id.get_image);
        personnum = (TextView) findViewById(R.id.num_text);
        img = (ImageView) findViewById(R.id.photo_image);
        lodingView = findViewById(R.id.loding);
    }

    private Button get_img;
    private Button dec_img;
    private TextView personnum;
    private ImageView img;
    private View lodingView;



}
