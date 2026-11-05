package dev.zer0.client.spotify;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.ui.core.Anim;
import dev.zer0.client.ui.core.Png;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Album artwork. The cover is downloaded once, then resampled (high-quality, off-thread) to the exact
 * pixel size each player needs, with anti-aliased rounded corners baked in - so artwork is never
 * stretched or shrunk by the GPU's nearest-neighbour sampling and always looks sharp.
 * The previous cover is kept for a cross-fade; older textures are freed (no leaks).
 */
public final class SpotifyArt {
    private SpotifyArt() {}

    public record Tex(Identifier id, int px) {}

    private static volatile String wanted;
    private static BufferedImage src, prevSrc;
    private static Map<Integer, Tex> cur = new HashMap<>(), prev = new HashMap<>();
    private static final Set<String> pending = new HashSet<>();
    private static double changedAt = -100;
    private static int serial;

    public static boolean hasArt() { return src != null; }
    public static boolean hasPrevious() { return prevSrc != null; }
    public static float fade() { return Anim.easeInOutCubic((float) ((Anim.seconds() - changedAt) / 0.45)); }

    /** Texture of the current (or previous) cover for a given on-screen pixel size. Render thread only. */
    public static Tex get(boolean previous, int px) {
        px = Math.max(16, Math.min(1024, px));
        Map<Integer, Tex> map = previous ? prev : cur;
        Tex t = map.get(px);
        if (t != null) return t;
        if (!previous) schedule(px);
        Tex best = null;   // meanwhile: closest size we already have
        for (Tex o : map.values()) if (best == null || Math.abs(o.px - px) < Math.abs(best.px - px)) best = o;
        return best;
    }

    private static void schedule(int px) {
        BufferedImage s = src;
        if (s == null) return;
        String key = System.identityHashCode(s) + ":" + px;
        if (!pending.add(key)) return;
        SpotifyClient.get().executor().execute(() -> {
            try {
                byte[] png = resample(s, px);
                Mc.mc().execute(() -> {
                    pending.remove(key);
                    if (s != src) return;
                    Identifier id = Mc.uploadPng("spotify/art_" + (serial++), png);
                    if (id != null) cur.put(px, new Tex(id, px));
                });
            } catch (Exception e) { pending.remove(key); }
        });
    }

    static void request(String url) {
        if (url == null || url.isEmpty() || url.equals(wanted)) return;
        wanted = url;
        SpotifyClient.get().executor().execute(() -> {
            try {
                HttpResponse<byte[]> r = SpotifyClient.get().http().send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (r.statusCode() / 100 != 2) return;
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(r.body()));
                if (img == null) return;
                Mc.mc().execute(() -> {
                    if (!url.equals(wanted)) return;
                    for (Tex t : prev.values()) Mc.destroyTexture(t.id);
                    prev = cur; cur = new HashMap<>();
                    prevSrc = src; src = img;
                    changedAt = Anim.seconds();
                });
            } catch (Exception e) { System.err.println("[ZER0] album art failed: " + e); }
        });
    }

    /** Multi-step bicubic downscale + rounded-corner alpha, as PNG bytes. */
    static byte[] resample(BufferedImage in, int px) throws Exception {
        BufferedImage cur = in;
        int w = in.getWidth();
        while (w / 2 >= px) { w /= 2; cur = scale(cur, w); }   // halving steps keep it smooth
        BufferedImage out = scale(cur, px);
        float r = px * 0.11f;
        for (int y = 0; y < px; y++) for (int x = 0; x < px; x++) {
            float cx = Math.max(r, Math.min(px - r, x + 0.5f)), cy = Math.max(r, Math.min(px - r, y + 0.5f));
            float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - cy) - r;
            if (d > -1f) {
                float a = Math.max(0, Math.min(1, 0.5f - d));
                int c = out.getRGB(x, y);
                out.setRGB(x, y, (Math.round(255 * a) << 24) | (c & 0xFFFFFF));
            }
        }
        return Png.encode(out.getRGB(0, 0, px, px, null, 0, px), px, px);
    }
    private static BufferedImage scale(BufferedImage in, int px) {
        BufferedImage out = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int s = Math.min(in.getWidth(), in.getHeight());   // centre-crop to square
        g.drawImage(in, 0, 0, px, px, (in.getWidth() - s) / 2, (in.getHeight() - s) / 2, (in.getWidth() + s) / 2, (in.getHeight() + s) / 2, null);
        g.dispose();
        return out;
    }

    public static void clear() {
        Mc.mc().execute(() -> {
            for (Tex t : cur.values()) Mc.destroyTexture(t.id);
            for (Tex t : prev.values()) Mc.destroyTexture(t.id);
            cur = new HashMap<>(); prev = new HashMap<>(); src = prevSrc = null; wanted = null;
        });
    }
}
