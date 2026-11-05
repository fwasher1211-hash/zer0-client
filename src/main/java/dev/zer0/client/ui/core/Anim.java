package dev.zer0.client.ui.core;

import dev.zer0.client.config.Zer0Settings;

/** Frame-rate independent animation helpers. All UI motion goes through here so "animations off" and speed apply everywhere. */
public final class Anim {
    private Anim() {}

    private static long lastFrame = System.nanoTime();
    private static float frameDt = 0.016f;
    private static long frameId;

    /** Called once per rendered frame by Zer0Screen / HUD. */
    public static void frame() {
        long now = System.nanoTime();
        frameDt = Math.min(0.1f, Math.max(0.0005f, (now - lastFrame) / 1e9f));
        lastFrame = now;
        frameId++;
    }
    public static float dt() { return frameDt; }
    public static long frameId() { return frameId; }
    public static double seconds() { return System.nanoTime() / 1e9; }

    public static boolean enabled() { return Zer0Settings.get().animations; }
    public static float speed() { return Zer0Settings.get().animSpeed * Layout.current().animScale; }

    /** Smoothly moves {@code cur} towards {@code target}; rate ~ how many "e-foldings" per second. */
    public static float approach(float cur, float target, float rate) {
        if (!enabled()) return target;
        float k = 1f - (float) Math.exp(-rate * speed() * frameDt);
        float v = cur + (target - cur) * k;
        return Math.abs(target - v) < 0.0005f ? target : v;
    }

    public static float clamp01(float t) { return t < 0 ? 0 : t > 1 ? 1 : t; }
    public static float easeOutCubic(float t) { t = clamp01(t); float u = 1 - t; return 1 - u * u * u; }
    public static float easeInOutCubic(float t) { t = clamp01(t); return t < .5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }
    public static float easeOutBack(float t) { t = clamp01(t); float c1 = 1.70158f, c3 = c1 + 1; return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** A value that eases toward a target each frame. */
    public static final class Smooth {
        public float v;
        private final float rate;
        public Smooth(float start, float rate) { this.v = start; this.rate = rate; }
        public float to(float target) { v = approach(v, target, rate); return v; }
        public void snap(float target) { v = target; }
    }

    /** A one-shot 0..1 timeline (page transitions, entrances). */
    public static final class Timeline {
        private double start = seconds();
        private final float duration;
        public Timeline(float durationSec) { this.duration = durationSec; }
        public void restart() { start = seconds(); }
        public float t() {
            if (!enabled()) return 1f;
            return clamp01((float) ((seconds() - start) * speed() / duration));
        }
        public float delayed(float delaySec) {
            if (!enabled()) return 1f;
            return clamp01((float) (((seconds() - start) * speed() - delaySec) / duration));
        }
    }
}
