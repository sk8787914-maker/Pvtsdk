package top.niunaijun.blackbox.core.service.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.NativeCore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.lsposed.lsparanoid.Obfuscate;
import org.lsposed.lsparanoid.DeobfuscatorHelper;



/**
 * LicenseVerifier
 * Original logic preserved + VBox compatible
 */
@Obfuscate
public final class LicenseVerifier {

    private static final String TAG = "LICENSE_DEBUG";

    public interface Callback {
        void onResult(boolean isValid, String message);
    }

    private LicenseVerifier() {
    }

    // ==================================================
    // MAIN VERIFY METHOD (UNCHANGED SIGNATURE)
    // ==================================================
    public static void verify(final String licenseKey,
                              final Callback callback) {

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection conn = null;

                try {
                    Context ctx = BlackBoxCore.getContext();
                    String packageName = BlackBoxCore.getHostPkg();
                    String appName = getAppName(ctx);
                    String deviceId = getDeviceId(ctx);

                    URL url = new URL(NativeCore.ActivateSdkLog());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setUseCaches(false);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    // 🔥 ORIGINAL PARAMS + EXTRA SAFE PARAMS
                    String params =
                            "user_key=" + URLEncoder.encode(licenseKey, "UTF-8") +
                            "&package_name=" + URLEncoder.encode(packageName, "UTF-8") +
                            "&app_name=" + URLEncoder.encode(appName, "UTF-8") +
                            "&device_id=" + URLEncoder.encode(deviceId, "UTF-8");

                    OutputStream os = conn.getOutputStream();
                    os.write(params.getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    return response.toString();

                } catch (Exception e) {
                    return "{\"status\":\"fail\",\"reason\":\""
                            + e.getMessage() + "\"}";
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Log.d(TAG, "Server Response: " + result);

                if (callback == null) return;

                try {
                    JSONObject json = new JSONObject(result);
                    if ("success".equalsIgnoreCase(
                            json.optString("status"))) {

                        callback.onResult(
                                true,
                                "License valid till: "
                                        + json.optString("expiry", "unknown"));

                    } else {
                        callback.onResult(
                                false,
                                json.optString("reason",
                                        "License invalid"));
                    }
                } catch (Exception e) {
                    callback.onResult(
                            false,
                            "JSON parsing error: " + e.getMessage());
                }
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // ==================================================
    // HELPERS (ADDED – SAFE)
    // ==================================================

    private static String getAppName(Context ctx) {
        try {
            if (ctx == null) return "";
            ApplicationInfo ai = ctx.getApplicationInfo();
            if (ai.labelRes != 0)
                return ctx.getString(ai.labelRes);
            if (ai.nonLocalizedLabel != null)
                return ai.nonLocalizedLabel.toString();
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String getDeviceId(Context ctx) {
        try {
            if (ctx == null) return "unknown";
            String id = Settings.Secure.getString(
                    ctx.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            return id != null ? id : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}