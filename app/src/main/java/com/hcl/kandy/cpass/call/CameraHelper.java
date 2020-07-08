package com.hcl.kandy.cpass.call;

import android.hardware.Camera;

import java.util.ArrayList;
import java.util.List;

class CameraHelper {
    private static List<List<Camera.Size>> supportedVideoSizes;

    private static boolean isCameraUsebyApp() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            return true;
        } finally {
            if (camera != null) camera.release();
        }
        return false;
    }

    static void setSupportedVideoSizes() {
        if (supportedVideoSizes == null) {
            supportedVideoSizes = new ArrayList<>();
            supportedVideoSizes.add(Camera.CameraInfo.CAMERA_FACING_BACK, new ArrayList<>());
            supportedVideoSizes.add(Camera.CameraInfo.CAMERA_FACING_FRONT, new ArrayList<>());

            if (!isCameraUsebyApp()) {
                Camera camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                if (camera.getParameters().getSupportedVideoSizes() == null) {
                    // If there is no supported video size that means application running on simulator
                    // Add a few primitive camera resolution size to be able to use app in simulators.
                    Camera.Size size1 = camera.new Size(320, 240);
                    Camera.Size size2 = camera.new Size(640, 480);
                    Camera.Size size3 = camera.new Size(1024, 768);
                    Camera.Size size4 = camera.new Size(1280, 960);

                    supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_FRONT).add(size4);
                    supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_FRONT).add(size3);
                    supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_FRONT).add(size2);
                    supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_FRONT).add(size1);

                } else {
                    for (Camera.Size size : camera.getParameters().getSupportedVideoSizes()) {
                        if (size.height <= 1280 && size.width <= 1280)
                            supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_FRONT).add(size);
                    }
                }
                camera.release();

                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                if (camera.getParameters().getSupportedVideoSizes() == null) {
                    // If there is no supported video size that means application running on simulator
                    // Add a few primitive camera resolution size to be able to use app in simulators.
                    Camera.Size size1 = camera.new Size(320, 240);
                    Camera.Size size2 = camera.new Size(640, 480);
                    Camera.Size size3 = camera.new Size(1024, 768);
                    Camera.Size size4 = camera.new Size(1280, 960);

                    supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_BACK).add(size4);
                    supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_BACK).add(size3);
                    supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_BACK).add(size2);
                    supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_BACK).add(size1);

                } else {
                    for (Camera.Size size : camera.getParameters().getSupportedVideoSizes()) {
                        if (size.height <= 1280 && size.width <= 1280)
                            supportedVideoSizes.get(Camera.CameraInfo.CAMERA_FACING_BACK).add(size);
                    }
                }
                camera.release();
            }
        }
    }

    static List<List<Camera.Size>> getSupportedVideoSizes() {
        return supportedVideoSizes;
    }
}
