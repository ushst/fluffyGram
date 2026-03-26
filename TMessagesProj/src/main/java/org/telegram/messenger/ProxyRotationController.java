package org.telegram.messenger;

import android.content.SharedPreferences;
import android.os.SystemClock;

import org.telegram.tgnet.ConnectionsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProxyRotationController implements NotificationCenter.NotificationCenterDelegate {
    private final static ProxyRotationController INSTANCE = new ProxyRotationController();
    private static final long PROXY_CHECK_CACHE_TTL_MS = 2 * 60 * 1000L;
    private static final long PERIODIC_RECHECK_INTERVAL_MS = 60 * 1000L;

    public final static int DEFAULT_TIMEOUT_INDEX = 1;
    public final static List<Integer> ROTATION_TIMEOUTS = Arrays.asList(
            5, 10, 15, 30, 60
    );
    public static final int DEFAULT_PING_SWITCH_THRESHOLD_MS = 20;
    public final static List<Integer> PING_SWITCH_THRESHOLDS = Arrays.asList(
            20, 40, 60, 80, 100
    );

    private boolean isCurrentlyChecking;
    private boolean forceSwitchOnCheckComplete;
    private final Runnable periodicCheckRunnable = () -> {
        if (!shouldUseProxyRotation()) {
            return;
        }
        schedulePeriodicCheck();
        if (!isCurrentlyChecking) {
            startProxyCheck(false);
        }
    };
    private Runnable checkProxyAndSwitchRunnable = () -> {
        startProxyCheck(true);
    };

    private void startProxyCheck(boolean forceSwitch) {
        isCurrentlyChecking = true;
        forceSwitchOnCheckComplete = forceSwitch;

        int currentAccount = UserConfig.selectedAccount;
        boolean startedCheck = false;
        for (int i = 0; i < SharedConfig.proxyList.size(); i++) {
            SharedConfig.ProxyInfo proxyInfo = SharedConfig.proxyList.get(i);
            if (proxyInfo.checking || SystemClock.elapsedRealtime() - proxyInfo.availableCheckTime < PROXY_CHECK_CACHE_TTL_MS) {
                continue;
            }
            startedCheck = true;
            proxyInfo.checking = true;
            proxyInfo.proxyCheckPingId = ConnectionsManager.getInstance(currentAccount).checkProxy(proxyInfo.address, proxyInfo.port, proxyInfo.username, proxyInfo.password, proxyInfo.secret, time -> AndroidUtilities.runOnUIThread(() -> {
                proxyInfo.availableCheckTime = SystemClock.elapsedRealtime();
                proxyInfo.checking = false;
                if (time == -1) {
                    proxyInfo.available = false;
                    proxyInfo.ping = 0;
                } else {
                    proxyInfo.ping = time;
                    proxyInfo.available = true;
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, proxyInfo);
            }));
        }

        if (!startedCheck) {
            isCurrentlyChecking = false;
            switchToBestAvailable(forceSwitchOnCheckComplete);
        }
    }

    public static void init() {
        INSTANCE.initInternal();
    }

    @SuppressWarnings("ComparatorCombinators")
    private void switchToBestAvailable(boolean forceSwitch) {
        isCurrentlyChecking = false;

        if (!SharedConfig.proxyRotationEnabled) {
            return;
        }

        List<SharedConfig.ProxyInfo> sortedList = new ArrayList<>(SharedConfig.proxyList);
        Collections.sort(sortedList, (o1, o2) -> Long.compare(o1.ping, o2.ping));
        SharedConfig.ProxyInfo bestProxy = null;
        for (SharedConfig.ProxyInfo info : sortedList) {
            if (info.checking || !info.available) {
                continue;
            }
            bestProxy = info;
            break;
        }

        if (bestProxy == null) {
            return;
        }

        SharedConfig.ProxyInfo currentProxy = SharedConfig.currentProxy;
        if (currentProxy == bestProxy) {
            return;
        }

        if (!forceSwitch && currentProxy != null && currentProxy.available) {
            long currentPing = currentProxy.ping > 0 ? currentProxy.ping : Long.MAX_VALUE;
            long bestPing = bestProxy.ping > 0 ? bestProxy.ping : Long.MAX_VALUE;
            if (bestPing + SharedConfig.proxyRotationPingThreshold >= currentPing) {
                return;
            }
        }

        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putString("proxy_ip", bestProxy.address);
        editor.putString("proxy_pass", bestProxy.password);
        editor.putString("proxy_user", bestProxy.username);
        editor.putInt("proxy_port", bestProxy.port);
        editor.putString("proxy_secret", bestProxy.secret);
        editor.putBoolean("proxy_enabled", true);

        if (!bestProxy.secret.isEmpty()) {
            editor.putBoolean("proxy_enabled_calls", false);
        }
        editor.apply();

        SharedConfig.currentProxy = bestProxy;
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyChangedByRotation);
        ConnectionsManager.setProxySettings(true, SharedConfig.currentProxy.address, SharedConfig.currentProxy.port, SharedConfig.currentProxy.username, SharedConfig.currentProxy.password, SharedConfig.currentProxy.secret);
    }

    private boolean shouldUseProxyRotation() {
        return SharedConfig.isProxyEnabled() && SharedConfig.proxyRotationEnabled && SharedConfig.proxyList.size() > 1;
    }

    private void cancelScheduledChecks() {
        AndroidUtilities.cancelRunOnUIThread(checkProxyAndSwitchRunnable);
        AndroidUtilities.cancelRunOnUIThread(periodicCheckRunnable);
    }

    private void schedulePeriodicCheck() {
        AndroidUtilities.cancelRunOnUIThread(periodicCheckRunnable);
        if (shouldUseProxyRotation()) {
            AndroidUtilities.runOnUIThread(periodicCheckRunnable, PERIODIC_RECHECK_INTERVAL_MS);
        }
    }

    private void initInternal() {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.didUpdateConnectionState);
        }
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyCheckDone);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.proxyCheckDone) {
            if (!shouldUseProxyRotation() || !isCurrentlyChecking) {
                return;
            }

            switchToBestAvailable(forceSwitchOnCheckComplete);
        } else if (id == NotificationCenter.proxySettingsChanged) {
            cancelScheduledChecks();
            if (shouldUseProxyRotation()) {
                schedulePeriodicCheck();
            }
        } else if (id == NotificationCenter.didUpdateConnectionState && account == UserConfig.selectedAccount) {
            if (!shouldUseProxyRotation()) {
                cancelScheduledChecks();
                return;
            }

            int state = ConnectionsManager.getInstance(account).getConnectionState();

            if (state == ConnectionsManager.ConnectionStateConnectingToProxy) {
                if (!isCurrentlyChecking) {
                    AndroidUtilities.runOnUIThread(checkProxyAndSwitchRunnable, ROTATION_TIMEOUTS.get(SharedConfig.proxyRotationTimeout) * 1000L);
                }
            } else if (state == ConnectionsManager.ConnectionStateConnected) {
                schedulePeriodicCheck();
            } else {
                cancelScheduledChecks();
            }
        }
    }
}
