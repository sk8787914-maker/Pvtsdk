package top.niunaijun.blackbox.core.system.user;

import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

public final class BUserHandle implements Parcelable {
    public static final int AID_APP_END = 19999;
    public static final int AID_APP_START = 10000;
    public static final int AID_CACHE_GID_START = 20000;
    public static final int AID_ROOT = 0;
    public static final int AID_SHARED_GID_START = 50000;
    public static final BUserHandle ALL = new BUserHandle(-1);
    public static final Parcelable.Creator<BUserHandle> CREATOR = new Parcelable.Creator<BUserHandle>() {
        public BUserHandle createFromParcel(Parcel in) {
            return new BUserHandle(in);
        }

        public BUserHandle[] newArray(int size) {
            return new BUserHandle[size];
        }
    };
    public static final BUserHandle CURRENT = new BUserHandle(-2);
    public static final BUserHandle CURRENT_OR_SELF = new BUserHandle(-3);
    public static final int ERR_GID = -1;
    public static final boolean MU_ENABLED = true;
    @Deprecated
    public static final BUserHandle OWNER = new BUserHandle(0);
    public static final int PER_USER_RANGE = 100000;
    public static final BUserHandle SYSTEM = new BUserHandle(0);
    public static final int USER_ALL = -1;
    public static final int USER_CURRENT = -2;
    public static final int USER_CURRENT_OR_SELF = -3;
    public static final int USER_NULL = -10000;
    @Deprecated
    public static final int USER_OWNER = 0;
    public static final int USER_SERIAL_SYSTEM = 0;
    public static final int USER_SYSTEM = 0;
    public static final int USER_XPOSED = -4;
    final int mHandle;

    public static boolean isSameUser(int uid1, int uid2) {
        return getUserId(uid1) == getUserId(uid2);
    }

    public static boolean isSameApp(int uid1, int uid2) {
        return getAppId(uid1) == getAppId(uid2);
    }

    public static boolean isApp(int uid) {
        int appId;
        if (uid <= 0 || (appId = getAppId(uid)) < 10000 || appId > 19999) {
            return false;
        }
        return true;
    }

    public static boolean isCore(int uid) {
        if (uid < 0 || getAppId(uid) >= 10000) {
            return false;
        }
        return true;
    }

    public static BUserHandle getUserHandleForUid(int uid) {
        return of(getUserId(uid));
    }

    public static int getUserId(int uid) {
        return uid / PER_USER_RANGE;
    }

    public static int getCallingUserId() {
        return getUserId(Binder.getCallingUid());
    }

    public static int getCallingAppId() {
        return getAppId(Binder.getCallingUid());
    }

    public static BUserHandle of(int userId) {
        return userId == 0 ? SYSTEM : new BUserHandle(userId);
    }

    public static int getUid(int userId, int appId) {
        return (userId * PER_USER_RANGE) + (appId % PER_USER_RANGE);
    }

    public static int getAppId(int uid) {
        return uid % PER_USER_RANGE;
    }

    public static int getUserGid(int userId) {
        return getUid(userId, 9997);
    }

    public static int getSharedAppGid(int uid) {
        return getSharedAppGid(getUserId(uid), getAppId(uid));
    }

    public static int getSharedAppGid(int userId, int appId) {
        if (appId >= 10000 && appId <= 19999) {
            return (appId - 10000) + AID_SHARED_GID_START;
        }
        if (appId < 0 || appId > 10000) {
            return -1;
        }
        return appId;
    }

    public static int getCacheAppGid(int uid) {
        return getCacheAppGid(getUserId(uid), getAppId(uid));
    }

    public static int getCacheAppGid(int userId, int appId) {
        if (appId < 10000 || appId > 19999) {
            return -1;
        }
        return getUid(userId, (appId - 10000) + 20000);
    }

    public static int parseUserArg(String arg) {
        if ("all".equals(arg)) {
            return -1;
        }
        if ("current".equals(arg) || "cur".equals(arg)) {
            return -2;
        }
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad user number: " + arg);
        }
    }

    public static int myUserId() {
        return getUserId(Process.myUid());
    }

    @Deprecated
    public boolean isOwner() {
        return equals(OWNER);
    }

    public boolean isSystem() {
        return equals(SYSTEM);
    }

    public BUserHandle(int h) {
        this.mHandle = h;
    }

    public int getIdentifier() {
        return this.mHandle;
    }

    public String toString() {
        return "UserHandle{" + this.mHandle + "}";
    }

    public boolean equals(Object obj) {
        if (obj != null) {
            try {
                if (this.mHandle == ((BUserHandle) obj).mHandle) {
                    return true;
                }
                return false;
            } catch (ClassCastException e) {
            }
        }
        return false;
    }

    public int hashCode() {
        return this.mHandle;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mHandle);
    }

    public static void writeToParcel(BUserHandle h, Parcel out) {
        if (h != null) {
            h.writeToParcel(out, 0);
        } else {
            out.writeInt(-10000);
        }
    }

    public static BUserHandle readFromParcel(Parcel in) {
        int h = in.readInt();
        if (h != -10000) {
            return new BUserHandle(h);
        }
        return null;
    }

    public BUserHandle(Parcel in) {
        this.mHandle = in.readInt();
    }
}
