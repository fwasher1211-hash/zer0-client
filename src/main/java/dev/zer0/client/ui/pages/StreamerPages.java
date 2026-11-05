package dev.zer0.client.ui.pages;

import com.example.donutqueue.ModConfig;
import com.example.donutqueue.Zer0Bridge;
import dev.zer0.client.compat.Mc;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.page.*;
import dev.zer0.client.ui.widgets.*;
import net.minecraft.text.Text;

import java.util.List;

import static dev.zer0.client.ui.pages.Ctl.*;

/** Queue (anti-gamble), Starting Bal, Bidding, Info Display, Chat, Ranks and Addons - all bound to the original mod. */
final class StreamerPages {
    private StreamerPages() {}

    static ModConfig c() { return Zer0Bridge.cfg(); }

    // ================================================================== QUEUE
    static final class Queue extends Page {
        Queue() { super("queue", "STREAMER", "Queue", "Anti-gamble payment queue: who paid and who's next", "Q"); }
        private final TextField removeField = new TextField("Player name");

        @Override protected void build(List<Section> out) {
            ModConfig c = c();
            Section now = section(out, "In the queue");
            now.add(new Row.Custom(w -> Math.max(1, Math.min(8, Zer0Bridge.queueSize())) * 24 + 8, (g, x, y, w, h, mx, my) -> {
                var q = Zer0Bridge.queue();
                if (q.isEmpty()) { Fonts.draw(g, Fonts.Role.UI, "Nobody is waiting. Payments that match your chat patterns are added automatically.", x, y + 11, Theme.textFaint, 7.5f); return; }
                float ry = y + 4;
                for (int i = 0; i < Math.min(8, q.size()); i++) {
                    var e = q.get(i);
                    if (i == 0) Render.rect(g, x - 4, ry, w + 8, 22, 8, Theme.accentSoft);
                    Fonts.draw(g, Fonts.Role.UI, String.valueOf(i + 1), x + 2, ry + 7, Theme.textFaint, 7.5f);
                    Zer0Bridge.drawHead(g, e.player(), Math.round(x + 14), Math.round(ry + 4), 14);
                    Fonts.draw(g, Fonts.Role.UI, e.player(), x + 34, ry + 7, Theme.text, 8.2f);
                    Fonts.draw(g, Fonts.Role.UI, ago(e.time()), x + 34 + Fonts.width(Fonts.Role.UI, e.player(), 8.2f) + 8, ry + 7.6f, Theme.textFaint, 7f);
                    boolean xh = mx >= x + w - 14 && mx < x + w + 2 && my >= ry && my < ry + 22;
                    Fonts.drawRight(g, Fonts.Role.UI, "$" + e.amount(), x + w - 22, ry + 7, Theme.good, 8.2f);
                    Render.circle(g, x + w - 7, ry + 11, 6, xh ? Col.withAlpha(Theme.bad, 200) : 0x16FFFFFF);
                    cross(g, x + w - 7, ry + 11, 2.2f, xh ? 0xFFFFFFFF : Theme.textMuted);
                    ry += 24;
                }
                if (q.size() > 8) Fonts.drawRight(g, Fonts.Role.UI, "+" + (q.size() - 8) + " more", x + w, y + h - 8, Theme.textFaint, 7f);
            }).click((x, y, w, h, mx, my, b) -> {
                var q = Zer0Bridge.queue();
                int i = (int) ((my - y - 4) / 24);
                if (i >= 0 && i < Math.min(8, q.size()) && mx >= x + w - 14) { Zer0Bridge.removeFromQueue(q.get(i).player()); Ui.toast("Removed " + q.get(i).player()); return true; }
                return false;
            }));
            now.buttons(
                    btn("Skip to next", () -> { Zer0Bridge.advanceQueue(); Ui.toast("Skipped to the next payment"); }).selected(() -> true),
                    btn("Clear queue", () -> { Zer0Bridge.clearQueue(); Ui.toast("Queue cleared"); }).confirm().kind(Button.Kind.DANGER));
            now.add(fieldWithButton("Remove a player", removeField, "Remove", n -> {
                if (n.isEmpty()) return;
                boolean ok = Zer0Bridge.removeFromQueue(n);
                Ui.toast(ok ? "Removed " + n : n + " is not in the queue", !ok);
            }));
            now.footer("Skip key: " + Zer0Bridge.keyLabel("SKIP") + "  ·  Lever pause key: " + Zer0Bridge.keyLabel("LEVER") + "  ·  /dq next  ·  /dq clear");

            Section hist = section(out, "Recently served");
            hist.add(new Row.Custom(w -> Math.max(1, Math.min(6, Zer0Bridge.history(6).size())) * 18 + 8, (g, x, y, w, h, mx, my) -> {
                var hs = Zer0Bridge.history(6);
                if (hs.isEmpty()) { Fonts.draw(g, Fonts.Role.UI, "Last bet: none yet", x, y + 8, Theme.textFaint, 7.5f); return; }
                float ry = y + 4;
                for (var e : hs) {
                    Fonts.draw(g, Fonts.Role.UI, e.player(), x, ry + 3, Theme.text, 8f);
                    Fonts.drawRight(g, Fonts.Role.UI, "$" + e.amount() + "   " + ago(e.time()), x + w, ry + 3, Theme.textMuted, 7.5f);
                    ry += 18;
                }
            }));

            Section rules = section(out, "Rules");
            rules.setting("Minimum payment", "Smaller payments are ignored. 50k, 1.5m, 0 = none. Enter to save.",
                    cfgAmount("none", () -> c.minimumAmount, v -> c.minimumAmount = v).size(110, 20));
            rules.setting("Lever flicks to advance", "How many lever flicks move the queue on (a note block always needs 1).",
                    cfgInt(1, 6, () -> c.flicksRequired, v -> c.flicksRequired = v), () -> { c.flicksRequired = 2; Zer0Bridge.save(); }, () -> c.flicksRequired != 2);
            rules.setting("Lever skip paused", "While paused, flicking the lever does nothing.", cfgToggle(() -> c.leverPaused, v -> c.leverPaused = v));
            rules.setting("Only one lever", "Only the lever at the saved position counts.", cfgToggle(() -> c.restrictLever, v -> c.restrictLever = v));
            rules.note(() -> c.restrictLever ? "Saved lever: " + c.leverX + ", " + c.leverY + ", " + c.leverZ : "");
            rules.setting("Chat messages", "Confirmation messages in chat for queue actions.", cfgToggle(() -> c.queueChatMessagesEnabled, v -> c.queueChatMessagesEnabled = v));

            Section bl = section(out, "Blacklist");
            TextField add = new TextField("Player name").max(16);
            bl.add(fieldWithButton("Never queue", add, "Add", n -> { if (!n.isEmpty()) { Zer0Bridge.blacklistAdd(n); Ui.toast(n + " blacklisted"); } }));
            bl.add(chips(() -> c.blacklist, n -> { c.blacklist.remove(n); Zer0Bridge.save(); }, "Nobody is blacklisted yet."));

            boxSection(out, hub, "queue", () -> c.hudEnabled, v -> c.hudEnabled = v, () -> c.hudScale, v -> c.hudScale = v,
                    () -> c.queueBoxColor, v -> c.queueBoxColor = v, 1f, 0xFF3399FF);
        }
    }

