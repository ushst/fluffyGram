/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.ushastoe.fluffy.fluffyConfig;

import java.util.ArrayList;

public class SnowflakesEffect {
    private final BatchParticlesDrawHelper.BatchParticlesBuffer batchParticlesBuffer;
    private final Paint batchParticlesPaint;

    private final Paint particlePaint;
    private final Paint particleThinPaint;
    private final Paint bitmapPaint = new Paint();
    private int colorKey = Theme.key_actionBarDefaultTitle;
    private int forcedColor;
    private final int viewType;
    private final int maxCount;

    private Bitmap[][] effectBitmaps = new Bitmap[fluffyConfig.SNOW_EFFECT_STYLE_MAX + 1][];
    private int lastColorMode = -1;
    private static final char[] PIXEL_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final String[] CODE_WORDS = new String[]{
            "while(true)",
            "sudo rm -rf /",
            "SELECT *",
            "0xDEADBEEF",
            "printf(\"%s\")",
            "lambda => {}",
            "curl -fsSL",
            "git push --force",
            "segfault",
            "shellcode",
            "openssl rand",
            "chmod +x",
            "npm install",
            "public static",
            "async await",
            "DROP TABLE",
            "#!/bin/bash",
            "StackOverflow",
            "firewall-cmd",
            "debugger;"
    };

    private long lastAnimationTime;

    private class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float velocity;
        float alpha;
        float lifeTime;
        float currentTime;
        float scale;
        int type;
        int effectStyle;
        int effectVariant;
        int color;

