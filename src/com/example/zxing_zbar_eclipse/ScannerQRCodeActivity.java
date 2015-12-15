package com.example.zxing_zbar_eclipse;

import java.io.IOException;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.zxing_zbar.R;
import com.google.zxing.BarcodeFormat;
import com.zbar.lib.InactivityTimer;
import com.zbar.lib.ScannerActivityHandler;
import com.zxing.CameraManager;
import com.zxing.ViewfinderView;

/**
 * BUG1. 扫描书本的条形码ISBN 得到的结果不对
 * @author Administrator
 *
 */
@SuppressLint({ "ShowToast", "CutPasteId" })
public class ScannerQRCodeActivity extends Activity implements Callback {

	private ScannerActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private ImageView lightImageView;
	private MediaPlayer mediaPlayer;
	private ImageView ivback;
	private ImageView ivflashLight;
	private boolean playBeep;
	private ProgressBar dlg_loading_progressBar;
	private static final float BEEP_VOLUME = 0.50f;
	private boolean vibrate;
	public boolean isFlashlightOpen = false;
	private Toast mToast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initLayout();
	}

	protected void initLayout() {
		try {
			setContentView(R.layout.activity_zxing_canner);
			// TextView tvTitle = (TextView) findViewById(R.id.topbarTv);
			ivflashLight = (ImageView) findViewById(R.id.capture_flashlight);
			CameraManager.init(getApplicationContext());
			viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
			lightImageView = (ImageView) findViewById(R.id.capture_flashlight);
			lightImageView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						if (isFlashlightOpen) {
							CameraManager.get().turnLightOff();
							ivflashLight
									.setImageResource(R.drawable.zxing_scan_flashlight_on);
							isFlashlightOpen = false;
						} else {
							CameraManager.get().turnLightOn();
							ivflashLight
									.setImageResource(R.drawable.zxing_scan_flashlight_off);
							isFlashlightOpen = true;
						}
					} catch (Exception e) {
						e.printStackTrace();
						Log.d("1236", "eeee:" + e.getMessage());
					}
				}
			});
			// ivback.setOnClickListener(new OnClickListener() {
			// @Override
			// public void onClick(View v) {
			// finish();
			// }
			// });
			hasSurface = false;
			inactivityTimer = new InactivityTimer(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		try {
			super.onResume();
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			if (hasSurface) {
				initCamera(surfaceHolder);
			} else {
				surfaceHolder.addCallback(this);
				surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			}
			decodeFormats = null;
			characterSet = null;
			playBeep = true;
			AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
			if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
				playBeep = false;
			}
			initBeepSound();
			vibrate = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	/**
	 * 处理扫描结果
	 * 
	 * @param result
	 * @param barcode
	 */
	public void handleDecode(String result) {
		try {

			inactivityTimer.onActivity();
			playBeepSoundAndVibrate();
			if (TextUtils.isEmpty(result)) {
				showToast("扫描失败");
				Message delayMsg = toast_Handler.obtainMessage(0);
				toast_Handler.sendMessageDelayed(delayMsg, 200);
			} else {
//				showToast("扫描成功" + result);
//				Message delayMsg = toast_Handler.obtainMessage(1);
//				toast_Handler.sendMessageDelayed(delayMsg, 200);
				
				Intent intent =new Intent();
				intent.putExtra("result", result);
				setResult(RESULT_OK,intent);
				finish();
			}
		} catch (Exception e) {
			showToast("扫描失败");
			Message delayMsg = toast_Handler.obtainMessage(0);
			toast_Handler.sendMessageDelayed(delayMsg, 200);
		}
	}

	public void showToast(String rsid) {
		if (mToast == null) {
			mToast = Toast.makeText(ScannerQRCodeActivity.this, rsid,
					Toast.LENGTH_SHORT);
		} else {
			mToast.setText(rsid);
			mToast.setDuration(Toast.LENGTH_SHORT);
		}
		mToast.setGravity(Gravity.CLIP_HORIZONTAL, 10, 30);
		mToast.show();
	}

	public void cancelToast() {
		if (mToast != null) {
			mToast.cancel();
		}
	}

	private Handler toast_Handler = new Handler() {
		@Override
		// 信息显示几秒后 重新扫描
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:// 扫描失败信息显示几秒后 重新扫描
				cancelToast();
				SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
				SurfaceHolder surfaceHolder = surfaceView.getHolder();
				initCamera(surfaceHolder);
				if (handler != null) {
					handler.restartPreviewAndDecode();
				}
				break;
			case 1:// 扫描成功 信息显示几秒后返回主界面
				cancelToast();
				//ScannerQRCodeActivity.this.finish();
			default:
				break;
			}

		}
	};

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (handler == null) {
			handler = new ScannerActivityHandler(this, decodeFormats,
					characterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
		CameraManager.get().stopPreview();

	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();

	}

	private void initBeepSound() {
		try {
			if (playBeep && mediaPlayer == null) {
				// The volume on STREAM_SYSTEM is not adjustable, and users
				// found it
				// too loud,
				// so we now play on the music stream.
				setVolumeControlStream(AudioManager.STREAM_MUSIC);
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mediaPlayer.setOnCompletionListener(beepListener);
				AssetFileDescriptor file = getResources().openRawResourceFd(
						R.raw.qrcode_completed);
				try {
					mediaPlayer.setDataSource(file.getFileDescriptor(),
							file.getStartOffset(), file.getLength());
					file.close();
					mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
					mediaPlayer.prepare();
				} catch (IOException e) {
					mediaPlayer = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

	@Override
	public void onBackPressed() {
		finish();
		super.onBackPressed();
	}

}