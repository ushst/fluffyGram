package org.ushastoe.fluffy.helpers;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.fluffyConfig;

public class WhisperHelper {
  private static OkHttpClient okHttpClient;
  private static final Gson gson = new Gson();
  private static final ExecutorService executorService =
      Executors.newCachedThreadPool();

  public static boolean useWorkersAi() { return fluffyConfig.useCloudFlare(); }

  public static void showErrorDialog(Exception e) {
    var fragment = LaunchActivity.getSafeLastFragment();
    var message = e.getLocalizedMessage();
    if (!BulletinFactory.canShowBulletin(fragment) || message == null) {
      return;
    }
    if (message.length() > 45) {
      AlertsCreator.showSimpleAlert(
          fragment, LocaleController.getString(R.string.ErrorOccurred),
          e.getMessage());
    } else {
      BulletinFactory.of(fragment).createErrorBulletin(message).show();
    }
  }

  public static void showCfCredentialsDialog(BaseFragment fragment) {
    var resourcesProvider = fragment.getResourceProvider();
    var context = fragment.getParentActivity();
    var builder = new AlertDialog.Builder(context, resourcesProvider);
    builder.setTitle(
        LocaleController.getString(R.string.CloudflareCredentials));
    builder.setMessage(AndroidUtilities.replaceSingleTag(
        LocaleController.getString(R.string.CloudflareCredentialsDialog), -1,
        AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD,
        ()
            -> Browser.openUrl(context, "https://ushst.ru/cloudflareCred"),
        resourcesProvider));
    builder.setCustomViewOffset(0);

    var ll = new LinearLayout(context);
    ll.setOrientation(LinearLayout.VERTICAL);

    var editTextAccountId = new EditTextBoldCursor(context) {
      @Override
      protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64),
                                                    MeasureSpec.EXACTLY));
      }
    };
    editTextAccountId.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    editTextAccountId.setTextColor(
        Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
    editTextAccountId.setText(fluffyConfig.cfAccountID);
    editTextAccountId.setHintText(
        LocaleController.getString(R.string.CloudflareAccountID));
    editTextAccountId.setHintColor(
        Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
    editTextAccountId.setHeaderHintColor(Theme.getColor(
        Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
    editTextAccountId.setSingleLine(true);
    editTextAccountId.setFocusable(true);
    editTextAccountId.setTransformHintToHeader(true);
    editTextAccountId.setLineColors(
        Theme.getColor(Theme.key_windowBackgroundWhiteInputField,
                       resourcesProvider),
        Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated,
                       resourcesProvider),
        Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
    editTextAccountId.setImeOptions(EditorInfo.IME_ACTION_NEXT);
    editTextAccountId.setBackground(null);
    editTextAccountId.requestFocus();
    editTextAccountId.setPadding(0, 0, 0, 0);
    ll.addView(editTextAccountId,
               LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 24,
                                         0, 24, 0));

    var editTextApiToken = new EditTextBoldCursor(context) {
      @Override
      protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64),
                                                    MeasureSpec.EXACTLY));
      }
    };
    editTextApiToken.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    editTextApiToken.setTextColor(
        Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
    editTextApiToken.setText(fluffyConfig.cfApiToken);
    editTextApiToken.setHintText(
        LocaleController.getString(R.string.CloudflareAPIToken));
    editTextApiToken.setHintColor(
        Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
    editTextApiToken.setHeaderHintColor(Theme.getColor(
        Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
    editTextApiToken.setSingleLine(true);
    editTextApiToken.setFocusable(true);
    editTextApiToken.setTransformHintToHeader(true);
    editTextApiToken.setLineColors(
        Theme.getColor(Theme.key_windowBackgroundWhiteInputField,
                       resourcesProvider),
        Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated,
                       resourcesProvider),
        Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
    editTextApiToken.setImeOptions(EditorInfo.IME_ACTION_DONE);
    editTextApiToken.setBackground(null);
    editTextApiToken.requestFocus();
    editTextApiToken.setPadding(0, 0, 0, 0);
    ll.addView(editTextApiToken,
               LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 24,
                                         0, 24, 0));

    builder.setView(ll);
    builder.setNegativeButton(LocaleController.getString(R.string.Cancel),
                              null);
    builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
    builder.setNeutralButton(LocaleController.getString(R.string.Test), null);
    var dialog = builder.create();
    fragment.showDialog(dialog);
    var button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

    if (button != null) {
      button.setOnClickListener(v -> {
        var accountId = editTextAccountId.getText();
        if (!TextUtils.isEmpty(accountId) && accountId.length() != 32) {
          AndroidUtilities.shakeViewSpring(editTextAccountId, -6);
          BotWebViewVibrationEffect.APP_ERROR.vibrate();
          return;
        }
        var apiToken = editTextApiToken.getText();
        if (!TextUtils.isEmpty(apiToken) && apiToken.length() != 40) {
          AndroidUtilities.shakeViewSpring(editTextApiToken, -6);
          BotWebViewVibrationEffect.APP_ERROR.vibrate();
          return;
        }
        fluffyConfig.setCfAccountID(accountId == null ? ""
                                                      : accountId.toString());
        fluffyConfig.setCfApiToken(apiToken == null ? "" : apiToken.toString());
        dialog.dismiss();
      });
    }
    var neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
    if (neutralButton != null) {
      neutralButton.setOnClickListener(v -> {
        var apiToken = editTextApiToken.getText().toString().trim();
        if (TextUtils.isEmpty(apiToken)) {
          AndroidUtilities.shakeViewSpring(editTextApiToken, -6);
          BotWebViewVibrationEffect.APP_ERROR.vibrate();
          return;
        }

        // Запускаем тест API токена
        new Thread(() -> {
          var result = testWorkersAi(apiToken);
          var isSuccess = result.keySet().iterator().next();
          var message = result.get(isSuccess);

          AndroidUtilities.runOnUIThread(() -> {
            AlertDialog.Builder resultDialog =
                new AlertDialog.Builder(context, resourcesProvider);
            resultDialog.setTitle(
                isSuccess ? LocaleController.getString(R.string.Success)
                          : LocaleController.getString(R.string.Error));
            resultDialog.setMessage(message);
            resultDialog.setPositiveButton(
                LocaleController.getString(R.string.OK), null);
            resultDialog.show();
          });
        }).start();
      });
    }
  }

  private static OkHttpClient getOkHttpClient() {
    if (okHttpClient == null) {
      var builder = new OkHttpClient.Builder();
      builder.connectTimeout(120, TimeUnit.SECONDS);
      builder.readTimeout(120, TimeUnit.SECONDS);
      builder.writeTimeout(120, TimeUnit.SECONDS);
      okHttpClient = builder.build();
    }
    return okHttpClient;
  }

  private static void extractAudio(String inputFilePath, String outputFilePath)
      throws IOException {
    var extractor = new MediaExtractor();
    extractor.setDataSource(inputFilePath);

    MediaFormat audioFormat = null;
    int audioTrackIndex = -1;
    for (int i = 0; i < extractor.getTrackCount(); i++) {
      var format = extractor.getTrackFormat(i);
      var mime = format.getString(MediaFormat.KEY_MIME);
      if (mime != null && mime.startsWith("audio/")) {
        audioFormat = format;
        audioTrackIndex = i;
        break;
      }
    }

    if (audioFormat == null) {
      throw new IOException("No audio track found in " + inputFilePath);
    }

    var muxer = new MediaMuxer(outputFilePath,
                               MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    var trackIndex = muxer.addTrack(audioFormat);
    muxer.start();

    extractor.selectTrack(audioTrackIndex);

    var bufferInfo = new MediaCodec.BufferInfo();
    var buffer = ByteBuffer.allocate(65536);

    while (true) {
      var sampleSize = extractor.readSampleData(buffer, 0);
      if (sampleSize < 0) {
        break;
      }

      bufferInfo.offset = 0;
      bufferInfo.size = sampleSize;
      bufferInfo.presentationTimeUs = extractor.getSampleTime();
      bufferInfo.flags = 0;

      muxer.writeSampleData(trackIndex, buffer, bufferInfo);
      extractor.advance();
    }

    muxer.stop();
    muxer.release();
    extractor.release();
  }

  public static Map<Boolean, String> testWorkersAi(String cfApiToken) {
    Future<Map<Boolean, String>> future = executorService.submit(() -> {
      var client = getOkHttpClient();
      var request =
          new Request.Builder()
              .url("https://api.cloudflare.com/client/v4/user/tokens/verify")
              .header("Authorization", "Bearer " + cfApiToken)
              .header("Content-Type", "application/json")
              .get()
              .build();
      try (Response response = client.newCall(request).execute()) {
        Map<Boolean, String> result = new HashMap<>();
        result.put(response.isSuccessful(), response.body().string());
        return result;
      } catch (Exception e) {
        e.printStackTrace();
        return Collections.singletonMap(false, e.getMessage());
      }
    });

    try {
      return future.get(); // Ожидаем завершения задачи и получаем результат
    } catch (Exception e) {
      e.printStackTrace();
      return Collections.singletonMap(false, "Ошибка выполнения");
    }
  }

  public static void requestWorkersAi(String path, boolean video,
                                      BiConsumer<String, Exception> callback) {
    if (TextUtils.isEmpty(fluffyConfig.cfAccountID) ||
        TextUtils.isEmpty(fluffyConfig.cfApiToken)) {
      callback.accept(null, new Exception(LocaleController.getString(
                                R.string.CloudflareCredentialsNotSet)));
      return;
    }
    executorService.submit(() -> {
      String audioPath;
      if (video) {
        var audioFile = new File(path + ".m4a");
        try {
          extractAudio(path, audioFile.getAbsolutePath());
        } catch (IOException e) {
          FileLog.e(e);
        }
        audioPath = audioFile.exists() ? audioFile.getAbsolutePath() : path;
      } else {
        audioPath = path;
      }
      var client = getOkHttpClient();
      var request =
          new Request.Builder()
              .url("https://api.cloudflare.com/client/v4/accounts/" +
                   fluffyConfig.cfAccountID + "/ai/run/@cf/openai/whisper")
              .header("Authorization", "Bearer " + fluffyConfig.cfApiToken)
              .post(RequestBody.create(
                  new File(audioPath),
                  MediaType.get(video ? "video/mp4" : "audio/ogg")));
      try (var response = client.newCall(request.build()).execute()) {
        var body = response.body().string();
        var whisperResponse = gson.fromJson(body, WhisperResponse.class);
        if (whisperResponse.success && whisperResponse.result != null) {
          callback.accept(whisperResponse.result.text, null);
        } else {
          var errors = whisperResponse.errors;
          callback.accept(null, new Exception(errors.size() == 1
                                                  ? errors.get(0).message
                                                  : errors.toString()));
        }
      } catch (Exception e) {
        callback.accept(null, e);
      }
    });
  }

  public static class Result {
    @SerializedName("text") @Expose public String text;
  }

  public static class WhisperResponse {
    @SerializedName("result") @Expose public Result result;
    @SerializedName("success") @Expose public Boolean success;
    @SerializedName("errors") @Expose public List<Error> errors;
  }

  public static class Error {
    @SerializedName("message") @Expose public String message;

    @NonNull
    @Override
    public String toString() {
      return message;
    }
  }
}