    // ================================================================== STARTING BAL
    static final class StartBal extends Page {
        StartBal() { super("startbal", "STREAMER", "Starting Bal", "Track earnings from a /bal reading towards a goal", "B"); }
        @Override protected void build(List<Section> out) {
            ModConfig c = c();
            Section s = section(out, "Tracking");
            s.add(new Row.Custom(70, (g, x, y, w, h, mx, my) -> {
                float t = c.goalAmount > 0 ? (float) Math.max(0, Math.min(1, c.earnedTotal / c.goalAmount)) : 0;
                Fonts.draw(g, Fonts.Role.HEADING, signed(c.earnedTotal), x, y + 8, moneyColor(c.earnedTotal), 16);
                Fonts.draw(g, Fonts.Role.UI, Zer0Bridge.trackingStatus(), x, y + 32, Theme.textMuted, 7.5f);
                Render.bar(g, x, y + 50, w, 7, t, Theme.dark ? 0x22FFFFFF : 0x1A14141B, t >= 1 ? Theme.good : Theme.accent);
                if (c.goalAmount > 0) Fonts.drawRight(g, Fonts.Role.UI, "goal " + Zer0Bridge.fmtCompact(c.goalAmount) + "  " + Math.round(t * 100) + "%", x + w, y + 12, Theme.textMuted, 7.5f);
            }));
            s.buttons(
                    btn(() -> c.trackingEnabled || c.awaitingBalCapture ? "Restart tracking" : "Start tracking", () -> Ui.toast(Zer0Bridge.startTracking())).selected(() -> true),
                    btn("Reset", () -> Ui.toast(Zer0Bridge.resetTracking())).confirm().kind(Button.Kind.DANGER));
            s.footer("Start tracking, then type /bal in chat - earnings count from that balance.");
            Section goal = section(out, "Goal");
            goal.setting("Goal amount", "e.g. 500m or 2b. Enter to save, empty to clear.", cfgAmount("no goal", () -> c.goalAmount, v -> { c.goalAmount = v; c.goalReachedNotified = false; }).size(110, 20));
            goal.setting("Chat messages", "Messages for tracking and goals.", cfgToggle(() -> c.startBalChatMessagesEnabled, v -> c.startBalChatMessagesEnabled = v));
            boxSection(out, hub, "startbal", () -> c.startBalBoxEnabled, v -> c.startBalBoxEnabled = v, () -> c.startBalBoxScale, v -> c.startBalBoxScale = v,
                    () -> c.startBalBoxColor, v -> c.startBalBoxColor = v, 1f, 0xFF33CC66);
        }
    }

