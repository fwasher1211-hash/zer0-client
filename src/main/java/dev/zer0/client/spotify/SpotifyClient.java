package dev.zer0.client.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.*;

/**
 * Spotify Web API client using the officially supported "Authorization Code with PKCE" flow:
 * no client secret is ever needed or stored. The user registers their own (free) Spotify app, pastes its
 * Client ID, and logs in through the real Spotify website in their browser. Only the refresh token is
 * kept, locally, in config/zer0client/settings.json (and never in exported profiles).
 *
 * Everything shown in the HUD comes from GET /v1/me/player. Playback control (play/pause/skip/volume)
 * is a Spotify Premium feature - the API answers 403 otherwise and ZER0 says so.
 */
public final class SpotifyClient {
    private static final SpotifyClient I = new SpotifyClient();
    public static SpotifyClient get() { return I; }

    public static final String SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing";
    public enum State { NOT_CONFIGURED, LOGGED_OUT, CONNECTING, CONNECTED, ERROR }

    public record Track(String id, String title, String artists, String album, String artUrl, long durationMs,
                        long progressMs, boolean playing, long fetchedAt, int volume, boolean supportsVolume, String device) {
        /** Progress extrapolated since the last poll (smooth bar without hammering the API). */
        public long progressNow() {
            if (!playing) return progressMs;
            return Math.min(durationMs, progressMs + (System.currentTimeMillis() - fetchedAt));
        }
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ZER0-Spotify"); t.setDaemon(true); return t;
    });
    private volatile State state = State.NOT_CONFIGURED;
    private volatile String message = "";
    private volatile Track track;
    private volatile String accessToken;
    private volatile long accessExpires;
    private volatile long nextPollAt, backoffUntil;
    private volatile boolean active;          // something on screen wants fresh data
    private volatile Future<?> loginTask;
    private ServerSocket loginSocket;

    private SpotifyClient() {}

    public State state() { return state; }
    public String message() { return message; }
    public Track track() { return track; }
    public boolean hasTrack() { return track != null && state == State.CONNECTED; }

    /** Called every client tick. */
    public void tick(boolean wanted) {
        Zer0Settings.Spotify s = Zer0Settings.get().spotify;
        if (state == State.CONNECTING) return;
        if (s.clientId == null || s.clientId.isBlank()) { state = State.NOT_CONFIGURED; track = null; return; }
        if (s.refreshToken == null || s.refreshToken.isBlank()) { if (state != State.ERROR) state = State.LOGGED_OUT; track = null; return; }
        if (state == State.NOT_CONFIGURED || state == State.LOGGED_OUT) state = State.CONNECTED;
        active = wanted;
        long now = System.currentTimeMillis();
        if (now >= nextPollAt && now >= backoffUntil) {
            nextPollAt = now + (wanted ? 1500 : 6000);
            exec.execute(this::poll);
        }
    }

    // ================================================================== login (PKCE + loopback redirect)
    public String redirectUri() { return "http://127.0.0.1:" + Zer0Settings.get().spotify.port + "/callback"; }

    public void login() {
        Zer0Settings.Spotify s = Zer0Settings.get().spotify;
        if (s.clientId == null || s.clientId.isBlank()) { state = State.NOT_CONFIGURED; message = "Paste your Client ID first."; return; }
        cancelLogin();
        state = State.CONNECTING; message = "Waiting for you to log in in the browser...";
        loginTask = exec.submit(() -> {
            try {
                String verifier = random(64);
                String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                        MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                String st = random(16);
                String url = "https://accounts.spotify.com/authorize?response_type=code"
                        + "&client_id=" + enc(s.clientId.trim()) + "&redirect_uri=" + enc(redirectUri())
                        + "&code_challenge_method=S256&code_challenge=" + challenge
                        + "&scope=" + enc(SCOPES) + "&state=" + st;
                try (ServerSocket ss = new ServerSocket()) {
                    loginSocket = ss;
                    ss.setReuseAddress(true);
                    ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), s.port));
                    ss.setSoTimeout(180_000);
                    Mc.mc().execute(() -> Mc.openUrl(url));
                    while (true) {
                        try (Socket sock = ss.accept()) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
                            String line = in.readLine();
                            if (line == null || !line.startsWith("GET /callback")) { respond(sock, 404, "Not found"); continue; }
                            String query = line.split(" ")[1];
                            query = query.contains("?") ? query.substring(query.indexOf('?') + 1) : "";
                            String code = param(query, "code"), gotState = param(query, "state"), err = param(query, "error");
                            if (err != null) { respond(sock, 200, page("Login cancelled", "Spotify said: " + err + ". You can close this tab.")); fail("Login cancelled: " + err); return; }
                            if (code == null || !st.equals(gotState)) { respond(sock, 400, page("Something went wrong", "The login response was not valid. Try again from Minecraft.")); fail("Login failed (state mismatch)."); return; }
                            respond(sock, 200, page("ZER0 is connected", "Spotify is linked to ZER0 CLIENT. You can close this tab and go back to Minecraft."));
                            exchange(code, verifier);
                            return;
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                fail("Login timed out. Press Connect to try again.");
            } catch (BindException e) {
                fail("Port " + s.port + " is in use. Change the port (and the Redirect URI in your Spotify app).");
            } catch (Exception e) {
                if (state == State.CONNECTING) fail("Login failed: " + e.getMessage());
            } finally { loginSocket = null; }
        });
    }
    public void cancelLogin() {
        try { if (loginSocket != null) loginSocket.close(); } catch (IOException ignored) {}
        if (loginTask != null) loginTask.cancel(true);
        if (state == State.CONNECTING) state = State.LOGGED_OUT;
    }
    public void logout() {
        cancelLogin();
        Zer0Settings.get().spotify.refreshToken = ""; Zer0Settings.touch();
        accessToken = null; track = null; state = State.LOGGED_OUT; message = "Disconnected.";
        SpotifyArt.clear();
    }

    private void exchange(String code, String verifier) throws Exception {
        Zer0Settings.Spotify s = Zer0Settings.get().spotify;
        JsonObject j = token("grant_type=authorization_code&code=" + enc(code) + "&redirect_uri=" + enc(redirectUri())
                + "&client_id=" + enc(s.clientId.trim()) + "&code_verifier=" + enc(verifier));
        applyToken(j);
        state = State.CONNECTED; message = "Connected.";
        nextPollAt = 0;
    }
    private boolean refresh() {
        Zer0Settings.Spotify s = Zer0Settings.get().spotify;
        try {
            JsonObject j = token("grant_type=refresh_token&refresh_token=" + enc(s.refreshToken) + "&client_id=" + enc(s.clientId.trim()));
            applyToken(j);
            return true;
        } catch (TokenException e) {
            s.refreshToken = ""; Zer0Settings.touch();
            fail("Spotify login expired or was revoked - press Connect again.");
            return false;
        } catch (Exception e) {
            message = "Can't reach Spotify (" + e.getClass().getSimpleName() + "). Retrying...";
            backoffUntil = System.currentTimeMillis() + 15000;
            return false;
        }
    }
    private static final class TokenException extends Exception { TokenException(String m) { super(m); } }
    private JsonObject token(String form) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .timeout(Duration.ofSeconds(12)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() / 100 != 2) {
            String desc = "";
            try { desc = JsonParser.parseString(r.body()).getAsJsonObject().get("error_description").getAsString(); } catch (Exception ignored) {}
            if (r.statusCode() == 400) throw new TokenException(desc);
            throw new IOException("HTTP " + r.statusCode() + " " + desc);
        }
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }
    private void applyToken(JsonObject j) {
        accessToken = j.get("access_token").getAsString();
        accessExpires = System.currentTimeMillis() + j.get("expires_in").getAsLong() * 1000 - 30_000;
        if (j.has("refresh_token")) { Zer0Settings.get().spotify.refreshToken = j.get("refresh_token").getAsString(); Zer0Settings.touch(); }
    }
    private boolean ensureToken() { return (accessToken != null && System.currentTimeMillis() < accessExpires) || refresh(); }
    private void fail(String m) { state = State.ERROR; message = m; }

    // ================================================================== polling
    private void poll() {
        if (state != State.CONNECTED || !ensureToken()) return;
        try {
            HttpResponse<String> r = api("GET", "/me/player?additional_types=episode", null);
            int code = r.statusCode();
            if (code == 204) { track = null; message = "Nothing playing."; return; }
            if (code == 401) { accessToken = null; return; }
            if (code == 429) { backoff(r); return; }
            if (code / 100 != 2) { message = "Spotify error " + code; return; }
            JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
            JsonElement itemEl = j.get("item");
            if (itemEl == null || itemEl.isJsonNull()) { track = null; message = "Nothing playing."; return; }
            JsonObject item = itemEl.getAsJsonObject();
            String type = str(item, "type");
            String title = str(item, "name");
            String artists, album, art = null;
            JsonArray images;
            if ("episode".equals(type) && item.has("show")) {
                JsonObject show = item.getAsJsonObject("show");
                artists = str(show, "publisher"); album = str(show, "name"); images = show.getAsJsonArray("images");
            } else {
                StringBuilder sb = new StringBuilder();
                for (JsonElement a : item.getAsJsonArray("artists")) { if (sb.length() > 0) sb.append(", "); sb.append(str(a.getAsJsonObject(), "name")); }
                artists = sb.toString();
                JsonObject al = item.getAsJsonObject("album");
                album = str(al, "name"); images = al.getAsJsonArray("images");
            }
            if (images != null && !images.isEmpty()) {   // pick the image closest to 300px
                int best = Integer.MAX_VALUE;
                for (JsonElement e : images) {
                    JsonObject im = e.getAsJsonObject();
                    int w = im.has("width") && !im.get("width").isJsonNull() ? im.get("width").getAsInt() : 300;
                    if (Math.abs(w - 300) < best) { best = Math.abs(w - 300); art = str(im, "url"); }
                }
            }
            JsonObject dev = j.has("device") && j.get("device").isJsonObject() ? j.getAsJsonObject("device") : new JsonObject();
            int vol = dev.has("volume_percent") && !dev.get("volume_percent").isJsonNull() ? dev.get("volume_percent").getAsInt() : -1;
            boolean sv = dev.has("supports_volume") && dev.get("supports_volume").getAsBoolean();
            track = new Track(str(item, "id"), title, artists, album, art, item.get("duration_ms").getAsLong(),
                    j.has("progress_ms") && !j.get("progress_ms").isJsonNull() ? j.get("progress_ms").getAsLong() : 0,
                    j.get("is_playing").getAsBoolean(), System.currentTimeMillis(), vol, sv, str(dev, "name"));
            message = "";
            SpotifyArt.request(art);
        } catch (Exception e) {
            message = "Can't reach Spotify right now.";
            backoffUntil = System.currentTimeMillis() + 5000;
        }
    }
    private void backoff(HttpResponse<String> r) {
        long secs = r.headers().firstValueAsLong("Retry-After").orElse(5);
        backoffUntil = System.currentTimeMillis() + secs * 1000;
    }

    // ================================================================== controls
    public void togglePlay() {
        Track t = track; if (t == null) return;
        boolean play = !t.playing();
        track = new Track(t.id(), t.title(), t.artists(), t.album(), t.artUrl(), t.durationMs(), t.progressNow(), play,
                System.currentTimeMillis(), t.volume(), t.supportsVolume(), t.device());   // optimistic
        control("PUT", play ? "/me/player/play" : "/me/player/pause");
    }
    public void next() { control("POST", "/me/player/next"); }
    public void previous() { control("POST", "/me/player/previous"); }
    public void setVolume(int pct) {
        Track t = track;
        if (t != null) track = new Track(t.id(), t.title(), t.artists(), t.album(), t.artUrl(), t.durationMs(), t.progressNow(), t.playing(),
                System.currentTimeMillis(), pct, t.supportsVolume(), t.device());
        control("PUT", "/me/player/volume?volume_percent=" + Math.max(0, Math.min(100, pct)));
    }
    private void control(String method, String path) {
        if (state != State.CONNECTED) return;
        exec.execute(() -> {
            if (!ensureToken()) return;
            try {
                HttpResponse<String> r = api(method, path, "");
                int c = r.statusCode();
                if (c == 403) message = "Playback control needs Spotify Premium.";
                else if (c == 404) message = "No active Spotify device - start playing on one first.";
                else if (c == 429) backoff(r);
                else message = "";
                nextPollAt = System.currentTimeMillis() + 350;   // refresh soon to confirm
            } catch (Exception e) { message = "Control failed: " + e.getClass().getSimpleName(); }
        });
    }

    private HttpResponse<String> api(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1" + path))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + accessToken);
        if (body == null) b.GET(); else b.method(method, HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpClient http() { return http; }
    ExecutorService executor() { return exec; }

    // ================================================================== helpers
    private static String str(JsonObject o, String k) { return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String param(String q, String key) {
        for (String kv : q.split("&")) { int i = kv.indexOf('='); if (i > 0 && kv.substring(0, i).equals(key)) return URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8); }
        return null;
    }
    private static String random(int n) {
        String a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
        SecureRandom r = new SecureRandom(); StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(a.charAt(r.nextInt(a.length())));
        return sb.toString();
    }
    private static void respond(Socket s, int code, String html) throws IOException {
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        OutputStream o = s.getOutputStream();
        o.write(("HTTP/1.1 " + code + " OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + b.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        o.write(b); o.flush();
    }
    private static String page(String title, String text) {
        return "<!doctype html><html><head><meta charset=utf-8><title>ZER0 CLIENT</title><style>"
                + "body{margin:0;height:100vh;display:grid;place-items:center;font-family:system-ui,-apple-system,Segoe UI,sans-serif;"
                + "background:radial-gradient(circle at 30% 20%,#2a2160,#0b0b12 60%);color:#fff}"
                + ".c{padding:36px 44px;border-radius:22px;background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.14);"
                + "backdrop-filter:blur(20px);max-width:420px;text-align:center}h1{font-weight:650;margin:0 0 10px;font-size:22px}"
                + "p{margin:0;opacity:.75;line-height:1.5}b{color:#8B7CFF}</style></head><body><div class=c><h1><b>ZER0</b> &middot; "
                + title + "</h1><p>" + text + "</p></div></body></html>";
    }
}
