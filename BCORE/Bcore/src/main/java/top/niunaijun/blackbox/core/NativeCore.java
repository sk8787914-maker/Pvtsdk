package top.niunaijun.blackbox.core;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import org.lsposed.lsparanoid.Obfuscate;
/**
 * Extended patched NativeCore with more anti-detection shims to cover
 * additional probes observed in the logs (proc/self/root, profile files, dev/urandom, etc.).
 *
 * Notes:
 *  - This uses simple path-to-path redirections via addIORule() implemented in native layer.
 *  - For profile files that are system-owned (permission denied), we create a benign copy
 *    under the BlackBox app's private storage and redirect the game's access to it.
 *
 * Keep expanding addIORule() targets when you find new probe paths in logs.
 */
@Obfuscate
public class NativeCore {
    public static final String TAG = "NativeCore";
    private static boolean isInjected = false;
    public static String libtarget = "libbgmi.so";

    static {
        System.loadLibrary("blackbox");
        
		if (!isInjected) {
			Context context = BlackBoxCore.getContext();
			File libFile = new File(context.getFilesDir(), "loader/" + libtarget);
			if (libFile.exists()) {
				Log.i(TAG, "Loading " + libtarget + " from loader directory");
				try {
					System.load(libFile.getAbsolutePath());
					isInjected = true;
					Log.i(TAG, libtarget + " successfully loaded from: " + libFile.getAbsolutePath());
				} catch (UnsatisfiedLinkError e) {
					Log.e(TAG, "Failed to load " + libtarget + ": " + e.getMessage());
				}
			} else {
				Log.e(TAG, libtarget + " not found at: " + libFile.getAbsolutePath());
			}
		} else {
			Log.i(TAG, "Library already injected");
		}
    }

    public static native void init(int apiLevel);

    public static native void enableIO();

    public static native void addIORule(String targetPath, String relocatePath);

    public static native void hideXposed();

    public static native boolean disableHiddenApi();

    public static native void init_seccomp();
    
    public static native String ActivateSdkLog();

    @Keep
    public static int getCallingUid(int origCallingUid) {
        if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
            return origCallingUid;
        if (origCallingUid > Process.LAST_APPLICATION_UID)
            return origCallingUid;

        if (origCallingUid == BlackBoxCore.getHostUid()) {
            if (BActivityThread.getAppPackageName().equals("com.google.android.gms")) {
                return Process.ROOT_UID;
            }

            if (BActivityThread.getAppPackageName().equals("com.google.android.webview")) {
                return Process.myUid();
            }
            return BActivityThread.getCallingBUid();
        }
        return origCallingUid;
    }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }
}