    // ================================================================== BIDDING
    static final class Bidding extends Page {
        Bidding() { super("bidding", "STREAMER", "Bidding", "Live bidding rounds paid in chat", "D"); }
        @Override protected void build(List<Section> out) {
            ModConfig c = c();
            Section s = section(out, "Round");
            s.add(new Row.Custom(58, (g, x, y, w, h, mx, my) -> {
                long left = Math.max(0, (c.biddingEndTimeMillis - System.currentTimeMillis()) / 1000);
                String state = c.biddingActive ? "Live · " + Zer0Bridge.formatDuration(left) + " left" : c.biddingFinished ? "Finished" : "No round running";
                Render.circle(g, x + 4, y + 13, 3.5f, c.biddingActive ? Theme.good : c.biddingFinished ? Theme.warn : Theme.textFaint);
                Fonts.draw(g, Fonts.Role.UI, state, x + 13, y + 9, Theme.text, 8.5f);
                String item = c.biddingItemId != null && !c.biddingItemId.isEmpty() ? c.biddingItemCount + "× " + Zer0Bridge.itemName(c.biddingItemId) : "No item";
                Fonts.drawRight(g, Fonts.Role.UI, item, x + w, y + 9, Theme.textMuted, 7.5f);
                boolean lead = c.biddingLeaderName != null && !c.biddingLeaderName.isEmpty();
                if (lead) Zer0Bridge.drawHead(g, c.biddingLeaderName, Math.round(x), Math.round(y + 28), 18);
                Fonts.draw(g, Fonts.Role.HEADING, lead ? c.biddingLeaderName + "  " + Zer0Bridge.fmt(c.biddingLeaderAmount) : "Waiting for bids", x + (lead ? 24 : 0), y + 31, lead ? Theme.text : Theme.textFaint, 11);
                if (c.biddingActive && c.biddingTotalDurationMillis > 0) {
                    float t = 1f - (float) (c.biddingEndTimeMillis - System.currentTimeMillis()) / c.biddingTotalDurationMillis;
                    Render.bar(g, x, y + h - 5, w, 3, t, 0x18FFFFFF, Theme.accent);
                }
            }));
            s.buttons(
                    btn(() -> c.biddingActive ? "Stop bidding" : "Start 60s round", () -> Ui.toast(c.biddingActive ? Zer0Bridge.stopBidding() : Zer0Bridge.startPlainBidding())).selected(() -> !c.biddingActive),
                    btn("Auction an item →", () -> hub.open("auctions")));
            s.footer("Bidding key: " + Zer0Bridge.keyLabel("BID") + "  ·  /bidding <number> starts a preset");
            Section r = section(out, "Rules");
            r.setting("Minimum bid", "Bids under this are ignored. 50k, 1m, 0 = none. Enter to save.", cfgAmount("none", () -> c.biddingMinimum, v -> c.biddingMinimum = v).size(110, 20));
            r.setting("Chat messages", "Messages when bids arrive and rounds end.", cfgToggle(() -> c.biddingChatMessagesEnabled, v -> c.biddingChatMessagesEnabled = v));
            Section bl = section(out, "Ignored bidders");
            TextField f = new TextField("Player name").max(16);
            bl.add(fieldWithButton("Ignore", f, "Add", n -> {
                if (n.isEmpty()) return;
                if (c.biddingBlacklist.stream().noneMatch(x -> x.equalsIgnoreCase(n))) c.biddingBlacklist.add(n);
                Zer0Bridge.save(); Ui.toast(n + " ignored for bidding");
            }));
            bl.add(chips(() -> c.biddingBlacklist, n -> { c.biddingBlacklist.remove(n); Zer0Bridge.save(); }, "Nobody is ignored."));
            boxSection(out, hub, "bidding", () -> c.biddingBoxEnabled, v -> c.biddingBoxEnabled = v, () -> c.biddingBoxScale, v -> c.biddingBoxScale = v,
                    () -> c.biddingBoxColor, v -> c.biddingBoxColor = v, 1f, 0xFFFFAA33);
        }
    }

