package org.ushastoe.fluffy.activities.elements;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.ushastoe.fluffy.fluffyConfig.getFirstName;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.ushastoe.fluffy.fluffyConfig;

@SuppressLint("ViewConstructor")
public class StickerSizePreviewMessagesCell extends LinearLayout {

  private final ChatMessageCell[] cells = new ChatMessageCell[2];
  private final MessageObject[] messageObjects = new MessageObject[2];
  private final int currentAccount = UserConfig.selectedAccount;
  private final BaseFragment fragment;

  private final String LOG_TAG = "StickerSizeDebug";

  public StickerSizePreviewMessagesCell(Context context,
                                        BaseFragment fragment) {
    super(context);
    this.fragment = fragment;
    var resourcesProvider = fragment.getResourceProvider();

    setWillNotDraw(true);
    setOrientation(LinearLayout.VERTICAL);
    setPadding(0, dp(11), 0, dp(11));

    for (int i = 0; i < cells.length; i++) {
      cells[i] = new ChatMessageCell(context, currentAccount, false, null,
                                     resourcesProvider);
      cells[i].setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
        @Override
        public boolean canPerformActions() {
          return true;
        }

        @Override
        public void didPressImage(ChatMessageCell cell, float x, float y,
                                  boolean fullPreview) {
          BulletinFactory.of(fragment)
              .createErrorBulletin("meow", resourcesProvider)
              .show();
        }
      });
      cells[i].isChat = false;
      cells[i].setFullyDraw(true);
      addView(cells[i], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                                                  LayoutHelper.WRAP_CONTENT));
    }

    // ╨Ъ╨╗╤О╤З╨╡╨▓╨╛╨╣ ╨╝╨╛╨╝╨╡╨╜╤В: ╨┐╤А╨╛╨▒╤Г╨╡╨╝ ╤Б╤А╨░╨╖╤Г ╨▓╨╖╤П╤В╤М ╤Б╨▓╨╡╨╢╨╕╨╣ ╤Б╤В╨╕╨║╨╡╤А ╨╕╨╖ ╨║╨╡╤И╨░. ╨Х╤Б╨╗╨╕ ╨╛╨╜ ╨╡╤Б╤В╤М
    // тАФ ╨┐╨╛╨║╨░╨╖╤Л╨▓╨░╨╡╨╝!
    ArrayList<TLRPC.Document> recent =
        MediaDataController.getInstance(currentAccount)
            .getRecentStickers(MediaDataController.TYPE_IMAGE);
    if (recent != null && !recent.isEmpty()) {
      Log.d(LOG_TAG, "Init: ╨┐╨╛╨║╨░╨╖╤Л╨▓╨░╨╡╨╝ ╤Б╤В╨╕╨║╨╡╤А ╨╕╨╖ ╨║╨╡╤И╨░");
      buildMessages(recent.get(0));
      updateCells();
    } else {
      Log.d(LOG_TAG, "Init: recent stickers ╨┐╤Г╤Б╤В, ╨┐╨╛╨║╨░╨╖╤Л╨▓╨░╨╡╨╝ ╨╖╨░╨│╨╗╤Г╤И╨║╤Г");
      buildMessages(null);
      updateCells();
    }

    // ╨Я╨╛╨┤╨┐╨╕╤Б╨║╨░ ╨╜╨░ ╨┐╨╛╨┤╨│╤А╤Г╨╖╨║╤Г recent stickers. ╨Ф╨╡╨╗╨╡╨│╨░╤В ╨▓╤Л╨╜╨╡╤Б╨╡╨╜ ╨╛╤В╨┤╨╡╨╗╤М╨╜╨╛ ╨┤╨╗╤П
    // removeObserver.
    NotificationCenter.getInstance(currentAccount)
        .addObserver(recentDocumentsDelegate,
                     NotificationCenter.recentDocumentsDidLoad);

    // ╨в╨╛╨╗╤М╨║╨╛ ╨╡╤Б╨╗╨╕ recent ╨┐╤Г╤Б╤В, ╨╕╨╜╨╕╤Ж╨╕╨╕╤А╤Г╨╡╨╝ ╨╖╨░╨│╤А╤Г╨╖╨║╤Г recent stickers.
    if (recent == null || recent.isEmpty()) {
      MediaDataController.getInstance(currentAccount)
          .loadRecents(MediaDataController.TYPE_IMAGE, false, true, false);
    }
  }

  private final NotificationCenter
      .NotificationCenterDelegate recentDocumentsDelegate =
      (id, accountId, args) -> {
    if (id == NotificationCenter.recentDocumentsDidLoad &&
        accountId == currentAccount) {
      ArrayList<TLRPC.Document> recent =
          MediaDataController.getInstance(currentAccount)
              .getRecentStickers(MediaDataController.TYPE_IMAGE);
      if (recent != null && !recent.isEmpty()) {
        Log.d(LOG_TAG, "Recent loaded, ╨┐╨╛╨║╨░╨╖╤Л╨▓╨░╨╡╨╝ ╤Б╤В╨╕╨║╨╡╤А");
        buildMessages(recent.get(0));
        updateCells();
      } else {
        Log.d(LOG_TAG, "Recent loaded, ╨╜╨╛ ╨▓╤Б╨╡ ╨╡╤Й╨╡ ╨┐╤Г╤Б╤В╨╛");
      }
    }
  };

  public void rebuildStickerPreview() {
    ArrayList<TLRPC.Document> recent =
        MediaDataController.getInstance(currentAccount)
            .getRecentStickers(MediaDataController.TYPE_IMAGE);
    if (recent != null && !recent.isEmpty()) {
      buildMessages(recent.get(0));
    } else {
      buildMessages(null);
    }
    updateCells();
  }

  private void buildMessages(@Nullable TLRPC.Document document) {
    Log.d(LOG_TAG, "buildMessages, document is null? " + (document == null));
    int date = (int)(System.currentTimeMillis() / 1000) - 60 * 60;
    TLRPC.TL_message msg = new TLRPC.TL_message();
    msg.date = date + 10;
    msg.dialog_id = 1;
    msg.flags = 257;
    msg.from_id = new TLRPC.TL_peerUser();
    msg.from_id.user_id =
        UserConfig.getInstance(currentAccount).getClientUserId();
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
    reply.message =
        LocaleController.getString(R.string.StickerSizeDialogMessageReplyTo);
    reply.date = date + 1270;
    reply.dialog_id = -1;
    reply.flags = 259;
    reply.id = 2;
    reply.media = new TLRPC.TL_messageMediaEmpty();
    reply.out = false;
    reply.peer_id = new TLRPC.TL_peerUser();
    reply.peer_id.user_id = 1;
    messageObjects[0].customReplyName = getFirstName();
    messageObjects[0].replyMessageObject =
        new MessageObject(currentAccount, reply, true, false);

    // Main
    TLRPC.TL_message main = new TLRPC.TL_message();
    main.message =
        LocaleController.getString(R.string.StickerSizeDialogMessage);
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
      cells[i].setMessageObject(messageObjects[i], null, false, false, false);
      cells[i].invalidate();
    }
  }

  private TLRPC.Document createFakeSticker(int date) {
    TLRPC.TL_document doc = new TLRPC.TL_document();
    doc.mime_type = "image/webp";
    doc.file_reference = new byte[0];
    doc.access_hash = 0;
    doc.date = date;
    TLRPC.TL_documentAttributeSticker attr =
        new TLRPC.TL_documentAttributeSticker();
    attr.alt = "ЁЯРИтмЫ";
    doc.attributes.add(attr);
    TLRPC.TL_documentAttributeImageSize size =
        new TLRPC.TL_documentAttributeImageSize();
    size.w = 512;
    size.h = 512;
    doc.attributes.add(size);
    return doc;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    NotificationCenter.getInstance(currentAccount)
        .removeObserver(recentDocumentsDelegate,
                        NotificationCenter.recentDocumentsDidLoad);
    Log.d(LOG_TAG, "onDetachedFromWindow, observer removed");
  }

  @Override
  protected void dispatchSetPressed(boolean pressed) {}
}
