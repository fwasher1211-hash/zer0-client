package dev.zer0.client.ui.core;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Premade backgrounds, rendered procedurally on a background thread into a texture (so they are smooth
 * gradients at any resolution and cost almost nothing per frame), plus a custom-image option.
 * Animated ones are re-rendered at a capped frame rate; static ones are rendered once per size.
 */
public final class Backgrounds {
    private Backgrounds() {}

    public record Def(String id, String name, boolean animated, boolean light) {}
    public static final List<Def> ALL = List.of(
            new Def("aurora", "Aurora", true, false),
            new Def("dark_gradient", "Dark Blur", false, false),
            new Def("black", "Minimal Black", false, false),
            new Def("frost", "Frosted White", false, true),
            new Def("purple_blue", "Purple / Blue", false, false),
            new Def("abstract", "Dark Abstract", true, false),
            new Def("animated", "Animated Gradient", true, false),
            new Def("landscape", "Minimal Landscape", false, false),
            new Def("custom", "Custom Image", false, false));

    public static Def def(String id) { return ALL.stream().filter(d -> d.id.equals(id)).findFirst().orElse(ALL.get(0)); }

    // ------------------------------------------------------------------ state
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ZER0-Backgrounds"); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY); return t;
    });
    private record Job(String key, String id, int w, int h, byte[] png) {}
    private static final ConcurrentLinkedQueue<Job> done = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean busy = new AtomicBoolean();

    private static Identifier curTex, prevTex;
    private static String curKey = "", prevKey = "";
    private static int curW, curH, prevW, prevH;
    private static double fadeStart, frameGap = 0.6;
    private static int slot;
    private static long lastRequest;
    private static String customError = "";

    private static final Map<String, Identifier> thumbs = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> thumbPending = new ConcurrentHashMap<>();

    public static String customError() { return customError; }

    /** Background currently shown (a pending preview overrides the saved choice). */
    public static String previewId;
    public static String activeId() { return previewId != null ? previewId : Zer0Settings.get().background; }

    // ------------------------------------------------------------------ drawing
    /** Fills the area with the active background. */
    public static void draw(DrawContext g, int x, int y, int w, int h, float alpha) {
        pump();
        Zer0Settings s = Zer0Settings.get();
        String id = activeId();
        Def d = def(id);
        double physW = w * Mc.guiScale(), physH = h * Mc.guiScale();
        // rendered at 1/(2*div) and bilinearly doubled on the worker thread
        float div = d.animated && s.animatedBackground ? (s.bgQuality == 0 ? 2.5f : s.bgQuality == 1 ? 1.8f : 1.3f)
                : (s.bgQuality == 0 ? 1.5f : s.bgQuality == 1 ? 1f : 0.75f);
        int gw = (int) Math.max(48, Math.min(1280, physW / div / 2)), gh = (int) Math.max(27, Math.min(800, physH / div / 2));
        boolean anim = d.animated && s.animatedBackground && Anim.enabled();
        long frameMs = 1000L / Math.max(5, s.bgFps);
        double t = anim ? Math.floor(Anim.seconds() * 1000 / frameMs) * frameMs / 1000.0 : 0;
        String key = id + "|" + gw + "x" + gh + "|" + t + "|" + s.bgSoftness + "|" + s.bgBrightness + "|" + s.accent
                + "|" + (id.equals("custom") ? s.customBgPath : "");
        if (!key.equals(curKey) && !busy.get() && System.currentTimeMillis() - lastRequest > (anim ? frameMs / 2 : 0)) request(key, id, gw, gh, t);

        int a8 = Math.round(255 * alpha);
        if (curTex == null) {                       // nothing yet: tasteful fallback gradient
            g.fillGradient(x, y, x + w, y + h, Col.withAlpha(0x14121F, a8), Col.withAlpha(0x07070B, a8));
            return;
        }
        boolean sameBgAnimating = prevKey.split("\\|")[0].equals(curKey.split("\\|")[0]) && d.animated;
        float f = sameBgAnimating ? Anim.clamp01((float) ((Anim.seconds() - fadeStart) / Math.max(0.05, frameGap)))   // blend frames = fluid motion
                : Anim.easeInOutCubic((float) ((Anim.seconds() - fadeStart) / 0.6));
        if (prevTex != null && f < 1f) Mc.drawImage(g, prevTex, x, y, w, h, prevW, prevH, Col.withAlpha(0xFFFFFF, a8));
        Mc.drawImage(g, curTex, x, y, w, h, curW, curH, Col.withAlpha(0xFFFFFF, Math.round(a8 * (prevTex == null ? 1 : f))));
    }

    /** Small preview tile (rendered once per background). */
    public static void drawThumb(DrawContext g, String id, int x, int y, int w, int h) {
        pump();
        Identifier t = thumbs.get(id);
        if (t == null) {
            if (thumbPending.putIfAbsent(id, true) == null) {
                WORKER.submit(() -> {
                    try {
                        BufferedImage img = round(render(id, 240, 135, 0), 16);
                        done.add(new Job("thumb", id, 240, 135, png(img)));
                    } catch (Throwable e) { thumbPending.remove(id); }
                });
            }
            g.fill(x, y, x + w, y + h, 0x40000000);
            return;
        }
        Mc.drawImage(g, t, x, y, w, h, 240, 135, 0xFFFFFFFF);
    }
    public static void invalidateThumb(String id) {
        Identifier t = thumbs.remove(id);
        if (t != null) Mc.destroyTexture(t);
        thumbPending.remove(id);
    }

    private static void request(String key, String id, int w, int h, double t) {
        busy.set(true);
        lastRequest = System.currentTimeMillis();
        WORKER.submit(() -> {
            try {
                BufferedImage img = up2(render(id, w, h, t));
                done.add(new Job(key, id, img.getWidth(), img.getHeight(), png(img)));
            }
            catch (Throwable e) { System.err.println("[ZER0] background render failed: " + e); done.add(new Job(key, id, 0, 0, null)); }
        });
    }

    /** Uploads finished renders (render thread). */
    private static void pump() {
        Job j;
        while ((j = done.poll()) != null) {
            if (j.key.equals("thumb")) {
                if (j.png != null) { Identifier id = Mc.uploadPng("bg/thumb_" + j.id, j.png); if (id != null) thumbs.put(j.id, id); }
                continue;
            }
            busy.set(false);
            if (j.png == null) { curKey = j.key; continue; }
            boolean newBg = !j.key.split("\\|")[0].equals(curKey.split("\\|")[0]) || curTex == null;
            Identifier id = Mc.uploadPng("bg/live_" + (slot++ % 3), j.png);
            if (id == null) continue;
            if (prevTex != null && !prevTex.equals(id) && !prevTex.equals(curTex)) Mc.destroyTexture(prevTex);
            prevTex = curTex; prevKey = curKey; prevW = curW; prevH = curH;
            curTex = id; curKey = j.key; curW = j.w; curH = j.h;
            double now = Anim.seconds();
            frameGap = newBg ? 0.6 : Math.min(0.5, Math.max(0.03, now - fadeStart));
            fadeStart = now;
        }
    }

    private static byte[] png(BufferedImage img) throws Exception {
        int w = img.getWidth(), h = img.getHeight();
        return Png.encode(img.getRGB(0, 0, w, h, null, 0, w), w, h);
    }

    /** Smooth 2x bilinear upscale so the texture never looks blocky when the GPU stretches it. */
    private static BufferedImage up2(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth() * 2, src.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    // ================================================================== generators
    private static BufferedImage render(String id, int W, int H, double t) {
        Zer0Settings s = Zer0Settings.get();
        float[] px = new float[W * H * 3];
        float soft = 0.55f + 0.9f * s.bgSoftness;
        switch (id) {
            case "black" -> { linear(px, W, H, 0x09090C, 0x050507, 90); blob(px, W, H, .5f, .45f, .9f * soft, 0x15151D, .9f, 2f); }
            case "frost" -> {
                linear(px, W, H, 0xF6F7FB, 0xE4E8F1, 120);
                blob(px, W, H, .18f, .22f, .45f * soft, 0xFFD6E8, .55f, 2f);
                blob(px, W, H, .86f, .3f, .5f * soft, 0xCFE0FF, .6f, 2f);
                blob(px, W, H, .55f, .95f, .55f * soft, 0xE3D9FF, .55f, 2f);
            }
            case "purple_blue" -> {
                linear(px, W, H, 0x5B2BE0, 0x1452D8, 135);
                blob(px, W, H, .2f, .15f, .55f * soft, 0xB27BFF, .55f, 2f);
                blob(px, W, H, .9f, .9f, .6f * soft, 0x0E2A7A, .7f, 2f);
            }
            case "abstract" -> {
                linear(px, W, H, 0x0D0C15, 0x07070B, 160);
                float k = (float) t * 0.05f;
                blob(px, W, H, .22f + .04f * (float) Math.sin(k * 3), .3f, .38f * soft, 0xD6336C, .5f, 2.2f);
                blob(px, W, H, .78f, .26f + .05f * (float) Math.cos(k * 2), .34f * soft, 0x22B8CF, .45f, 2.2f);
                blob(px, W, H, .6f + .05f * (float) Math.cos(k * 4), .82f, .42f * soft, 0xF08C00, .32f, 2.2f);
                band(px, W, H, -0.35f, 0.62f, .1f * soft, 0xFFFFFF, .05f);
            }
            case "animated" -> {
                linear(px, W, H, 0x0B0B14, 0x07070B, 90);
                float k = (float) t * 0.12f;
                int c1 = rgb(Col.hsv(k * .05f + .70f, .62f, .95f)), c2 = rgb(Col.hsv(k * .05f + .92f, .55f, .9f)), c3 = rgb(Col.hsv(k * .05f + .52f, .6f, .9f));
                blob(px, W, H, .5f + .3f * (float) Math.cos(k), .45f + .25f * (float) Math.sin(k * 1.3), .6f * soft, c1, .6f, 2f);
                blob(px, W, H, .5f + .32f * (float) Math.cos(k * .8 + 2), .5f + .28f * (float) Math.sin(k + 1), .55f * soft, c2, .55f, 2f);
                blob(px, W, H, .5f + .3f * (float) Math.cos(k * 1.2 + 4), .5f + .3f * (float) Math.sin(k * .7 + 3), .5f * soft, c3, .5f, 2f);
            }
            case "landscape" -> landscape(px, W, H, soft);
            case "custom" -> { BufferedImage img = custom(s.customBgPath, W, H); if (img != null) return finish(img, s); linear(px, W, H, 0x14121F, 0x07070B, 90); }
            case "aurora" -> aurora(px, W, H, t, soft);
            default -> {  // dark_gradient
                linear(px, W, H, 0x15132A, 0x07070C, 140);
                blob(px, W, H, .25f, .3f, .55f * soft, s.accent, .38f, 2f);
                blob(px, W, H, .82f, .78f, .6f * soft, 0x1D4ED8, .3f, 2f);
            }
        }
        return toImage(px, W, H, s.bgBrightness);
    }

    private static int rgb(int argb) { return argb & 0xFFFFFF; }

    private static void linear(float[] px, int W, int H, int c1, int c2, float angleDeg) {
        double a = Math.toRadians(angleDeg), dx = Math.cos(a), dy = Math.sin(a);
        double min = Math.min(0, dx) + Math.min(0, dy), max = Math.max(0, dx) + Math.max(0, dy);
        float r1 = Col.r(c1), g1 = Col.g(c1), b1 = Col.b(c1), r2 = Col.r(c2), g2 = Col.g(c2), b2 = Col.b(c2);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            float f = (float) (((double) x / W * dx + (double) y / H * dy - min) / (max - min));
            f = f * f * (3 - 2 * f);
            int i = (y * W + x) * 3;
            px[i] = r1 + (r2 - r1) * f; px[i + 1] = g1 + (g2 - g1) * f; px[i + 2] = b1 + (b2 - b1) * f;
        }
    }

    /** Soft radial light blended "screen"-style. cx,cy in 0..1, radius relative to width. */
    private static void blob(float[] px, int W, int H, float cx, float cy, float radius, int color, float strength, float falloff) {
        float R = radius * W, r = Col.r(color), gg = Col.g(color), b = Col.b(color);
        int x0 = Math.max(0, (int) (cx * W - R)), x1 = Math.min(W, (int) (cx * W + R) + 1);
        int y0 = Math.max(0, (int) (cy * H - R)), y1 = Math.min(H, (int) (cy * H + R) + 1);
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            float dx = x - cx * W, dy = y - cy * H;
            float d = (float) Math.sqrt(dx * dx + dy * dy) / R;
            if (d >= 1) continue;
            float k = (float) Math.pow(1 - d * d, falloff) * strength;
            int i = (y * W + x) * 3;
            px[i] += (r - px[i]) * k; px[i + 1] += (gg - px[i + 1]) * k; px[i + 2] += (b - px[i + 2]) * k;
        }
    }

    private static void band(float[] px, int W, int H, float slope, float offset, float width, int color, float strength) {
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            float fx = (float) x / W, fy = (float) y / H;
            float d = Math.abs(fy - (offset + slope * fx)) / width;
            if (d >= 1) continue;
            float k = (1 - d * d) * (1 - d * d) * strength;
            int i = (y * W + x) * 3;
            px[i] += (Col.r(color) - px[i]) * k; px[i + 1] += (Col.g(color) - px[i + 1]) * k; px[i + 2] += (Col.b(color) - px[i + 2]) * k;
        }
    }

    private static void aurora(float[] px, int W, int H, double t, float soft) {
        linear(px, W, H, 0x050A17, 0x0B1730, 90);
        float tt = (float) t;
        int[] cols = {0x3BF5B0, 0x2DD4BF, 0x8B5CF6};
        for (int x = 0; x < W; x++) {
            float fx = (float) x / W;
            for (int layer = 0; layer < 3; layer++) {
                float center = .30f + layer * .07f
                        + .07f * (float) Math.sin(fx * (5.0 + layer) + tt * (0.25 + 0.08 * layer) + layer * 1.7)
                        + .03f * (float) Math.sin(fx * 13.0 - tt * 0.4 + layer);
                float thick = (.07f + .03f * (float) Math.sin(fx * 7 + tt * .3 + layer)) * soft;
                float streak = .85f + .15f * (float) Math.sin(fx * 34 + layer * 3 + tt * 0.6);
                int c = layer == 2 ? cols[2] : Col.mix(0xFF000000 | cols[0], 0xFF000000 | cols[1], fx) & 0xFFFFFF;
                float strength = (layer == 2 ? .35f : .55f) * streak;
                for (int y = 0; y < H; y++) {
                    float fy = (float) y / H, d = (fy - center) / thick;
                    float k = d < 0 ? (float) Math.exp(-d * d * 4) : (float) Math.exp(-d * d * 0.7);   // long glow downward
                    k *= strength; if (k < 0.004f) continue;
                    int i = (y * W + x) * 3;
                    px[i] = Math.min(255, px[i] + Col.r(c) * k); px[i + 1] = Math.min(255, px[i + 1] + Col.g(c) * k); px[i + 2] = Math.min(255, px[i + 2] + Col.b(c) * k);
                }
            }
        }
        blob(px, W, H, .5f, 1.15f, .9f, 0x02040A, .7f, 1.5f);
    }

    private static void landscape(float[] px, int W, int H, float soft) {
        linear(px, W, H, 0x2A2F66, 0xF4A27E, 90);                 // dusk sky
        blob(px, W, H, .68f, .52f, .3f * soft, 0xFFC9A0, .42f, 2f); // sun glow
        // sun disc
        float sx = .68f * W, sy = .52f * H, sr = .045f * W;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            float d = (float) Math.hypot(x - sx, y - sy) - sr;
            if (d < 1.5f) { float k = Math.min(1, (1.5f - d) / 1.5f); int i = (y * W + x) * 3;
                px[i] += (255 - px[i]) * k; px[i + 1] += (236 - px[i + 1]) * k; px[i + 2] += (214 - px[i + 2]) * k; }
        }
        int[] layerCol = {0x8C6E9E, 0x5B4B7E, 0x3A305E, 0x1E1A38};
        float[] base = {.60f, .67f, .75f, .84f};
        Random rnd = new Random(7);
        for (int L = 0; L < 4; L++) {
            double p1 = rnd.nextDouble() * 6, p2 = rnd.nextDouble() * 6, p3 = rnd.nextDouble() * 6;
            for (int x = 0; x < W; x++) {
                double fx = (double) x / W;
                double h = base[L] - .07 * Math.sin(fx * (2.2 + L) + p1) - .035 * Math.sin(fx * (6.3 + L * 2) + p2) - .012 * Math.sin(fx * 21 + p3);
                double top = h * H;
                for (int y = Math.max(0, (int) top); y < H; y++) {
                    float k = (float) Math.min(1, y + 1 - top);   // anti-aliased ridge line
                    int i = (y * W + x) * 3, c = layerCol[L];
                    float haze = Math.max(0, 1 - (float) (y - top) / (H * .35f)) * .25f * (3 - L) / 3f;
                    float r = Col.r(c) + (240 - Col.r(c)) * haze, g = Col.g(c) + (170 - Col.g(c)) * haze, b = Col.b(c) + (150 - Col.b(c)) * haze;
                    px[i] += (r - px[i]) * k; px[i + 1] += (g - px[i + 1]) * k; px[i + 2] += (b - px[i + 2]) * k;
                }
            }
        }
    }

    private static BufferedImage custom(String path, int W, int H) {
        customError = "";
        if (path == null || path.isBlank()) { customError = "No image chosen yet."; return null; }
        try {
            File f = new File(path.trim().replace("\"", ""));
            if (!f.isFile()) { customError = "File not found."; return null; }
            BufferedImage src = ImageIO.read(f);
            if (src == null) { customError = "Not a PNG/JPG/GIF/BMP image."; return null; }
            double sc = Math.max((double) W / src.getWidth(), (double) H / src.getHeight());
            int dw = (int) Math.ceil(src.getWidth() * sc), dh = (int) Math.ceil(src.getHeight() * sc);
            BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, (W - dw) / 2, (H - dh) / 2, dw, dh, null);
            g.dispose();
            return out;
        } catch (Exception e) { customError = "Could not read image: " + e.getMessage(); return null; }
    }

    private static BufferedImage finish(BufferedImage img, Zer0Settings s) {
        int W = img.getWidth(), H = img.getHeight();
        float[] px = new float[W * H * 3];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int c = img.getRGB(x, y), i = (y * W + x) * 3;
            px[i] = Col.r(c); px[i + 1] = Col.g(c); px[i + 2] = Col.b(c);
        }
        return toImage(px, W, H, s.bgBrightness);
    }

    /** Brightness + light dithering (prevents gradient banding). */
    private static BufferedImage toImage(float[] px, int W, int H, float bright) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        int[] out = new int[W * H];
        Random rnd = new Random(1234);
        for (int p = 0; p < W * H; p++) {
            int i = p * 3;
            float n = (rnd.nextFloat() - .5f) * 1.6f;
            int r = clamp(px[i] * bright + n), g = clamp(px[i + 1] * bright + n), b = clamp(px[i + 2] * bright + n);
            out[p] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        img.setRGB(0, 0, W, H, out, 0, W);
        return img;
    }
    private static int clamp(float v) { return v < 0 ? 0 : v > 255 ? 255 : (int) v; }

    /** Anti-aliased rounded corners baked into a thumbnail (so tiles have no sharp corners). */
    private static BufferedImage round(BufferedImage img, float r) {
        int W = img.getWidth(), H = img.getHeight();
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            float cx = Math.max(r, Math.min(W - r, x + 0.5f)), cy = Math.max(r, Math.min(H - r, y + 0.5f));
            float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - cy) - r;
            if (d > -1f) {
                float a = Math.max(0, Math.min(1, 0.5f - d));
                int c = img.getRGB(x, y);
                img.setRGB(x, y, (Math.round((c >>> 24) * a) << 24) | (c & 0xFFFFFF));
            }
        }
        return img;
    }

    /** For the custom-image preview: re-render it next frame. */
    public static void refresh() { curKey = ""; }
}
