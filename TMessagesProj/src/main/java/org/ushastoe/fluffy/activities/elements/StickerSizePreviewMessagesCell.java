package org.ushastoe.fluffy.activities.elements;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.ushastoe.fluffy.fluffyConfig.getFirstName;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.MediaDataController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.BackgroundGradientDrawable;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MotionBackgroundDrawable;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class StickerSizePreviewMessagesCell extends LinearLayout {

    private final ChatMessageCell[] cells = new ChatMessageCell[2];
    private final MessageObject[] messageObjects = new MessageObject[2];
    private final FrameLayout fragmentView;
    private final Drawable shadowDrawable;
    private final int currentAccount = UserConfig.selectedAccount;
    private final BaseFragment fragment;
    private BackgroundGradientDrawable.Disposable backgroundGradientDisposable;

    private final String LOG_TAG = "StickerSizeDebug";

    public StickerSizePreviewMessagesCell(Context context, BaseFragment fragment) {
        super(context);
        this.fragment = fragment;
        var resourcesProvider = fragment.getResourceProvider();
        fragmentView = (FrameLayout) fragment.getFragmentView();

        setWillNotDraw(false);
        setOrientation(LinearLayout.VERTICAL);
        setPadding(0, dp(11), 0, dp(11));

        shadowDrawable = Theme.getThemedDrawable(context, R.drawable.greydivider_bottom,
                Theme.getColor(Theme.key_windowBackgroundGrayShadow, resourcesProvider));

        for (int i = 0; i < cells.length; i++) {
            cells[i] = new ChatMessageCell(context, currentAccount, false, null, resourcesProvider);
            cells[i].setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                @Override
                public boolean canPerformActions() {
                    return true;
                }

                @Override
                public void didPressImage(ChatMessageCell cell, float x, float y, boolean fullPreview) {
                    BulletinFactory.of(fragment).createErrorBulletin("meow", resourcesProvider).show();
                }
            });
            cells[i].isChat = false;
            cells[i].setFullyDraw(true);
            addView(cells[i], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        // Ключевой момент: пробуем сразу взять свежий стикер из кеша. Если он есть — показываем!
        ArrayList<TLRPC.Document> recent = MediaDataController.getInstance(currentAccount).getRecentStickers(MediaDataController.TYPE_IMAGE);
        if (recent != null && !recent.isEmpty()) {
            Log.d(LOG_TAG, "Init: показываем стикер из кеша");
            buildMessages(recent.get(0));
            updateCells();
        } else {
            Log.d(LOG_TAG, "Init: recent stickers пуст, показываем заглушку");
            buildMessages(null);
            updateCells();
        }

        // Подписка на подгрузку recent stickers. Делегат вынесен отдельно для removeObserver.
        NotificationCenter.getInstance(currentAccount).addObserver(recentDocumentsDelegate, NotificationCenter.recentDocumentsDidLoad);

        // Только если recent пуст, инициируем загрузку recent stickers.
        if (recent == null || recent.isEmpty()) {
            MediaDataController.getInstance(currentAccount).loadRecents(MediaDataController.TYPE_IMAGE, false, true, false);
        }
    }

    private final NotificationCenter.NotificationCenterDelegate recentDocumentsDelegate = (id, accountId, args) -> {
        if (id == NotificationCenter.recentDocumentsDidLoad && accountId == currentAccount) {
            ArrayList<TLRPC.Document> recent = MediaDataController.getInstance(currentAccount).getRecentStickers(MediaDataController.TYPE_IMAGE);
            if (recent != null && !recent.isEmpty()) {
                Log.d(LOG_TAG, "Recent loaded, показываем стикер");
                buildMessages(recent.get(0));
                updateCells();
            } else {
                Log.d(LOG_TAG, "Recent loaded, но все еще пусто");
            }
        }
    };

    public void rebuildStickerPreview() {
        ArrayList<TLRPC.Document> recent = MediaDataController.getInstance(currentAccount).getRecentStickers(MediaDataController.TYPE_IMAGE);
        if (recent != null && !recent.isEmpty()) {
            buildMessages(recent.get(0));
        } else {
            buildMessages(null);
        }
        updateCells();
    }

    private void buildMessages(@Nullable TLRPC.Document document) {
        Log.d(LOG_TAG, "buildMessages, document is null? " + (document == null));
        int date = (int) (System.currentTimeMillis() / 1000) - 60 * 60;
        TLRPC.TL_message msg = new TLRPC.TL_message();
        msg.date = date + 10;
        msg.dialog_id = 1;
        msg.flags = 257;
        msg.from_id = new TLRPC.TL_peerUser();
        msg.from_id.user_id = UserConfig.getInstance(currentAccount).getClientUserId();
        msg.id = 1;
        msg.media = new TLRPC.TL_messageMediaDocument();
        msg.media.flags = 1;
        msg.media.document = document != null ? document : createFakeSticker(date);
        msg.message = "";
        msg.out = true;
        msg.peer_id = new TLRPC.TL_peerUser();
        msg.peer_id.user_id = 0;
        messageObjects[0] = new MessageObject(currentAccount, msg, true, false);
        messageObjects[0].useCustomPhoto = true;

        // Reply
        TLRPC.TL_message reply = new TLRPC.TL_message();
        reply.message = LocaleController.getString(R.string.StickerSizeDialogMessageReplyTo);
        reply.date = date + 1270;
        reply.dialog_id = -1;
        reply.flags = 259;
        reply.id = 2;
        reply.media = new TLRPC.TL_messageMediaEmpty();
        reply.out = false;
        reply.peer_id = new TLRPC.TL_peerUser();
        reply.peer_id.user_id = 1;
        messageObjects[0].customReplyName = getFirstName();
        messageObjects[0].replyMessageObject = new MessageObject(currentAccount, reply, true, false);

        // Main
        TLRPC.TL_message main = new TLRPC.TL_message();
        main.message = LocaleController.getString(R.string.StickerSizeDialogMessage);
        main.date = date + 1270;
        main.dialog_id = -1;
        main.flags = 259;
        main.id = 3;
        main.reply_to = new TLRPC.TL_messageReplyHeader();
        main.reply_to.flags |= 16;
        main.reply_to.reply_to_msg_id = 2;
        main.media = new TLRPC.TL_messageMediaEmpty();
        main.out = false;
        main.peer_id = new TLRPC.TL_peerUser();
        main.peer_id.user_id = 1;
        messageObjects[1] = new MessageObject(currentAccount, main, true, false);
        messageObjects[1].replyMessageObject = messageObjects[0];
    }

    private void updateCells() {
        Log.d(LOG_TAG, "updateCells called");
        for (int i = 0; i < cells.length; i++) {
            cells[i].setMessageObject(messageObjects[i], null, false, false);
            cells[i].invalidate();
        }
    }

    private TLRPC.Document createFakeSticker(int date) {
        TLRPC.TL_document doc = new TLRPC.TL_document();
        doc.mime_type = "image/webp";
        doc.file_reference = new byte[0];
        doc.access_hash = 0;
        doc.date = date;
        TLRPC.TL_documentAttributeSticker attr = new TLRPC.TL_documentAttributeSticker();
        attr.alt = "🐈⬛";
        doc.attributes.add(attr);
        TLRPC.TL_documentAttributeImageSize size = new TLRPC.TL_documentAttributeImageSize();
        size.w = 512;
        size.h = 512;
        doc.attributes.add(size);
        return doc;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int stickerSize = org.ushastoe.fluffy.fluffyConfig.getStickerSize();
        Log.d(LOG_TAG, "onDraw, stickerSize=" + stickerSize);

        Drawable drawable = Theme.getCachedWallpaperNonBlocking();
        if (drawable == null) return;
        drawable.setAlpha(255);
        if (drawable instanceof ColorDrawable || drawable instanceof GradientDrawable || drawable instanceof MotionBackgroundDrawable) {
            drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            if (drawable instanceof BackgroundGradientDrawable bg) {
                backgroundGradientDisposable = bg.drawExactBoundsSize(canvas, this);
            } else {
                drawable.draw(canvas);
            }
        } else if (drawable instanceof BitmapDrawable bd) {
            if (bd.getTileModeX() == Shader.TileMode.REPEAT) {
                float scale = 2.0f / AndroidUtilities.density;
                canvas.save();
                canvas.scale(scale, scale);
                drawable.setBounds(0, 0, (int) Math.ceil(getMeasuredWidth() / scale), (int) Math.ceil(getMeasuredHeight() / scale));
            } else {
                int viewHeight = getMeasuredHeight();
                float scaleX = (float) getMeasuredWidth() / drawable.getIntrinsicWidth();
                float scaleY = (float) viewHeight / drawable.getIntrinsicHeight();
                float scale = Math.max(scaleX, scaleY);
                int width = (int) (drawable.getIntrinsicWidth() * scale);
                int height = (int) (drawable.getIntrinsicHeight() * scale);
                int x = (getMeasuredWidth() - width) / 2;
                int y = (viewHeight - height) / 2;
                canvas.save();
                canvas.clipRect(0, 0, width, viewHeight);
                drawable.setBounds(x, y, x + width, y + height);
            }
            drawable.draw(canvas);
            canvas.restore();
        }
        shadowDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
        shadowDrawable.draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (backgroundGradientDisposable != null) {
            backgroundGradientDisposable.dispose();
            backgroundGradientDisposable = null;
        }
        NotificationCenter.getInstance(currentAccount).removeObserver(recentDocumentsDelegate, NotificationCenter.recentDocumentsDidLoad);
        Log.d(LOG_TAG, "onDetachedFromWindow, observer removed");
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {}
}
