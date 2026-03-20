package org.telegram.messenger;

import android.content.Context;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.CreateCredentialCancellationException;
import androidx.credentials.exceptions.CreateCredentialInterruptedException;
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialInterruptedException;
import androidx.credentials.exceptions.NoCredentialException;

import org.json.JSONObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.AlertDialog;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.ushastoe.fluffy.hooks.FluffyPasskeysHook;

@RequiresApi(api = 28)
public class PasskeysController {

    public static void create(Context context, int currentAccount, Utilities.Callback2<TL_account.Passkey, String> done) {
        if (!BuildVars.SUPPORTS_PASSKEYS) return;

        final CredentialManager credentialManager = CredentialManager.create(context);
        final AlertDialog progressDialog = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.showDelayed(500);

        ConnectionsManager.getInstance(currentAccount).sendRequestTyped(
            new TL_account.initPasskeyRegistration(),
            AndroidUtilities::runOnUIThread,
            (res, err) -> {
                progressDialog.dismiss();
                if (err != null) {
                    done.run(null, err.text);
                    return;
                }

                final FluffyPasskeysHook.CreateOptions createOptions;
                try {
                    createOptions = FluffyPasskeysHook.prepareCreateOptions(res.options.data);
                } catch (Exception e) {
                    FileLog.e(e);
                    done.run(null, e.getMessage());
                    return;
                }

                try {
                    credentialManager.createCredential(context, createOptions.request, ktxCallback((res2, err2) -> {
                        if (err2 instanceof CreateCredentialCancellationException || err2 instanceof CreateCredentialInterruptedException) {
                            AndroidUtilities.runOnUIThread(() -> {
                                done.run(null, "CANCELLED");
                            });
                            return;
                        } else if (err2 instanceof CreateCredentialNoCreateOptionException) {
                            AndroidUtilities.runOnUIThread(() -> {
                                done.run(null, "EMPTY");
                            });
                            return;
                        } else if (err2 != null) {
                            FileLog.e(err2);
                            AndroidUtilities.runOnUIThread(() -> {
                                done.run(null, err2.getMessage());
                            });
                            return;
                        }

                        final TL_account.registerPasskey req2 = new TL_account.registerPasskey();

                        try {
                            final String responseJson = res2.getData().getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON");
                            FluffyPasskeysHook.applyRegisterResponse(req2, responseJson, createOptions.clientDataJson);
                        } catch (Exception e) {
                            FileLog.e(e);
                            AndroidUtilities.runOnUIThread(() -> {
                                done.run(null, e.getMessage());
                            });
                            return;
                        }

                        AndroidUtilities.runOnUIThread(() -> {
                            final AlertDialog progressDialog2 = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
                            progressDialog2.showDelayed(500);

                            final int requestId = ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req2, AndroidUtilities::runOnUIThread, (passkey, err3) -> {
                                progressDialog2.dismiss();
                                if (err3 != null) {
                                    done.run(null, err3.text);
                                } else {
                                    done.run(passkey, null);
                                }
                            });
                            progressDialog2.setOnCancelListener(d -> {
                                ConnectionsManager.getInstance(currentAccount).cancelRequest(requestId, true);
                                done.run(null, "CANCELLED");
                            });
                        });
                    }));
                } catch (Exception e) {
                    FileLog.e(e);
                    AndroidUtilities.runOnUIThread(() -> {
                        done.run(null, e.getMessage());
                    });
                }
            }
        );
    }

    public static Runnable login(Context context, int currentAccount, boolean clickedButton, Utilities.Callback3<Long, TLRPC.auth_Authorization, String> done) {
        if (!BuildVars.SUPPORTS_PASSKEYS) return null;

        final CredentialManager credentialManager = CredentialManager.create(context);

        final boolean[] cancelled = new boolean[1];
        final Runnable[] cancel = new Runnable[1];

        final TL_account.initPasskeyLogin req = new TL_account.initPasskeyLogin();
        req.api_id = BuildVars.APP_ID;
        req.api_hash = BuildVars.APP_HASH;
        final int requestId = ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, AndroidUtilities::runOnUIThread, (res, err) -> {
            if (cancelled[0]) return;
            if (err != null) {
                done.run(0L, null, err.text);
                return;
            }

            final FluffyPasskeysHook.LoginOptions loginOptions;
            try {
                loginOptions = FluffyPasskeysHook.prepareLoginOptions(res.options.data, clickedButton);
            } catch (Exception e) {
                FileLog.e(e);
                done.run(0L, null, e.getMessage());
                return;
            }

            try {
                final CancellationSignal cancellationSignal = new CancellationSignal();
                credentialManager.getCredentialAsync(context, loginOptions.request, cancellationSignal, context.getMainExecutor(), new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse res2) {
                        final Credential credential = res2.getCredential();

                        final FluffyPasskeysHook.LoginResult loginResult;

                        final TL_account.finishPasskeyLogin req2 = new TL_account.finishPasskeyLogin();

                        try {
                            final String responseJson = credential.getData().getString("androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON");
                            loginResult = FluffyPasskeysHook.applyLoginResponse(req2, responseJson, loginOptions.clientDataJson);
                        } catch (Exception e) {
                            FileLog.e(e);
                            done.run(0L, null, e.getMessage());
                            return;
                        }

                        final AlertDialog progressDialog = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
                        progressDialog.showDelayed(500);

                        if (loginResult.datacenterId != ConnectionsManager.getInstance(currentAccount).getCurrentDatacenterId()) {
                            final int from_dc_id = ConnectionsManager.getInstance(currentAccount).getCurrentDatacenterId();
                            final long from_auth_key_id = ConnectionsManager.getInstance(currentAccount).getCurrentAuthKeyId();

                            ConnectionsManager.getInstance(currentAccount).setDefaultDatacenterId(loginResult.datacenterId);

                            req2.flags |= TLObject.FLAG_0;
                            req2.from_dc_id = from_dc_id;
                            req2.from_auth_key_id = from_auth_key_id;
                        }

                        final int requestId = ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req2, AndroidUtilities::runOnUIThread, (auth, err3) -> {
                            progressDialog.dismiss();
                            if (err3 != null) {
                                done.run(loginResult.userId, null, err3.text);
                            } else {
                                done.run(loginResult.userId, auth, null);
                            }
                        }, loginResult.datacenterId, ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagInvokeAfter);

                        progressDialog.setOnCancelListener(d -> {
                            ConnectionsManager.getInstance(currentAccount).cancelRequest(requestId, true);
                            done.run(loginResult.userId, null, "CANCELLED");
                        });
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException err2) {
                        if (err2 instanceof NoCredentialException) {
                            done.run(0L, null, "EMPTY");
                        } else if (err2 instanceof GetCredentialCancellationException) {
                            done.run(0L, null, "CANCELLED");
                        } else if (err2 instanceof GetCredentialInterruptedException) {
                            done.run(0L, null, "CANCELLED");
                        } else if (err2 != null) {
                            done.run(0L, null, err2.getMessage());
                        }
                    }
                });

                cancel[0] = cancellationSignal::cancel;
            } catch (Exception e) {
                done.run(0L, null, e.getMessage());
            }

        }, ConnectionsManager.RequestFlagWithoutLogin);

        cancel[0] = () -> ConnectionsManager.getInstance(currentAccount).cancelRequest(requestId, true);

        return () -> {
            cancelled[0] = true;
            if (cancel[0] != null) {
                cancel[0].run();
            }
        };
    }

    public static <T> Continuation<T> ktxCallback(Utilities.Callback2<T, Throwable> done) {
        return ktxCallback(EmptyCoroutineContext.INSTANCE, done);
    }

    public static <T> Continuation<T> ktxCallback(CoroutineContext ctx, Utilities.Callback2<T, Throwable> done) {
        return new Continuation<T>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return ctx;
            }

            @Override
            public void resumeWith(@NonNull Object result) {
                if (result instanceof Result.Failure) {
                    done.run(null, ((Result.Failure) result).exception);
                } else {
                    done.run((T) result, null);
                }
            }
        };
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