        public void draw(Canvas canvas) {
            switch (type) {
                case 0: {
                    particlePaint.setColor(color);
                    particlePaint.setAlpha((int) (255 * alpha));
                    canvas.drawPoint(x, y, particlePaint);
                    break;
                }
                case 1:
                default: {
                    Bitmap shapeBitmap = getEffectBitmap(effectStyle, effectVariant);
                    if (shapeBitmap == null) {
                        return;
                    }
                    bitmapPaint.setAlpha((int) (255 * alpha));
                    canvas.save();
                    canvas.scale(scale, scale, x, y);
                    canvas.drawBitmap(shapeBitmap, x, y, bitmapPaint);
                    canvas.restore();
                    break;
                }
            }

        }
    }

    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<Particle> freeParticles = new ArrayList<>();

    private int color;

    public SnowflakesEffect(int viewType) {
        this.viewType = viewType;
        this.maxCount = viewType == 0 ? 100 : 300;
        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStrokeWidth(dp(1.5f));
        particlePaint.setStrokeCap(Paint.Cap.ROUND);
        particlePaint.setStyle(Paint.Style.STROKE);

        particleThinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleThinPaint.setStrokeWidth(dp(0.5f));
        particleThinPaint.setStrokeCap(Paint.Cap.ROUND);
        particleThinPaint.setStyle(Paint.Style.STROKE);

        if (BatchParticlesDrawHelper.isAvailable()) {
            batchParticlesBuffer = new BatchParticlesDrawHelper.BatchParticlesBuffer(maxCount);
            batchParticlesPaint = BatchParticlesDrawHelper.createBatchParticlesPaint(createParticlesBitmap(true));
        } else {
            batchParticlesBuffer = null;
            batchParticlesPaint = null;
        }

        updateColors();

        for (int a = 0; a < 20; a++) {
            freeParticles.add(new Particle());
        }
    }

    public void setForcedColor(int forcedColor) {
        this.forcedColor = forcedColor;
        updateColors();
    }

    public void setColorKey(int key) {
        colorKey = key;
        updateColors();
    }

    public void updateColors() {
        final int color = forcedColor != 0 ? forcedColor : Theme.getColor(colorKey) & 0xffe6e6e6;
        if (this.color != color) {
            this.color = color;
            particlePaint.setColor(color);
            particleThinPaint.setColor(color);
            for (int i = 0; i < effectBitmaps.length; i++) {
                effectBitmaps[i] = null;
            }
        }
    }

    private void checkColorModeChanged() {
        int currentMode = fluffyConfig.snowEffectColorMode;
        if (lastColorMode != currentMode) {
            lastColorMode = currentMode;
            for (int i = 0; i < effectBitmaps.length; i++) {
                effectBitmaps[i] = null;
            }
        }
    }

    private Bitmap getEffectBitmap(int style, int variant) {
        checkColorModeChanged();
        int maxIndex = effectBitmaps.length - 1;
        int clampedStyle = Math.max(fluffyConfig.SNOW_EFFECT_STYLE_SNOWFLAKE,
                Math.min(style, maxIndex));
        int variantCount = getVariantCountForStyle(clampedStyle);
        int clampedVariant = Math.max(0, Math.min(variant, Math.max(variantCount - 1, 0)));
        Bitmap[] variants = effectBitmaps[clampedStyle];
        if (variants == null || variants.length != variantCount) {
            variants = new Bitmap[variantCount];
            effectBitmaps[clampedStyle] = variants;
        }
        Bitmap bitmap = variants[clampedVariant];
        if (bitmap == null) {
            bitmap = variants[clampedVariant] = createBitmapForStyle(clampedStyle, clampedVariant);
        }
        return bitmap;
    }

    private int getBaseVariantCount(int style) {
        if (style == fluffyConfig.SNOW_EFFECT_STYLE_PIXELS) {
            return PIXEL_CHARACTERS.length;
        } else if (style == fluffyConfig.SNOW_EFFECT_STYLE_CODEWORDS) {
            return CODE_WORDS.length;
        }
        return 1;
    }

    private int getVariantCountForStyle(int style) {
        int base = getBaseVariantCount(style);
        if (base == 1 && fluffyConfig.snowEffectColorMode == fluffyConfig.SNOW_EFFECT_COLOR_MODE_RAINBOW) {
            return 6;
        }
        return base;
    }

    private float getSpeedMultiplier() {
        switch (fluffyConfig.snowEffectSpeedMode) {
            case fluffyConfig.SNOW_EFFECT_SPEED_SLOW:
                return 0.7f;
            case fluffyConfig.SNOW_EFFECT_SPEED_FAST:
                return 1.4f;
            case fluffyConfig.SNOW_EFFECT_SPEED_NORMAL:
            default:
                return 1.0f;
        }
    }

    private Bitmap createBitmapForStyle(int style, int variant) {
        switch (style) {
            case fluffyConfig.SNOW_EFFECT_STYLE_STAR:
                return createStarBitmap(style, variant);
            case fluffyConfig.SNOW_EFFECT_STYLE_BUBBLE:
                return createBubbleBitmap(style, variant);
            case fluffyConfig.SNOW_EFFECT_STYLE_CRYSTAL:
                return createCrystalBitmap(style, variant);
            case fluffyConfig.SNOW_EFFECT_STYLE_HEART:
                return createHeartBitmap(style, variant);
            case fluffyConfig.SNOW_EFFECT_STYLE_DROP:
                return createDropBitmap(style, variant);
            case fluffyConfig.SNOW_EFFECT_STYLE_CONFETTI:
                return createConfettiBitmap(style, variant);
            case fluffyConfig.SNOW_EFFECT_STYLE_PIXELS:
                return createPixelLetterBitmap(style, variant);
            case fluffyConfig.SNOW_EFFECT_STYLE_CODEWORDS:
                return createCodeWordBitmap(style, variant);
            case fluffyConfig.SNOW_EFFECT_STYLE_SNOWFLAKE:
            default:
                return createSnowflakeBitmap(style, variant);
        }
    }

    private Bitmap createSnowflakeBitmap(int style, int variant) {
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStrokeWidth(AndroidUtilities.dpf2(0.5f));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(getColorForMode(style, variant));
        Bitmap bitmap = Bitmap.createBitmap(AndroidUtilities.dp(16), AndroidUtilities.dp(16), Bitmap.Config.ARGB_8888);
        Canvas bitmapCanvas = new Canvas(bitmap);
        float px = AndroidUtilities.dpf2(2.0f) * 2;
        float px1 = -AndroidUtilities.dpf2(0.57f) * 2;
        float py1 = AndroidUtilities.dpf2(1.55f) * 2;
        float angle = (float) -Math.PI / 2;
        float angleDiff = (float) (Math.PI / 180 * 60);
        for (int a = 0; a < 6; a++) {
            float x = AndroidUtilities.dp(8);
            float y = AndroidUtilities.dp(8);
            float x1 = (float) Math.cos(angle) * px;
            float y1 = (float) Math.sin(angle) * px;
            float cx = x1 * 0.66f;
            float cy = y1 * 0.66f;
            bitmapCanvas.drawLine(x, y, x + x1, y + y1, strokePaint);

            float angle2 = (float) (angle - Math.PI / 2);
            x1 = (float) (Math.cos(angle2) * px1 - Math.sin(angle2) * py1);
            y1 = (float) (Math.sin(angle2) * px1 + Math.cos(angle2) * py1);
            bitmapCanvas.drawLine(x + cx, y + cy, x + x1, y + y1, strokePaint);

            x1 = (float) (-Math.cos(angle2) * px1 - Math.sin(angle2) * py1);
            y1 = (float) (-Math.sin(angle2) * px1 + Math.cos(angle2) * py1);
            bitmapCanvas.drawLine(x + cx, y + cy, x + x1, y + y1, strokePaint);

            angle += angleDiff;
        }
        return bitmap;
    }

    private Paint createFillPaint(int alpha, int style, int variant) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        int baseColor = getColorForMode(style, variant);
        paint.setColor((baseColor & 0x00ffffff) | ((alpha & 0xff) << 24));
        return paint;
    }

    private Bitmap createStarBitmap(int style, int variant) {
        int size = AndroidUtilities.dp(16);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float cx = size / 2f;
        float cy = size / 2f;
        float outerRadius = AndroidUtilities.dpf2(5.2f);
        float innerRadius = AndroidUtilities.dpf2(2.2f);
        Path starPath = new Path();
        for (int i = 0; i < 10; i++) {
            double angleRad = Math.PI / 2 + i * Math.PI / 5;
            float radius = (i % 2 == 0) ? outerRadius : innerRadius;
            float px = (float) (cx + Math.cos(angleRad) * radius);
            float py = (float) (cy - Math.sin(angleRad) * radius);
            if (i == 0) {
                starPath.moveTo(px, py);
            } else {
                starPath.lineTo(px, py);
            }
        }
        starPath.close();
        canvas.drawPath(starPath, createFillPaint(230, style, variant));
        return bitmap;
    }

    private Bitmap createBubbleBitmap(int style, int variant) {
        int size = AndroidUtilities.dp(16);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float cx = size / 2f;
        float cy = size / 2f;
        float radius = AndroidUtilities.dpf2(4.6f);
        canvas.drawCircle(cx, cy, radius, createFillPaint(150, style, variant));
        return bitmap;
    }

    private Bitmap createCrystalBitmap(int style, int variant) {
        int size = AndroidUtilities.dp(16);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float cx = size / 2f;
        float cy = size / 2f;
        float radius = AndroidUtilities.dpf2(5.3f);
        Path path = new Path();
        path.moveTo(cx, cy - radius);
        path.lineTo(cx + radius * 0.85f, cy);
        path.lineTo(cx, cy + radius);
        path.lineTo(cx - radius * 0.85f, cy);
        path.close();
        canvas.drawPath(path, createFillPaint(210, style, variant));
        return bitmap;
    }

    private Bitmap createHeartBitmap(int style, int variant) {
        int size = AndroidUtilities.dp(16);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float cx = size / 2f;
        float cy = size / 2f;
        float radius = AndroidUtilities.dpf2(4.8f);
        Path path = new Path();
        path.moveTo(cx, cy + radius * 0.9f);
        path.cubicTo(cx - radius * 1.5f, cy - radius * 0.2f, cx - radius * 0.9f, cy - radius * 1.7f, cx, cy - radius * 0.9f);
        path.cubicTo(cx + radius * 0.9f, cy - radius * 1.7f, cx + radius * 1.5f, cy - radius * 0.2f, cx, cy + radius * 0.9f);
        path.close();
        canvas.drawPath(path, createFillPaint(220, style, variant));
        return bitmap;
    }

    private Bitmap createDropBitmap(int style, int variant) {
        int size = AndroidUtilities.dp(16);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float cx = size / 2f;
        float cy = size / 2f;
        float radius = AndroidUtilities.dpf2(5.2f);
        Path path = new Path();
        path.moveTo(cx, cy - radius);
        path.cubicTo(cx - radius, cy - radius * 0.2f, cx - radius * 0.7f, cy + radius * 0.6f, cx, cy + radius);
        path.cubicTo(cx + radius * 0.7f, cy + radius * 0.6f, cx + radius, cy - radius * 0.2f, cx, cy - radius);
        path.close();
        canvas.drawPath(path, createFillPaint(190, style, variant));
        return bitmap;
    }

    private Bitmap createConfettiBitmap(int style, int variant) {
        int size = AndroidUtilities.dp(16);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float rectHalfWidth = AndroidUtilities.dpf2(1.4f);
        float rectHalfHeight = AndroidUtilities.dpf2(5.0f);
        RectF rect = new RectF(-rectHalfWidth, -rectHalfHeight, rectHalfWidth, rectHalfHeight);
        Paint paint = createFillPaint(255, style, variant);
        canvas.save();
        canvas.translate(size / 2f, size / 2f);
        canvas.rotate(-25);
        canvas.drawRoundRect(rect, AndroidUtilities.dpf2(1.2f), AndroidUtilities.dpf2(1.2f), paint);
        canvas.restore();
        return bitmap;
    }

    private Bitmap createPixelLetterBitmap(int style, int variant) {
        int size = AndroidUtilities.dp(16);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(getColorForMode(style, variant));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(AndroidUtilities.dp(10));
        paint.setFakeBoldText(true);
        paint.setAntiAlias(false);
        paint.setSubpixelText(false);

        char letter = PIXEL_CHARACTERS[variant % PIXEL_CHARACTERS.length];
        Rect bounds = new Rect();
        String letterString = String.valueOf(letter);
        paint.getTextBounds(letterString, 0, 1, bounds);
        float cx = size / 2f;
        float cy = size / 2f - bounds.exactCenterY();
        canvas.drawText(letterString, cx, cy, paint);
        return bitmap;
    }

    private Bitmap createCodeWordBitmap(int style, int variant) {
        String word = CODE_WORDS[Math.abs(variant) % CODE_WORDS.length];
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(AndroidUtilities.dp(9.5f));
        paint.setStyle(Paint.Style.FILL);

        Rect bounds = new Rect();
        paint.getTextBounds(word, 0, word.length(), bounds);
        int width = Math.max(AndroidUtilities.dp(22), bounds.width() + AndroidUtilities.dp(8));
        int height = Math.max(AndroidUtilities.dp(12), bounds.height() + AndroidUtilities.dp(8));

        int colorMode = fluffyConfig.snowEffectColorMode;
        if (colorMode == fluffyConfig.SNOW_EFFECT_COLOR_MODE_RAINBOW) {
            int color1 = getRainbowColor(variant * 2);
            int color2 = getRainbowColor(variant * 2 + 1);
            Shader shader = new LinearGradient(0, 0, width, height, new int[]{color1, color2}, null, Shader.TileMode.CLAMP);
            paint.setShader(shader);
        } else {
            paint.setShader(null);
            int textColor = colorMode == fluffyConfig.SNOW_EFFECT_COLOR_MODE_MATRIX
                    ? getMatrixColor()
                    : getColorForMode(style, variant);
            paint.setColor(textColor);
            paint.setAlpha(230);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float x = (width - bounds.width()) / 2f - bounds.left;
        float y = (height - bounds.height()) / 2f - bounds.top;
        canvas.drawText(word, x, y, paint);
        return bitmap;
    }

    private int getRainbowColor(int index) {
        float hue = (index * 42.5f) % 360f;
        return Color.HSVToColor(255, new float[]{hue, 0.85f, 1f});
    }

    private int getMatrixColor() {
        return Color.argb(230, 90, 255, 130);
    }

    private int getColorForMode(int style, int variant) {
        switch (fluffyConfig.snowEffectColorMode) {
            case fluffyConfig.SNOW_EFFECT_COLOR_MODE_RAINBOW:
                return getRainbowColor(variant);
            case fluffyConfig.SNOW_EFFECT_COLOR_MODE_MATRIX:
                return getMatrixColor();
            case fluffyConfig.SNOW_EFFECT_COLOR_MODE_THEME:
            default:
                return color;
        }
    }

    private void updateParticles(long dt) {
        int count = particles.size();
        for (int a = 0; a < count; a++) {
            Particle particle = particles.get(a);
            if (particle.currentTime >= particle.lifeTime) {
                if (freeParticles.size() < 40) {
                    freeParticles.add(particle);
                }
                particles.remove(a);
                a--;
                count--;
                continue;
            }
            if (viewType == 0) {
                if (particle.currentTime < 200.0f) {
                    particle.alpha = AndroidUtilities.accelerateInterpolator.getInterpolation(particle.currentTime / 200.0f);
                } else {
                    particle.alpha = 1.0f - AndroidUtilities.decelerateInterpolator.getInterpolation((particle.currentTime - 200.0f) / (particle.lifeTime - 200.0f));
                }
            } else {
                if (particle.currentTime < 200.0f) {
                    particle.alpha = AndroidUtilities.accelerateInterpolator.getInterpolation(particle.currentTime / 200.0f);
                } else if (particle.lifeTime - particle.currentTime < 2000) {
                    particle.alpha = AndroidUtilities.decelerateInterpolator.getInterpolation((particle.lifeTime - particle.currentTime) / 2000);
                }
            }
            particle.x += particle.vx * particle.velocity * dt / 500.0f;
            particle.y += particle.vy * particle.velocity * dt / 500.0f;
            particle.currentTime += dt;
        }
    }

    public void onDraw(View parent, Canvas canvas) {
        if (parent == null || canvas == null || !LiteMode.isEnabled(LiteMode.FLAG_CHAT_BACKGROUND)) {
            return;
        }

        if (batchParticlesBuffer != null) {
            final int count = Math.min(maxCount, particles.size());
            final int texSize = dp(TEXTURE_SIZE_DP);

            for (int a = 0; a < count; a++) {
                Particle particle = particles.get(a);
                final float x = particle.x, y = particle.y;
                final float h = particle.type == 0 ? (texSize / 2f) : (texSize / 2f * particle.scale);
                final float tx = particle.type == 0 ? texSize : 0;

                batchParticlesBuffer.setParticleColor(a, ColorUtils.setAlphaComponent(color, (int) (255 * particle.alpha)));
                batchParticlesBuffer.setParticleVertexCords(a, x - h, y - h, x + h, y + h);
                batchParticlesBuffer.setParticleTextureCords(a, tx, 0, tx + texSize, texSize);
            }
            BatchParticlesDrawHelper.draw(canvas, batchParticlesBuffer, count, batchParticlesPaint);
        } else {
            final int count = particles.size();
            for (int a = 0; a < count; a++) {
                Particle particle = particles.get(a);
                particle.draw(canvas);
            }
        }

        int createPerFrame = viewType == 0 ? 1 : 10;
        if (particles.size() < maxCount) {
            for (int i = 0; i < createPerFrame; i++) {
                if (particles.size() < maxCount && Utilities.random.nextFloat() > 0.7f) {
                    int statusBarHeight = AndroidUtilities.statusBarHeight;
                    float cx = Utilities.random.nextFloat() * parent.getMeasuredWidth();
                    float cy;
                    if (viewType == 0) {
                        cy = statusBarHeight + Utilities.random.nextFloat() * (parent.getMeasuredHeight() - dp(20) - statusBarHeight);
                    } else {
                        cy = Utilities.random.nextFloat() * (parent.getMeasuredHeight());
                    }

                    int angle = Utilities.random.nextInt(40) - 20 + 90;
                    float vx = (float) Math.cos(Math.PI / 180.0 * angle);
                    float vy = (float) Math.sin(Math.PI / 180.0 * angle);

                    Particle newParticle;
                    if (!freeParticles.isEmpty()) {
                        newParticle = freeParticles.get(0);
                        freeParticles.remove(0);
                    } else {
                        newParticle = new Particle();
                    }
                    newParticle.x = cx;
                    newParticle.y = cy;

                    newParticle.vx = vx;
                    newParticle.vy = vy;

                    newParticle.alpha = 0.0f;
                    newParticle.currentTime = 0;

                    newParticle.scale = Utilities.random.nextFloat() * 1.2f;
                    newParticle.type = Utilities.random.nextInt(2);
                    int activeStyle = Math.max(fluffyConfig.SNOW_EFFECT_STYLE_SNOWFLAKE,
                            Math.min(fluffyConfig.snowEffectStyle, fluffyConfig.SNOW_EFFECT_STYLE_MAX));
                    newParticle.effectStyle = activeStyle;
                    int variantCount = getVariantCountForStyle(activeStyle);
                    newParticle.effectVariant = variantCount > 1 ? Utilities.random.nextInt(variantCount) : 0;
                    if (activeStyle == fluffyConfig.SNOW_EFFECT_STYLE_PIXELS || activeStyle == fluffyConfig.SNOW_EFFECT_STYLE_CODEWORDS) {
                        newParticle.type = 1;
                    }
                    newParticle.color = getColorForMode(activeStyle, newParticle.effectVariant);

                    if (viewType == 0) {
                        newParticle.lifeTime = 2000 + Utilities.random.nextInt(100);
                    } else {
                        newParticle.lifeTime = 3000 + Utilities.random.nextInt(2000);
                    }
                    float baseVelocity = 20.0f + Utilities.random.nextFloat() * 4.0f;
                    newParticle.velocity = baseVelocity * getSpeedMultiplier();
                    particles.add(newParticle);
                }
            }
        }

        long newTime = System.currentTimeMillis();
        long dt = Math.min(17, newTime - lastAnimationTime);
        updateParticles(dt);
        lastAnimationTime = newTime;
        parent.invalidate();
    }


    private static final int TEXTURE_SIZE_DP = 10;
    private static Bitmap createParticlesBitmap(boolean useFull) {
        final Paint particleThinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleThinPaint.setStrokeWidth(dp(0.5f));
        particleThinPaint.setStrokeCap(Paint.Cap.ROUND);
        particleThinPaint.setStyle(Paint.Style.STROKE);
        particleThinPaint.setColor(0xFFFFFFFF);

        final Bitmap particleBitmap = Bitmap.createBitmap(useFull ? dp(TEXTURE_SIZE_DP * 2) : dp(TEXTURE_SIZE_DP), dp(TEXTURE_SIZE_DP), Bitmap.Config.ARGB_8888);
        final Canvas bitmapCanvas = new Canvas(particleBitmap);
        final float px = dpf2(2.0f) * 2;
        final float px1 = -dpf2(0.57f) * 2;
        final float py1 = dpf2(1.55f) * 2;
        final float x = dp(TEXTURE_SIZE_DP / 2f);
        final float y = dp(TEXTURE_SIZE_DP / 2f);
        final float angleDiff = (float) (Math.PI / 180 * 60);

        float angle = (float) -Math.PI / 2;
        for (int a = 0; a < 6; a++) {
            float x1 = (float) Math.cos(angle) * px;
            float y1 = (float) Math.sin(angle) * px;
            float cx = x1 * 0.66f;
            float cy = y1 * 0.66f;
            bitmapCanvas.drawLine(x, y, x + x1, y + y1, particleThinPaint);

            float angle2 = (float) (angle - Math.PI / 2);
            x1 = (float) (Math.cos(angle2) * px1 - Math.sin(angle2) * py1);
            y1 = (float) (Math.sin(angle2) * px1 + Math.cos(angle2) * py1);
            bitmapCanvas.drawLine(x + cx, y + cy, x + x1, y + y1, particleThinPaint);

            x1 = (float) (-Math.cos(angle2) * px1 - Math.sin(angle2) * py1);
            y1 = (float) (-Math.sin(angle2) * px1 + Math.cos(angle2) * py1);
            bitmapCanvas.drawLine(x + cx, y + cy, x + x1, y + y1, particleThinPaint);

            angle += angleDiff;
        }

        if (useFull) {
            final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            particlePaint.setStrokeWidth(dp(1.5f));
            particlePaint.setStrokeCap(Paint.Cap.ROUND);
            particlePaint.setStyle(Paint.Style.STROKE);
            particlePaint.setColor(0xFFFFFFFF);
            bitmapCanvas.drawPoint(dp(TEXTURE_SIZE_DP * 1.5f), dp(TEXTURE_SIZE_DP / 2f), particlePaint);
        }

        return particleBitmap;
    }
}
