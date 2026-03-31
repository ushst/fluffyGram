package org.ushastoe.fluffy.patches;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.QRCodeBottomSheet;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LoginActivity;

import java.util.ArrayList;
import java.util.WeakHashMap;

public final class LoginQrPatch {
    private static final int BUTTON_TAG = 910231;
    private static final int LINK_TAG = 910232;
    private static final int REQUEST_FLAGS = ConnectionsManager.RequestFlagFailOnServerErrors
            | ConnectionsManager.RequestFlagWithoutLogin
            | ConnectionsManager.RequestFlagTryDifferentDc
            | ConnectionsManager.RequestFlagEnableUnauthorized;
    private static final WeakHashMap<LoginActivity, Controller> CONTROLLERS = new WeakHashMap<>();

    private LoginQrPatch() {
    }

    public static void attach(LoginActivity fragment) {
        AndroidUtilities.runOnUIThread(() -> controller(fragment).attachButton());
    }

    public static boolean onMenuItemClick(LoginActivity fragment, int id) {
        return false;
    }

    public static void attachPhoneViewLink(LoginActivity fragment, ViewGroup container, boolean afterTestBackend) {
        if (!afterTestBackend || container.findViewWithTag(LINK_TAG) != null) {
            return;
        }

        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }

        TextView link = new TextView(activity);
        link.setTag(LINK_TAG);
        link.setText(LocaleController.getString(R.string.FluffyQrLoginMenu));
        link.setTextSize(16);
        link.setGravity(Gravity.CENTER_VERTICAL);
        int linkColor = Theme.getColor(Theme.key_windowBackgroundWhiteLinkText);
        link.setTextColor(linkColor);
        link.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        link.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.msg_qrcode, 0, 0, 0);
        Drawable[] drawables = link.getCompoundDrawablesRelative();
        if (drawables[0] != null) {
            drawables[0].mutate().setColorFilter(new PorterDuffColorFilter(linkColor, PorterDuff.Mode.SRC_IN));
        }
        link.setCompoundDrawablePadding(AndroidUtilities.dp(8));
        int horizontalPadding = AndroidUtilities.dp(12);
        int verticalPadding = AndroidUtilities.dp(8);
        link.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        link.setOnClickListener(v -> controller(fragment).beginExportFlow());
        container.addView(link, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 28, 0, 16, 0));
    }

    public static void detach(LoginActivity fragment) {
        Controller controller = CONTROLLERS.remove(fragment);
        if (controller != null) {
            controller.destroy();
        }
    }

    private static Controller controller(LoginActivity fragment) {
        Controller controller = CONTROLLERS.get(fragment);
        if (controller == null) {
            controller = new Controller(fragment);
            CONTROLLERS.put(fragment, controller);
        }
        return controller;
    }

    private static final class Controller implements NotificationCenter.NotificationCenterDelegate {
        private final LoginActivity fragment;
        private QRCodeBottomSheet qrCodeBottomSheet;
        private AlertDialog progressDialog;
        private Runnable refreshRunnable;
        private Runnable retryRunnable;
        private boolean observerRegistered;
        private boolean requestInFlight;
        private boolean restartRequested;

        private Controller(LoginActivity fragment) {
            this.fragment = fragment;
        }

        private void attachButton() {
            View root = fragment.getFragmentView();
            if (!(root instanceof ViewGroup)) {
                return;
            }
            ViewGroup rootView = (ViewGroup) root;
            View button = rootView.findViewWithTag(BUTTON_TAG);
            if (button != null) {
                rootView.removeView(button);
            }
        }

        private void beginExportFlow() {
            ConnectionsManager.getInstance(fragment.getCurrentAccount()).cleanup(false);
            restartRequested = false;
            startExportFlow(false);
        }

        private void startExportFlow(boolean fromRetry) {
            Activity activity = fragment.getParentActivity();
            if (activity == null) {
                return;
            }
            if (requestInFlight) {
                restartRequested = true;
                return;
            }
            requestInFlight = true;
            restartRequested = false;
            removeObserver();
            dismissQrCodeBottomSheet();
            dismissProgressDialog();
            progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
            progressDialog.setCanCancel(false);
            progressDialog.show();
            TLRPC.TL_auth_exportLoginToken exportRequest = new TLRPC.TL_auth_exportLoginToken();
            exportRequest.api_id = BuildVars.APP_ID;
            exportRequest.api_hash = BuildVars.APP_HASH;
            exportRequest.except_ids = collectExceptIds();
            ensureObserver();

            ConnectionsManager.getInstance(fragment.getCurrentAccount()).sendRequest(
                    exportRequest,
                    (response, error) -> AndroidUtilities.runOnUIThread(() -> handleExportResponse(response, error)),
                    null,
                    null,
                    null,
                    REQUEST_FLAGS,
                    ConnectionsManager.DEFAULT_DATACENTER_ID,
                    ConnectionsManager.ConnectionTypeGeneric,
                    false
            );
        }

        private ArrayList<Long> collectExceptIds() {
            ArrayList<Long> exceptIds = new ArrayList<>();
            for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
                UserConfig userConfig = UserConfig.getInstance(account);
                if (!userConfig.isClientActivated()) {
                    continue;
                }
                long userId = userConfig.getClientUserId();
                if (userId != 0) {
                    exceptIds.add(userId);
                }
            }
            return exceptIds;
        }

        private void handleExportResponse(TLObject response, TLRPC.TL_error error) {
            requestInFlight = false;
            dismissProgressDialog();
            if (fragment.getParentActivity() == null) {
                return;
            }
            if (response instanceof TLRPC.TL_auth_loginToken) {
                TLRPC.TL_auth_loginToken loginToken = (TLRPC.TL_auth_loginToken) response;
                showQrCode(loginToken);
                return;
            }
            if (response instanceof TLRPC.TL_auth_loginTokenMigrateTo) {
                TLRPC.TL_auth_loginTokenMigrateTo migrateTo = (TLRPC.TL_auth_loginTokenMigrateTo) response;
                importLoginToken(migrateTo);
                return;
            }
            if (response instanceof TLRPC.TL_auth_loginTokenSuccess) {
                completeLogin((TLRPC.TL_auth_loginTokenSuccess) response);
                return;
            }
            if (error != null) {
                handleError(error);
                return;
            }
            showError(LocaleController.getString(R.string.FluffyQrLoginExportFailed));
            continuePendingRestart();
        }

        private void importLoginToken(TLRPC.TL_auth_loginTokenMigrateTo response) {
            Activity activity = fragment.getParentActivity();
            if (activity == null) {
                return;
            }
            dismissProgressDialog();
            progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
            progressDialog.setCanCancel(false);
            progressDialog.show();
            ConnectionsManager.getInstance(fragment.getCurrentAccount()).setDefaultDatacenterId(response.dc_id);
            TLRPC.TL_auth_importLoginToken request = new TLRPC.TL_auth_importLoginToken();
            request.token = response.token;
            ConnectionsManager.getInstance(fragment.getCurrentAccount()).sendRequest(
                    request,
                    (response1, error1) -> AndroidUtilities.runOnUIThread(() -> handleExportResponse(response1, error1)),
                    null,
                    null,
                    null,
                    REQUEST_FLAGS,
                    ConnectionsManager.DEFAULT_DATACENTER_ID,
                    ConnectionsManager.ConnectionTypeGeneric,
                    false
            );
        }

        private void showQrCode(TLRPC.TL_auth_loginToken token) {
            Activity activity = fragment.getParentActivity();
            if (activity == null) {
                return;
            }
            String link = "tg://login?token=" + Base64.encodeToString(token.token, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            qrCodeBottomSheet = new QRCodeBottomSheet(
                    activity,
                    LocaleController.getString(R.string.FluffyQrLoginSheetTitle),
                    link,
                    LocaleController.getString(R.string.FluffyQrLoginSheetHelp),
                    false
            );
            qrCodeBottomSheet.setCenterAnimation(R.raw.qr_code_logo);
            qrCodeBottomSheet.setOnDismissListener(dialog -> {
                qrCodeBottomSheet = null;
                cancelRefresh();
                cancelRetry();
                if (!requestInFlight) {
                    removeObserver();
                }
            });
            qrCodeBottomSheet.show();

            int delaySeconds = token.expires - ConnectionsManager.getInstance(fragment.getCurrentAccount()).getCurrentTime();
            if (delaySeconds < 0) {
                delaySeconds = 20;
            }
            scheduleRefresh(delaySeconds * 1000L);
        }

        private void completeLogin(TLRPC.TL_auth_loginTokenSuccess success) {
            removeObserver();
            dismissQrCodeBottomSheet();
            cancelRefresh();
            cancelRetry();
            fragment.applyFluffyQrLoginAuthorization(success.authorization);
        }

        private void handleError(TLRPC.TL_error error) {
            if (error.text != null && error.text.contains("SESSION_PASSWORD_NEEDED")) {
                requestPassword();
            } else if (error.text != null && error.text.startsWith("AUTH_TOKEN_")) {
                scheduleRetry(1000);
            } else {
                showError(error.text);
                continuePendingRestart();
            }
        }

        private void requestPassword() {
            Activity activity = fragment.getParentActivity();
            if (activity == null) {
                return;
            }
            dismissProgressDialog();
            progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
            progressDialog.setCanCancel(false);
            progressDialog.show();
            TL_account.getPassword request = new TL_account.getPassword();
            ConnectionsManager.getInstance(fragment.getCurrentAccount()).sendRequest(
                    request,
                    (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        requestInFlight = false;
                        dismissProgressDialog();
                        if (error != null) {
                            showError(error.text);
                            continuePendingRestart();
                            return;
                        }
                        if (response instanceof TL_account.Password) {
                            removeObserver();
                            dismissQrCodeBottomSheet();
                            cancelRefresh();
                            cancelRetry();
                            fragment.applyFluffyQrLoginPassword((TL_account.Password) response);
                        } else {
                            showError(LocaleController.getString(R.string.FluffyQrLoginExportFailed));
                            continuePendingRestart();
                        }
                    }),
                    null,
                    null,
                    null,
                    ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagWithoutLogin,
                    ConnectionsManager.DEFAULT_DATACENTER_ID,
                    ConnectionsManager.ConnectionTypeGeneric,
                    false
            );
        }

        private void continuePendingRestart() {
            if (!requestInFlight && restartRequested) {
                restartRequested = false;
                startExportFlow(true);
            }
        }

        private void showError(String text) {
            if (fragment.getParentActivity() == null) {
                return;
            }
            String message = text;
            if (message == null || message.length() == 0) {
                message = LocaleController.getString(R.string.FluffyQrLoginExportFailed);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
            builder.setTitle(LocaleController.getString(R.string.FluffyQrLoginSheetTitle));
            builder.setMessage(message);
            builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
            fragment.showDialog(builder.create());
        }

        private void ensureObserver() {
            if (!observerRegistered) {
                NotificationCenter.getInstance(fragment.getCurrentAccount()).addObserver(this, NotificationCenter.updateLoginToken);
                observerRegistered = true;
            }
        }

        private void removeObserver() {
            if (observerRegistered) {
                NotificationCenter.getInstance(fragment.getCurrentAccount()).removeObserver(this, NotificationCenter.updateLoginToken);
                observerRegistered = false;
            }
        }

        private void scheduleRefresh(long delayMs) {
            cancelRefresh();
            refreshRunnable = () -> startExportFlow(true);
            AndroidUtilities.runOnUIThread(refreshRunnable, delayMs);
        }

        private void cancelRefresh() {
            if (refreshRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(refreshRunnable);
                refreshRunnable = null;
            }
        }

        private void scheduleRetry(long delayMs) {
            cancelRetry();
            retryRunnable = () -> {
                retryRunnable = null;
                ConnectionsManager.getInstance(fragment.getCurrentAccount()).cleanup(false);
                startExportFlow(true);
            };
            AndroidUtilities.runOnUIThread(retryRunnable, delayMs);
        }

        private void cancelRetry() {
            if (retryRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(retryRunnable);
                retryRunnable = null;
            }
        }

        private void dismissProgressDialog() {
            if (progressDialog != null) {
                try {
                    progressDialog.dismiss();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                progressDialog = null;
            }
        }

        private void dismissQrCodeBottomSheet() {
            if (qrCodeBottomSheet != null) {
                try {
                    qrCodeBottomSheet.dismiss();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                qrCodeBottomSheet = null;
            }
        }

        private void destroy() {
            requestInFlight = false;
            restartRequested = false;
            removeObserver();
            cancelRefresh();
            cancelRetry();
            dismissProgressDialog();
            dismissQrCodeBottomSheet();
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.updateLoginToken) {
                if (qrCodeBottomSheet == null) {
                    return;
                }
                if (requestInFlight) {
                    restartRequested = true;
                    return;
                }
                startExportFlow(true);
            }
        }
    }
}
