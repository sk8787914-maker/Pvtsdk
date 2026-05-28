package top.niunaijun.blackbox.core.system.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.service.api.LicenseVerifier;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public final class MetaActivationManager {

    private static final String TAG = "BoxActivate";

    private static volatile boolean integrityChecked = false;
    private static volatile boolean licenseActivated = false;
    private static volatile String licenseMessage = "License not activated";

    private static boolean testMode = false;

    // 🔥 ADD: single background worker
    private static final ExecutorService WORKER =
            Executors.newSingleThreadExecutor();

    // 🔥 ADD: toast spam protection
    private static final AtomicBoolean toastShown = new AtomicBoolean(false);

    static {
        safeIntegrityInit();
    }

    // ---------------- INTEGRITY ----------------

    private static void safeIntegrityInit() {
        try {
            runIntegrityCheck();
        } catch (Throwable t) {
            integrityChecked = false;
            Log.e(TAG, "Integrity init failed", t);
        }
    }

    private static void runIntegrityCheck() {
        if (!verifyStructure()) {
            integrityChecked = false;
            Log.e(TAG, "Integrity structure failed");
            return;
        }
        integrityChecked = true;
        Log.d(TAG, "Integrity OK");
    }

    private static boolean verifyStructure() {
        try {
            Class<?> cls = MetaActivationManager.class;
            cls.getDeclaredField("licenseActivated");
            cls.getDeclaredField("integrityChecked");
            cls.getDeclaredMethod("isActivated");
            cls.getDeclaredMethod("activateBox", String.class);
            cls.getDeclaredMethod("getLicenseMessage");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // ---------------- ACTIVATION ----------------

    // 🔥 BACKWARD COMPAT
    public static void activateSdk(String licenseKey) {
        activateBox(licenseKey);
    }

    public static void activateBox(final String licenseKey) {
        if (!integrityChecked) {
            showToastOnce();
            return;
        }

        if (testMode) {
            licenseActivated = true;
            licenseMessage = "Activated (test mode)";
            return;
        }

        if (licenseKey == null || licenseKey.isEmpty()) {
            licenseActivated = false;
            licenseMessage = "Invalid key";
            showToastOnce();
            return;
        }

        // 🔥 FIX: no new Thread()
        WORKER.execute(() -> verifyLicenseInternal(licenseKey));
    }

    private static void verifyLicenseInternal(String key) {
        try {
            LicenseVerifier.verify(key, (valid, msg) -> {
                licenseActivated = valid;
                licenseMessage = (msg != null) ? msg : "Unknown";

                if (!valid) {
                    showToastOnce();
                }

                Log.d(TAG,
                        "License result=" + valid +
                        ", message=" + licenseMessage);
            });
        } catch (Throwable t) {
            licenseActivated = false;
            licenseMessage = "Activation failed";
            Log.e(TAG, "verifyLicenseInternal error", t);
            showToastOnce();
        }
    }

    // ---------------- PUBLIC API ----------------

    public static boolean isActivated() {
        if (!integrityChecked) {
            showToastOnce();
            return false;
        }
        if (!licenseActivated) {
            showToastOnce();
        }
        return licenseActivated;
    }

    // 🔥 BACKWARD COMPAT (BlackBoxCore)
    public static boolean isLicenseActivated() {
        return isActivated();
    }

    public static String getLicenseMessage() {
        return integrityChecked ? licenseMessage : "Integrity failed";
    }

    // ---------------- UI ----------------

    // 🔥 FIX: toast spam + lifecycle safe
    private static void showToastOnce() {
        if (!toastShown.compareAndSet(false, true)) return;

        final Context ctx = BlackBoxCore.getContext();
        if (ctx == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(
                        ctx,
                        "License expired or invalid",
                        Toast.LENGTH_LONG
                ).show();
            } catch (Throwable ignored) {
            }
        });
    }

    // ---------------- UTILS ----------------

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Throwable e) {
            return "";
        }
    }
}