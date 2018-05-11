package com.zl.tesseract.scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.zxing.Result;
import com.zl.tesseract.R;
import com.zl.tesseract.scanner.camera.CameraManager;
import com.zl.tesseract.scanner.decode.CaptureActivityHandler;
import com.zl.tesseract.scanner.decode.DecodeManager;
import com.zl.tesseract.scanner.decode.InactivityTimer;
import com.zl.tesseract.scanner.domain.MailAnalysisResult;
import com.zl.tesseract.scanner.domain.ResponseBody;
import com.zl.tesseract.scanner.service.AppService;
import com.zl.tesseract.scanner.tess.TesseractCallback;
import com.zl.tesseract.scanner.tess.TesseractThread;
import com.zl.tesseract.scanner.utils.BitmapUtil;
import com.zl.tesseract.scanner.utils.MailAnalyzer;
import com.zl.tesseract.scanner.utils.Tools;
import com.zl.tesseract.scanner.view.ImageDialog;
import com.zl.tesseract.scanner.view.ScannerFinderView;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 二维码扫描类。
 */
public class ScannerActivity extends Activity implements Callback, Camera.PictureCallback, Camera.ShutterCallback{

    private static final String TAG = "ScannerActivity";

    private static final int CONNECT_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 20;
    private static final int READ_TIMEOUT = 20;

    Retrofit retrofit = null;
    AppService service = null;
    Call<ResponseBody> response = null;

    private CaptureActivityHandler mCaptureActivityHandler;
    private boolean mHasSurface;
    private InactivityTimer mInactivityTimer;
    private ScannerFinderView mQrCodeFinderView;
    private SurfaceView mSurfaceView;
    private ViewStub mSurfaceViewStub;
    private DecodeManager mDecodeManager = new DecodeManager();
    private Switch switch1;
    private Button bt;

