package net.kdt.pojavlaunch.firefly.utils;

import net.kdt.pojavlaunch.firefly.Tools;
import net.kdt.pojavlaunch.firefly.value.MinecraftAccount;
import java.io.File;

/** Cliorius: manajemen sesi akun — logout semua sesi. */
public final class SessionManager {
    private SessionManager() {}

    public static final class LogoutResult {
        public final int removed;
        public final int microsoftRevoked;
        public LogoutResult(int removed, int microsoftRevoked) {
            this.removed = removed;
            this.microsoftRevoked = microsoftRevoked;
        }
    }

    public static LogoutResult logoutAll() {
        File dir = new File(Tools.DIR_ACCOUNT_NEW);
        int removed = 0, msRevoked = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((f, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    try {
                        MinecraftAccount acc = MinecraftAccount.parse(Tools.read(f.getAbsolutePath()));
                        if (acc.isMicrosoft) msRevoked++;
                    } catch (Exception ignored) {}
                    f.delete();
                    removed++;
                }
            }
        }
        return new LogoutResult(removed, msRevoked);
    }

    public static int countAccounts() {
        File dir = new File(Tools.DIR_ACCOUNT_NEW);
        File[] files = dir.listFiles((f, name) -> name.endsWith(".json"));
        return files == null ? 0 : files.length;
    }
}
