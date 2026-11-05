package dev.zer0.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings owned by ZER0 (look, layout, fonts, Spotify). The original mod's own settings stay in its
 * ModConfig (config/donutqueue.json) and are edited through {@code Zer0Bridge}.
 * Saved to config/zer0client/settings.json, debounced.
 */
public final class Zer0Settings {
    // appearance
    public boolean dark = true;
    public int accent = 0x8B7CFF;
    public float uiAlpha = 1.0f;          // glass opacity multiplier ("UI transparency")
    public boolean glassHud = true;       // redraw the streamer HUD boxes as rounded glass
    // background
    public String background = "aurora";
    public float bgSoftness = 0.65f;      // softness/blur of generated backgrounds
    public float bgBrightness = 1.0f;
    public float bgOpacity = 1.0f;        // how much of the background covers the world in in-game menus
    public String customBgPath = "";
    public boolean worldBehindMenus = true;
    // layout
    public String layout = "glass";
    public List<String> hiddenPages = new ArrayList<>();
    // fonts
    public String fontUi = "inter", fontHeading = "inter", fontButton = "inter", fontHud = "inter";
    public boolean boldUi = false, boldHeading = true, boldButton = false, boldHud = false;
    public float fontScale = 1.0f;
    // animation & performance
    public boolean animations = true;
    public float animSpeed = 1.0f;
    public boolean pageTransitions = true;
    public boolean animatedBackground = true;
    public int bgQuality = 1;             // 0 low, 1 balanced, 2 high
    public int bgFps = 20;
    // general
    public boolean customTitle = true;
    public boolean tooltips = true;
    public boolean confirmDangerous = true;
    public boolean uiSounds = true;
    public String playAddress = "donutsmp.net";
    public int lastHubPage = 0;
    // spotify
    public Spotify spotify = new Spotify();

    public static final class Spotify {
        public String clientId = "";
        public String refreshToken = "";
        public int port = 8888;
        public boolean hudEnabled = true;
        public boolean hideWhenIdle = true;
        public int x = 10, y = 10;
        public float scale = 1.0f;
        public float alpha = 0.9f;
        public String layout = "card";    // card | compact | cover
        public boolean showArt = true, showTitle = true, showArtist = true, showProgress = true,
                showControls = true, showLogo = true, showVolume = false;
        public boolean useThemeAccent = false;
        public int accent = 0x1DB954;
    }

    // ------------------------------------------------------------------ persistence
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Zer0Settings instance;
    private static volatile boolean dirty;
    private static volatile long lastTouch;

    public static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("zer0client");
        try { Files.createDirectories(p); } catch (IOException ignored) {}
        return p;
    }

    public static Zer0Settings get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static Zer0Settings load() {
        try {
            Path f = dir().resolve("settings.json");
            if (Files.exists(f)) {
                Zer0Settings s = GSON.fromJson(Files.readString(f), Zer0Settings.class);
                if (s != null) return s.fixup();
            }
        } catch (Exception e) { System.err.println("[ZER0] settings load failed, using defaults: " + e); }
        return new Zer0Settings();
    }

    private Zer0Settings fixup() {
        if (spotify == null) spotify = new Spotify();
        if (hiddenPages == null) hiddenPages = new ArrayList<>();
        uiAlpha = clamp(uiAlpha, 0.3f, 1f); fontScale = clamp(fontScale, 0.75f, 1.5f);
        bgOpacity = clamp(bgOpacity, 0f, 1f); bgBrightness = clamp(bgBrightness, 0.3f, 1.5f);
        bgSoftness = clamp(bgSoftness, 0f, 1f); animSpeed = clamp(animSpeed, 0.4f, 2.5f);
        spotify.scale = clamp(spotify.scale, 0.6f, 2f); spotify.alpha = clamp(spotify.alpha, 0.2f, 1f);
        bgFps = Math.max(5, Math.min(60, bgFps));
        return this;
    }
    private static float clamp(float v, float a, float b) { return Float.isNaN(v) ? a : Math.max(a, Math.min(b, v)); }

    /** Mark changed; written to disk ~0.6 s later from the client tick. */
    public static void touch() { dirty = true; lastTouch = System.currentTimeMillis(); }
    public static void tick() { if (dirty && System.currentTimeMillis() - lastTouch > 600) flush(); }
    public static synchronized void flush() {
        if (instance == null) return;
        dirty = false;
        try { Files.writeString(dir().resolve("settings.json"), GSON.toJson(instance)); }
        catch (IOException e) { System.err.println("[ZER0] settings save failed: " + e); }
    }

    // ------------------------------------------------------------------ profiles (Configurations page)
    private static Path profileDir() {
        Path d = dir().resolve("profiles");
        try { Files.createDirectories(d); } catch (IOException ignored) {}
        return d;
    }
    public static List<String> profiles() {
        List<String> out = new ArrayList<>();
        try (var st = Files.list(profileDir())) {
            st.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                String n = p.getFileName().toString(); out.add(n.substring(0, n.length() - 5));
            });
        } catch (IOException ignored) {}
        out.sort(String::compareToIgnoreCase);
        return out;
    }
    /** Saves the look (never the Spotify login) as a named profile. */
    public static boolean saveProfile(String name) {
        if (safe(name).isEmpty()) return false;
        try {
            Zer0Settings copy = GSON.fromJson(GSON.toJson(get()), Zer0Settings.class);
            copy.spotify.refreshToken = ""; copy.spotify.clientId = "";
            Files.writeString(profileDir().resolve(safe(name) + ".json"), GSON.toJson(copy));
            return true;
        } catch (Exception e) { return false; }
    }
    public static boolean loadProfile(String name) {
        try {
            Zer0Settings n = GSON.fromJson(Files.readString(profileDir().resolve(safe(name) + ".json")), Zer0Settings.class);
            if (n == null) return false;
            n.fixup();
            n.spotify.refreshToken = get().spotify.refreshToken;
            n.spotify.clientId = get().spotify.clientId;
            instance = n; touch(); return true;
        } catch (Exception e) { return false; }
    }
    public static boolean deleteProfile(String name) {
        try { return Files.deleteIfExists(profileDir().resolve(safe(name) + ".json")); } catch (IOException e) { return false; }
    }
    /** Resets every ZER0 setting except the Spotify login. */
    public static void resetAll() {
        Spotify keep = get().spotify;
        instance = new Zer0Settings();
        instance.spotify.clientId = keep.clientId; instance.spotify.refreshToken = keep.refreshToken;
        touch();
    }
    public static String safe(String n) { return n == null ? "" : n.replaceAll("[^A-Za-z0-9_\\- ]", "").trim(); }
}
