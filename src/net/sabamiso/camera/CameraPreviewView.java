package net.sabamiso.camera;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

@SuppressLint("ViewConstructor")
public class CameraPreviewView extends SurfaceView implements
		Camera.PreviewCallback, SurfaceHolder.Callback {

	Camera camera;
	SurfaceHolder holder;

	int capture_w;
	int capture_h;
	boolean use_inside_camera;

	Mat img_yuv420sp;
	Mat img_rgb;
	
	CameraPreviewListener camera_preview_listener;

	@SuppressWarnings("deprecation")
	public CameraPreviewView(Context context, int capture_w, int capture_h, boolean use_inside_camera,
			CameraPreviewListener camera_preview_listener) {
		super(context);

		this.capture_w = capture_w;
		this.capture_h = capture_h;
		this.use_inside_camera = use_inside_camera;
		this.camera_preview_listener = camera_preview_listener;

		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			camera = Camera.open(getCameraId());
			camera.setPreviewDisplay(holder);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		if (camera != null) {
			camera.stopFaceDetection();
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		stop();
	}

	@SuppressWarnings({ "deprecation" })
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Camera.Parameters params = camera.getParameters();
		params.setPreviewSize(capture_w, capture_h);
		params.setPreviewFormat(PixelFormat.YCbCr_420_SP);
		camera.setParameters(params);

		camera.startPreview();

		Camera.Parameters p = camera.getParameters();
		android.hardware.Camera.Size s = p.getPreviewSize();

		capture_w = s.width;
		capture_h = s.height;
		createCvMat();

		camera.setPreviewCallback(this);
	}

	private void createCvMat() {
		releaseCvMat();
		img_yuv420sp = new Mat(capture_h + capture_h / 2, capture_w,
				CvType.CV_8UC1);
		img_rgb = new Mat(capture_h, capture_w, CvType.CV_8UC3);
	}

	private void releaseCvMat() {
		if (img_yuv420sp != null) {
			img_yuv420sp.release();
			img_yuv420sp = null;
		}

		if (img_rgb != null) {
			img_rgb.release();
			img_rgb = null;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		img_yuv420sp.put(0, 0, data);
		Imgproc.cvtColor(img_yuv420sp, img_rgb, Imgproc.COLOR_YUV420sp2BGR);

		if (camera_preview_listener != null) {
			camera_preview_listener.onPreviewFrame(img_rgb);
		}
	}
	
	int getCameraId() {
		int camera_id = 0;
		int num = Camera.getNumberOfCameras();
		if (num <= 1) return camera_id;

		int camera_facing_type = CameraInfo.CAMERA_FACING_BACK;
		if (use_inside_camera) camera_facing_type = CameraInfo.CAMERA_FACING_FRONT;
		
		for (int i = 0; i < num; i++) {
			CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == camera_facing_type) {
            	camera_id = i;
            }
        }
		
		return camera_id;
	}
}
