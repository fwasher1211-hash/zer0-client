package dev.zer0.client.ui.pages;

import com.example.donutqueue.ModConfig;
import com.example.donutqueue.Zer0Bridge;
import dev.zer0.client.compat.Mc;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.page.*;
import dev.zer0.client.ui.widgets.*;
import net.minecraft.item.ItemStack;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static dev.zer0.client.ui.pages.Ctl.*;

/** Auctions & presets, Earnings, Sessions, Payment log. */
final class DataPages {
    private DataPages() {}

    // ================================================================== AUCTIONS
    static final class Auctions extends Page {
        Auctions() { super("auctions", "STREAMER", "Auctions", "Auction an item with a timer, minimum bid and presets", "A"); }
        private String itemId = "";
        private final TextField query = new TextField("Search items...").icon("search").max(32);
        private final TextField amount = new TextField("1").max(4), time = new TextField("5m, 60, 1h").max(8), min = new TextField("50k, 1m").max(12);
        private float gridScroll;

        @Override protected void build(List<Section> out) {
            ModConfig c = Zer0Bridge.cfg();
            if (amount.text.isEmpty()) amount.value("1");
            amount.filter = ch -> Character.isDigit(ch);
            query.onChange = s -> gridScroll = 0;

            Section pick = section(out, "Item");
            pick.add(new Row.Custom(34, (g, x, y, w, h, mx, my) -> {
                ItemStack st = itemId.isEmpty() ? ItemStack.EMPTY : Zer0Bridge.stack(itemId);
                Render.rect(g, x, y + 5, 24, 24, 7, Theme.field);
                if (!st.isEmpty()) g.drawItem(st, Math.round(x + 4), Math.round(y + 9));
                Fonts.draw(g, Fonts.Role.UI, itemId.isEmpty() ? "No item selected" : Zer0Bridge.itemName(itemId), x + 32, y + 13, itemId.isEmpty() ? Theme.textFaint : Theme.text, 8.5f);
            }));
            pick.buttons(btn("Use held item", this::useHeld), btn("Clear", () -> itemId = "").kind(Button.Kind.GHOST));
            pick.wide("", "", query);
            final int CELL = 22;
            pick.add(new Row.Custom(w -> CELL * 4 + 6, (g, x, y, w, h, mx, my) -> {
                List<Zer0Bridge.ItemRow> list = filtered();
                int cols = Math.max(1, (int) (w / CELL));
                float maxScroll = Math.max(0, ((list.size() + cols - 1) / cols) * CELL - (h - 6));
                gridScroll = Math.max(0, Math.min(gridScroll, maxScroll));
                g.enableScissor((int) x, (int) y + 2, (int) (x + w), (int) (y + h - 2));
                int first = (int) (gridScroll / CELL) * cols;
                for (int i = first; i < list.size(); i++) {
                    int r = i / cols, col = i % cols;
                    float cx = x + col * CELL, cy = y + 3 + r * CELL - gridScroll;
                    if (cy > y + h) break;
                    Zer0Bridge.ItemRow it = list.get(i);
                    boolean hov = mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL && my > y && my < y + h;
                    boolean sel = it.id().equals(itemId);
                    if (sel || hov) Render.rect(g, cx + 1, cy + 1, CELL - 2, CELL - 2, 6, sel ? Theme.accentSoft : Theme.cardHover);
                    g.drawItem(it.stack(), Math.round(cx + 3), Math.round(cy + 3));
                    if (hov) Ui.hoverTip(it, it.name(), mx, my);
                }
                g.disableScissor();
                if (list.isEmpty()) Fonts.drawCentered(g, Fonts.Role.UI, "No items match.", x + w / 2, y + h / 2 - 4, Theme.textFaint, 7.5f);
            }).click((x, y, w, h, mx, my, b) -> {
                List<Zer0Bridge.ItemRow> list = filtered();
                int cols = Math.max(1, (int) (w / CELL));
                int col = (int) ((mx - x) / CELL), row = (int) ((my - y - 3 + gridScroll) / CELL);
                int i = row * cols + col;
                if (col < cols && i >= 0 && i < list.size()) { itemId = list.get(i).id(); Sounds.click(); return true; }
                return false;
            }).onScroll((mx, my, a) -> { gridScroll -= (float) a * CELL; return true; }));

            Section set = section(out, "Auction");
            set.setting("Amount", "How many of the item.", amount.size(80, 20));
            set.setting("Duration", "60 = seconds, or 5m, 1h.", time.size(110, 20));
            set.setting("Minimum bid", "Optional. 50k, 1m ...", min.size(110, 20));
            set.buttons(
                    btn("Start auction", this::start).selected(() -> true),
                    btn("Stop & reset", () -> { Zer0Bridge.auctionStop(); Ui.toast("Bidding stopped and reset."); }).kind(Button.Kind.DANGER),
                    btn("Save as preset…", () -> { if (validate() != null) Ui.toast("Click an empty slot's “Save here” below"); }).kind(Button.Kind.GHOST));
            set.note(() -> c.biddingActive ? "A round is live - see the Bidding page for the leader." : "");

            Section pr = section(out, "Presets");
            int n = Zer0Bridge.presetSlots();
            for (int i = 0; i < n; i++) {
                final int slot = i;
                Button start = btn("Start", () -> Ui.toast(Zer0Bridge.startPreset(slot) ? "Preset " + (slot + 1) + " started" : "Preset is empty", false));
                Button save = btn("Save here", () -> {
                    Object[] v = validate(); if (v == null) return;
                    Zer0Bridge.savePreset(slot, (String) v[0], (Integer) v[1], (Long) v[2], (Double) v[3]);
                    Ui.toast("Saved to preset " + (slot + 1));
                });
                Button clear = btn("Clear", () -> { Zer0Bridge.clearPreset(slot); Ui.toast("Preset " + (slot + 1) + " cleared"); }).kind(Button.Kind.GHOST);
                pr.add(new Row.Custom(34, (g, x, y, w, h, mx, my) -> {
                    var p = Zer0Bridge.presets().get(slot);
                    Fonts.draw(g, Fonts.Role.UI, String.valueOf(slot + 1), x + 2, y + 13, Theme.textFaint, 8f);
                    if (p.isEmpty()) {
                        Fonts.draw(g, Fonts.Role.UI, "Empty", x + 16, y + 13, Theme.textFaint, 8f);
                        save.place(x + w - 70, y + 7, 70, 20); save.render(g, mx, my);
                    } else {
                        ItemStack st = Zer0Bridge.stack(p.itemId);
                        if (!st.isEmpty()) g.drawItem(st, Math.round(x + 16), Math.round(y + 9));
                        Fonts.draw(g, Fonts.Role.UI, p.count + "× " + Zer0Bridge.itemName(p.itemId), x + 38, y + 8, Theme.text, 8f);
                        Fonts.draw(g, Fonts.Role.UI, Zer0Bridge.formatDuration(p.seconds) + (p.minimum > 0 ? "  ·  min " + Zer0Bridge.fmtCompact(p.minimum) : ""), x + 38, y + 19, Theme.textMuted, 7f);
                        clear.place(x + w - 104, y + 7, 46, 20); clear.render(g, mx, my);
                        start.place(x + w - 54, y + 7, 54, 20); start.render(g, mx, my);
                    }
                }).with(start, save, clear));
            }
            pr.footer("In chat: /bidding <number> starts that preset.");
        }

