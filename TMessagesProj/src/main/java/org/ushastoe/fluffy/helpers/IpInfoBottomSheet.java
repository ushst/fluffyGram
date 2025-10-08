package org.ushastoe.fluffy.helpers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.ushastoe.fluffy.helpers.IpApiHelper;

public class IpInfoBottomSheet extends BottomSheet {

  private final BaseFragment fragment;
  private final String ipAddress;

  private FrameLayout contentFrame;
  private TextView resultTextView;
  private ProgressBar progressBar;
  private ImageView copyButton;

  private String resultTextToCopy;

  private IpInfoBottomSheet(BaseFragment fragment, String ipAddress) {
    super(fragment.getParentActivity(), false);
    this.fragment = fragment;
    this.ipAddress = ipAddress;
    this.resultTextToCopy = "";

    // Основной контейнер
    FrameLayout container = new FrameLayout(getContext());
    container.setBackgroundColor(getThemedColor(Theme.key_dialogBackground));
    setCustomView(container);

    // Заголовок
    TextView titleTextView = new TextView(getContext());
    titleTextView.setText(
        LocaleController.getString("checkIp", R.string.checkIp));
    titleTextView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
    titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
    titleTextView.setTypeface(AndroidUtilities.bold());
    titleTextView.setPadding(dp(22), dp(20), dp(22), dp(20));
    container.addView(titleTextView,
                      LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                               LayoutHelper.WRAP_CONTENT,
                                               Gravity.TOP));

    // Контейнер для контента (результат или прогресс-бар)
    contentFrame = new FrameLayout(getContext());
    container.addView(contentFrame,
                      LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 150,
                                               Gravity.TOP, 0, 70, 0, 80));

    // Прогресс-бар
    progressBar = new ProgressBar(getContext());
    contentFrame.addView(progressBar,
                         LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT,
                                                  LayoutHelper.WRAP_CONTENT,
                                                  Gravity.CENTER));

    // TextView для результата
    resultTextView = new TextView(getContext());
    resultTextView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
    resultTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
    resultTextView.setPadding(dp(22), 0, dp(22), 0);
    resultTextView.setGravity(Gravity.TOP);
    resultTextView.setVisibility(GONE);
    contentFrame.addView(resultTextView,
                         LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                                  LayoutHelper.MATCH_PARENT));

    // Нижняя панель с кнопками
    FrameLayout buttonView = new FrameLayout(getContext());
    buttonView.setBackgroundColor(getThemedColor(Theme.key_dialogBackground));
    container.addView(buttonView,
                      LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                               LayoutHelper.WRAP_CONTENT,
                                               Gravity.BOTTOM));

    View buttonShadowView = new View(getContext());
    buttonShadowView.setBackgroundColor(
        getThemedColor(Theme.key_dialogShadowLine));
    buttonView.addView(
        buttonShadowView,
        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                 AndroidUtilities.getShadowHeight()));

    // Кнопка "Закрыть"
    TextView closeButton = new TextView(getContext());
    closeButton.setLines(1);
    closeButton.setSingleLine(true);
    closeButton.setGravity(Gravity.CENTER);
    closeButton.setTextColor(
        Theme.getColor(Theme.key_featuredStickers_buttonText));
    closeButton.setTypeface(AndroidUtilities.bold());
    closeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
    closeButton.setText(getString("Close", R.string.Close));
    closeButton.setBackground(Theme.AdaptiveRipple.filledRect(
        Theme.getColor(Theme.key_featuredStickers_addButton), 6));
    closeButton.setOnClickListener(v -> dismiss());
    buttonView.addView(
        closeButton,
        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48,
                                 Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 16,
                                 16, 72, 16));

    // Кнопка "Копировать"
    copyButton = new ImageView(getContext());
    copyButton.setScaleType(ImageView.ScaleType.CENTER);
    copyButton.setImageResource(R.drawable.msg_copy);
    copyButton.setColorFilter(
        Theme.getColor(Theme.key_featuredStickers_buttonText));
    copyButton.setBackground(Theme.AdaptiveRipple.filledRect(
        Theme.getColor(Theme.key_featuredStickers_addButton), 6));
    copyButton.setOnClickListener(v -> {
      if (!TextUtils.isEmpty(resultTextToCopy)) {
        AndroidUtilities.addToClipboard(resultTextToCopy);
        // В вашем стиле используется BulletinFactory, можно использовать и его,
        // или простой Toast
        Toast
            .makeText(getContext(),
                      getString("TextCopied", R.string.TextCopied),
                      Toast.LENGTH_SHORT)
            .show();
      }
    });
    copyButton.setEnabled(false);
    copyButton.setAlpha(0.5f);
    buttonView.addView(
        copyButton, LayoutHelper.createFrame(
                        48, 48, Gravity.BOTTOM | Gravity.RIGHT, 0, 16, 16, 16));

    // Запускаем загрузку данных
    fetchIpInfo();
  }

  private void fetchIpInfo() {
    IpApiHelper.getIpInfo(ipAddress, (ipInfo, exception) -> {
      // Вся работа с UI должна быть в главном потоке
      AndroidUtilities.runOnUIThread(() -> {
        progressBar.setVisibility(GONE);
        resultTextView.setVisibility(VISIBLE);

        if (exception != null) {
          resultTextView.setText("Error: " + exception.getMessage());
          resultTextToCopy = "Error: " + exception.getMessage();
        } else if (ipInfo != null && "success".equals(ipInfo.status)) {
          resultTextToCopy = buildInfoString(ipInfo);
          resultTextView.setText(resultTextToCopy);
          copyButton.setEnabled(true);
          copyButton.setAlpha(1.0f);
        } else {
          String error = (ipInfo != null && ipInfo.status != null)
                             ? ipInfo.status
                             : "Unknown error";
          resultTextView.setText("Failed to get info: " + error);
          resultTextToCopy = "Failed to get info: " + error;
        }
      });
    });
  }

  private String buildInfoString(IpApiHelper.IpInfoResponse info) {
    StringBuilder sb = new StringBuilder();
    sb.append("IP: ").append(info.query).append("\n");
    sb.append("Country: ")
        .append(info.country)
        .append(" (")
        .append(info.countryCode)
        .append(")\n");
    sb.append("Region: ").append(info.regionName).append("\n");
    sb.append("City: ").append(info.city).append("\n");
    sb.append("ISP: ").append(info.isp).append("\n");
    sb.append("Coordinates: ").append(info.lat).append(", ").append(info.lon);
    return sb.toString();
  }

  /**
   * Статический метод для удобного создания и отображения BottomSheet.
   */
  public static void show(BaseFragment fragment, String ipAddress) {
    if (fragment == null || fragment.getParentActivity() == null) {
      return;
    }
    IpInfoBottomSheet bottomSheet = new IpInfoBottomSheet(fragment, ipAddress);
    fragment.showDialog(bottomSheet);
  }
}