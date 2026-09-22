package com.fgmachines.rck;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Sidecar/debug-only deterministic UI capture used by the CI release gate.
 * It draws the real Android view hierarchy into a PNG instead of relying on
 * the emulator compositor/screencap, which can return black frames on no-KVM
 * hosted runners even when the Activity is resumed.
 */
final class UiGateCapture {
    static final String CAPTURE_FILE = "fg_ui_capture.png";
    static final String MARKER_FILE = "fg_ui_gate_state";
    private static final String TAG = "FGLinkUiGate";

    private UiGateCapture() { }

    static boolean isEnabled(Activity activity) {
        String pkg = activity.getPackageName();
        return pkg.endsWith(".debug") || pkg.contains(".sidecar");
    }

    static void capture(Activity activity, String pageKey) {
        if (!isEnabled(activity) || pageKey == null || pageKey.trim().isEmpty()) return;
        View root = activity.getWindow().getDecorView();
        root.postDelayed(() -> captureNow(activity, root, pageKey.trim(), 0), 1200L);
    }

    private static void captureNow(Activity activity, View root, String pageKey, int attempt) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        int width = root.getWidth();
        int height = root.getHeight();
        if ((width <= 0 || height <= 0 || !root.isLaidOut()) && attempt < 12) {
            root.postDelayed(() -> captureNow(activity, root, pageKey, attempt + 1), 500L);
            return;
        }
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "capture-invalid-size " + pageKey + " " + width + "x" + height);
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        root.draw(canvas);

        try (FileOutputStream out = activity.openFileOutput(CAPTURE_FILE, Activity.MODE_PRIVATE)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IllegalStateException("PNG compression returned false");
            }
        } catch (Exception error) {
            Log.e(TAG, "Could not write UI capture for " + pageKey, error);
            bitmap.recycle();
            return;
        }
        bitmap.recycle();

        try (FileOutputStream marker =
                     activity.openFileOutput(MARKER_FILE, Activity.MODE_PRIVATE)) {
            marker.write(pageKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            Log.e(TAG, "Could not write UI capture marker for " + pageKey, error);
            return;
        }
        Log.i(TAG, "capture-ready " + pageKey + " " + width + "x" + height);
    }
}
