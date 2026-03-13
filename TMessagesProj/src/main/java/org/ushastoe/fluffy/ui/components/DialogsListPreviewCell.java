package org.ushastoe.fluffy.ui.components;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Components.LayoutHelper;

public class DialogsListPreviewCell extends FrameLayout {

    private final DialogCell dialogCell;
    private final DialogCell.CustomDialog previewDialog;

    public DialogsListPreviewCell(Context context) {
        super(context);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        dialogCell = new DialogCell(null, context, false, false);
        dialogCell.setClickable(false);
        dialogCell.setFocusable(false);
        addView(dialogCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

        previewDialog = new DialogCell.CustomDialog();
        previewDialog.id = 1;
        previewDialog.name = "fluffyGram";
        previewDialog.message = LocaleController.getString(R.string.AppName);
        previewDialog.date = (int) (System.currentTimeMillis() / 1000L);
        previewDialog.unread_count = 3;
        previewDialog.pinned = true;
        previewDialog.muted = false;
        previewDialog.verified = false;
        previewDialog.type = 0;
        previewDialog.sent = 0;

        refresh();
    }

    public void refresh() {
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        dialogCell.setDialog(previewDialog);
        dialogCell.requestLayout();
        dialogCell.invalidate();
        invalidate();
    }
}
