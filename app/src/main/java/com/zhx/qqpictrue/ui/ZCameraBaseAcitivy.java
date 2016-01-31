package com.zhx.qqpictrue.ui;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import java.io.IOException;
import java.util.List;

import com.zhx.qqpictrue.R;
import com.zhx.qqpictrue.utils.DisplayUtil;
import com.zhx.qqpictrue.utils.ImageUtil;
import com.zhx.qqpictrue.widget.OverlayerView;

/**
 * 
 * 希望有一天可以开源出来 org.zhx
 * 
 * @version 1.0, 2015-11-15 下午5:22:17
 * @author zhx
 */
public class ZCameraBaseAcitivy extends Activity implements PictureCallback,
		Callback, OnClickListener {
	private static final String TAG = ZCameraBaseAcitivy.class.getSimpleName();

	public static BitmapFactory.Options opt;
	static {
		// 缩小原图片大小
		opt = new BitmapFactory.Options();
		opt.inSampleSize = 2;
	}

	private SurfaceView mPreView;
	private SurfaceHolder mHolder;

	private Camera mCamera;
	private boolean isPreview = false;
	private Point displayPx;
	private ImageView tpImg, showImg;
	private Button saveBtn;
	// 取景框
	private OverlayerView mLayer;
	private Rect rect;
	private boolean isTake = false;
	private String[] flashMedols={Parameters.FLASH_MODE_AUTO,Parameters.FLASH_MODE_ON,Parameters.FLASH_MODE_OFF,Parameters.FLASH_MODE_TORCH};
    private int[]    modelResId={R.drawable.ic_camera_top_bar_flash_auto_normal,R.drawable.ic_camera_top_bar_flash_on_normal,R.drawable.ic_camera_top_bar_flash_off_normal,R.drawable.ic_camera_top_bar_flash_torch_normal};
    /**
     * 切换摄像头
     */
    private ImageView swImg;
    private ImageView flashModelImg;
    /**
     * 当前是否是前置摄像头
     */
    private boolean isFrontCamera = false;
    int modelIndex=0;

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.zcamera_base_layout);
		displayPx = DisplayUtil.getScreenMetrics(this);
		mPreView = (SurfaceView) findViewById(R.id.z_base_camera_preview);
		tpImg = (ImageView) findViewById(R.id.z_take_pictrue_img);
		saveBtn = (Button) findViewById(R.id.z_base_camera_save);
		showImg = (ImageView) findViewById(R.id.z_base_camera_showImg);
		mLayer = (OverlayerView) findViewById(R.id.z_base_camera_over_img);
        swImg = (ImageView) findViewById(R.id.btn_switch_camera);
        swImg.setOnClickListener(this);
        flashModelImg= (ImageView) findViewById(R.id.btn_flash_mode);
        flashModelImg.setOnClickListener(this);
        // 设置取景框的 magin 这里最好 将 这些从dp 转化为px; 距 左 、上 、右、下的 距离 单位是dp
        rect = DisplayUtil.createCenterScreenRect(this, new Rect(50, 100, 50,
                100));
		mLayer.setmCenterRect(rect);

		saveBtn.setOnClickListener(this);
		showImg.setOnClickListener(this);
		tpImg.setOnClickListener(this);
		mHolder = mPreView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
        openCamera();
    }

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (mCamera != null) {
			if (isPreview) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                isPreview = false;
            }

		}
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
        super.onRestart();
        initCamera();
    }

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		initCamera();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
        // 当holder被回收时 释放硬件
        releaseCamera();

    }

    void releaseCamera() {
        if (mCamera != null) {
            if (isPreview) {
                mCamera.stopPreview();
			}
			mCamera.release();
            mCamera = null;
        }
        isPreview = false;
    }

    void switchCamera() throws Exception {
        isFrontCamera = !isFrontCamera;
        releaseCamera();
        openCamera();
        initCamera();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    void openCamera() {
        if (!isFrontCamera) {
            mCamera = Camera.open();
        } else {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, cameraInfo);
                {
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        mCamera = Camera.open(i);
                        isFrontCamera = true;
                    }
                }
            }
        }
    }

	/**
     *
     * @param
     * @return
     * @throws Exception
	 * @author zhx
	 */
	public void initCamera() {

		if (mCamera != null && !isPreview) {
			try {
				Camera.Parameters parameters = mCamera.getParameters();
                // 设置闪光灯为自动 前置摄像头时 不能设置
                if (!isFrontCamera) {
                    parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
                }

				resetCameraSize(parameters);
				// 设置图片格式
				parameters.setPictureFormat(ImageFormat.JPEG);
				// 设置JPG照片的质量
				parameters.set("jpeg-quality", 100);
				// 通过SurfaceView显示取景画面
				mCamera.setPreviewDisplay(mHolder);
				// 开始预览
				mCamera.startPreview();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			isPreview = true;
        }

    }

	/**
	 * 旋转相机和设置预览大小
     *
     * @param parameters
     */
	public void resetCameraSize(Parameters parameters) {
		if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
			mCamera.setDisplayOrientation(90);
		} else {
			mCamera.setDisplayOrientation(0);
        }
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        if (sizeList.size() > 0) {
            Camera.Size cameraSize = sizeList.get(0);
            // 设置预览图片大小 为设备长宽
            parameters.setPreviewSize(cameraSize.width, cameraSize.height);
        }
        sizeList = parameters.getSupportedPictureSizes();
        if (sizeList.size() > 0) {
            Camera.Size cameraSize = sizeList.get(0);
            for (Camera.Size size : sizeList) {

                if (size.width * size.height == displayPx.x * displayPx.y) {
                    cameraSize = size;
                    break;
                }
            }
            // 设置图片大小 为设备长宽
            parameters.setPictureSize(cameraSize.width, cameraSize.height);
        }

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.z_take_pictrue_img:
			// 拍照前 线对焦 对焦后 拍摄（适用于自动对焦）
			isTake = true;
//			mCamera.autoFocus(autoFocusCallback);
			// 手动对焦
			 mCamera.takePicture(null, null, ZCameraBaseAcitivy.this);
            break;
            case R.id.btn_switch_camera:
                try {
                    switchCamera();
                } catch (Exception e) {
                    mCamera = null;
                    e.printStackTrace();
                }
                break;
            case R.id.btn_flash_mode:
                modelIndex++;
                if(modelIndex>=flashMedols.length){
                    modelIndex=0;
                }
                Parameters parameters=mCamera.getParameters();
                List<String> flashmodels=parameters.getSupportedFlashModes();
                if(flashmodels.contains(flashMedols[modelIndex])){
                    parameters.setFlashMode(flashMedols[modelIndex]);
                   flashModelImg.setImageResource(modelResId[modelIndex]);
                }
                mCamera.setParameters(parameters);
                break;

		default:
			break;
		}
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		isTake = false;
		// 拍照回掉回来的 图片数据。
		Bitmap bitmap = BitmapFactory
				.decodeByteArray(data, 0, data.length, opt);
		Bitmap bm = null;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			Matrix matrix = new Matrix();
			matrix.setRotate(90, 0.1f, 0.1f);
			bm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
					bitmap.getHeight(), matrix, false);
            if (isFrontCamera) {
                //前置摄像头旋转图片270度。
                matrix.setRotate(270);
                bm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            }
        } else {
            bm = bitmap;
        }

        if (rect != null) {
            bitmap = ImageUtil.getRectBmp(rect, bm, displayPx);
        }
        ImageUtil.recycleBitmap(bm);
        showImg.setImageBitmap(bitmap);
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.startPreview();
			isPreview = true;
		}
	}

	AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			if (isTake) {
				// 点击拍照按钮 对焦 后 拍照
				// 第一个参数 是拍照的声音，未压缩的数据，压缩后的数据
				mCamera.takePicture(null, null, ZCameraBaseAcitivy.this);
			}
		}
	};

}
