package com.zbar.lib;

import java.util.Hashtable;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.zxing_zbar.R;
import com.example.zxing_zbar_eclipse.ScannerQRCodeActivity;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.zxing.CameraManager;

final class DecodeHandler extends Handler {

	private static final String TAG = DecodeHandler.class.getSimpleName();

	private final ScannerQRCodeActivity activity;
	private final MultiFormatReader multiFormatReader;

	DecodeHandler(ScannerQRCodeActivity activity,
			Hashtable<DecodeHintType, Object> hints) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case R.id.decode:
			// Log.d(TAG, "Got decode message");
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
		case R.id.quit:
			Looper.myLooper().quit();
			break;
		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		// modify here
		byte[] rotatedData = new byte[data.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				rotatedData[x * height + height - y - 1] = data[x + y * width];
		}
		int tmp = width; // Here we are swapping, that's the difference to #11
		width = height;
		height = tmp;
		Rect rect = new Rect(CameraManager.get().getFramingRect());
		ZbarManager manager = new ZbarManager();
		String result = manager.decode(rotatedData, width, height, true,
				rect.left, rect.top, rect.right - rect.left, rect.bottom
						- rect.top);
		// PlanarYUVLuminanceSource source =
		// CameraManager.get().buildLuminanceSource(rotatedData, width, height);
		// BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		// try {
		// rawResult = multiFormatReader.decodeWithState(bitmap);
		// } catch (ReaderException re) {
		// // continue
		// } finally {
		// multiFormatReader.reset();
		// }

		if (result != null) {
			if(activity.getHandler()!=null){
				Message message = Message.obtain(activity.getHandler(),
						R.id.decode_succeeded, result);
				// 涓嶉渶瑕佸垱寤哄浘鐗�
				// Bundle bundle = new Bundle();
				// bundle.putParcelable(DecodeThread.BARCODE_BITMAP,
				// source.renderCroppedGreyscaleBitmap());
				// message.setData(bundle);
				// Log.d(TAG, "Sending decode succeeded message...");
				message.sendToTarget();
			}
		
		} else {
			if(activity.getHandler()!=null){
				Message message = Message.obtain(activity.getHandler(),
						R.id.decode_failed);
				message.sendToTarget();
			}
		}
	}

}