        private List<Zer0Bridge.ItemRow> filtered() {
            String q = query.text.trim().toLowerCase();
            List<Zer0Bridge.ItemRow> all = Zer0Bridge.items();
            if (q.isEmpty()) return all;
            List<Zer0Bridge.ItemRow> out = new ArrayList<>();
            for (var it : all) if (it.lower().contains(q) || it.id().contains(q)) out.add(it);
            return out;
        }
        private void useHeld() {
            var p = Mc.mc().player;
            if (p == null || p.getMainHandStack().isEmpty()) { Ui.toast("Your main hand is empty.", true); return; }
            ItemStack held = p.getMainHandStack();
            for (var it : Zer0Bridge.items()) if (it.stack().getItem() == held.getItem()) { itemId = it.id(); amount.value(String.valueOf(held.getCount())); return; }
            Ui.toast("That item is not in the list.", true);
        }
        /** Same checks and messages as the original auction screen. Returns {id, count, seconds, min} or null. */
        private Object[] validate() {
            if (itemId.isEmpty()) { Ui.toast("Pick an item first.", true); return null; }
            int count;
            try { count = Integer.parseInt(amount.text.trim()); } catch (Exception e) { count = 0; }
            if (count < 1) { Ui.toast("Amount must be at least 1.", true); return null; }
            long secs = Zer0Bridge.parseDurationSeconds(time.text.trim().isEmpty() ? "60" : time.text.trim());
            if (secs <= 0) { Ui.toast("Give a valid time, e.g. 60, 5m or 1h.", true); return null; }
            double m = min.text.trim().isEmpty() ? 0 : Zer0Bridge.parseAmount(min.text.trim());
            return new Object[]{itemId, count, secs, Math.max(0, m)};
        }
        private void start() {
            Object[] v = validate(); if (v == null) return;
            Zer0Bridge.startAuction((String) v[0], (Integer) v[1], (Long) v[2], (Double) v[3]);
            Ui.toast("Auction started");
        }
    }

