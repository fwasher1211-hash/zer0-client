package dev.zer0.client.ui.pages;

import com.example.donutqueue.ModConfig;
import com.example.donutqueue.Zer0Bridge;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.spotify.SpotifyClient;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.page.*;
import dev.zer0.client.ui.widgets.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static dev.zer0.client.ui.pages.Ctl.*;

/** Home (layout-dependent dashboard) and the Modules overview. */
final class HomePages {
    private HomePages() {}

    static final List<String> ACTIONS = List.of("SESSION", "STATS", "BIDDING", "TRACKING", "LEVER", "EARNINGS", "SESSIONS", "QUEUE");

    /** Label of a quick action, same wording as the original main menu. */
    static String actionLabel(String a) {
        ModConfig c = Zer0Bridge.cfg();
        return switch (a) {
            case "SESSION" -> Zer0Bridge.sessionRunning() ? "End session" : "Start session";
            case "STATS" -> "Stats";
            case "BIDDING" -> c.biddingActive ? "Stop bidding" : "Start bidding";
            case "TRACKING" -> c.trackingEnabled || c.awaitingBalCapture ? "Reset tracking" : "Start tracking";
            case "LEVER" -> c.leverPaused ? "Resume lever" : "Pause lever";
            case "EARNINGS" -> "Earnings";
            case "SESSIONS" -> "Sessions";
            case "QUEUE" -> "Anti gamble";
            default -> a;
        };
    }
    static void runAction(String a, dev.zer0.client.ui.screens.HubScreen hub) {
        ModConfig c = Zer0Bridge.cfg();
        switch (a) {
            case "SESSION" -> Ui.toast(Zer0Bridge.toggleSession());
            case "STATS", "EARNINGS" -> hub.open("earnings");
            case "SESSIONS" -> hub.open("sessions");
            case "QUEUE" -> hub.open("queue");
            case "BIDDING" -> Ui.toast(c.biddingActive ? Zer0Bridge.stopBidding() : Zer0Bridge.startPlainBidding());
            case "TRACKING" -> Ui.toast(c.trackingEnabled || c.awaitingBalCapture ? Zer0Bridge.resetTracking() : Zer0Bridge.startTracking());
            case "LEVER" -> { c.leverPaused = !c.leverPaused; Zer0Bridge.save(); Ui.toast(c.leverPaused ? "Lever skip paused." : "Lever skip resumed."); }
            default -> {}
        }
    }