    // ================================================================== INFO DISPLAY
    static final class Info extends Page {
        Info() { super("info", "STREAMER", "Info Display", "An on-screen card with your IGN, limits and game", "I"); }
        @Override protected void build(List<Section> out) {
            ModConfig c = c();
            Section s = section(out, "Content");
            s.setting("IGN", "", cfgText("your name", 25, () -> c.infoIgn, v -> c.infoIgn = v).size(150, 20));
            s.setting("Minimum", "", cfgText("e.g. 10k", 25, () -> c.infoMin, v -> c.infoMin = v).size(150, 20));
            s.setting("Maximum", "", cfgText("e.g. 5m", 25, () -> c.infoMax, v -> c.infoMax = v).size(150, 20));
            s.setting("Game", "", cfgText("e.g. Coinflip", 25, () -> c.infoGame, v -> c.infoGame = v).size(150, 20));
            s.setting("Custom text", "Up to 25 characters.", cfgText("anything", 25, () -> c.infoCustomText, v -> c.infoCustomText = v).size(150, 20));
            s.add(new Row.Custom(80, (g, x, y, w, h, mx, my) -> {   // live preview
                float bw = Math.min(200, w), bx = x + (w - bw) / 2f, by = y + 6;
                Render.glass(g, bx, by, bw, 68, 10, 0xA0141418, 0x30FFFFFF, true);
                String[][] rows = {{"IGN", c.infoIgn}, {"Min", c.infoMin}, {"Max", c.infoMax}, {"Game", c.infoGame}};
                float ry = by + 7;
                for (String[] r : rows) {
                    Fonts.draw(g, Fonts.Role.HUD, r[0], bx + 10, ry, 0xFFAAAAAA, 7.5f);
                    Fonts.drawRight(g, Fonts.Role.HUD, r[1] == null || r[1].isBlank() ? "-" : r[1], bx + bw - 10, ry, 0xFF000000 | c.infoBoxColor, 7.5f);
                    ry += 11;
                }
                if (c.infoCustomText != null && !c.infoCustomText.isBlank()) Fonts.drawCentered(g, Fonts.Role.HUD, c.infoCustomText, bx + bw / 2, ry + 1, 0xFF000000 | c.infoBoxColor, 7.5f);
            }).titled("Preview", ""));
            boxSection(out, hub, "info", () -> c.infoBoxEnabled, v -> c.infoBoxEnabled = v, () -> c.infoBoxScale, v -> c.infoBoxScale = v,
                    () -> c.infoBoxColor, v -> c.infoBoxColor = v, 1f, 0xFFFF66FF);
        }
    }

