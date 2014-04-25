package net.sabamiso.p5_sumaho_player;

import java.io.IOException;

import net.sabamiso.camera.CameraPreviewView;
import net.sabamiso.camera.HTTPImageServer;
import net.sabamiso.utils.Config;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

@SuppressWarnings("unused")
public class SumahoMainActivity extends Activity implements
		net.sabamiso.camera.CameraPreviewListener {

	SumahoView sumaho_view;
	CameraPreviewView preview_view;
	HTTPImageServer http_image_server;
	Config cf;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		
		Config.init(this);
		cf = Config.getInstance();

		super.onCreate(savedInstanceState);

		OpenCVLoader.initDebug();

		RelativeLayout layout = new RelativeLayout(this);
		setContentView(layout);

		@SuppressWarnings("deprecation")
		int fp = ViewGroup.LayoutParams.FILL_PARENT;

		if (cf.getBoolean("enable_camera", false)) {
			// setup invisible capture preview view...
			preview_view = new CameraPreviewView(
					this, 
					cf.getInt("capture_image_width", 640), 
					cf.getInt("capture_image_height", 480),
					cf.getBoolean("use_inside_camera", false),
					this);
			layout.addView(preview_view,
					new RelativeLayout.LayoutParams(fp, fp));
			
			// setup http server
			http_image_server = new HTTPImageServer(cf.getInt(
					"tcp_listen_port_for_camera", 8080));
			http_image_server.setJpegQuality(cf.getInt("capture_jpeg_quality", 60));
		}

		//
		sumaho_view = new SumahoView(this);
		layout.addView(sumaho_view, new RelativeLayout.LayoutParams(fp, fp));
	}

	@Override
	protected void onResume() {
		super.onResume();
		sumaho_view.start();

		if (preview_view != null) {
			try {
				http_image_server.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onPause() {
		sumaho_view.stop();

		if (preview_view != null) {
			http_image_server.stop();
			preview_view.stop();
		}

		super.onPause();
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode != KeyEvent.KEYCODE_BACK) {
			return super.onKeyDown(keyCode, event);
		} else {
			return false;
		}
	}

	@Override
	public void onPreviewFrame(Mat image) {
		if (image == null || image.empty() == true || http_image_server == null)
			return;

		Mat tmp_img = new Mat();

		boolean flip_h = cf.getBoolean("camera_flip_h");
		boolean inside_camera = cf.getBoolean("use_inside_camera");

		Configuration config = getResources().getConfiguration();

		if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
			// rotate 90deg
			if (inside_camera) {
				// rotate 90 deg counter-clockwise
				Core.flip(image.t(), tmp_img, 0);
			}
			else {
				// rotate 90 deg clockwise
				Core.flip(image.t(), tmp_img, 1);
			}
			if (flip_h) {
				Core.flip(tmp_img, tmp_img, 1);
			}
				
			http_image_server.setImage(tmp_img);
		} else {
			if (flip_h) {
				Core.flip(image, tmp_img, 1);
				http_image_server.setImage(tmp_img);
			} else {
				http_image_server.setImage(image);
			}
		}

	}
}