    /** Graph data for the home chart, following the original's graph-range / gains settings. */
    static double[] graph() {
        ModConfig c = Zer0Bridge.cfg();
        if (c.mainGraphRange == 0) {
            double[][] pts = Zer0Bridge.dayPoints(LocalDate.now().toString());
            if (pts != null && pts.length >= 2) {
                double[] v = new double[pts.length];
                for (int i = 0; i < pts.length; i++) v[i] = pts[i].length > 1 ? pts[i][1] : pts[i][0];
                if (c.mainGraphGains) { double b = v[0]; for (int i = 0; i < v.length; i++) v[i] -= b; }
                return v;
            }
            return Zer0Bridge.recentBalances();
        }
        List<Zer0Bridge.DayRow> days = Zer0Bridge.days();
        int n = c.mainGraphRange == 1 ? 7 : c.mainGraphRange == 2 ? 30 : days.size();
        n = Math.min(n, days.size());
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            Zer0Bridge.DayRow d = days.get(days.size() - n + i);
            v[i] = c.mainGraphGains ? d.net() : d.balance();
        }
        return v;
    }
    static String rangeLabel() {
        return switch (Zer0Bridge.cfg().mainGraphRange) { case 0 -> "Today"; case 1 -> "Last 7 days"; case 2 -> "Last 30 days"; default -> "All time"; };
    }

    // ================================================================== HOME
    static final class Home extends Page {
        Home() { super("home", "STREAMER", "Home", "Your stream at a glance", "H"); }

        @Override protected void build(List<Section> out) {
            Layout L = Layout.current();
            ModConfig c = Zer0Bridge.cfg();
            if (L.home == Layout.Home.HERO) {
                section(out, "").bare().add(new Row.Custom(34, (g, x, y, w, h, mx, my) -> {
                    int hr = LocalTime.now().getHour();
                    String greet = hr < 5 ? "Late night stream" : hr < 12 ? "Good morning" : hr < 18 ? "Good afternoon" : "Good evening";
                    Fonts.draw(g, Fonts.Role.UI, greet + "  ·  " + LocalDate.now(), x + 2, y + 4, Theme.textMuted, 9f);
                    if (SpotifyClient.get().hasTrack()) {
                        SpotifyClient.Track t = SpotifyClient.get().track();
                        Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, "♪  " + t.title() + " — " + t.artists(), 7.5f, w), x + 2, y + 20, Theme.accent, 7.5f);
                    }
                }));
            }
            List<String> order = new java.util.ArrayList<>(c.mainOrder == null ? List.of("START_BAL", "TODAY", "GRAPH", "STATS", "BUTTONS") : c.mainOrder);
            if (L.home == Layout.Home.DASHBOARD) {   // dashboard: statistics first, then the chart
                order.remove("STATS"); order.add(0, "STATS");
                if (order.remove("GRAPH")) order.add(1, "GRAPH");
            }
            for (String part : order) {
                switch (part) {
                    case "STATS" -> { if (c.mainShowStats) stats(out, L); }
                    case "TODAY" -> { if (c.mainShowToday) today(out); }
                    case "START_BAL" -> { if (c.mainShowStartBal) startBal(out); }
                    case "GRAPH" -> { if (c.mainShowGraph) chart(out, L); }
                    case "BUTTONS" -> { if (c.mainShowButtons) actions(out); }
                    default -> {}
                }
            }
            customise(out);
        }

        private void stats(List<Section> out, Layout L) {
            Section s = section(out, "Live").bare();
            int n = 4;
            s.add(tiles(n, L.home == Layout.Home.DASHBOARD ? 118 : 104, L.home == Layout.Home.DASHBOARD ? 64 : 54, i -> (g, x, y, w, h, mx, my) -> {
                ModConfig c = Zer0Bridge.cfg();
                switch (i) {
                    case 0 -> {
                        var next = Zer0Bridge.queue();
                        stat(g, x, y, w, h, "Queue", String.valueOf(next.size()), Theme.text,
                                next.isEmpty() ? "nobody waiting" : "next: " + next.get(0).player() + " · " + next.get(0).amount());
                    }
                    case 1 -> { double v = Zer0Bridge.todayNet(); stat(g, x, y, w, h, "Today", signed(v), moneyColor(v), "net change"); }
                    case 2 -> {
                        Zer0Bridge.SessionRow r = Zer0Bridge.runningSession();
                        stat(g, x, y, w, h, "Session", r == null ? "Off" : dur(r.durSec()), r == null ? Theme.textMuted : Theme.text,
                                r == null ? "start one below" : signed(r.net()) + " · " + r.payments() + " payments");
                    }
                    default -> {
                        boolean act = c.biddingActive;
                        long left = Math.max(0, (c.biddingEndTimeMillis - System.currentTimeMillis()) / 1000);
                        stat(g, x, y, w, h, "Bidding", act ? Zer0Bridge.formatDuration(left) : c.biddingFinished ? "Finished" : "Idle",
                                act ? Theme.accent : Theme.textMuted,
                                c.biddingLeaderName != null && !c.biddingLeaderName.isEmpty() ? "lead: " + c.biddingLeaderName + " " + Zer0Bridge.fmtCompact(c.biddingLeaderAmount) : "no bids yet");
                    }
                }
            }));
        }

        private void today(List<Section> out) {
            Section s = section(out, "Today's top players");
            s.add(new Row.Custom(w -> Math.max(1, Zer0Bridge.topPlayers(LocalDate.now().toString(), 5).size()) * 18 + 10, (g, x, y, w, h, mx, my) -> {
                var top = Zer0Bridge.topPlayers(LocalDate.now().toString(), 5);
                if (top.isEmpty()) { Fonts.draw(g, Fonts.Role.UI, "No payments yet today.", x, y + (h - Fonts.px(Fonts.Role.UI, 7.5f)) / 2f, Theme.textFaint, 7.5f); return; }
                float ry = y + 4;
                for (var e : top) {
                    Zer0Bridge.drawHead(g, e.getKey(), Math.round(x), Math.round(ry + 2), 12);
                    Fonts.draw(g, Fonts.Role.UI, e.getKey(), x + 18, ry + 4, Theme.text, 8f);
                    Fonts.drawRight(g, Fonts.Role.UI, signed(e.getValue()), x + w, ry + 4, moneyColor(e.getValue()), 8f);
                    ry += 18;
                }
            }));
        }

        private void startBal(List<Section> out) {
            Section s = section(out, "Starting balance");
            s.add(new Row.Custom(46, (g, x, y, w, h, mx, my) -> {
                ModConfig c = Zer0Bridge.cfg();
                Fonts.draw(g, Fonts.Role.UI, Zer0Bridge.trackingStatus(), x, y + 9, Theme.text, 8.5f);
                float t = c.goalAmount > 0 ? (float) Math.max(0, Math.min(1, c.earnedTotal / c.goalAmount)) : 0;
                Render.bar(g, x, y + 28, w, 6, t, Theme.dark ? 0x22FFFFFF : 0x1A14141B, Theme.accent);
                if (c.goalAmount > 0) Fonts.drawRight(g, Fonts.Role.UI, Math.round(t * 100) + "%", x + w, y + 9, Theme.textMuted, 8f);
            }));
        }

        private void chart(List<Section> out, Layout L) {
            Section s = section(out, "Balance");
            float ch = L.home == Layout.Home.DASHBOARD ? 120 : 90;
            s.add(new Row.Custom(ch, (g, x, y, w, h, mx, my) -> {
                double[] v = graph();
                Fonts.draw(g, Fonts.Role.UI, rangeLabel() + (Zer0Bridge.cfg().mainGraphGains ? " · gains" : " · balance"), x, y + 6, Theme.textMuted, 7f);
                if (v == null || v.length < 2) { Fonts.drawCentered(g, Fonts.Role.UI, "Not enough data yet - payments and /bal readings appear here.", x + w / 2, y + h / 2 - 4, Theme.textFaint, 7.5f); return; }
                Render.areaChart(g, x, y + 20, w, h - 26, v, Theme.accent, true);
            }));
        }

        private void actions(List<Section> out) {
            Section s = section(out, "Quick actions");
            List<Button> bs = new ArrayList<>();
            ModConfig c = Zer0Bridge.cfg();
            for (String a : c.mainButtons == null ? List.of("SESSION", "STATS", "BIDDING", "TRACKING") : c.mainButtons)
                if (ACTIONS.contains(a)) bs.add(new Button(() -> actionLabel(a), () -> runAction(a, hub)));
            if (!bs.isEmpty()) bs.get(0).selected(() -> true);
            s.buttons(bs.toArray(new Button[0]));
        }

        private void customise(List<Section> out) {
            ModConfig c = Zer0Bridge.cfg();
            Section s = section(out, "Customise home");
            s.setting("Starting balance card", "", cfgToggle(() -> c.mainShowStartBal, v -> { c.mainShowStartBal = v; rebuild(); }));
            s.setting("Today's players", "", cfgToggle(() -> c.mainShowToday, v -> { c.mainShowToday = v; rebuild(); }));
            s.setting("Balance chart", "", cfgToggle(() -> c.mainShowGraph, v -> { c.mainShowGraph = v; rebuild(); }));
            s.setting("Live statistics", "", cfgToggle(() -> c.mainShowStats, v -> { c.mainShowStats = v; rebuild(); }));
            s.setting("Quick actions", "", cfgToggle(() -> c.mainShowButtons, v -> { c.mainShowButtons = v; rebuild(); }));
            s.setting("Chart range", "", new Segmented(List.of(new Dropdown.Opt("0", "Today"), new Dropdown.Opt("1", "7d"), new Dropdown.Opt("2", "30d"), new Dropdown.Opt("3", "All")),
                    () -> String.valueOf(c.mainGraphRange), v -> { c.mainGraphRange = Integer.parseInt(v); Zer0Bridge.save(); }).size(160, 20));
            s.setting("Chart shows", "Balance over time, or gains only.", new Segmented(List.of(new Dropdown.Opt("bal", "Balance"), new Dropdown.Opt("gain", "Gains")),
                    () -> c.mainGraphGains ? "gain" : "bal", v -> { c.mainGraphGains = v.equals("gain"); Zer0Bridge.save(); }).size(130, 20));
            // which quick actions are shown, in order
            s.add(new Row.Custom(w -> 52, (g, x, y, w, h, mx, my) -> {
                Fonts.draw(g, Fonts.Role.UI, "Quick action buttons", x, y + 6, Theme.text, 8.5f);
                float cx = x, cy = y + 24;
                List<String> on = c.mainButtons == null ? List.of() : c.mainButtons;
                for (String a : ACTIONS) {
                    String lab = a.charAt(0) + a.substring(1).toLowerCase();
                    float tw = Fonts.width(Fonts.Role.UI, lab, 7.2f) + 16;
                    if (cx + tw > x + w) break;
                    boolean sel = on.contains(a), hov = mx >= cx && mx < cx + tw && my >= cy && my < cy + 16;
                    Render.rect(g, cx, cy, tw, 16, 8, sel ? Theme.accent : hov ? Theme.cardHover : Theme.card);
                    Fonts.drawCentered(g, Fonts.Role.UI, lab, cx + tw / 2, cy + (16 - Fonts.px(Fonts.Role.UI, 7.2f)) / 2f, sel ? Theme.accentText : Theme.textMuted, 7.2f);
                    cx += tw + 4;
                }
            }).click((x, y, w, h, mx, my, b) -> {
                float cx = x, cy = y + 24;
                for (String a : ACTIONS) {
                    String lab = a.charAt(0) + a.substring(1).toLowerCase();
                    float tw = Fonts.width(Fonts.Role.UI, lab, 7.2f) + 16;
                    if (mx >= cx && mx < cx + tw && my >= cy && my < cy + 16) {
                        if (c.mainButtons == null) c.mainButtons = new ArrayList<>();
                        if (!c.mainButtons.remove(a)) c.mainButtons.add(a);
                        Zer0Bridge.save(); Sounds.click(); rebuild(); return true;
                    }
                    cx += tw + 4;
                }
                return false;
            }));
        }
    }

    // ================================================================== MODULES
    static final class Modules extends Page {
        Modules() { super("modules", "STREAMER", "Modules", "Turn features on and off", "M"); }

        private record Mod(String name, String desc, String page, java.util.function.Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {}

        @Override protected void build(List<Section> out) {
            ModConfig c = Zer0Bridge.cfg();
            Zer0Settings z = Zer0Settings.get();
            List<Mod> mods = List.of(
                    new Mod("Queue HUD", "Who paid and who's next", "queue", () -> c.hudEnabled, v -> c.hudEnabled = v),
                    new Mod("Starting Bal", "Goal progress box", "startbal", () -> c.startBalBoxEnabled, v -> c.startBalBoxEnabled = v),
                    new Mod("Bidding box", "Live auction box", "bidding", () -> c.biddingBoxEnabled, v -> c.biddingBoxEnabled = v),
                    new Mod("Info Display", "IGN, min/max, game", "info", () -> c.infoBoxEnabled, v -> c.infoBoxEnabled = v),
                    new Mod("Earnings box", "Balance graph + top players", "earnings", () -> c.earningsBoxEnabled, v -> c.earningsBoxEnabled = v),
                    new Mod("Earnings tracking", "Count payments per player", "earnings", () -> c.earningsTrackingEnabled, v -> c.earningsTrackingEnabled = v),
                    new Mod("Rank colours", "Tab list & name tags", "ranks", () -> c.rankColorsEnabled, v -> c.rankColorsEnabled = v),
                    new Mod("Payment colours", "Recolour pay messages", "chat", () -> c.chatPaymentsEnabled, v -> c.chatPaymentsEnabled = v),
                    new Mod("Whisper style", "Cleaner whispers", "chat", () -> c.chatWhispersEnabled, v -> c.chatWhispersEnabled = v),
                    new Mod("Spotify HUD", "Now playing on screen", "spotify", () -> z.spotify.hudEnabled, v -> { z.spotify.hudEnabled = v; Zer0Settings.touch(); }),
                    new Mod("Glass HUD", "Rounded frosted boxes", "hud", () -> z.glassHud, v -> { z.glassHud = v; Zer0Settings.touch(); }));
            List<Toggle> toggles = new ArrayList<>();
            for (Mod m : mods) toggles.add(new Toggle(m.get, v -> { m.set.accept(v); Zer0Bridge.save(); }));
            Section s = section(out, "").bare();
            float tileH = 58;
            Row.Custom grid = tiles(mods.size(), 150, tileH, i -> (g, x, y, w, h, mx, my) -> {
                Mod m = mods.get(i);
                boolean on = m.get.get(), hov = mx >= x && mx < x + w && my >= y && my < y + h;
                Layout L = Layout.current();
                int body = Col.mix(Theme.card, Theme.cardHover, hov ? 1 : 0);
                if (L.panels == Layout.Panels.GLASS) Render.glass(g, x, y, w, h, L.radius, body, Theme.panelEdge, L.shadows); else Render.rect(g, x, y, w, h, L.radius, body);
                Render.circle(g, x + 14, y + 15, 3, on ? Theme.good : Theme.textFaint);
                Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, m.name, 8.5f, w - 60), x + 22, y + 11, Theme.text, 8.5f);
                Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, m.desc, 7f, w - 24), x + 12, y + 28, Theme.textMuted, 7f);
                Fonts.draw(g, Fonts.Role.UI, "Open →", x + 12, y + h - 15, hov && my > y + h - 20 ? Theme.accent : Theme.textFaint, 7f);
                Toggle t = toggles.get(i);
                t.place(x + w - 42, y + 9, 30, 17); t.render(g, mx, my);
            });
            for (Toggle t : toggles) grid.with(t);
            grid.click((x, y, w, h, mx, my, b) -> {
                int cols = Math.max(1, Math.min(mods.size(), (int) ((w + 8) / (150 + 8))));
                float tw = (w - (cols - 1) * 8) / cols;
                int col = (int) ((mx - x) / (tw + 8)), row = (int) ((my - y - 2) / (tileH + 8));
                int i = row * cols + col;
                if (i < 0 || i >= mods.size()) return false;
                float ty = y + 2 + row * (tileH + 8);
                if (my > ty + tileH - 20) { hub.open(mods.get(i).page); return true; }
                return false;
            });
            s.add(grid);
        }
    }
}
