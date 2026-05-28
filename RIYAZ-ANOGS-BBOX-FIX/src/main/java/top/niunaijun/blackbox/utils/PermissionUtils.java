/**
 * @Description: Permission Handling Utility
 * @Author: xxxx
 * @CreateDate: 2024/8/1 23:52
 */
package top.niunaijun.blackbox.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.RequestPermissionsActivity;
import top.niunaijun.blackbox.core.system.am.IRequestPermissionsResult;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

public class PermissionUtils {

    public static Set<String> DANGEROUS_PERMISSION = new HashSet<String>() {{
        add(Manifest.permission.BODY_SENSORS);
        add(Manifest.permission.READ_EXTERNAL_STORAGE);
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // AUDIO/MICROPHONE (Voice Chat Permission)
        add(Manifest.permission.RECORD_AUDIO);
    }};
    
    public interface CallBack {
        boolean onResult(int requestCode, String[] permissions, int[] grantResults);
    }
    
    public static boolean isCheckPermissionRequired(ApplicationInfo info) {
        return BuildCompat.isM() && BlackBoxCore.getContext().getApplicationInfo().targetSdkVersion >= 23 && info.targetSdkVersion < 23;
    }
    
    public static String[] findDangerousPermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new String[0];
        }
        List<String> filteredPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (DANGEROUS_PERMISSION.contains(permission)) {
                filteredPermissions.add(permission);
            }
        }
        return filteredPermissions.toArray(new String[0]);
    }
    
    public static boolean checkPermissions(String[] permissions) {
        if (permissions == null || permissions.length == 0 || BlackBoxCore.get() == null) {
            return false;
        }
        for (String permission : permissions) {
            if (BlackBoxCore.get().checkSelfPermission(permission)) {
                return false;
            }
        }
        return true;
    }
    
    public static void startRequestPermissions(Context context, String[] permissions, final CallBack callBack) {
        if (context == null || permissions == null || permissions.length == 0 || callBack == null) {
            return; // Prevents unnecessary execution
        }

        RequestPermissionsActivity.request(context, permissions, new IRequestPermissionsResult.Stub() {
            @Override
            public boolean onResult(int requestCode, String[] permissions, int[] grantResults) throws RemoteException {
                return callBack != null && callBack.onResult(requestCode, permissions, grantResults);
            }
        });
    }
    
    public static boolean isRequestGranted(int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) {
            return false;
        }
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
