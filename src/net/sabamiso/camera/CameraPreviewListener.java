package net.sabamiso.camera;

import org.opencv.core.Mat;

public interface CameraPreviewListener {
	void onPreviewFrame(Mat image);
}