    private ProgressDialog progressDialog;
    private Bitmap bmp;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        initView();
        initData();
        initRetrofit();
    }

    private void initRetrofit() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://imgs-sandbox.intsig.net")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(AppService.class);
    }

    private void initView() {
        mQrCodeFinderView = (ScannerFinderView) findViewById(R.id.qr_code_view_finder);
        mSurfaceViewStub = (ViewStub) findViewById(R.id.qr_code_view_stub);
        switch1 = (Switch) findViewById(R.id.switch1);
        mHasSurface = false;

        bt = (Button) findViewById(R.id.bt);

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bt.setEnabled(false);
                buildProgressDialog();
                CameraManager.get().takeShot(ScannerActivity.this, ScannerActivity.this, ScannerActivity.this);
            }
        });

        Switch switch2 = (Switch) findViewById(R.id.switch2);
        switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CameraManager.get().setFlashLight(isChecked);
            }
        });
    }

    public Rect getCropRect() {
        return mQrCodeFinderView.getRect();
    }

    public boolean isQRCode() {
        return switch1.isChecked();
    }

    private void initData() {
        mInactivityTimer = new InactivityTimer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CameraManager.init();
        initCamera();
    }

    private void initCamera() {
        if (null == mSurfaceView) {
            mSurfaceViewStub.setLayoutResource(R.layout.layout_surface_view);
            mSurfaceView = (SurfaceView) mSurfaceViewStub.inflate();
        }
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        if (mHasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCaptureActivityHandler != null) {
            try {
                mCaptureActivityHandler.quitSynchronously();
                mCaptureActivityHandler = null;
                mHasSurface = false;
                if (null != mSurfaceView) {
                    mSurfaceView.getHolder().removeCallback(this);
                }
                CameraManager.get().closeDriver();
            } catch (Exception e) {
                // 关闭摄像头失败的情况下,最好退出该Activity,否则下次初始化的时候会显示摄像头已占用.
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (null != mInactivityTimer) {
            mInactivityTimer.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Handler scan result
     *
     * @param result
     */
    public void handleDecode(Result result) {
        mInactivityTimer.onActivity();
        if (null == result) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            handleResult(result);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            if (!CameraManager.get().openDriver(surfaceHolder)) {
                return;
            }
        } catch (IOException e) {
            // 基本不会出现相机不存在的情况
            Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        } catch (RuntimeException re) {
            re.printStackTrace();
            return;
        }
        mQrCodeFinderView.setVisibility(View.VISIBLE);
        findViewById(R.id.qr_code_view_background).setVisibility(View.GONE);
        if (mCaptureActivityHandler == null) {
            mCaptureActivityHandler = new CaptureActivityHandler(this);
        }
    }

    public void restartPreview() {
        if (null != mCaptureActivityHandler) {
            try {
                mCaptureActivityHandler.restartPreviewAndDecode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    public Handler getCaptureActivityHandler() {
        return mCaptureActivityHandler;
    }

    private void handleResult(Result result) {
        if (TextUtils.isEmpty(result.getText())) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(200L);

            Log.e("aaa", result.getBitmap() == null ? "true" : "false");

            if (result.getBitmap() != null)
                loadData(result.getBitmap());
//            if (switch1.isChecked()) {
//                qrSucceed(result.getText());
//            } else {
//                phoneSucceed(result.getText(), result.getBitmap());
//            }

        }
    }

    private void loadData(Bitmap bitmap) {
        RequestBody body = null;
        try {
            body = RequestBody.create(MediaType.parse("multipart/form-data"), BitmapUtil.bitmap2Bytes(bitmap));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            response = service.scanner("qymgc", "XNs44IV5LZsI", body);
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response == null || response.body() == null)
                    return;
                Log.d(TAG, response.body().toString());
                List<String> strs = response.body().getLinesText();

                if (strs.size() == 0) {
//                    tv_orgin_result.setText("识别结果为空");
                    return;
                }

                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < strs.size(); i++) {
                    stringBuilder.append(strs.get(i) + "\n");
                }
//                tv_orgin_result.setText(stringBuilder.toString());

                MailAnalysisResult result = MailAnalyzer.doMailAnalysis(strs);
                Log.d(TAG, result.toString());
                if (result.getCode() == 200) {
//                    tv_result.setText(result.toString());
                } else if (result.getCode() == 500) {
//                    tv_result.setText("识别结果为空");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                final Throwable cause = t.getCause() != null ? t.getCause() : t;
                if (cause != null) {
                    if (cause instanceof ConnectException) {
                        Toast.makeText(ScannerActivity.this, "未能连接到服务器", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ScannerActivity.this, "连接超时，请稍后重试", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (data == null) {
            return;
        }
        mCaptureActivityHandler.onPause();
        bmp = null;
        bmp = Tools.getFocusedBitmap(this, camera, data, getCropRect());

        TesseractThread mTesseractThread = new TesseractThread(bmp, new TesseractCallback() {

            @Override
            public void succeed(String result) {
                Message message = Message.obtain();
                message.what = 0;
                message.obj = result;
                mHandler.sendMessage(message);
            }

            @Override
            public void fail() {
                Message message = Message.obtain();
                message.what = 1;
                mHandler.sendMessage(message);
            }
        });

        Thread thread = new Thread(mTesseractThread);
        thread.start();
    }

    @Override
    public void onShutter() {}

    private void qrSucceed(String result){
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.notification)
                .setMessage(result)
                .setPositiveButton(R.string.positive_button_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        restartPreview();
                    }
                })
                .show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                restartPreview();
            }
        });
    }

    private void phoneSucceed(String result, Bitmap bitmap){
        ImageDialog dialog = new ImageDialog(this);
        dialog.addBitmap(bitmap);
        dialog.addTitle(TextUtils.isEmpty(result) ? "未识别到手机号码" : result);
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                restartPreview();
            }
        });
    }

    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            bt.setEnabled(true);
            cancelProgressDialog();
            switch (msg.what){
                case 0:
                    phoneSucceed((String) msg.obj, bmp);
                    break;
                case 1:
                    Toast.makeText(ScannerActivity.this, "无法识别", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    public void buildProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.setMessage("识别中...");
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    public void cancelProgressDialog() {
        if (progressDialog != null){
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }
}