    // ================================================================== CHAT
    static final class Chat extends Page {
        Chat() { super("chat", "STREAMER", "Chat", "Payment colours and cleaner whispers (only colours change)", "C"); }
        @Override protected void build(List<Section> out) {
            ModConfig c = c(), d = Zer0Bridge.defaults();
            Section s = section(out, "Styling");
            s.setting("Colour payment messages", "Name in your name colour, amount green (received) or red (sent). The text itself never changes.",
                    cfgToggle(() -> c.chatPaymentsEnabled, v -> c.chatPaymentsEnabled = v));
            s.setting("Restyle whispers", "You → Name and Name → You, bold white message.", cfgToggle(() -> c.chatWhispersEnabled, v -> c.chatWhispersEnabled = v));
            Section col = section(out, "Colours");
            col.setting("Player name", "", cfgColor(() -> c.chatNameColor, v -> c.chatNameColor = v), () -> { c.chatNameColor = d.chatNameColor; Zer0Bridge.save(); }, () -> c.chatNameColor != d.chatNameColor);
            col.setting("They paid you", "", cfgColor(() -> c.chatReceivedColor, v -> c.chatReceivedColor = v), () -> { c.chatReceivedColor = d.chatReceivedColor; Zer0Bridge.save(); }, () -> c.chatReceivedColor != d.chatReceivedColor);
            col.setting("You paid them", "", cfgColor(() -> c.chatSentColor, v -> c.chatSentColor = v), () -> { c.chatSentColor = d.chatSentColor; Zer0Bridge.save(); }, () -> c.chatSentColor != d.chatSentColor);
            Section pv = section(out, "Preview");
            pv.add(new Row.Custom(64, (g, x, y, w, h, mx, my) -> {
                Render.rect(g, x - 4, y + 4, w + 8, h - 8, 8, 0x80000000);
                Text a = Text.literal("Forsurewaterr paid you $50M."), b = Text.literal("You paid Forsurewaterr $100M.");
                if (c.chatPaymentsEnabled) { a = Zer0Bridge.previewPaid(a); b = Zer0Bridge.previewPaid(b); }
                Text wo = c.chatWhispersEnabled ? Zer0Bridge.previewWhisperOut("Forsurewaterr", "hello") : Text.literal("You whisper to Forsurewaterr: hello");
                g.drawText(Mc.tr(), a, Math.round(x + 4), Math.round(y + 11), 0xFFFFFFFF, true);
                g.drawText(Mc.tr(), b, Math.round(x + 4), Math.round(y + 25), 0xFFFFFFFF, true);
                g.drawText(Mc.tr(), wo, Math.round(x + 4), Math.round(y + 39), 0xFFFFFFFF, true);
            }));
            pv.footer("Queue, earnings, bidding and whisper detection always read the server's original message.");
        }
    }

    // ================================================================== RANKS
    static final class Ranks extends Page {
        Ranks() { super("ranks", "STREAMER", "Ranks", "Rank colours in the tab list and on name tags", "R"); }
        @Override protected void build(List<Section> out) {
            ModConfig c = c(), d = Zer0Bridge.defaults();
            Section s = section(out, "Rank colours");
            s.setting("Recolour names by rank", "Applies to the tab list and name tags.", cfgToggle(() -> c.rankColorsEnabled, v -> c.rankColorsEnabled = v));
            s.setting("Staff team colour", "", cfgColor(() -> c.staffColor, v -> c.staffColor = v), () -> { c.staffColor = d.staffColor; Zer0Bridge.save(); }, () -> c.staffColor != d.staffColor);
        }
    }

    // ================================================================== ADDONS
    static final class Addons extends Page {
        Addons() { super("addons", "STREAMER", "Addons", "Screens added by other mods", "+"); }
        @Override public boolean visible() { return super.visible() && !Zer0Bridge.addons().isEmpty(); }
        @Override protected void build(List<Section> out) {
            Section s = section(out, "Installed addons");
            for (var a : Zer0Bridge.addons())
                s.setting(a.label(), "Opens the addon's own screen.", btn("Open", () -> {
                    var scr = Zer0Bridge.openAddon(a, Mc.mc().currentScreen);
                    if (scr != null) Mc.mc().setScreen(scr); else Ui.toast("That addon could not be opened.", true);
                }));
        }
    }
}
