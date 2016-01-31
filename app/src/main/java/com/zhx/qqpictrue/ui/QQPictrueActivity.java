package com.zhx.qqpictrue.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Gallery;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.zhx.qqpictrue.R;
import com.zhx.qqpictrue.adapter.QQGralleryAdapter;
import com.zhx.qqpictrue.bean.ImageFloder;

public class QQPictrueActivity extends Activity implements View.OnClickListener {
	private Gallery mRecyclerView;
	private QQGralleryAdapter mAdapter;
	private List<String> mDatas;
	private ProgressDialog mProgressDialog;

	/**
	 * 存储文件夹中的图片数量
	 */
	private int mPicsSize;
	/**
	 * 图片数量最多的文件夹
	 */
	private File mImgDir;
	/**
	 * 所有的图片
	 */
	private List<String> mImgs;
	/**
	 * 临时的辅助类，用于防止同一个文件夹的多次扫描
	 */
	private HashSet<String> mDirPaths = new HashSet<String>();

	/**
	 * 扫描拿到所有的图片文件夹
	 */
	private List<ImageFloder> mImageFloders = new ArrayList<ImageFloder>();

	int totalCount = 0;
	private TextView ablumTv, cameraTv, cacelTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		mRecyclerView = (Gallery) findViewById(R.id.id_recyclerview_horizontal);
		ablumTv = (TextView) findViewById(R.id.album_tv);
		ablumTv.setOnClickListener(this);
		cameraTv = (TextView) findViewById(R.id.camera_tv);
		cameraTv.setOnClickListener(this);
		cacelTv = (TextView) findViewById(R.id.cacel_tv);
		cacelTv.setOnClickListener(this);

		new Thread(new Runnable() {
			@Override
			public void run() {
				getImages();
			}
		}).start();
	}

	private void getImages() {
		String firstImage = null;
		Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		ContentResolver mContentResolver = QQPictrueActivity.this
				.getContentResolver();

		// 只查询jpeg和png的图片
		Cursor mCursor = mContentResolver.query(mImageUri, null,
				MediaStore.Images.Media.MIME_TYPE + "=? or "
						+ MediaStore.Images.Media.MIME_TYPE + "=?",
				new String[] { "image/jpeg", "image/png" },
				MediaStore.Images.Media.DATE_MODIFIED);

		while (mCursor.moveToNext()) {
			// 获取图片的路径
			String path = mCursor.getString(mCursor
					.getColumnIndex(MediaStore.Images.Media.DATA));

			Log.e("TAG", path);
			// 拿到第一张图片的路径
			if (firstImage == null)
				firstImage = path;
			// 获取该图片的父路径名
			File parentFile = new File(path).getParentFile();
			if (parentFile == null)
				continue;
			String dirPath = parentFile.getAbsolutePath();
			ImageFloder imageFloder = null;
			// 利用一个HashSet防止多次扫描同一个文件夹（不加这个判断，图片多起来还是相当恐怖的~~）
			if (mDirPaths.contains(dirPath)) {
				continue;
			} else {
				mDirPaths.add(dirPath);
				// 初始化imageFloder
				imageFloder = new ImageFloder();
				imageFloder.setDir(dirPath);
				imageFloder.setFirstImagePath(path);
			}

			int picSize = parentFile.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(".jpg") || filename.endsWith(".png")
							|| filename.endsWith(".jpeg"))
						return true;
					return false;
				}
			}).length;
			totalCount += picSize;

			imageFloder.setCount(picSize);
			mImageFloders.add(imageFloder);

			if (picSize > mPicsSize) {
				mPicsSize = picSize;
				// 显示扫描到的第一个文件目录
				if (mImgDir == null) {
					mImgDir = parentFile;
				}
			}
		}
		mCursor.close();

		// 扫描完成，辅助的HashSet也就可以释放内存了
		mDirPaths = null;

		// 通知Handler扫描图片完成
		mHandler.sendEmptyMessage(0x110);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (mImgDir == null) {
				Toast.makeText(getApplicationContext(), "擦，一张图片没扫描到",
						Toast.LENGTH_SHORT).show();
				return;
			}

			mImgs = Arrays.asList(mImgDir.list());
			Collections.reverse(mImgs);
			mAdapter = new QQGralleryAdapter(QQPictrueActivity.this, mImgs,
					R.layout.grid_item, mImgDir.getAbsolutePath());
			mRecyclerView.setAdapter(mAdapter);
		}
	};

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.album_tv:
			startActivity(new Intent(this, MainActivity.class));
			break;
		case R.id.camera_tv:
			startActivity(new Intent(this, ZCameraBaseAcitivy.class));
			break;
		case R.id.cacel_tv:
			finish();
			break;
		}
	}
}