    // ================================================================== EARNINGS
    static final class Earnings extends Page {
        Earnings() { super("earnings", "STREAMER", "Earnings", "Daily totals, top players and the earnings box", "E"); }
        private String day = LocalDate.now().toString();
        private float listScroll;

        @Override protected void build(List<Section> out) {
            ModConfig c = Zer0Bridge.cfg();
            Section s = section(out, "Tracking");
            s.setting("Track earnings per player", "Counts what each player paid or won.", cfgToggle(() -> c.earningsTrackingEnabled, v -> c.earningsTrackingEnabled = v));
            s.setting("Players in the box", "", cfgInt(1, 10, () -> c.earningsShowCount, v -> c.earningsShowCount = v), () -> { c.earningsShowCount = 5; Zer0Bridge.save(); }, () -> c.earningsShowCount != 5);
            s.setting("Show losses", "List who won the most from you instead of who paid most.", cfgToggle(() -> c.earningsTopShowLost, v -> c.earningsTopShowLost = v));
            s.setting("Chat messages", "", cfgToggle(() -> c.earningsChatMessagesEnabled, v -> c.earningsChatMessagesEnabled = v));

            Section d = section(out, "Days");
            d.add(new Row.Custom(w -> 110, (g, x, y, w, h, mx, my) -> {
                List<Zer0Bridge.DayRow> days = Zer0Bridge.days();
                if (days.isEmpty()) { Fonts.drawCentered(g, Fonts.Role.UI, "No earnings recorded yet.", x + w / 2, y + h / 2 - 4, Theme.textFaint, 7.5f); return; }
                double max = 1; for (var r : days) max = Math.max(max, Math.abs(r.net()));
                float bw = 14, gap = 4, total = days.size() * (bw + gap);
                float maxScroll = Math.max(0, total - w);
                listScroll = Math.max(0, Math.min(listScroll, maxScroll));
                float base = y + h / 2f + 4;
                g.enableScissor((int) x, (int) y, (int) (x + w), (int) (y + h));
                float bx = x + w - total + maxScroll - listScroll;
                for (var r : days) {
                    float bh = (float) (Math.abs(r.net()) / max * (h / 2f - 16));
                    boolean sel = r.date().equals(day), hov = mx >= bx && mx < bx + bw && my >= y && my < y + h;
                    int col = Col.withAlpha(r.net() >= 0 ? Theme.good : Theme.bad, sel ? 255 : hov ? 200 : 130);
                    if (r.net() >= 0) Render.rect(g, bx, base - bh, bw, Math.max(2, bh), 3, col); else Render.rect(g, bx, base, bw, Math.max(2, bh), 3, col);
                    if (hov) Ui.hoverTip(r, r.date() + "  " + signed(r.net()) + "  ·  balance " + Zer0Bridge.fmtCompact(r.balance()), mx, my);
                    bx += bw + gap;
                }
                g.disableScissor();
                g.fill(Math.round(x), Math.round(base), Math.round(x + w), Math.round(base) + 1, Theme.divider);
            }).click((x, y, w, h, mx, my, b) -> {
                List<Zer0Bridge.DayRow> days = Zer0Bridge.days();
                float bw = 14, gap = 4, total = days.size() * (bw + gap), maxScroll = Math.max(0, total - w);
                float bx = x + w - total + maxScroll - listScroll;
                for (var r : days) { if (mx >= bx && mx < bx + bw) { day = r.date(); Sounds.click(); return true; } bx += bw + gap; }
                return false;
            }).onScroll((mx, my, a) -> { listScroll += (float) a * 18; return true; }));
            d.footer("Click a bar to inspect that day.");

            Section one = section(out, "Selected day");
            one.add(new Row.Custom(96, (g, x, y, w, h, mx, my) -> {
                double net = 0, bal = 0;
                for (var r : Zer0Bridge.days()) if (r.date().equals(day)) { net = r.net(); bal = r.balance(); }
                Fonts.draw(g, Fonts.Role.UI, day.equals(LocalDate.now().toString()) ? "Today" : day, x, y + 6, Theme.textMuted, 7.5f);
                Fonts.draw(g, Fonts.Role.HEADING, signed(net), x, y + 18, moneyColor(net), 15);
                Fonts.drawRight(g, Fonts.Role.UI, "balance " + Zer0Bridge.fmtCompact(bal), x + w, y + 8, Theme.textMuted, 7.5f);
                double[][] pts = Zer0Bridge.dayPoints(day);
                if (pts != null && pts.length >= 2) {
                    double[] v = new double[pts.length];
                    for (int i = 0; i < pts.length; i++) v[i] = pts[i].length > 1 ? pts[i][1] : pts[i][0];
                    Render.areaChart(g, x, y + 40, w, h - 44, v, moneyColor(net), true);
                } else Fonts.draw(g, Fonts.Role.UI, "No graph points for this day.", x, y + 54, Theme.textFaint, 7.5f);
            }));
            one.add(new Row.Custom(w -> 6 * 16 + 24, (g, x, y, w, h, mx, my) -> {
                float half = (w - 12) / 2;
                list(g, x, y, half, "Top players", Zer0Bridge.topPlayers(day, 6));
                list(g, x + half + 12, y, half, "Biggest losses", Zer0Bridge.topLosses(day, 6));
            }));

            Section ng = section(out, "Not gambling");
            ng.note("Players marked here (e.g. alts or friends sending money) are left out of losses lists.");
            TextField f = new TextField("Player name").max(16);
            ng.add(fieldWithButton("Mark", f, "Add", n -> { if (!n.isEmpty()) { Zer0Bridge.markNonGamble(n); Ui.toast(n + " marked as not gambling"); } }));
            ng.add(chips(() -> c.nonGamblePlayers, n -> Zer0Bridge.unmarkNonGamble(n), "Nobody marked."));
            ng.buttons(btn("Clear list", Zer0Bridge::clearNonGamble).confirm().kind(Button.Kind.DANGER));

            boxSection(out, hub, "earnings", () -> c.earningsBoxEnabled, v -> c.earningsBoxEnabled = v, () -> c.earningsBoxScale, v -> c.earningsBoxScale = v,
                    () -> c.earningsBoxColor, v -> c.earningsBoxColor = v, 1f, 0xFFFFD700);
        }

