package org.ushastoe.fluffy.activities.elements;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public final class FluffySettingsScaffold {

    public interface CardRowProvider {
        boolean isCardRow(int position);
    }

    private static final int LIST_OUTER_PADDING_DP = 12;
    private static final int CARD_RADIUS_DP = 22;
    private static final int CARD_HORIZONTAL_INSET_DP = 2;
    private static final int HEADER_TEXT_SIZE_DP = 13;
    private static final int SECTION_TEXT_SIZE_DP = 14;
    private static final int INFO_RADIUS_DP = 18;

    private FluffySettingsScaffold() {
    }

    public static int getListOuterPadding() {
        return AndroidUtilities.dp(LIST_OUTER_PADDING_DP);
    }

    public static int getCardHorizontalInset() {
        return AndroidUtilities.dp(CARD_HORIZONTAL_INSET_DP);
    }

    public static int getCardRadius() {
        return AndroidUtilities.dp(CARD_RADIUS_DP);
    }

    public static void applyPageContentPadding(LinearLayout content) {
        content.setPadding(getListOuterPadding(), getListOuterPadding(), getListOuterPadding(), AndroidUtilities.dp(24));
    }

    public static void styleHeader(TextView view) {
        view.setTypeface(AndroidUtilities.bold());
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, HEADER_TEXT_SIZE_DP);
        view.setLetterSpacing(0.02f);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        view.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
    }

    public static void styleSectionTitle(TextView view) {
        view.setTypeface(AndroidUtilities.bold());
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, SECTION_TEXT_SIZE_DP);
        view.setLetterSpacing(0.02f);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        view.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
    }

    public static void styleInfoBlock(TextView view) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        view.setLineSpacing(AndroidUtilities.dp(2), 1.05f);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        view.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(INFO_RADIUS_DP), Theme.getColor(Theme.key_windowBackgroundWhite)));
        view.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(14), AndroidUtilities.dp(18), AndroidUtilities.dp(14));
    }

    public static Drawable createCardBackground() {
        return Theme.createRoundRectDrawable(getCardRadius(), Theme.getColor(Theme.key_windowBackgroundWhite));
    }

    public static void styleCardContainer(FrameLayout card) {
        card.setBackground(createCardBackground());
        card.setPadding(0, 0, 0, 0);
    }

    public static RecyclerView.LayoutParams createCardSectionLayoutParams(int bottomMarginDp) {
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = AndroidUtilities.dp(bottomMarginDp);
        int sideInset = getCardHorizontalInset();
        params.leftMargin = sideInset;
        params.rightMargin = sideInset;
        return params;
    }

    public static void applyCardRowStyle(View view, boolean isCardRow, boolean top, boolean bottom, boolean clickable) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (params != null) {
            int inset = isCardRow ? getCardHorizontalInset() : 0;
            if (params.leftMargin != inset || params.rightMargin != inset) {
                params.leftMargin = inset;
                params.rightMargin = inset;
                view.setLayoutParams(params);
            }
        }
        if (!isCardRow) {
            return;
        }
        if (!clickable) {
            view.setBackground(null);
            return;
        }

        int radius = getCardRadius();
        int topLeft = top ? radius : 0;
        int topRight = top ? radius : 0;
        int bottomRight = bottom ? radius : 0;
        int bottomLeft = bottom ? radius : 0;
        int selectorColor = Theme.getColor(Theme.key_listSelector);

        view.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                topLeft,
                topRight,
                bottomRight,
                bottomLeft,
                0,
                selectorColor,
                selectorColor
        ));
    }

    public static RecyclerView.ItemDecoration createCardDecoration(CardRowProvider cardRowProvider) {
        return new RecyclerView.ItemDecoration() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF rect = new RectF();
            private final Path path = new Path();
            private final float[] radii = new float[8];

            @Override
            public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));

                for (int i = 0; i < parent.getChildCount(); i++) {
                    View child = parent.getChildAt(i);
                    int position = parent.getChildAdapterPosition(child);
                    if (!cardRowProvider.isCardRow(position)) {
                        continue;
                    }
                    boolean top = !cardRowProvider.isCardRow(position - 1);
                    boolean bottom = !cardRowProvider.isCardRow(position + 1);

                    rect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                    setRadii(top, bottom);
                    path.reset();
                    path.addRoundRect(rect, radii, Path.Direction.CW);
                    canvas.drawPath(path, paint);
                }
            }

            private void setRadii(boolean top, boolean bottom) {
                float topRadius = top ? getCardRadius() : 0f;
                float bottomRadius = bottom ? getCardRadius() : 0f;
                radii[0] = radii[1] = topRadius;
                radii[2] = radii[3] = topRadius;
                radii[4] = radii[5] = bottomRadius;
                radii[6] = radii[7] = bottomRadius;
            }
        };
    }
}