        static void list(net.minecraft.client.gui.DrawContext g, float x, float y, float w, String title, List<java.util.Map.Entry<String, Double>> rows) {
            Fonts.draw(g, Fonts.Role.UI, title.toUpperCase(), x, y + 6, Theme.textMuted, 6.5f);
            if (rows.isEmpty()) { Fonts.draw(g, Fonts.Role.UI, "-", x, y + 22, Theme.textFaint, 7.5f); return; }
            float ry = y + 20;
            for (var e : rows) {
                Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, e.getKey(), 7.8f, w - 60), x, ry, Theme.text, 7.8f);
                Fonts.drawRight(g, Fonts.Role.UI, signed(e.getValue()), x + w, ry, moneyColor(e.getValue()), 7.8f);
                ry += 16;
            }
        }
    }

    // ================================================================== SESSIONS
    static final class SessionsPage extends Page {
        SessionsPage() { super("sessions", "STREAMER", "Sessions", "Record a stream from start to end", "S"); }
        private Zer0Bridge.SessionRow open;
        private static final String[] POS = {"Top mid", "Top left", "Top right", "Hotbar"};

        @Override protected void build(List<Section> out) {
            ModConfig c = Zer0Bridge.cfg();
            Section live = section(out, "Live session");
            live.add(new Row.Custom(48, (g, x, y, w, h, mx, my) -> {
                Zer0Bridge.SessionRow r = Zer0Bridge.runningSession();
                Render.circle(g, x + 4, y + 13, 3.5f, r != null ? Theme.bad : Theme.textFaint);
                Fonts.draw(g, Fonts.Role.UI, r != null ? "Recording · " + dur(r.durSec()) : "Not recording", x + 13, y + 9, Theme.text, 8.5f);
                if (r != null) {
                    Fonts.draw(g, Fonts.Role.HEADING, signed(r.net()), x, y + 25, moneyColor(r.net()), 13);
                    Fonts.drawRight(g, Fonts.Role.UI, r.payments() + " payments  ·  " + r.events() + " events", x + w, y + 29, Theme.textMuted, 7.5f);
                }
            }));
            live.buttons(btn(() -> Zer0Bridge.sessionRunning() ? "End session" : "Start session", () -> { Ui.toast(Zer0Bridge.toggleSession()); rebuild(); }).selected(() -> true));
            live.setting("Live box position", "Where the recording badge sits on the HUD.",
                    new Segmented(List.of(new Dropdown.Opt("0", "Top mid"), new Dropdown.Opt("1", "Left"), new Dropdown.Opt("2", "Right"), new Dropdown.Opt("3", "Hotbar")),
                            () -> String.valueOf(c.liveBoxPos), v -> { c.liveBoxPos = Integer.parseInt(v); Zer0Bridge.save(); }).size(210, 20));
            live.setting("Live box colour", "", cfgColor(() -> c.liveBoxColor, v -> c.liveBoxColor = v));

            if (open != null) detail(out);

            Section list = section(out, "History");
            List<Zer0Bridge.SessionRow> all = Zer0Bridge.sessions();
            if (all.isEmpty()) list.note("No sessions yet.");
            SimpleDateFormat fmt = new SimpleDateFormat("MMM d, HH:mm");
            for (Zer0Bridge.SessionRow r : all) {
                list.add(new Row.Custom(30, (g, x, y, w, h, mx, my) -> {
                    boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
                    boolean sel = open != null && open.handle() == r.handle();
                    if (hov || sel) Render.rect(g, x - 6, y + 2, w + 12, h - 4, 8, sel ? Theme.accentSoft : Theme.cardHover);
                    Fonts.draw(g, Fonts.Role.UI, fmt.format(new Date(r.startMs())) + (r.running() ? "  ·  live" : ""), x, y + 7, Theme.text, 8f);
                    Fonts.draw(g, Fonts.Role.UI, dur(r.durSec()) + "  ·  " + r.payments() + " payments", x, y + 18, Theme.textMuted, 7f);
                    Fonts.drawRight(g, Fonts.Role.UI, signed(r.net()), x + w, y + 11, moneyColor(r.net()), 8.5f);
                }).click((x, y, w, h, mx, my, b) -> { open = (open != null && open.handle() == r.handle()) ? null : r; Sounds.click(); rebuild(); return true; }));
            }
        }

        private void detail(List<Section> out) {
            Zer0Bridge.SessionRow r = open;
            Section d = section(out, "Session · " + r.date());
            d.add(new Row.Custom(110, (g, x, y, w, h, mx, my) -> {
                Fonts.draw(g, Fonts.Role.HEADING, signed(r.net()), x, y + 6, moneyColor(r.net()), 15);
                Fonts.drawRight(g, Fonts.Role.UI, dur(r.durSec()) + (r.hasBal() ? "  ·  " + Zer0Bridge.fmtCompact(r.startBal()) + " → " + Zer0Bridge.fmtCompact(r.endBal()) : ""), x + w, y + 10, Theme.textMuted, 7.5f);
                double[][] pts = Zer0Bridge.sessionPoints(r);
                if (pts != null && pts.length >= 2) {
                    double[] v = new double[pts.length];
                    for (int i = 0; i < pts.length; i++) v[i] = pts[i].length > 1 ? pts[i][1] : pts[i][0];
                    Render.areaChart(g, x, y + 30, w, h - 34, v, moneyColor(r.net()), true);
                } else Fonts.draw(g, Fonts.Role.UI, "No balance graph (start Starting Bal tracking during a session to record one).", x, y + 50, Theme.textFaint, 7.2f);
            }));
            d.add(new Row.Custom(w -> 5 * 16 + 24, (g, x, y, w, h, mx, my) -> {
                float half = (w - 12) / 2;
                Earnings.list(g, x, y, half, "Top gains", Zer0Bridge.sessionTopGains(r, 5));
                Earnings.list(g, x + half + 12, y, half, "Top losses", Zer0Bridge.sessionTopLosses(r, 5));
            }));
            d.add(new Row.Custom(w -> Math.min(10, Math.max(1, Zer0Bridge.sessionPayments(r).size())) * 15 + 26, (g, x, y, w, h, mx, my) -> {
                Fonts.draw(g, Fonts.Role.UI, "SESSION PAYMENTS", x, y + 6, Theme.textMuted, 6.5f);
                var ps = Zer0Bridge.sessionPayments(r);
                if (ps.isEmpty()) { Fonts.draw(g, Fonts.Role.UI, "No payments in this session.", x, y + 20, Theme.textFaint, 7.5f); return; }
                SimpleDateFormat f = new SimpleDateFormat("HH:mm");
                float ry = y + 20;
                for (int i = 0; i < Math.min(10, ps.size()); i++) {
                    var p = ps.get(i);
                    Fonts.draw(g, Fonts.Role.UI, f.format(new Date(p.time())) + "   " + p.player(), x, ry, Theme.text, 7.6f);
                    Fonts.drawRight(g, Fonts.Role.UI, signed(p.amount()), x + w, ry, moneyColor(p.amount()), 7.6f);
                    ry += 15;
                }
            }));
            d.buttons(btn("Close", () -> { open = null; rebuild(); }), btn("Delete session", () -> {
                if (Zer0Bridge.deleteSession(r)) Ui.toast("Session deleted"); else Ui.toast("Cannot delete a running session", true);
                open = null; rebuild();
            }).confirm().kind(Button.Kind.DANGER));
        }
    }

    // ================================================================== PAYMENTS
    static final class Payments extends Page {
        Payments() { super("payments", "STREAMER", "Payments", "Every payment you sent or received", "P"); }
        private final TextField search = new TextField("Search name").icon("search").max(16);
        private float scroll;
        @Override protected void build(List<Section> out) {
            Section s = section(out, "");
            s.wide("", "", search);
            search.onChange = q -> scroll = 0;
            s.add(new Row.Custom(36, (g, x, y, w, h, mx, my) -> {
                double got = 0, paid = 0;
                for (var p : rows()) { if (p.amount() >= 0) got += p.amount(); else paid -= p.amount(); }
                float third = w / 3;
                Fonts.draw(g, Fonts.Role.UI, "GOT", x, y + 5, Theme.textMuted, 6.3f);
                Fonts.draw(g, Fonts.Role.HEADING, "+$" + Zer0Bridge.fmtCompact(got), x, y + 16, Theme.good, 11);
                Fonts.draw(g, Fonts.Role.UI, "PAID", x + third, y + 5, Theme.textMuted, 6.3f);
                Fonts.draw(g, Fonts.Role.HEADING, "-$" + Zer0Bridge.fmtCompact(paid), x + third, y + 16, Theme.bad, 11);
                Fonts.draw(g, Fonts.Role.UI, "TOTAL", x + third * 2, y + 5, Theme.textMuted, 6.3f);
                Fonts.draw(g, Fonts.Role.HEADING, signed(got - paid), x + third * 2, y + 16, moneyColor(got - paid), 11);
            }));
            final float RH = 17;
            s.add(new Row.Custom(w -> 15 * RH + 8, (g, x, y, w, h, mx, my) -> {
                var list = rows();
                if (list.isEmpty()) { Fonts.drawCentered(g, Fonts.Role.UI, search.text.isBlank() ? "No payments logged yet (only payments from now on are listed)." : "No payments with that name.", x + w / 2, y + 20, Theme.textFaint, 7.5f); return; }
                float max = Math.max(0, list.size() * RH - (h - 8));
                scroll = Math.max(0, Math.min(scroll, max));
                SimpleDateFormat f = new SimpleDateFormat("MM-dd HH:mm");
                g.enableScissor((int) x - 4, (int) y + 2, (int) (x + w + 4), (int) (y + h - 2));
                int first = (int) (scroll / RH);
                for (int i = first; i < list.size(); i++) {
                    float ry = y + 4 + i * RH - scroll;
                    if (ry > y + h) break;
                    var p = list.get(i);
                    if (i % 2 == 0) Render.rect(g, x - 4, ry, w + 8, RH, 6, Theme.dark ? 0x08FFFFFF : 0x3CFFFFFF);
                    Fonts.draw(g, Fonts.Role.UI, f.format(new Date(p.time())), x + 2, ry + 4.5f, Theme.textFaint, 7.2f);
                    Fonts.draw(g, Fonts.Role.UI, (p.amount() >= 0 ? "got from " : "paid ") + p.player(), x + 66, ry + 4.5f, Theme.text, 7.6f);
                    Fonts.drawRight(g, Fonts.Role.UI, signed(p.amount()), x + w - 2, ry + 4.5f, moneyColor(p.amount()), 7.6f);
                }
                g.disableScissor();
            }).onScroll((mx, my, a) -> { scroll -= (float) a * RH * 2; return true; }));
        }
        private List<Zer0Bridge.PaymentRow> cache; private int rev = -1; private String q = "";
        private List<Zer0Bridge.PaymentRow> rows() {
            String nq = search.text.trim().toLowerCase();
            if (cache == null || rev != Zer0Bridge.paymentRevision() || !nq.equals(q)) {
                rev = Zer0Bridge.paymentRevision(); q = nq;
                List<Zer0Bridge.PaymentRow> out = new ArrayList<>();
                for (var p : Zer0Bridge.allPayments()) if (nq.isEmpty() || p.player().toLowerCase().contains(nq)) out.add(p);
                cache = out;
            }
            return cache;
        }
    }
}
